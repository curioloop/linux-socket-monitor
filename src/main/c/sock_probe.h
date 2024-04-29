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
#ifndef SOCK_PROBE_INCLUDED
#define SOCK_PROBE_INCLUDED

#include <stdint.h>
#include <string.h>

#include <arpa/inet.h>

#ifdef __cplusplus
# define SOCK_PROBE__BEGIN_DECLS	extern "C" {
# define SOCK_PROBE__END_DECLS		}
#else
# define SOCK_PROBE__BEGIN_DECLS
# define SOCK_PROBE__END_DECLS
#endif

SOCK_PROBE__BEGIN_DECLS

//There are currently 11 states, but the first state is stored in pos. 1.
//Therefore, I need a 12 bit bitmask
#define TCPF_ALL 0xFFF

//Copied from libmnl source
#define SOCKET_BUFFER_SIZE (getpagesize() < 8192L ? getpagesize() : 8192L)

//Copied from iproute2/ss source
#define PID_ENT_HASH_SIZE	256
#define PID_ENT_HASH(ino) (((ino >> 24) ^ (ino >> 16) ^ (ino >> 8) ^ ino) & (PID_ENT_HASH_SIZE - 1))

#define SHOW_IPV4 1
#define SHOW_IPV6 2
#define SHOW_TCP  1
#define SHOW_UDP  2
#define SHOW_ALL  3

#define COND_OR	     1
#define COND_AND	 2
#define COND_NOT	 3
#define DST_PORT_GE  4
#define DST_PORT_LE  5
#define SRC_PORT_GE  6
#define SRC_PORT_LE  7
#define DST_PORT_EQ  8
#define SRC_PORT_EQ  9

struct port_filter {
    int type;
    int port;
    struct port_filter *pred;
    struct port_filter *post;
};

struct sock_filter {
    uint8_t	show_families:2, show_protocols:2, only_curr_user:1, only_curr_proc:1;
    struct port_filter *port_filters;
};

struct pid_ent {
    struct pid_ent	*next;
    unsigned int	ino;
    int		pid;
};

typedef const char * cst_str;

enum {
    TCP_TIMER_OFF, // no timer is active
    TCP_TIMER_RETRANSMIT, // a retransmit timer
    TCP_TIMER_KEEPALIVE, // a keep-alive timer
    TCP_TIMER_TIME_WAIT, // a TIME_WAIT timer
    TCP_TIMER_ZERO_WIN_PROBE, // a zero window probe timer
    TCP_TIMER_UNKNOWN
};

static const char *tmr_name[] = {
        "OFF",
        "ON",
        "KEEPALIVE",
        "TIME-WAIT",
        "PERSIST",
        "UNKNOWN"
};

//Kernel TCP states. /include/net/tcp_states.h
enum {
    UNKNOWN,
    TCP_ESTABLISHED,
    TCP_SYN_SENT,
    TCP_SYN_RECV,
    TCP_FIN_WAIT1,
    TCP_FIN_WAIT2,
    TCP_TIME_WAIT,
    TCP_CLOSE,
    TCP_CLOSE_WAIT,
    TCP_LAST_ACK,
    TCP_LISTEN,
    TCP_CLOSING
};

static const char *sstate_name[] = {
        "UNKNOWN",
        [TCP_ESTABLISHED] = "ESTAB",
        [TCP_SYN_SENT] = "SYN-SENT",
        [TCP_SYN_RECV] = "SYN-RECV",
        [TCP_FIN_WAIT1] = "FIN-WAIT-1",
        [TCP_FIN_WAIT2] = "FIN-WAIT-2",
        [TCP_TIME_WAIT] = "TIME-WAIT",
        [TCP_CLOSE] = "UNCONN",
        [TCP_CLOSE_WAIT] = "CLOSE-WAIT",
        [TCP_LAST_ACK] = "LAST-ACK",
        [TCP_LISTEN] = 	"LISTEN",
        [TCP_CLOSING] = "CLOSING",
};

struct inet_sock_stat {

    char local[INET6_ADDRSTRLEN+1];
    char remote[INET6_ADDRSTRLEN+1];
    uint16_t local_port;
    uint16_t remote_port;
    uint8_t	 inet_family;
    uint8_t	 conn_state;
    cst_str state_name;

    uint32_t pid;
    uint32_t uid;
    cst_str username;

    // For listening sockets: the number of pending connections.
    // For other sockets: the amount of data in the incoming queue.
    uint32_t request_queue;
    // For listening sockets: the backlog length.
    // For other sockets: the amount of memory available for sending.
    uint32_t waiting_queue;

    uint32_t backlog_packets; // The amount of packets in the backlog (not yet processed).
    uint32_t rcv_queue_mem;   // The amount of data in receive queue.
    uint32_t snd_queue_mem;   // The amount of data in send queue.
    uint32_t rcv_sock_buf;    // The receive socket buffer as set by SO_RCVBUF.
    uint32_t snd_sock_buf;    // The send socket buffer as set by SO_SNDBUF.
    uint32_t tcp_fwd_alloc;   // The amount of memory scheduled for future use (TCP only).
    uint32_t tcp_queued_mem;  // The amount of data queued by TCP, but not yet sent.
};

struct tcp_stat {

    uint8_t options;
    uint8_t retransmits;
    uint8_t probes;
    uint8_t backoff;

    uint8_t timer;
    cst_str timer_name;
    uint8_t timer_retransmits; // For timer 1,2,4
    uint32_t timer_timeout;  // For all timer, expiration time in milliseconds

    uint8_t	snd_wnd_scale : 4, rcv_wnd_scale : 4;
    uint32_t snd_mss;
    uint32_t rcv_mss;

    uint32_t retransmit_timeout; // RTO
    uint32_t acknowledge_timeout; // ANO

    // rtt is the smooth rtt of delays between sent packets and received ACK.
    uint32_t round_trip_time; // RRT
    uint32_t round_trip_time_var;

    uint32_t total_retrans;

    uint32_t snd_cwnd;
    uint32_t snd_ssthresh;
    double snd_bandwidth;

    // rcv_rtt is the time to receive one full window.
    // Its used by DRS (Dynamic Right-Sizing)
    // Check tcp_rcv_rtt_measure() & tcp_rcv_rtt_update() for details.
    uint32_t rcv_rrt;

    // The number of received bytes that were transferred to userspace over the previous round trip time (RTT).
    // This supports TCP stack receive window (RWIN) auto-tuning and does not have any known VCL use cases.
    uint32_t rcv_space;
};

typedef void (*sock_visitor_func)(void* ctx, struct inet_sock_stat* sock, struct tcp_stat *tcp, const char* debug);

struct sock_visitor {
    void* visit_ctx;
    sock_visitor_func visit_func;
    struct pid_ent *pid_hash[PID_ENT_HASH_SIZE];
};


int collect_sock_stat(struct sock_visitor *visitor, struct sock_filter *filter);

SOCK_PROBE__END_DECLS

#endif // SOCK_PROBE_INCLUDED