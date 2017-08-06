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
 * Created by ynfeng on 2017/7/28.
 */
public class PacketDescriptor {

    public static long setPacketType(long packetDescriptor, PacketType packetType) {
        return (packetDescriptor & ~0x03L) | (packetType.getValue() & 0x03);
    }

    public static long setCommandType(long packetDescriptor, long commandType) {
        return (packetDescriptor & ~(0xFFL << 2)) | ((commandType & 0xFF) << 2);
    }

    public static long setPacketLen(long packetDescriptor, long packetLen) {
        return (packetDescriptor & ~(0xFFFFFFL << 10)) | ((packetLen & 0xFFFFFF) << 10);
    }

    public static long setPacketStartPos(long packetDescriptor, long packetStartPos) {
        return (packetDescriptor & ~(0x2FFFFFFFL << 34)) | ((packetStartPos & 0x2FFFFFFF) << 34);
    }

    public static PacketType getPacketType(long packetDescriptor) {
        int type = (int) (packetDescriptor & 0x03);
        PacketType packetType = null;
        switch (type) {
            case 0:
                packetType = PacketType.FULL;
                break;
            case 1:
                packetType = PacketType.LONG_HALF;
                break;
            case 2:
                packetType = packetType.SHORT_HALF;
                break;
            default:
                throw new IllegalArgumentException("Wrong packetDescriptor " + packetDescriptor);
        }
        return packetType;
    }

    public static byte getCommandType(long packetDescriptor) {
        return (byte) ((packetDescriptor >>> 2) & 0xFF);
    }

    public static int getPacketStartPos(long packetDescriptor) {
        return (int) ((packetDescriptor >>> 34) & 0x2FFFFFFF);
    }

    public static int getPacketLen(long packetDescriptor) {
        return (int) ((packetDescriptor >>> 10) & 0xFFFFFF);
    }

    public enum PacketType {
        FULL(0), LONG_HALF(1), SHORT_HALF(2);

        PacketType(int value) {
            this.value = value;
        }

        private int value;

        public int getValue() {
            return value;
        }
    }
}
