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

import com.curioloop.linux.socket.probe.InetSockStat;
import com.curioloop.linux.socket.probe.TcpStat;
import lombok.RequiredArgsConstructor;

import java.util.function.ToDoubleFunction;


/**
 * Represents a gauge for monitoring socket statistics.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/20
 */
@RequiredArgsConstructor(staticName = "of")
public class SockGauge extends Number {

    /** The key associated with the socket. */
    final SockKey key;

    /** The monitor used for monitoring socket statistics. */
    final SockMonitor monitor;

    /** The function used to compute the double value. */
    final ToDoubleFunction<InetSockStat<TcpStat>> func;

    @Override
    public int intValue() {
        return (int) doubleValue();
    }

    @Override
    public long longValue() {
        return (long) doubleValue();
    }

    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    @Override
    public double doubleValue() {
        return func.applyAsDouble(monitor.tcpStats().get(key));
    }

    /**
     * Creates a SockGauge for monitoring the transmission queue.
     *
     * @param key the key associated with the socket.
     * @param monitor the monitor used for monitoring socket statistics.
     * @param defaultValue the default value if the socket is null.
     * @return a SockGauge for monitoring the transmission queue.
     */
    public static SockGauge txQueue(SockKey key, SockMonitor monitor, double defaultValue) {
        return SockGauge.of(key, monitor, sock -> {
            if (sock == null) return defaultValue;
            return sock.requestQueue();
        });
    }

    /**
     * Creates a SockGauge for monitoring the receive queue.
     *
     * @param key the key associated with the socket.
     * @param monitor the monitor used for monitoring socket statistics.
     * @param defaultValue the default value if the socket is null.
     * @return a SockGauge for monitoring the receive queue.
     */
    public static SockGauge rxQueue(SockKey key, SockMonitor monitor, double defaultValue) {
        return SockGauge.of(key, monitor, sock -> {
            if (sock == null) return defaultValue;
            return sock.waitingQueue();
        });
    }

    /**
     * Creates a SockGauge for monitoring the round-trip time.
     *
     * @param key the key associated with the socket.
     * @param monitor the monitor used for monitoring socket statistics.
     * @param defaultValue the default value if the socket is null or if the socket info is null.
     * @return a SockGauge for monitoring the round-trip time.
     */
    public static SockGauge rrt(SockKey key, SockMonitor monitor, double defaultValue) {
        return SockGauge.of(key, monitor, sock -> {
            if (sock == null || sock.info() == null) return defaultValue;
            return sock.info().roundTripTime();
        });
    }

    /**
     * Creates a SockGauge for monitoring the retransmit timeout.
     *
     * @param key the key associated with the socket.
     * @param monitor the monitor used for monitoring socket statistics.
     * @param defaultValue the default value if the socket is null or if the socket info is null.
     * @return a SockGauge for monitoring the retransmit timeout.
     */
    public static SockGauge rto(SockKey key, SockMonitor monitor, double defaultValue) {
        return SockGauge.of(key, monitor, sock -> {
            if (sock == null || sock.info() == null) return defaultValue;
            return sock.info().retransmitTimeout();
        });
    }

    /**
     * Creates a SockGauge for monitoring the acknowledge timeout.
     *
     * @param key the key associated with the socket.
     * @param monitor the monitor used for monitoring socket statistics.
     * @param defaultValue the default value if the socket is null or if the socket info is null.
     * @return a SockGauge for monitoring the acknowledge timeout.
     */
    public static SockGauge ano(SockKey key, SockMonitor monitor, double defaultValue) {
        return SockGauge.of(key, monitor, sock -> {
            if (sock == null || sock.info() == null) return defaultValue;
            return sock.info().acknowledgeTimeout();
        });
    }

    /**
     * Creates a SockGauge for monitoring the total number of retransmissions.
     *
     * @param key the key associated with the socket.
     * @param monitor the monitor used for monitoring socket statistics.
     * @param defaultValue the default value if the socket is null or if the socket info is null.
     * @return a SockGauge for monitoring the total number of retransmissions.
     */
    public static SockGauge reTrans(SockKey key, SockMonitor monitor, double defaultValue) {
        return SockGauge.of(key, monitor, sock -> {
            if (sock == null || sock.info() == null) return defaultValue;
            return sock.info().totalRetransmit();
        });
    }

    /**
     * Creates a SockGauge for monitoring the congestion window size.
     *
     * @param key the key associated with the socket.
     * @param monitor the monitor used for monitoring socket statistics.
     * @param defaultValue the default value if the socket is null or if the socket info is null.
     * @return a SockGauge for monitoring the congestion window size.
     */
    public static SockGauge cWnd(SockKey key, SockMonitor monitor, double defaultValue) {
        return SockGauge.of(key, monitor, sock -> {
            if (sock == null || sock.info() == null) return defaultValue;
            return sock.info().congestionWindow();
        });
    }

    /**
     * Creates a SockGauge for monitoring the slow start threshold.
     *
     * @param key the key associated with the socket.
     * @param monitor the monitor used for monitoring socket statistics.
     * @param defaultValue the default value if the socket is null or if the socket info is null.
     * @return a SockGauge for monitoring the slow start threshold.
     */
    public static SockGauge ssThresh(SockKey key, SockMonitor monitor, double defaultValue) {
        return SockGauge.of(key, monitor, sock -> {
            if (sock == null || sock.info() == null) return defaultValue;
            return sock.info().slowStartThreshold();
        });
    }

}
