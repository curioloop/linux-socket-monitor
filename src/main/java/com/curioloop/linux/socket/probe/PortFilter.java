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
 * The {@code PortFilter} class represents a filter for network ports based on their attributes.
 * It allows filtering based on port values, operations, and sides (source or destination).
 *
 * @author curioloops@gmail.com
 * @since 2024/4/20
 */
@Data
@Accessors(fluent = true)
public class PortFilter {

    /**
     * Enumeration representing the side of the connection, either source or destination.
     */
    public enum Side { SRC, DST }

    /**
     * Enumeration representing operations for port filtering.
     */
    public enum Op { AND, OR, NOT, GE, LE, EQ }

    /** The operation to be performed by the filter. */
    private Op op;

    /** The side of the connection to filter (source or destination). */
    private Side side;

    /** The value to compare against when filtering. */
    private int value;

    /** Reference to the current filter. */
    private PortFilter curr;

    /** Reference to the next filter. */
    private PortFilter next;

    /**
     * Creates a new PortFilter instance with the equal to (EQ) operation.
     *
     * @param side the side of the connection to filter (source or destination)
     * @param value the value to compare against
     * @return a PortFilter instance with the equal to (EQ) operation
     */
    public static PortFilter eq(Side side, int value) {
        return new PortFilter().op(Op.EQ).side(side).value(value);
    }

    /**
     * Creates a new PortFilter instance with the greater than or equal to (GE) operation.
     *
     * @param side the side of the connection to filter (source or destination)
     * @param value the value to compare against
     * @return a PortFilter instance with the greater than or equal to (GE) operation
     */
    public static PortFilter ge(Side side, int value) {
        return new PortFilter().op(Op.GE).side(side).value(value);
    }

    /**
     * Creates a new PortFilter instance with the less than or equal to (LE) operation.
     *
     * @param side the side of the connection to filter (source or destination)
     * @param value the value to compare against
     * @return a PortFilter instance with the less than or equal to (LE) operation
     */
    public static PortFilter le(Side side, int value) {
        return new PortFilter().op(Op.LE).side(side).value(value);
    }

    /**
     * Creates a new PortFilter instance with the logical NOT operation.
     *
     * @return a PortFilter instance with the logical NOT operation
     */
    public PortFilter not() {
        return new PortFilter().op(Op.NOT).curr(this);
    }

    /**
     * Combines the current PortFilter with another using the logical OR operation.
     *
     * @param other the other PortFilter to combine with
     * @return a PortFilter instance with the logical OR operation
     */
    public PortFilter or(PortFilter other) {
        return new PortFilter().op(Op.OR).curr(this).next(other);
    }

    /**
     * Combines the current PortFilter with another using the logical AND operation.
     *
     * @param other the other PortFilter to combine with
     * @return a PortFilter instance with the logical AND operation
     */
    public PortFilter and(PortFilter other) {
        return new PortFilter().op(Op.AND).curr(this).next(other);
    }

}
