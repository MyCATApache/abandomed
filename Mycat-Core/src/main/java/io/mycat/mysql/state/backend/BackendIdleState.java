package io.mycat.mysql.state.backend;


import java.io.IOException;

import io.mycat.mysql.state.PacketProcessStateTemplete;
import io.mycat.net2.Connection;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.mysql.packet.MySQLPacket;

/**
 * 空闲状态
 *
 * @author ynfeng
 */
public class BackendIdleState extends PacketProcessStateTemplete {

    public static final BackendIdleState INSTANCE = new BackendIdleState();

    private BackendIdleState() {
    }

    @Override
    public boolean handleShortHalfPacket(Connection connection, Object attachment, int packetStartPos) throws IOException {
        return false;
    }

    @Override
    public boolean handleLongHalfPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException {
        return handleFullPacket(connection, attachment, packetStartPos, packetLen, type);
    }

    @Override
    public boolean handleFullPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException {
        MySQLBackendConnection mySQLBackendConnection = (MySQLBackendConnection) connection;
        switch (type) {
            case MySQLPacket.COM_QUERY:
                mySQLBackendConnection.getDataBuffer().packetIterator().fallback();
                mySQLBackendConnection.getProtocolStateMachine().setNextState(BackendComQueryState.INSTANCE);
                return true;
            default:
                break;
        }
        return false;
    }
}
