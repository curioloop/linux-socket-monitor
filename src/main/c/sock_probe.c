/**
 * Copyright © 2024 CurioLoop (curioloops@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <alloca.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <dirent.h>
#include <pwd.h>
#include <asm/types.h>
#include <sys/socket.h>
#include <sys/errno.h>
#include <linux/netlink.h>
#include <linux/rtnetlink.h>
#include <linux/tcp.h>
#include <linux/sock_diag.h>
#include <linux/inet_diag.h>

#include "sock_probe.h"

static int pid_ent_add(struct pid_ent **hash_tab, unsigned int ino, int pid) {
    struct pid_ent *p = malloc(sizeof(struct pid_ent));
    if (!p) return EXIT_FAILURE;
    p->next = NULL;
    p->ino = ino;
    p->pid = pid;
    struct pid_ent **pp = &hash_tab[PID_ENT_HASH(ino)];
    p->next = *pp;
    *pp = p;
    return EXIT_SUCCESS;
}

static int pid_ent_hash_build(struct pid_ent **hash_tab) {

    memset(hash_tab, 0, sizeof(struct pid_ent*) * PID_ENT_HASH_SIZE);

    // Find the location of the /proc
    const char *root = getenv("PROC_ROOT") ? : "/proc/";
    char name[1024];
    strcpy(name, root);
    if (strlen(name) == 0 || name[strlen(name)-1] != '/')
        strcat(name, "/");
    int nameoff = strlen(name);

    int code = EXIT_SUCCESS;
    DIR *dir = opendir(name);
    if (!dir) return code;

    // Traverse the subdirectories under /proc
    struct dirent *d;
    while ((d = readdir(dir)) != NULL) {
        int pid;
        char crap;
        if (sscanf(d->d_name, "%d%c", &pid, &crap) != 1)
            continue;

        sprintf(name + nameoff, "%d/fd/", pid);
        int pos = strlen(name);

        DIR *dir1;
        if ((dir1 = opendir(name)) == NULL)
            continue;

        // Traverse all file descriptors in the /proc/{pid}/fd
        struct dirent *d1;
        while ((d1 = readdir(dir1)) != NULL) {
            int fd;
            if (sscanf(d1->d_name, "%d%c", &fd, &crap) != 1) continue;
            sprintf(name+pos, "%d", fd);

            // Find the file linked to fd
            char lnk[64];
            ssize_t link_len = readlink(name, lnk, sizeof(lnk)-1);
            if (link_len == -1) continue;
            lnk[link_len] = '\0';

            // Ignore file which path not starts with 'socket:['
            const char *pattern = "socket:[";
            if (strncmp(lnk, pattern, strlen(pattern))) continue;

            // Read the ino of socket
            unsigned int ino;
            sscanf(lnk, "socket:[%u]", &ino);

            code = pid_ent_add(hash_tab, ino, pid);
            if (code) break;
        }
        closedir(dir1);
        if (code) break;
    }
    closedir(dir);
    return code;
}

static void pid_ent_hash_free(struct pid_ent **hash_tab) {
    int i=0;
    while (i<PID_ENT_HASH_SIZE) {
        struct pid_ent *p = hash_tab[i];
        while (p) {
            struct pid_ent *t = p->next;
            free(p);
            p = t;
        }
        i++;
    }
}

static struct pid_ent* find_pid_ent(struct pid_ent **hash_tab, unsigned ino){
    if (!ino) return NULL;
    struct pid_ent *p = hash_tab[PID_ENT_HASH(ino)];
    while (p && p->ino != ino) p = p->next;
    return p;
}

//Copied from iproute2/ss source
static void filter_patch(char *a, int len, int reloc)
{
    while (len > 0) {
        struct inet_diag_bc_op *op = (struct inet_diag_bc_op*)a;
        if (op->no == len+4)
            op->no += reloc;
        len -= op->yes;
        a += op->yes;
    }
    if (len < 0)
        abort();
}

//Copied from iproute2/ss source
static int filter_compile(struct port_filter *f, char **bytecode)
{

    struct port_filter *eq_filter = NULL;
    if (DST_PORT_EQ == f->type || SRC_PORT_EQ == f->type) {
        eq_filter = alloca(3 * sizeof(struct port_filter));
    }

    switch (f->type) {
        case DST_PORT_GE:
        {
            if (!(*bytecode=malloc(8))) abort();
            ((struct inet_diag_bc_op*)*bytecode)[0] = (struct inet_diag_bc_op){ INET_DIAG_BC_D_GE, 8, 12 };
            ((struct inet_diag_bc_op*)*bytecode)[1] = (struct inet_diag_bc_op){ 0, 0, f->port };
            return 8;
        }
        case DST_PORT_LE:
        {
            if (!(*bytecode=malloc(8))) abort();
            ((struct inet_diag_bc_op*)*bytecode)[0] = (struct inet_diag_bc_op){ INET_DIAG_BC_D_LE, 8, 12 };
            ((struct inet_diag_bc_op*)*bytecode)[1] = (struct inet_diag_bc_op){ 0, 0, f->port };
            return 8;
        }
        case SRC_PORT_GE:
        {
            if (!(*bytecode=malloc(8))) abort();
            ((struct inet_diag_bc_op*)*bytecode)[0] = (struct inet_diag_bc_op){ INET_DIAG_BC_S_GE, 8, 12 };
            ((struct inet_diag_bc_op*)*bytecode)[1] = (struct inet_diag_bc_op){ 0, 0, f->port };
            return 8;
        }
        case SRC_PORT_LE:
        {
            if (!(*bytecode=malloc(8))) abort();
            ((struct inet_diag_bc_op*)*bytecode)[0] = (struct inet_diag_bc_op){ INET_DIAG_BC_S_LE, 8, 12 };
            ((struct inet_diag_bc_op*)*bytecode)[1] = (struct inet_diag_bc_op){ 0, 0, f->port };
            return 8;
        }
        case DST_PORT_EQ:
        {
            eq_filter[0].type = COND_AND;
            eq_filter[0].pred = eq_filter+1;
            eq_filter[0].post = eq_filter+2;
            eq_filter[1].type = DST_PORT_GE;
            eq_filter[1].port = f->port;
            eq_filter[2].type = DST_PORT_LE;
            eq_filter[2].port = f->port;
            f = eq_filter;
            goto and;
        }
        case SRC_PORT_EQ:
        {
            eq_filter[0].type = COND_AND;
            eq_filter[0].pred = eq_filter+1;
            eq_filter[0].post = eq_filter+2;
            eq_filter[1].type = SRC_PORT_GE;
            eq_filter[1].port = f->port;
            eq_filter[2].type = SRC_PORT_LE;
            eq_filter[2].port = f->port;
            f = eq_filter;
            goto and;
        }
        and:
        case COND_AND:
        {
            char *a1, *a2, *a, l1, l2;
            l1 = filter_compile(f->pred, &a1);
            l2 = filter_compile(f->post, &a2);
            if (!(a = malloc(l1+l2))) abort();
            memcpy(a, a1, l1);
            memcpy(a+l1, a2, l2);
            free(a1); free(a2);
            filter_patch(a, l1, l2);
            *bytecode = a;
            return l1+l2;
        }
        case COND_OR:
        {
            char *a1, *a2, *a, l1, l2;
            l1 = filter_compile(f->pred, &a1);
            l2 = filter_compile(f->post, &a2);
            if (!(a = malloc(l1+l2+4))) abort();
            memcpy(a, a1, l1);
            memcpy(a+l1+4, a2, l2);
            free(a1); free(a2);
            *(struct inet_diag_bc_op*)(a+l1) = (struct inet_diag_bc_op){ INET_DIAG_BC_JMP, 4, l2+4 };
            *bytecode = a;
            return l1+l2+4;
        }
        case COND_NOT:
        {
            char *a1, *a, l1;
            l1 = filter_compile(f->pred, &a1);
            if (!(a = malloc(l1+4))) abort();
            memcpy(a, a1, l1);
            free(a1);
            *(struct inet_diag_bc_op*)(a+l1) = (struct inet_diag_bc_op){ INET_DIAG_BC_JMP, 4, 8 };
            *bytecode = a;
            return l1+4;
        }
        default:
            abort();
    }
}

int send_diag_msg(int sock_fd, int family, int protocol, struct port_filter *filter) {

    // UDP/TCP socket statistics request
    struct inet_diag_req_v2 sock_diag_req;
    memset(&sock_diag_req, 0, sizeof(sock_diag_req));

    sock_diag_req.sdiag_family = family;
    sock_diag_req.sdiag_protocol = protocol;
    sock_diag_req.idiag_states = TCPF_ALL & ~((1<<TCP_SYN_RECV) | (1<<TCP_TIME_WAIT) | (1<<TCP_CLOSE)); // Filter based on connection status

    // Collect the diagnostic information on demand
    sock_diag_req.idiag_ext |= (1<<(INET_DIAG_MEMINFO-1));
    sock_diag_req.idiag_ext |= (1<<(INET_DIAG_SKMEMINFO-1));
    sock_diag_req.idiag_ext |= (1<<(INET_DIAG_INFO-1));
    sock_diag_req.idiag_ext |= (1<<(INET_DIAG_VEGASINFO-1));
    sock_diag_req.idiag_ext |= (1<<(INET_DIAG_CONG-1));

    // Netlink header
    struct nlmsghdr nl_msg_header;
    memset(&nl_msg_header, 0, sizeof(nl_msg_header));

    nl_msg_header.nlmsg_type = SOCK_DIAG_BY_FAMILY; // Set the message type to sock_diag
    nl_msg_header.nlmsg_flags = NLM_F_DUMP | NLM_F_REQUEST; // Request batch socket information
    nl_msg_header.nlmsg_len = NLMSG_LENGTH(sizeof(sock_diag_req)); // Set message length

    struct iovec iov[4];
    iov[0].iov_base = (void*) &nl_msg_header;
    iov[0].iov_len = sizeof(nl_msg_header);
    iov[1].iov_base = (void*) &sock_diag_req;
    iov[1].iov_len = sizeof(sock_diag_req);

    // Perform pre-filtering on ports to reduce unnecessary messages
    char *bc = NULL;
    struct rtattr rta = { .rta_type = INET_DIAG_REQ_BYTECODE };
    if (filter) {
        int bc_len = filter_compile(filter, &bc);
        rta.rta_len = RTA_LENGTH(bc_len);
        iov[2] = (struct iovec){ &rta, sizeof(rta) };
        iov[3] = (struct iovec){ bc, bc_len };
        nl_msg_header.nlmsg_len += rta.rta_len;
    }

    // Specify the request destination
    struct sockaddr_nl sock_addr;
    memset(&sock_addr, 0, sizeof(sock_addr));
    sock_addr.nl_family = AF_NETLINK;
    sock_addr.nl_pid = 0; // A PID of 0 means communicating to kernel
    // This message is a unicast message, no need to specify a group

    // Build message body
    struct msghdr msg = (struct msghdr) {
            .msg_name = (void*)&sock_addr,
            .msg_namelen = sizeof(sock_addr),
            .msg_iov = iov,
            .msg_iovlen = filter ? 4 : 2,
    };

    int code = sendmsg(sock_fd, &msg, 0);
    if (bc) free(bc);
    return code;
}

static void inet_show_sock(struct nlmsghdr *nlh, struct sock_visitor *visitor, struct sock_filter *filter) {

    struct inet_diag_msg *r = NLMSG_DATA(nlh);
    struct inet_sock_stat s;
    memset(&s, 0, sizeof(s));

    s.inet_family = r->idiag_family;
    s.local_port = ntohs(r->id.idiag_sport);
    s.remote_port = ntohs(r->id.idiag_dport);
    if(r->idiag_family == AF_INET){
        inet_ntop(AF_INET, (struct in_addr*) &(r->id.idiag_src), s.local, INET_ADDRSTRLEN);
        inet_ntop(AF_INET, (struct in_addr*) &(r->id.idiag_dst), s.remote, INET_ADDRSTRLEN);
    } else if(r->idiag_family == AF_INET6){
        inet_ntop(AF_INET6, (struct in_addr6*) &(r->id.idiag_src), s.local, INET6_ADDRSTRLEN);
        inet_ntop(AF_INET6, (struct in_addr6*) &(r->id.idiag_dst), s.remote, INET6_ADDRSTRLEN);
    } else {
        fprintf(stderr, "Unknown family\n");
        return; // skip
    }

    if (filter->only_curr_user && getuid() != r->idiag_uid)
        return; // skip

    struct pid_ent *e = find_pid_ent(visitor->pid_hash, r->idiag_inode);
    if (e) {
        if (filter->only_curr_proc && getpid() != e->pid)
            return; // skip
        s.pid = e->pid;
    } else if (filter->only_curr_proc) {
        return; // skip
    }

    // char debug[1024];
    // sprintf(debug, "%d/%d/%d", filter->only_curr_user, filter->only_curr_proc, e != NULL);

    s.conn_state = r->idiag_state;
    s.state_name = sstate_name[s.conn_state];

    s.request_queue = r->idiag_rqueue;
    s.waiting_queue = r->idiag_wqueue;

    struct passwd *u = getpwuid(r->idiag_uid);
    s.uid = r->idiag_uid;
    if (u) s.username = u->pw_name;

    struct tcp_stat t;
    if (r->idiag_timer) {
        if (r->idiag_timer > TCP_TIMER_UNKNOWN)
            r->idiag_timer = TCP_TIMER_UNKNOWN;
        t.timer = r->idiag_timer;
        t.timer_name = tmr_name[r->idiag_timer];
        t.timer_retransmits = r->idiag_retrans;
        t.timer_timeout = r->idiag_expires;
    }

    struct rtattr* mem_info = NULL;
    struct rtattr* tcp_info = NULL;
    struct rtattr* vegas_info = NULL;
    int rta_len = nlh->nlmsg_len - NLMSG_LENGTH(sizeof(*r));
    if (rta_len > 0) {
        struct rtattr *attr = (struct rtattr*) (r+1);
        while (RTA_OK(attr, rta_len)) {
            if (attr->rta_type == INET_DIAG_SKMEMINFO) {
                // socket memory usage
                mem_info = attr;
            } else if (attr->rta_type == INET_DIAG_INFO) {
                // TCP statistics
                tcp_info = attr;
            } else if (attr->rta_type == INET_DIAG_VEGASINFO) {
                vegas_info = attr;
            }
            attr = RTA_NEXT(attr, rta_len);
        }
    }

    if (mem_info) {
        const __u32 *i = (__u32 *) RTA_DATA(mem_info);
        s.rcv_queue_mem = i[SK_MEMINFO_RMEM_ALLOC];
        s.snd_queue_mem = i[SK_MEMINFO_WMEM_ALLOC];
        s.rcv_sock_buf = i[SK_MEMINFO_RCVBUF];
        s.snd_sock_buf = i[SK_MEMINFO_SNDBUF];
        s.tcp_fwd_alloc = i[SK_MEMINFO_FWD_ALLOC];
        s.tcp_queued_mem = i[SK_MEMINFO_WMEM_QUEUED];
        if (RTA_PAYLOAD(mem_info) >= (SK_MEMINFO_BACKLOG + 1) * sizeof(__u32))
            s.backlog_packets = i[SK_MEMINFO_BACKLOG];
    }

    if (tcp_info) {
        const struct tcp_info *info;
        int len = RTA_PAYLOAD(tcp_info);
        if (len < sizeof(*info)) {
            /* workaround for older kernels with fewer fields */
            info = alloca(sizeof(*info));
            memset((void *)info, 0, sizeof(*info));
            memcpy((void *)info, RTA_DATA(tcp_info), len);
        } else
            info = RTA_DATA(tcp_info);

        t.options = info->tcpi_options;
        t.retransmits = info->tcpi_retransmits;
        t.probes = info->tcpi_probes;
        t.backoff = info->tcpi_backoff;

        if (info->tcpi_options & TCPI_OPT_WSCALE) {
            t.snd_wnd_scale = info->tcpi_snd_wscale;
            t.rcv_wnd_scale = info->tcpi_rcv_wscale;
        }

        if (info->tcpi_rto && info->tcpi_rto != 3000000)
            t.retransmit_timeout = info->tcpi_rto;
        if (info->tcpi_ato)
            t.acknowledge_timeout = info->tcpi_ato;
        if (info->tcpi_snd_mss)
            t.snd_mss = info->tcpi_snd_mss;
        if (info->tcpi_rcv_mss)
            t.rcv_mss = info->tcpi_rcv_mss;

        if (info->tcpi_rtt) {
            t.round_trip_time = info->tcpi_rtt;
            t.round_trip_time_var = info->tcpi_rttvar;
        }

        t.total_retrans = info->tcpi_total_retrans;

        if (info->tcpi_snd_cwnd != 2)
            t.snd_cwnd = info->tcpi_snd_cwnd;
        if (info->tcpi_snd_ssthresh < 0xFFFF)
            t.snd_ssthresh = info->tcpi_snd_ssthresh;

        double rtt = info->tcpi_rtt;
        if (vegas_info) {
            const struct tcpvegas_info *vinfo = RTA_DATA(vegas_info);
            if (vinfo->tcpv_enabled &&
                vinfo->tcpv_rtt && vinfo->tcpv_rtt != 0x7fffffff)
                rtt =  vinfo->tcpv_rtt;
        }
        if (rtt > 0 && info->tcpi_snd_mss && info->tcpi_snd_cwnd) {
            t.snd_bandwidth = (double) info->tcpi_snd_cwnd *
                              (double) info->tcpi_snd_mss * 8000000.
                              / rtt;
        }

        if (info->tcpi_rcv_rtt)
            t.rcv_rrt = info->tcpi_rcv_rtt;
        if (info->tcpi_rcv_space)
            t.rcv_space = info->tcpi_rcv_space;
    }
    visitor->visit_func(visitor->visit_ctx, &s, &t, NULL);
}

