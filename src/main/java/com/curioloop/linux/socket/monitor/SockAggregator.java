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

import java.net.InetSocketAddress;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Aggregates socket addresses for dynamic socket set.
 * <p>
 * Reduce unnecessary memory footprint by:
 * <ul>
 *     <li>Collect socket direct from pojo</li>
 *     <li>Cache the snapshot of socket set</li>
 * </ul>
 *
 * @author curioloops@gmail.com
 * @since 2024/4/20
 */
public class SockAggregator implements Predicate<SockKey> {

    /** Enumeration for matching modes. */
    public enum MatchMode {
        LOCAL, REMOTE, BOTH
    }

    /** Time when the address was last updated. */
    private volatile long addressUpdatedTime;

    /** Cache for aggregated socket addresses. */
    private volatile Map<Integer, Set<String>> socketAddresses;

    /** Matching mode for the aggregator. */
    final MatchMode matchMode;

    /** Refresh interval for updating socket addresses. */
    final long refreshInterval;

    /** Supplier for remote address sources. */
    final Supplier<Collection<? extends Supplier<Stream<InetSocketAddress>>>> remoteAddressSources;

    /**
     * Constructs a SockAggregator with given collection.
     *
     * @param matchMode the matching mode for the aggregator.
     * @param refreshInterval the refresh interval for updating socket addresses cache.
     * @param remoteAddressSources the remote address sources.
     */
    public SockAggregator(MatchMode matchMode, long refreshInterval, Collection<? extends Supplier<Stream<InetSocketAddress>>> remoteAddressSources) {
        this(matchMode, refreshInterval, () -> remoteAddressSources);
    }

    /**
     * Constructs a SockAggregator with the given supplier.
     *
     * @param matchMode the matching mode for the aggregator.
     * @param refreshInterval the refresh interval for updating socket addresses cache.
     * @param remoteAddressSources the supplier for remote address sources.
     */
    public SockAggregator(MatchMode matchMode, long refreshInterval, Supplier<Collection<? extends Supplier<Stream<InetSocketAddress>>>> remoteAddressSources) {
        this.matchMode = Objects.requireNonNull(matchMode);
        this.refreshInterval = refreshInterval;
        this.remoteAddressSources = Objects.requireNonNull(remoteAddressSources);
    }

    private void aggregateSocketAddress() {
        long now = System.currentTimeMillis();
        if (now - addressUpdatedTime >= refreshInterval) {
            synchronized (this) {
                if (now - addressUpdatedTime >= refreshInterval) {
                    try {
                        Collection<? extends Supplier<Stream<InetSocketAddress>>> sources = remoteAddressSources.get();
                        if (sources == null) sources = Collections.emptyList();
                        socketAddresses = sources.stream().
                                filter(Objects::nonNull).flatMap(Supplier::get).filter(Objects::nonNull).collect(
                                        Collectors.groupingBy(InetSocketAddress::getPort,
                                                Collectors.mapping(sa -> sa.getAddress().getHostAddress(), Collectors.toSet())));
                    } catch (Exception e) {
                        socketAddresses = Collections.emptyMap(); // clear address
                        throw new RuntimeException(e);
                    }
                    addressUpdatedTime = now;
                }
            }
        }
    }

    @Override
    public boolean test(SockKey key) {
        aggregateSocketAddress();
        Set<String> localSet;
        Set<String> remoteSet;
        if (matchMode == MatchMode.LOCAL) {
            localSet = socketAddresses.get(key.localPort());
            return localSet != null && localSet.contains(key.localIp());
        }
        if (matchMode == MatchMode.REMOTE) {
            remoteSet = socketAddresses.get(key.remotePort());
            return remoteSet != null && remoteSet.contains(key.remoteIp());
        }
        localSet = socketAddresses.get(key.localPort());
        remoteSet = socketAddresses.get(key.remotePort());
        return localSet != null && localSet.contains(key.localIp()) ||
               remoteSet != null && remoteSet.contains(key.remoteIp());
    }
}
