/*
 * Copyright (c) 2017, MyCAT and/or its affiliates. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */
package io.mycat.buffer;

/**
 * Created by ynfeng on 2017/7/27.
 */
public interface PacketIterator {

    /**
     * Returns {@code true} if there is a packet, {@code false} there is no more packet.
     */
    boolean hasPacket();

    /**
     * Get next packet.
     * If {@link #hasPacket()} return {@code false} this method return 0,{@code true} return a packet descriptor.
     * Packet descriptor is a 64-bit integer,the structure is as follows,
     * <pre>
     * +-------------------------------------------+----------------------------+-----------------------------------+----------------+
     * |                30 bits                    |           24 bits          |           8 bits                  |      2 bits    |
     * | The packet start position in buffer       | The length of packet       |         command type              |   packet type  |
     * +-------------------------------------------+----------------------------+-----------------------------------+----------------+
     * </pre>
     * A packet up to 16MB,24-bit is enough.
     * 2-bit packet type may be {@link PacketDescriptor.PacketType#LONG_HALF} or
     * {@link PacketDescriptor.PacketType#SHORT_HALF} or {@link PacketDescriptor.PacketType#FULL}
     * When packet type is {@link PacketDescriptor.PacketType#SHORT_HALF} the packet length and command type are meaningless.
     */
    long nextPacket();

    /**
     * Reset the internal state.
     */
    void reset();

    /**
     * fallback to previous packet.
     * if success return {@code true}
     */
    boolean fallback();
}