int recv_diag_msg(int sock_fd, struct sock_visitor *visitor, struct sock_filter *filter) {

    char buf[SOCKET_BUFFER_SIZE];
    struct iovec iov[3];
    iov[0] = (struct iovec){
            .iov_base = buf,
            .iov_len = sizeof(buf)
    };

    struct sockaddr_nl sock_addr;
    memset(&sock_addr, 0, sizeof(sock_addr));
    sock_addr.nl_family = AF_NETLINK;

    for(;;) {

        struct msghdr msg = (struct msghdr) {
                (void*)&sock_addr, sizeof(sock_addr),
                iov,	1,
                NULL,	0,
                0
        };

        int status = recvmsg(sock_fd, &msg, 0);
        if (status < 0) {
            if (errno == EINTR)
                continue;
            perror("OVERRUN");
            continue;
        }
        if (status == 0) {
            fprintf(stderr, "EOF on netlink\n");
            return EXIT_SUCCESS;
        }

        struct nlmsghdr *h = (struct nlmsghdr*) buf;
        while (NLMSG_OK(h, status)) {

            if(h->nlmsg_type == NLMSG_DONE)
                goto done;

            if (h->nlmsg_type == NLMSG_ERROR) {
                struct nlmsgerr *err = (struct nlmsgerr*) NLMSG_DATA(h);
                if (h->nlmsg_len < NLMSG_LENGTH(sizeof(struct nlmsgerr))) {
                    fprintf(stderr, "ERROR truncated\n");
                } else {
                    errno = -err->error;
                    if (errno == EOPNOTSUPP) {
                        return EXIT_FAILURE;
                    }
                    perror("TCPDIAG answers");
                }
                goto done;
            }

            inet_show_sock(h, visitor, filter);
            h = NLMSG_NEXT(h, status);
        }
    }

    done:
    return EXIT_SUCCESS;
}


