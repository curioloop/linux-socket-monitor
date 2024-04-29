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
#include <stdio.h>

#include "sock_probe_native.h"

static jobject new_sock_stat_obj(JNIEnv *env, struct inet_sock_stat *s, char *debug) {

    jclass cls = (*env)->FindClass(env, SOCK_STAT_JAVA_CLASS);
    jmethodID init = (*env)->GetMethodID(env, cls, "<init>", "()V");
    jobject sock_stat = (*env)->NewObject(env, cls, init);

    jclass cs = (*env)->FindClass(env, CONN_STATE_ENUM_CLASS);
    jmethodID find_conn_state = (*env)->GetStaticMethodID(env, cs, CONN_STATE_FUNC_NAME, CONN_STATE_FUNC_SIG);
    jobject sock_conn_state = (*env)->CallStaticObjectMethod(env, cs, find_conn_state, s->conn_state);

    jclass is = (*env)->FindClass(env, INET_FAMILY_ENUM_CLASS);
    jfieldID fid;
    if (s->inet_family == AF_INET) {
        fid = (*env)->GetStaticFieldID(env, is, "IPv4", INET_FAMILY_ENUM_SIG);
    } else {
        fid = (*env)->GetStaticFieldID(env, is, "IPv6", INET_FAMILY_ENUM_SIG);
    }
    jobject sock_inet_family = (*env)->GetStaticObjectField(env, is, fid);

    jfieldID remote_ip = (*env)->GetFieldID(env, cls, "remoteIP", "Ljava/lang/String;");
    jfieldID local_ip = (*env)->GetFieldID(env, cls, "localIP", "Ljava/lang/String;");
    jfieldID remote_port = (*env)->GetFieldID(env, cls, "remotePort", "I");
    jfieldID local_port = (*env)->GetFieldID(env, cls, "localPort", "I");

    jfieldID conn_state = (*env)->GetFieldID(env, cls, "connState", CONN_STATE_ENUM_SIG);
    jfieldID inet_family = (*env)->GetFieldID(env, cls, "inetFamily", INET_FAMILY_ENUM_SIG);
    jfieldID req_queue = (*env)->GetFieldID(env, cls, "requestQueue", "J");
    jfieldID wait_queue = (*env)->GetFieldID(env, cls, "waitingQueue", "J");

    jfieldID pid = (*env)->GetFieldID(env, cls, "processID", "I");
    jfieldID debug_str = (*env)->GetFieldID(env, cls, "debug", "Ljava/lang/String;");

    (*env)->SetObjectField(env, sock_stat, remote_ip, (*env)->NewStringUTF(env, s->remote));
    (*env)->SetObjectField(env, sock_stat, local_ip, (*env)->NewStringUTF(env, s->local));
    (*env)->SetIntField(env, sock_stat, local_port, s->local_port);
    (*env)->SetIntField(env, sock_stat, remote_port, s->remote_port);
    (*env)->SetObjectField(env, sock_stat, conn_state, sock_conn_state);
    (*env)->SetObjectField(env, sock_stat, inet_family, sock_inet_family);
    (*env)->SetLongField(env, sock_stat, req_queue, s->request_queue);
    (*env)->SetLongField(env, sock_stat, wait_queue, s->waiting_queue);

    (*env)->SetIntField(env, sock_stat, pid, s->pid);
    if (debug) {
        (*env)->SetObjectField(env, sock_stat, debug_str, (*env)->NewStringUTF(env, debug));
    }

    return sock_stat;
}


