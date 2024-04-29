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
package com.curioloop.linux.socket.probe.demo;

import com.curioloop.linux.socket.probe.InetProto;
import com.curioloop.linux.socket.probe.SockFilter;
import com.curioloop.linux.socket.monitor.SockMonitor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MicrometerDemo {

    @SneakyThrows
    public static void main(String[] args) {

        Map<Integer, Set<String>> searchMap = new HashMap<>();
        searchMap.put(80, Collections.singleton("10.0.0.1"));
        searchMap.put(443, Collections.singleton("10.0.0.1"));

        log.info("init monitor");
        MicrometerCollector collector = new MicrometerCollector(key -> {
            Set<String> hosts = searchMap.get(key.remotePort());
            return hosts != null && hosts.contains(key.remoteIp());
        });

        SockMonitor monitor = new SockMonitor();
        monitor.addCollector(collector);

        log.info("start monitor thread");
        ScheduledFuture<?> schedule = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            log.info("collect TCP status start");
            SockFilter filter = new SockFilter().protocol(InetProto.TCP).currentProc(true);
            try {
                boolean success = monitor.refreshStats(filter);
                log.info("collect TCP status done: {}", success);
            } catch (Throwable e) {
                log.info("collect TCP status fail", e);
            }
        }, 0, 10, TimeUnit.SECONDS);

        TimeUnit.MINUTES.sleep(1);

        log.info("stop monitor thread");
        schedule.cancel(true);
        monitor.removeCollector(collector);
        collector.close();

        log.info("quit monitor");
    }

}