int collect_with_filter(int sock_fd, struct sock_visitor *visitor, struct sock_filter *filter, int family, int protocol) {
    //Send the request for the sockets we are interested in
    if (send_diag_msg(sock_fd, family, protocol, filter->port_filters) < 0) {
        perror("sendmsg: ");
        return EXIT_FAILURE;
    }
    return recv_diag_msg(sock_fd, visitor, filter);
}

int collect_sock_stat(struct sock_visitor *visitor, struct sock_filter *filter) {

    int sock_fd = 0;
    if ((sock_fd = socket(AF_NETLINK, SOCK_DGRAM, NETLINK_INET_DIAG)) < 0) {
        perror("socket: ");
        return EXIT_FAILURE;
    }

    int code = pid_ent_hash_build(visitor->pid_hash);
    if (code == EXIT_SUCCESS && filter->show_families & SHOW_IPV4) {
        if (filter->show_protocols & SHOW_TCP) {
            code = collect_with_filter(sock_fd, visitor, filter, AF_INET, IPPROTO_TCP);
        }
        if (code == EXIT_SUCCESS && filter->show_protocols & SHOW_UDP) {
            code = collect_with_filter(sock_fd, visitor, filter, AF_INET, IPPROTO_UDP);
        }
    }
    if (code == EXIT_SUCCESS && filter->show_families & SHOW_IPV6) {
        if (filter->show_protocols & SHOW_TCP) {
            code = collect_with_filter(sock_fd, visitor, filter, AF_INET6, IPPROTO_TCP);
        }
        if (code == EXIT_SUCCESS && filter->show_protocols & SHOW_UDP) {
            code = collect_with_filter(sock_fd, visitor, filter, AF_INET6, IPPROTO_UDP);
        }
    }
    pid_ent_hash_free(visitor->pid_hash);
    close(sock_fd);
    return code;

}
