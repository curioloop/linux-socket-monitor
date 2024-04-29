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

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * The {@code SockFilter} class represents a filter for network sockets based on various attributes.
 * It includes filters for Internet Protocol (IP) family, protocol, user ownership, process ownership, port filtering, and port filter count.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/20
 */
@Data
@Accessors(fluent = true)
public class SockFilter {

    /** The Internet Protocol (IP) family to filter. */
    InetFamily family;

    /** The Internet Protocol (IP) protocol to filter. */
    InetProto protocol;

    /** Indicates whether to filter sockets based on the current user. */
    boolean currentUser;

    /** Indicates whether to filter sockets based on the current process. */
    boolean currentProc;

    /** The port filters to apply. */
    PortFilter portFilters;

    /** The number of port filters. */
    transient int portFilterNum;


}
