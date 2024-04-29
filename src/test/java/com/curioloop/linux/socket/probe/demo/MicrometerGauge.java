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

import com.curioloop.linux.socket.monitor.SockGauge;
import com.curioloop.linux.socket.monitor.SockKey;
import com.curioloop.linux.socket.monitor.SockMonitor;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Accessors(fluent = true)
@RequiredArgsConstructor
public class MicrometerGauge {

    public static final String
            SOCK_RX = "sock_rx",
            SOCK_TX = "sock_tx",
            SOCK_RRT = "sock_rrt",
            SOCK_RTO = "sock_rto",
            SOCK_ANO = "sock_ano",
            SOCK_CWND = "sock_cwnd",
            SOCK_RETRANS = "sock_retrans",
            SOCK_SSTHRESH = "sock_ssthresh";
    
    final Meter.Id rx, tx, rrt, rto, ano;
    final Meter.Id cwnd, retrans, ssthresh;
    final Object[] refers;

    public void unregister(MeterRegistry registry) {
        registry.remove(rx);
        registry.remove(tx);
        registry.remove(rrt);
        registry.remove(rto);
        registry.remove(ano);
        registry.remove(cwnd);
        registry.remove(retrans);
        registry.remove(ssthresh);
        Arrays.fill(refers, null);
    }

    public static MicrometerGauge register(MeterRegistry registry, SockMonitor monitor, SockKey key, Tags tags) {
        Meter.Id rx = new Meter.Id(SOCK_RX, tags, null, null, Meter.Type.GAUGE);
        Meter.Id tx = new Meter.Id(SOCK_TX, tags, null, null, Meter.Type.GAUGE);
        Meter.Id rrt = new Meter.Id(SOCK_RRT, tags, null, null, Meter.Type.GAUGE);
        Meter.Id rto = new Meter.Id(SOCK_RTO, tags, null, null, Meter.Type.GAUGE);
        Meter.Id ano = new Meter.Id(SOCK_ANO, tags, null, null, Meter.Type.GAUGE);
        Meter.Id cwnd = new Meter.Id(SOCK_CWND, tags, null, null, Meter.Type.GAUGE);
        Meter.Id retrans = new Meter.Id(SOCK_RETRANS, tags, null, null, Meter.Type.GAUGE);
        Meter.Id ssthresh = new Meter.Id(SOCK_SSTHRESH, tags, null, null, Meter.Type.GAUGE);
        List<SockGauge> refers = new ArrayList<>();

        refers.add(registry.gauge(rx.getName(), rx.getTags(), SockGauge.rxQueue(key, monitor, 0)));
        refers.add(registry.gauge(tx.getName(), tx.getTags(), SockGauge.txQueue(key, monitor, 0)));
        refers.add(registry.gauge(rrt.getName(), rrt.getTags(), SockGauge.rrt(key, monitor, 0)));
        refers.add(registry.gauge(rto.getName(), rto.getTags(), SockGauge.rto(key, monitor, 0)));
        refers.add(registry.gauge(ano.getName(), ano.getTags(), SockGauge.ano(key, monitor, 0)));
        refers.add(registry.gauge(cwnd.getName(), cwnd.getTags(), SockGauge.cWnd(key, monitor, 0)));
        refers.add(registry.gauge(ssthresh.getName(), ssthresh.getTags(), SockGauge.ssThresh(key, monitor, 0)));
        refers.add(registry.gauge(retrans.getName(), retrans.getTags(), SockGauge.reTrans(key, monitor, 0)));
        return new MicrometerGauge(rx, tx, rrt, rto, ano, cwnd, retrans, ssthresh, refers.toArray());
    }
}
