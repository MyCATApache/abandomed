package io.mycat.buffer;

/**
 * Created by ynfeng on 2017/7/27.
 */
public class SimplePacketIterator implements PacketIterator {
    private MycatByteBuffer buffer;
    private int nextPacketPos;

    public SimplePacketIterator(MycatByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public boolean hasPacket() {
        int writeIndex = buffer.writeIndex();
        if (writeIndex - 1 > nextPacketPos) {
            return true;
        }
        return false;
    }

    @Override
    public long nextPacket() {
        long packetDescriptor = 0;
        int writeIndex = buffer.writeIndex();
        PacketDescriptor.PacketType packetType = null;
        if (writeIndex - nextPacketPos >= 5) {
            int packetLen = (int) buffer.getFixInt(nextPacketPos, 3);
            int commandType = buffer.getByte(nextPacketPos + 4);
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
            packetDescriptor = PacketDescriptor.setPacketType(packetDescriptor, PacketDescriptor.PacketType.SHORT_HALF);
            packetDescriptor = PacketDescriptor.setPacketStartPos(packetDescriptor, nextPacketPos);
        }
        return packetDescriptor;
    }

    @Override
    public void reset() {
        this.nextPacketPos = 0;
    }
}