static jobject new_tcp_stat_obj(JNIEnv *env, struct tcp_stat *t) {

    jclass cls = (*env)->FindClass(env, TCP_STAT_JAVA_CLASS);
    jmethodID init = (*env)->GetMethodID(env, cls, "<init>", "()V");
    jobject tcp_stat = (*env)->NewObject(env, cls, init);

    jfieldID rrt = (*env)->GetFieldID(env, cls, "roundTripTime", "I");
    jfieldID rrt_var = (*env)->GetFieldID(env, cls, "roundTripTimeVar", "I");
    jfieldID rto = (*env)->GetFieldID(env, cls, "retransmitTimeout", "I");
    jfieldID ato = (*env)->GetFieldID(env, cls, "acknowledgeTimeout", "I");

    jfieldID cwnd = (*env)->GetFieldID(env, cls, "congestionWindow", "I");
    jfieldID ssthresh = (*env)->GetFieldID(env, cls, "slowStartThreshold", "I");
    jfieldID bandwidth = (*env)->GetFieldID(env, cls, "estimatedBandwidth", "D");
    jfieldID total_retrans = (*env)->GetFieldID(env, cls, "totalRetransmit", "I");

    (*env)->SetIntField(env, tcp_stat, rrt, t->round_trip_time);
    (*env)->SetIntField(env, tcp_stat, rrt_var, t->round_trip_time_var);
    (*env)->SetIntField(env, tcp_stat, rto, t->retransmit_timeout);
    (*env)->SetIntField(env, tcp_stat, ato, t->acknowledge_timeout);

    (*env)->SetIntField(env, tcp_stat, cwnd, t->snd_cwnd);
    (*env)->SetIntField(env, tcp_stat, ssthresh, t->snd_ssthresh);
    (*env)->SetDoubleField(env, tcp_stat, bandwidth, t->snd_bandwidth);
    (*env)->SetIntField(env, tcp_stat, total_retrans, t->total_retrans);

    return tcp_stat;
}

void visit_sock(void *ctx, struct inet_sock_stat *s, struct tcp_stat *t, char* debug) {
    struct visit_sock_ctx *c = (struct visit_sock_ctx *)ctx;
    JNIEnv *env = c->env;

    jobject sock_stat = new_sock_stat_obj(env, s, debug);
    jobject tcp_stat = new_tcp_stat_obj(env, t);
    (*env)->CallVoidMethod(env, c->obj, c->mid, sock_stat, tcp_stat);
}

static void ensure_sock_filter(JNIEnv *env, jobject flt, struct sock_filter *f) {

    jclass sock_flt_cls = (*env)->GetObjectClass(env, flt);
    jclass inet_family_enum = (*env)->FindClass(env, INET_FAMILY_ENUM_CLASS);
    jclass inet_protocol_enum = (*env)->FindClass(env, INET_PROTO_ENUM_CLASS);

    memset(f, 0, sizeof(*f));

    jobject inet_family = (*env)->GetObjectField(env, flt,
                          (*env)->GetFieldID(env, sock_flt_cls, "family", INET_FAMILY_ENUM_SIG));
    if (inet_family == NULL) {
       f->show_families = SHOW_ALL;
    } else {
        jobject ipv4 = (*env)->GetStaticObjectField(env, inet_family_enum,
                       (*env)->GetStaticFieldID(env, inet_family_enum, "IPv4", INET_FAMILY_ENUM_SIG));
        jobject ipv6 = (*env)->GetStaticObjectField(env, inet_family_enum,
                       (*env)->GetStaticFieldID(env, inet_family_enum, "IPv6", INET_FAMILY_ENUM_SIG));
        if ((*env)->IsSameObject(env, inet_family, ipv4))
            f->show_families |= SHOW_IPV4;
        if ((*env)->IsSameObject(env, inet_family, ipv6))
            f->show_families |= SHOW_IPV6;
    }

    jobject inet_protocol = (*env)->GetObjectField(env, flt,
                            (*env)->GetFieldID(env, sock_flt_cls, "protocol", INET_PROTO_ENUM_SIG));

    if (inet_protocol == NULL) {
       f->show_protocols = SHOW_ALL;
    } else {
        jobject tcp = (*env)->GetStaticObjectField(env, inet_protocol_enum,
                      (*env)->GetStaticFieldID(env, inet_protocol_enum, "TCP", INET_PROTO_ENUM_SIG));
        jobject udp = (*env)->GetStaticObjectField(env, inet_protocol_enum,
                      (*env)->GetStaticFieldID(env, inet_protocol_enum, "UDP", INET_PROTO_ENUM_SIG));
        if ((*env)->IsSameObject(env, inet_protocol, tcp))
            f->show_protocols |= SHOW_TCP;
        if ((*env)->IsSameObject(env, inet_protocol, udp))
            f->show_protocols |= SHOW_UDP;
    }

    f->only_curr_user = (*env)->GetBooleanField(env, flt,
                        (*env)->GetFieldID(env, sock_flt_cls, "currentUser", "Z"));
    f->only_curr_proc = (*env)->GetBooleanField(env, flt,
                        (*env)->GetFieldID(env, sock_flt_cls, "currentProc", "Z"));
}


