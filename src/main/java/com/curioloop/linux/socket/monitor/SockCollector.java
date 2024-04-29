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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

/**
 * An abstract class for collecting and managing socket meters.
 *
 * @param <Meter> the type of the meter associated with the socket.
 * @author curioloops@gmail.com
 * @since 2024/4/20
 */
@RequiredArgsConstructor
public abstract class SockCollector<Meter> implements AutoCloseable {

    /** Predicate to match sockets. */
    protected final Predicate<SockKey> socketMatcher;

    /** Map to store managed meters. */
    protected final Map<SockKey, Meter> managedMeters = new HashMap<>();

    /**
     * Creates a meter for the specified socket key and monitor.
     *
     * @param key the key associated with the socket.
     * @param monitor the monitor used for monitoring socket statistics.
     * @return a meter for the specified socket key and monitor.
     */
    protected abstract Meter createMeter(SockKey key, SockMonitor monitor);

    /**
     * Destroys the meter associated with the specified socket key.
     *
     * @param key the key associated with the socket.
     * @param meter the meter to destroy.
     */
    protected abstract void destroyMeter(SockKey key, Meter meter);

    /**
     * Refreshes the meters based on the provided socket monitor.
     *
     * @param monitor the monitor used for monitoring socket statistics.
     * @return true if any changes were made to the meters, false otherwise.
     */
    public boolean refreshMeters(SockMonitor monitor) {
        boolean changed = false;
        Map<SockKey, InetSockStat<TcpStat>> tcpStats = monitor.tcpStats();
        // Add meter when matching socket found
        for (SockKey key : tcpStats.keySet()) {
            if (socketMatcher.test(key) && !managedMeters.containsKey(key)) {
                Meter meter = createMeter(key, monitor);
                if (meter != null) {
                    managedMeters.put(key, meter);
                    changed = true;
                }
            }
        }
        // Remove meter when matching socket gone
        Iterator<Map.Entry<SockKey, Meter>> iterator = managedMeters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SockKey, Meter> entry = iterator.next();
            SockKey key = entry.getKey();
            if (!tcpStats.containsKey(key)) {
                destroyMeter(key, entry.getValue());
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Closes the collector by destroying all managed meters.
     */
    @Override
    public void close() {
        managedMeters.forEach(this::destroyMeter);
        managedMeters.clear();
    }

}
