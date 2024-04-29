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

import com.curioloop.linux.socket.monitor.SockCollector;
import com.curioloop.linux.socket.monitor.SockKey;
import com.curioloop.linux.socket.monitor.SockMonitor;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;

import java.time.Duration;
import java.util.function.Predicate;

public class MicrometerCollector extends SockCollector<MicrometerGauge> {

    final MeterRegistry registry;

    public MicrometerCollector(Predicate<SockKey> socketMatcher) {
        super(socketMatcher);
        this.registry = new LoggingMeterRegistry(new LoggingRegistryConfig() {
            @Override
            public boolean enabled() {
                return true;
            }
            public Duration step() {
                return Duration.ofSeconds(10);
            }
            public String get(String key) {
                return null;
            }
        }, Clock.SYSTEM);
    }

    @Override
    protected MicrometerGauge createMeter(SockKey key, SockMonitor monitor) {
        if (monitor.isSupported()) {
            // Must keep strong reference to micrometer gauge.
            // Otherwise, it will be GC.
            return MicrometerGauge.register(registry, monitor, key, Tags.empty());
        }
        return null;
    }

    @Override
    protected void destroyMeter(SockKey key, MicrometerGauge gauge) {
        gauge.unregister(registry);
    }

    @Override
    public void close() {
        super.close();
        registry.close();
    }

}
