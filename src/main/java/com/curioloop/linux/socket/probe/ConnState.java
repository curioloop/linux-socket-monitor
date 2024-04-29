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
package com.curioloop.linux.socket.probe;

/**
 * The {@code ConnState} enum represents TCP connection states.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/20
 */
public enum ConnState {
    UNKNOWN,
    ESTABLISHED,
    SYN_SENT,
    SYN_RECV,
    FIN_WAIT1,
    FIN_WAIT2,
    TIME_WAIT,
    CLOSE,
    CLOSE_WAIT,
    LAST_ACK,
    LISTEN,
    CLOSING;

    static final ConnState[] status = values();

    /**
     * Returns the {@code ConnState} corresponding to the given integer value.
     * If the value is out of bounds, {@code null} is returned.
     *
     * @param value an integer value representing the index of the desired connection state
     * @return the connection state corresponding to the given value, or {@code null} if out of bounds
     */
    public static ConnState of(int value) {
        if (value < 0 || value >= status.length) return null;
        return status[value];
    }
}
