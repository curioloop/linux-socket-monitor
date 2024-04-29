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

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * The {@code SockKey} class represents a key that uniquely identifies a network socket.
 * It includes the remote and local IP addresses along with remote and local port numbers.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/20
 */
@Data
@Accessors(fluent = true)
public class SockKey {

    final String remoteIp;
    final String localIp;
    final int remotePort;
    final int localPort;

    @Override
    public String toString() {
        return "(" + localIp + ":" + localPort + "->" + remoteIp + ":" + remotePort + ")";
    }
}
