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

import com.curioloop.linux.socket.probe.utils.NativeUtils;
import com.curioloop.linux.socket.probe.utils.OsUtils;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.*;

/**
 * The {@code LinuxSocketProbe} class provides functionality to collect statistics about Linux network sockets.
 * It includes methods to collect socket statistics for both TCP and UDP protocols based on specified filters.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/20
 */
@Getter
@Accessors(fluent = true)
public class LinuxSocketProbe {

    /** The list of TCP socket statistics. */
    private List<InetSockStat<TcpStat>> tcpSocks;

    /** The list of UDP socket statistics. */
    private List<InetSockStat<Void>> udpSocks;

    private native int collectStat(SockFilter filter);

    @SuppressWarnings("all")
    private void visitTcpStat(InetSockStat sock, TcpStat tcp) {
        try {
            if (tcpSocks != null) {
                tcpSocks.add(sock.info(tcp));
            }
        } catch (Throwable ignore) {}
    }

    @SuppressWarnings("all")
    private void visitUdpStat(InetSockStat sock) {
        try {
            if (udpSocks != null) {
                udpSocks.add(sock);
            }
        } catch (Throwable ignore) {}
    }

    /**
     * Collects socket statistics based on the provided filter.
     *
     * @param filter the filter to apply when collecting socket statistics
     * @return {@code true} if the statistics are collected successfully, {@code false} otherwise
     */
    public boolean collectSocketStat(SockFilter filter) {
        filter = ensureFilter(filter);
        if (filter.protocol != InetProto.UDP) tcpSocks = new ArrayList<>();
        if (filter.protocol != InetProto.TCP) udpSocks = new ArrayList<>();
        return collectStat(filter) == 0;
    }

    /**
     * Ensures that the provided filter is valid and prepares it for use.
     *
     * @param filter the filter to ensure validity and prepare
     * @return a copy of the valid and prepared filter
     */
    private SockFilter ensureFilter(SockFilter filter) {
        SockFilter copy = new SockFilter();
        if (filter == null) return copy;
        int portFilterNum = 0;
        if (filter.portFilters != null) {
            Set<PortFilter> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            Queue<PortFilter> queue = new LinkedList<>();
            queue.add(filter.portFilters);  // 简单检测是否存储循环引用，可能误报
            for (PortFilter pf = queue.poll(); pf != null; pf = queue.poll()) {
                if (!visited.add(pf)) throw new IllegalStateException("circular reference");
                if (pf.curr() != null) queue.add(pf.curr());
                if (pf.next() != null) queue.add(pf.next());
                if (pf.op() == null) throw new IllegalArgumentException("op required");
                switch (pf.op()) {
                    case EQ: case GE: case LE:
                        if (pf.side() == null) throw new IllegalArgumentException("side required");
                        break;
                    case AND: case OR:
                        if (pf.curr() == null || pf.next() == null) throw new IllegalArgumentException("sub-filter required");
                        break;
                    case NOT:
                        if (pf.curr() == null) throw new IllegalArgumentException("sub-filter required");
                        if (pf.next() != null) throw new IllegalArgumentException("sub-filter redundant");
                        break;
                }
            }
            portFilterNum = visited.size();
        }
        return copy.family(filter.family).protocol(filter.protocol).currentUser(filter.currentUser).currentProc(filter.currentProc)
                .portFilters(filter.portFilters).portFilterNum(portFilterNum);
    }

    private static final Throwable UNAVAILABILITY_CAUSE;

    static {
        Throwable cause = null;
        if (!"linux".equals(OsUtils.NORMALIZED_OS)) {
            cause = new UnsatisfiedLinkError("Only supported on Linux");
        } else {
            String sharedLibName = "linux_socket_probe_" + OsUtils.NORMALIZED_ARCH;
            String sharedLibFile = "lib" + sharedLibName + ".so";
            String sharedLibPath = "/META-INF/native/" + sharedLibFile;
            try {
                NativeUtils.loadLibraryFromJar(sharedLibPath);
            } catch (Throwable ex) {
                cause = ex;
            }
        }
        UNAVAILABILITY_CAUSE = cause;
    }

    /**
     * Returns the cause of unavailability of the Linux socket probe functionality.
     *
     * @return the cause of unavailability, or {@code null} if available
     */
    public static Throwable unavailabilityCause() {
        return UNAVAILABILITY_CAUSE;
    }

}
