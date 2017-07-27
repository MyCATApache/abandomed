package io.mycat.buffer;

/**
 * Created by ynfeng on 2017/7/27.
 */
public class SimplePacketIterator implements PacketIterator {
    private MycatByteBuffer buffer;
    private int lastPacketPos;
    private int lastPacketLen;

    public SimplePacketIterator(MycatByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public boolean hasPacket() {
        return true;
    }

    @Override
    public long nextPacket() {
        long packetDescriptor = 0;
        int writeIndex = buffer.writeIndex();
        if (writeIndex - lastPacketLen < 5) {
            lastPacketPos += lastPacketLen;
            packetDescriptor = PacketDescriptor.setPacketType(packetDescriptor, PacketDescriptor.PacketType.SHORT_HALF);
            packetDescriptor = PacketDescriptor.setPacketStartPos(packetDescriptor, lastPacketPos);
            return packetDescriptor;
        }
        lastPacketPos += lastPacketLen;
        int packetLen = (int) buffer.getFixInt(lastPacketPos, 3);
        int commandType = buffer.getByte(lastPacketPos + 4);
        lastPacketLen = packetLen + 4;
        packetDescriptor = PacketDescriptor.setPacketLen(packetDescriptor, packetLen);
        packetDescriptor = PacketDescriptor.setPacketStartPos(packetDescriptor, lastPacketPos);
        packetDescriptor = PacketDescriptor.setCommandType(packetDescriptor, commandType);
        if (writeIndex >= (lastPacketPos + packetLen)) {
            packetDescriptor = PacketDescriptor.setPacketType(packetDescriptor, PacketDescriptor.PacketType.LONG_HALF);
        } else {
            packetDescriptor = PacketDescriptor.setPacketType(packetDescriptor, PacketDescriptor.PacketType.FULL);
        }
        return packetDescriptor;
    }

    @Override
    public void reset() {
        this.lastPacketLen = this.lastPacketPos = 0;
    }
}
