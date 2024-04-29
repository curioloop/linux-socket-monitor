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
package com.curioloop.linux.socket.monitor;

import com.curioloop.linux.socket.probe.*;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;


/**
 * Monitors socket statistics and manages socket collectors.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/20
 */
@Accessors(fluent = true)
public class SockMonitor {

    /** Snapshot of TCP socket statistics. */
    @Getter private volatile Map<SockKey, InetSockStat<TcpStat>> tcpStats = Collections.emptyMap();

    /** Socket collectors associated with this monitor. */
    private final List<SockCollector<?>> collectors = new CopyOnWriteArrayList<>();

    /**
     * Checks if socket statistics monitoring is supported.
     *
     * @return true if supported, false otherwise.
     */
    public boolean isSupported() {
        return LinuxSocketProbe.unavailabilityCause() == null;
    }

    /**
     * Refreshes socket statistics based on the provided socket filter.
     *
     * @param sockFilter the filter used to collect socket statistics.
     * @return true if statistics were refreshed successfully, false otherwise.
     */
    public boolean refreshStats(SockFilter sockFilter) {
        if (!isSupported()) return false;
        LinuxSocketProbe probe = new LinuxSocketProbe();
        if (!probe.collectSocketStat(sockFilter)) {
            return false;
        }
        Map<SockKey,InetSockStat<TcpStat>> newSocks = new HashMap<>();
        for (InetSockStat<TcpStat> sock : probe.tcpSocks()) {
            String remoteIP = transferToIpv4(sock.inetFamily(), sock.remoteIP());
            String localIP = transferToIpv4(sock.inetFamily(), sock.localIP());
            if (remoteIP != null && localIP != null) {
                SockKey sockKey = new SockKey(remoteIP, localIP, sock.remotePort(), sock.localPort());
                newSocks.put(sockKey, sock);
            }
        }
        this.tcpStats = Collections.unmodifiableMap(newSocks);
        for (SockCollector<?> collector : collectors) {
            collector.refreshMeters(this);
        }
        return true;
    }

    /**
     * Adds a socket collector to the monitor.
     *
     * @param collector the collector to add.
     * @return true if added successfully, false if the collector already exists.
     */
    public synchronized boolean addCollector(SockCollector<?> collector) {
        boolean existed = collectors.contains(collector);
        return !existed && collectors.add(collector);
    }

    /**
     * Removes a socket collector from the monitor.
     *
     * @param collector the collector to remove.
     * @return true if removed successfully, false otherwise.
     */
    public synchronized boolean removeCollector(SockCollector<?> collector) {
        return collectors.remove(collector);
    }

    /** The IPv6 prefix for IPV4-compatibility. */
    static final String COMP_IPV6_PREFIX = "::ffff:";

    /** Pattern to match IPv4 format. */
    static final Pattern IPV4_FMT = Pattern.compile("^\\d+\\.\\d+\\.\\d+\\.\\d+$");

    /**
     * Transfer given ip to IPv4 if possible.
     *
     * @param family the InetFamily of the address.
     * @param ip the IP address.
     * @return the IPv4 address if available, otherwise null.
     */
    static String transferToIpv4(InetFamily family, String ip) {
        if (family == InetFamily.IPv4) return ip;
        if (ip.startsWith(COMP_IPV6_PREFIX)) {
            ip = ip.substring(COMP_IPV6_PREFIX.length());
            if (IPV4_FMT.matcher(ip).matches()) return ip;
        }
        return null;
    }

}