static int copy_port_filter(JNIEnv *env, jobject flt, const struct pf_enum_ctx *ec, struct port_filter *pf, const int pf_num, int n) {

    if (n == pf_num) return -1;
    struct port_filter *p = pf + (n++);

    jclass cls = (*env)->GetObjectClass(env, flt);
    jfieldID get_op = (*env)->GetFieldID(env, cls, "op", PORT_FILTER_OP_ENUM_SIG);
    jfieldID get_side = (*env)->GetFieldID(env, cls, "side", PORT_FILTER_SIDE_ENUM_SIG);
    jfieldID get_value = (*env)->GetFieldID(env, cls, "value", "I");
    jfieldID get_curr = (*env)->GetFieldID(env, cls, "curr", PORT_FILTER_JAVA_CLASS_SIG);
    jfieldID get_next = (*env)->GetFieldID(env, cls, "next", PORT_FILTER_JAVA_CLASS_SIG);

    int dst_side;
    jobject side = (*env)->GetObjectField(env, flt, get_side);
    if (side) {
        if ((*env)->IsSameObject(env, side, ec->SIDE_DST))
            dst_side = 1;
        else if ((*env)->IsSameObject(env, side, ec->SIDE_SRC))
            dst_side = 0;
        else return -1;
    }

    jobject op = (*env)->GetObjectField(env, flt, get_op);
    int value = (*env)->GetIntField(env, flt, get_value);
    if ((*env)->IsSameObject(env, op, ec->OP_LE)) {
        p->type = dst_side ? DST_PORT_LE : SRC_PORT_LE;
        p->port = value;
    } else if ((*env)->IsSameObject(env, op, ec->OP_GE)) {
        p->type = dst_side ? DST_PORT_GE : SRC_PORT_GE;
        p->port = value;
    } else if ((*env)->IsSameObject(env, op, ec->OP_EQ)) {
        p->type = dst_side ? DST_PORT_EQ : SRC_PORT_EQ;
        p->port = value;
    } else {
        if ((*env)->IsSameObject(env, op, ec->OP_OR)) {
            p->type = COND_OR;
        } else if ((*env)->IsSameObject(env, op, ec->OP_AND)) {
            p->type = COND_AND;
        } else if ((*env)->IsSameObject(env, op, ec->OP_NOT)) {
            p->type = COND_NOT;
        } else {
            return -1;
        }
        jobject curr = (*env)->GetObjectField(env, flt, get_curr);
        jobject next = (*env)->GetObjectField(env, flt, get_next);
        if (curr) {
            p->pred = pf + n;
            n = copy_port_filter(env, curr, ec, pf, pf_num, n);
            if (n < 0) return n;
        }
        if (next) {
            p->post = pf + n;
            n = copy_port_filter(env, next, ec, pf, pf_num, n);
            if (n < 0) return n;
        }
    }

    return n;
}

static int get_port_filter_num(JNIEnv *env, jobject flt) {
    return (*env)->GetIntField(env, flt,
           (*env)->GetFieldID(env,
           (*env)->GetObjectClass(env, flt), "portFilterNum", "I"));
}

