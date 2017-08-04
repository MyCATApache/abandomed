package io.mycat.mysql.state.backend;


import java.io.IOException;

import io.mycat.mysql.state.PacketProcessStateTemplete;
import io.mycat.net2.Connection;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.mysql.packet.MySQLPacket;

/**
 * COM_QUERY响应
 *
 * @author ynfeng
 */
public class BackendComQueryResponseState extends PacketProcessStateTemplete {
    public static final BackendComQueryResponseState INSTANCE = new BackendComQueryResponseState();

    private BackendComQueryResponseState() {
    }


    @Override
    public boolean handleShortHalfPacket(Connection connection, Object attachment, int packetStartPos) throws IOException {
        return false;
    }

    @Override
    public boolean handleLongHalfPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException {
        MySQLBackendConnection mySQLBackendConnection = (MySQLBackendConnection) connection;
        mySQLBackendConnection.startTransfer(mySQLBackendConnection.getMySQLFrontConnection(), mySQLBackendConnection.getDataBuffer());
        return false;
    }

    @Override
    public boolean handleFullPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException {
        MySQLBackendConnection mySQLBackendConnection = (MySQLBackendConnection) connection;
        if (type == MySQLPacket.ERROR_PACKET) {
            mySQLBackendConnection.getProtocolStateMachine().setNextState(BackendIdleState.INSTANCE);
            mySQLBackendConnection.startTransfer(mySQLBackendConnection.getMySQLFrontConnection(), mySQLBackendConnection.getDataBuffer());
            interruptIterate();
            return false;
        } else {
            mySQLBackendConnection.getProtocolStateMachine().setNextState(BackendComQueryColumnDefState.INSTANCE);
            interruptIterate();
            return true;
        }
    }
}
