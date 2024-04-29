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

import com.curioloop.linux.socket.probe.InetProto;
import com.curioloop.linux.socket.probe.LinuxSocketProbe;
import com.curioloop.linux.socket.probe.SockFilter;
import com.curioloop.linux.socket.probe.PortFilter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

@Slf4j
public class SocketProbeTest extends LinuxSocketTest {

    static final int port = 2333;

    @Test
    public void testProbe() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        prepareSocket("127.0.0.1", port, done);

        LinuxSocketProbe probe = new LinuxSocketProbe();
        PortFilter portFilter = PortFilter.eq(PortFilter.Side.DST, port).or(PortFilter.eq(PortFilter.Side.SRC, port));
        SockFilter sockFilter = new SockFilter().protocol(InetProto.TCP).portFilters(portFilter);
        for (int i=0; i<100; i++) {
            Assertions.assertTrue(probe.collectSocketStat(sockFilter));
            Assertions.assertNotNull(probe.tcpSocks());
            Assertions.assertEquals(3, probe.tcpSocks().size());
        }
        probe.tcpSocks().forEach(s -> log.info("{}", s));
        done.countDown();
    }

}