static int ensure_port_filter(JNIEnv *env, jobject flt, struct port_filter *pf, int pf_num) {

    jclass port_flt_op_enum = (*env)->FindClass(env, PORT_FILTER_OP_ENUM_CLASS);
    jclass port_flt_side_enum = (*env)->FindClass(env, PORT_FILTER_SIDE_ENUM_CLASS);

    jobject AND = (*env)->GetStaticObjectField(env, port_flt_op_enum,
                  (*env)->GetStaticFieldID(env, port_flt_op_enum, "AND", PORT_FILTER_OP_ENUM_SIG));
    jobject OR =  (*env)->GetStaticObjectField(env, port_flt_op_enum,
                  (*env)->GetStaticFieldID(env, port_flt_op_enum, "OR", PORT_FILTER_OP_ENUM_SIG));
    jobject NOT = (*env)->GetStaticObjectField(env, port_flt_op_enum,
                  (*env)->GetStaticFieldID(env, port_flt_op_enum, "NOT", PORT_FILTER_OP_ENUM_SIG));
    jobject GE =  (*env)->GetStaticObjectField(env, port_flt_op_enum,
                  (*env)->GetStaticFieldID(env, port_flt_op_enum, "GE", PORT_FILTER_OP_ENUM_SIG));
    jobject LE =  (*env)->GetStaticObjectField(env, port_flt_op_enum,
                  (*env)->GetStaticFieldID(env, port_flt_op_enum, "LE", PORT_FILTER_OP_ENUM_SIG));
    jobject EQ =  (*env)->GetStaticObjectField(env, port_flt_op_enum,
                  (*env)->GetStaticFieldID(env, port_flt_op_enum, "EQ", PORT_FILTER_OP_ENUM_SIG));

    jobject SRC = (*env)->GetStaticObjectField(env, port_flt_side_enum,
                  (*env)->GetStaticFieldID(env, port_flt_side_enum, "SRC", PORT_FILTER_SIDE_ENUM_SIG));
    jobject DST = (*env)->GetStaticObjectField(env, port_flt_side_enum,
                  (*env)->GetStaticFieldID(env, port_flt_side_enum, "DST", PORT_FILTER_SIDE_ENUM_SIG));

    struct pf_enum_ctx ec = {
        .OP_AND = AND, .OP_OR = OR, .OP_NOT = NOT, .OP_GE = GE, .OP_LE = LE, .OP_EQ = EQ,
        .SIDE_SRC = SRC, .SIDE_DST = DST
    };

    memset(pf, 0, pf_num * sizeof(struct port_filter));

    jobject port_filters = (*env)->GetObjectField(env, flt,
                           (*env)->GetFieldID(env, (*env)->GetObjectClass(env, flt), "portFilters", PORT_FILTER_JAVA_CLASS_SIG));

    int code = copy_port_filter(env, port_filters, &ec, pf, pf_num, 0);
    return code == pf_num ? EXIT_SUCCESS : EXIT_FAILURE;
}

JNIEXPORT int JNICALL Java_com_curioloop_linux_socket_probe_LinuxSocketProbe_collectStat(JNIEnv *env, jobject obj, jobject flt) {
    jmethodID mid = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, obj), TCP_PROBE_CB_NAME, TCP_PROBE_CB_SIG);
    struct visit_sock_ctx ctx = { .env = env, .obj = obj, .mid = mid };
    struct sock_visitor visitor = { .visit_ctx = &ctx, .visit_func = visit_sock };
    struct sock_filter filter;
    ensure_sock_filter(env, flt, &filter);

    int port_filter_num = get_port_filter_num(env, flt);
    if (port_filter_num) {
        filter.port_filters = alloca(port_filter_num * sizeof(struct port_filter));
        int err = ensure_port_filter(env, flt, filter.port_filters, port_filter_num);
        if (err) return err;
    }

    return collect_sock_stat(&visitor, &filter);
}