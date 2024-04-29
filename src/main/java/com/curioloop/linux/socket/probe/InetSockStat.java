/*
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
package com.curioloop.linux.socket.probe;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * The {@code InetSockStat} class represents socket statistics for Internet Protocol (IP) connections.
 * It encapsulates information such as connection state, IP family, remote and local IP addresses and ports, process ID,
 * request and waiting queues, socket information, and debugging details.
 *
 * @param <SockInfo> the type of additional socket information to be stored
 * @author curioloops@gmail.com
 * @since 2024/4/20
 */
@Data
@Accessors(fluent = true)
public class InetSockStat<SockInfo> {

    /**
     * The connection state of the socket.
     */
    private ConnState connState;

    /**
     * The Internet Protocol family of the socket.
     */
    private InetFamily inetFamily;

    /**
     * The IP address of the remote endpoint.
     */
    private String remoteIP;

    /**
     * The IP address of the local endpoint.
     */
    private String localIP;

    /**
     * The port number of the remote endpoint.
     */
    private int remotePort;

    /**
     * The port number of the local endpoint.
     */
    private int localPort;

    /**
     * The process ID associated with the socket.
     */
    private int processID;

    /**
     * The number of requests in the socket's request queue.
     */
    private long requestQueue;

    /**
     * The number of sockets waiting in the socket's waiting queue.
     */
    private long waitingQueue;

    /**
     * Additional information about the socket.
     */
    private SockInfo info;

    /**
     * Debugging information related to the socket.
     */
    private String debug;

}
