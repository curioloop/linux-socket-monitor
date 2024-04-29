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
package com.curioloop.linux.socket.probe.test;

import com.curioloop.linux.socket.monitor.SockCollector;
import com.curioloop.linux.socket.probe.InetProto;
import com.curioloop.linux.socket.probe.SockFilter;
import com.curioloop.linux.socket.monitor.SockAggregator;
import com.curioloop.linux.socket.monitor.SockAggregator.MatchMode;
import com.curioloop.linux.socket.monitor.SockKey;
import com.curioloop.linux.socket.monitor.SockMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class SocketMonitorTest extends LinuxSocketTest {

    static final int portPlain = 5353;
    static final int portAgg = 5454;

    static class MockCollector extends SockCollector<Object> {

        final Object meter = new Object();

        final MatchMode mode;
        final int expectPort;

        SockKey key;
        SockMonitor monitor;

        public MockCollector(MatchMode mode, int port, Predicate<SockKey> socketMatcher) {
            super(socketMatcher);
            this.mode = mode;
            this.expectPort = port;
            Assertions.assertTrue(mode == MatchMode.REMOTE || mode == MatchMode.LOCAL);
        }

        @Override
        protected Object createMeter(SockKey key, SockMonitor monitor) {
            if (monitor.isSupported()) {
                Assertions.assertNotNull(key);
                Assertions.assertNotNull(monitor);
                if (mode == MatchMode.LOCAL)
                    Assertions.assertEquals(expectPort, key.localPort());
                if (mode == MatchMode.REMOTE)
                    Assertions.assertEquals(expectPort, key.remotePort());
                this.key = key;
                this.monitor = monitor;
                return meter;
            }
            return null;
        }
        @Override
        protected void destroyMeter(SockKey key, Object meter) {
            Assertions.assertEquals(this.meter, meter);
        }
    }

    @Test
    public void testMonitorPlain() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        prepareSocket("127.0.0.1", portPlain, done);

        Map<Integer, Set<String>> searchMap = new HashMap<>();
        searchMap.put(portPlain, Collections.singleton("127.0.0.1"));

        MockCollector collectorLocal = new MockCollector(MatchMode.LOCAL, portPlain, key -> key.localPort() == portPlain);
        MockCollector collectorRemote = new MockCollector(MatchMode.REMOTE, portPlain, key -> key.remotePort() == portPlain);

        SockMonitor monitor = new SockMonitor();
        Assertions.assertTrue(monitor.addCollector(collectorLocal));
        Assertions.assertTrue(monitor.addCollector(collectorRemote));
        Assertions.assertFalse(monitor.addCollector(collectorLocal));
        Assertions.assertFalse(monitor.addCollector(collectorRemote));

        SockFilter filter = new SockFilter().protocol(InetProto.TCP);
        boolean success = monitor.refreshStats(filter);
        Assertions.assertTrue(success);

        Assertions.assertEquals(monitor, collectorLocal.monitor);
        Assertions.assertEquals(monitor, collectorRemote.monitor);
        Assertions.assertNotNull(collectorLocal.key);
        Assertions.assertNotNull(collectorRemote.key);
        Assertions.assertEquals(portPlain, collectorLocal.key.localPort());
        Assertions.assertEquals(portPlain, collectorRemote.key.remotePort());

        Assertions.assertTrue(monitor.removeCollector(collectorLocal));
        Assertions.assertTrue(monitor.removeCollector(collectorRemote));
        Assertions.assertFalse(monitor.removeCollector(collectorLocal));
        Assertions.assertFalse(monitor.removeCollector(collectorRemote));

        collectorLocal.close();
        collectorRemote.close();
        done.countDown();
    }

    @Test
    public void testMonitorAgg() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        prepareSocket("127.0.0.1", portAgg, done);

        long interval = TimeUnit.SECONDS.toMillis(1);

        Map<String, Supplier<Stream<InetSocketAddress>>> pojo = Collections.singletonMap(
                "", Collections.singletonList(new InetSocketAddress("127.0.0.1", portAgg))::stream
        );

        MockCollector collectorLocal = new MockCollector(MatchMode.LOCAL, portAgg, new SockAggregator(MatchMode.LOCAL, interval, pojo::values));
        MockCollector collectorRemote = new MockCollector(MatchMode.REMOTE, portAgg, new SockAggregator(MatchMode.REMOTE, interval, pojo::values));

        SockMonitor monitor = new SockMonitor();
        Assertions.assertTrue(monitor.addCollector(collectorLocal));
        Assertions.assertTrue(monitor.addCollector(collectorRemote));
        Assertions.assertFalse(monitor.addCollector(collectorLocal));
        Assertions.assertFalse(monitor.addCollector(collectorRemote));

        SockFilter filter = new SockFilter().protocol(InetProto.TCP);
        boolean success = monitor.refreshStats(filter);
        Assertions.assertTrue(success);

        Assertions.assertEquals(monitor, collectorLocal.monitor);
        Assertions.assertEquals(monitor, collectorRemote.monitor);
        Assertions.assertNotNull(collectorLocal.key);
        Assertions.assertNotNull(collectorRemote.key);
        Assertions.assertEquals(portAgg, collectorLocal.key.localPort());
        Assertions.assertEquals(portAgg, collectorRemote.key.remotePort());

        Assertions.assertTrue(monitor.removeCollector(collectorLocal));
        Assertions.assertTrue(monitor.removeCollector(collectorRemote));
        Assertions.assertFalse(monitor.removeCollector(collectorLocal));
        Assertions.assertFalse(monitor.removeCollector(collectorRemote));

        collectorLocal.close();
        collectorRemote.close();
        done.countDown();
    }

}
