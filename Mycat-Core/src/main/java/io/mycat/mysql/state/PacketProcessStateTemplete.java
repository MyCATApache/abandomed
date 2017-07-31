package io.mycat.mysql.state;

import io.mycat.buffer.MycatByteBuffer;
import io.mycat.buffer.PacketDescriptor;
import io.mycat.buffer.PacketIterator;
import io.mycat.machine.StateMachine;
import io.mycat.net2.Connection;

import java.io.IOException;

/**
 * Created by ynfeng on 2017/7/31.
 */
public abstract class PacketProcessStateTemplete extends AbstractMysqlConnectionState {

    @Override
    public boolean handle(StateMachine context, Connection connection, Object attachment) throws IOException {
        MycatByteBuffer buffer = connection.getDataBuffer();
        PacketIterator it = buffer.packetIterator();
        boolean result = false;
        while (it.hasPacket()) {
            long pakcetDescriptor = it.nextPacket();
            int packetStartPos = PacketDescriptor.getPacketStartPos(pakcetDescriptor);
            int packetLen = PacketDescriptor.getPacketLen(pakcetDescriptor);
            byte type = PacketDescriptor.getCommandType(pakcetDescriptor);
            PacketDescriptor.PacketType packetType = PacketDescriptor.getPacketType(pakcetDescriptor);
            switch (packetType) {
                case FULL:
                    result = handleFullPacket(connection, attachment, packetStartPos, packetLen, type);
                    break;
                case LONG_HALF:
                    result = handleLongHalfPacket(connection, attachment, packetStartPos, packetLen, type);
                    break;
                case SHORT_HALF:
                    result = handleShortHalfPacket(connection, attachment, packetStartPos);
                    break;
            }
            if (stopProcess()) {
                return result;
            }
        }
        return result;
    }

    public abstract boolean stopProcess();

    public abstract boolean handleShortHalfPacket(Connection connection, Object attachment, int packetStartPos);

    public abstract boolean handleLongHalfPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type);

    public abstract boolean handleFullPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type);
}
