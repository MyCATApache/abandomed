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
public class SimplePacketIterator implements PacketIterator {
    private MycatByteBuffer buffer;
    private int nextPacketPos;
    private PacketDescriptor.PacketType lastPacketType;
    private int lastPacketPos;
    private int lastPacketLen;
    private int lastCommandType;

    public SimplePacketIterator(MycatByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public boolean hasPacket() {
        int writeIndex = buffer.writeIndex();
        if (nextPacketPos < 0) {
            /**
             * 小于0是因为一次compact造成，正在迭代的包数据被清空。
             * 此时应该处于半包状态,所以是有包需要处理的
             */
            return true;
        } else if (writeIndex > nextPacketPos) {
            return true;
        }

        return false;
    }

    @Override
    public long nextPacket() {
        long packetDescriptor = 0;
        int writeIndex = buffer.writeIndex();
        PacketDescriptor.PacketType packetType = null;
        int packetLen = 0;
        byte commandType = 0;
        if (nextPacketPos < 0) {
            //对齐指针
            if (lastPacketType == PacketDescriptor.PacketType.LONG_HALF ||
                    lastPacketType == PacketDescriptor.PacketType.FULL) {
                if (nextPacketPos + lastPacketLen + 3 <= writeIndex) {
                    nextPacketPos = writeIndex + 1;
                }
                packetDescriptor = PacketDescriptor.setPacketLen(packetDescriptor, lastPacketLen + 4);
                packetDescriptor = PacketDescriptor.setPacketStartPos(packetDescriptor, nextPacketPos);
                packetDescriptor = PacketDescriptor.setCommandType(packetDescriptor, lastCommandType);
                packetDescriptor = PacketDescriptor.setPacketType(packetDescriptor, lastPacketType);
                return packetDescriptor;
            }
        }
        if (writeIndex - nextPacketPos >= 5) {
            packetLen = (int) buffer.getFixInt(nextPacketPos, 3);
            commandType = buffer.getByte(nextPacketPos + 4);
            if (writeIndex - nextPacketPos < packetLen + 4) {
                packetType = PacketDescriptor.PacketType.LONG_HALF;
            } else {
                packetType = PacketDescriptor.PacketType.FULL;
            }
            packetDescriptor = PacketDescriptor.setPacketLen(packetDescriptor, packetLen + 4);
            packetDescriptor = PacketDescriptor.setPacketStartPos(packetDescriptor, nextPacketPos);
            packetDescriptor = PacketDescriptor.setCommandType(packetDescriptor, commandType);
            packetDescriptor = PacketDescriptor.setPacketType(packetDescriptor, packetType);
            if (packetType == PacketDescriptor.PacketType.FULL) {
                nextPacketPos += packetLen + 4;
            }
        } else {
            packetType = PacketDescriptor.PacketType.SHORT_HALF;
            packetDescriptor = PacketDescriptor.setPacketType(packetDescriptor, packetType);
            packetDescriptor = PacketDescriptor.setPacketStartPos(packetDescriptor, nextPacketPos);
        }
        lastPacketType = packetType;
        lastCommandType = commandType;
        lastPacketPos = nextPacketPos;
        lastPacketLen = packetLen;
        return packetDescriptor;
    }

    @Override
    public void reset() {
        this.nextPacketPos = 0;
    }

    @Override
    public boolean fallback() {
        if (nextPacketPos - lastPacketPos >= 0) {
            nextPacketPos -= lastPacketPos;
            return true;
        }
        return false;
    }

    protected void setNextPacketPos(int nextPacketPos) {
        this.nextPacketPos = nextPacketPos;
    }

    public int getNextPacketPos() {
        return nextPacketPos;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[nextPacketPos=" + nextPacketPos + ",lastPacketType=" + lastPacketType + "" +
                ",lastCommandType=" + lastCommandType + ",lastPacketLen=" + lastPacketLen + ",lastPacketPos=" + lastPacketPos + "]";
    }
}
