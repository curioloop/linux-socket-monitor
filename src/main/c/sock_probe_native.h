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
#ifndef TCP_PROBE_NATIVE_INCLUDED
#define TCP_PROBE_NATIVE_INCLUDED

#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <alloca.h>

#include "sock_probe.h"

#ifdef __cplusplus
# define TCP_PROBE_NATIVE__BEGIN_DECLS	extern "C" {
# define TCP_PROBE_NATIVE__END_DECLS		}
#else
# define TCP_PROBE_NATIVE__BEGIN_DECLS
# define TCP_PROBE_NATIVE__END_DECLS
#endif

TCP_PROBE_NATIVE__BEGIN_DECLS

#define PORT_FILTER_JAVA_CLASS "com/curioloop/linux/socket/probe/PortFilter"
#define PORT_FILTER_JAVA_CLASS_SIG "Lcom/curioloop/linux/socket/probe/PortFilter;"

#define PORT_FILTER_SIDE_ENUM_CLASS "com/curioloop/linux/socket/probe/PortFilter$Side"
#define PORT_FILTER_SIDE_ENUM_SIG "Lcom/curioloop/linux/socket/probe/PortFilter$Side;"

#define PORT_FILTER_OP_ENUM_CLASS "com/curioloop/linux/socket/probe/PortFilter$Op"
#define PORT_FILTER_OP_ENUM_SIG "Lcom/curioloop/linux/socket/probe/PortFilter$Op;"

#define SOCK_STAT_JAVA_CLASS "com/curioloop/linux/socket/probe/InetSockStat"
#define TCP_STAT_JAVA_CLASS "com/curioloop/linux/socket/probe/TcpStat"

#define CONN_STATE_ENUM_CLASS "com/curioloop/linux/socket/probe/ConnState"
#define CONN_STATE_ENUM_SIG "Lcom/curioloop/linux/socket/probe/ConnState;"

#define INET_FAMILY_ENUM_CLASS "com/curioloop/linux/socket/probe/InetFamily"
#define INET_FAMILY_ENUM_SIG "Lcom/curioloop/linux/socket/probe/InetFamily;"

#define INET_PROTO_ENUM_CLASS "com/curioloop/linux/socket/probe/InetProto"
#define INET_PROTO_ENUM_SIG "Lcom/curioloop/linux/socket/probe/InetProto;"

#define CONN_STATE_FUNC_NAME "of"
#define CONN_STATE_FUNC_SIG "(I)Lcom/curioloop/linux/socket/probe/ConnState;"

#define TCP_PROBE_CB_NAME "visitTcpStat"
#define TCP_PROBE_CB_SIG "(Lcom/curioloop/linux/socket/probe/InetSockStat;Lcom/curioloop/linux/socket/probe/TcpStat;)V"

struct pf_enum_ctx {
    jobject OP_AND, OP_OR, OP_NOT, OP_GE, OP_LE, OP_EQ;
    jobject SIDE_SRC, SIDE_DST;
};

struct visit_sock_ctx {
    JNIEnv *env;
    jobject obj;
    jmethodID mid;
};

JNIEXPORT int JNICALL Java_com_curioloop_linux_socket_probe_LinuxSocketProbe_collectStat(JNIEnv *, jobject, jobject);

TCP_PROBE_NATIVE__END_DECLS

#endif // TCP_PROBE_NATIVE_INCLUDED