package io.mycat.buffer;

/**
 * Created by ynfeng on 2017/7/28.
 */
public class PacketDescriptor {

    public static long setPacketType(long packetDescriptor, PacketType packetType) {
        return (packetDescriptor | (packetType.getValue() & 0x03));
    }

    public static long setCommandType(long packetDescriptor, int commandType) {
        return packetDescriptor | ((commandType & 0x3F) << 2);
    }

    public static long setPacketLen(long packetDescriptor, int packetLen) {
        return packetDescriptor | ((packetLen & 0xFFFFFF) << 8);
    }

    public static long setPacketStartPos(long packetDescriptor, int packetStartPos) {
        return packetDescriptor | ((packetStartPos & 0xFFFFFFFF) << 32);
    }

    static enum PacketType {
        SHORT_HALF(0), LONG_HALF(1), FULL(2);

        PacketType(int value) {
            this.value = value;
        }

        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }


}
