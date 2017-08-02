package io.mycat.mysql.state.backend;


import java.io.IOException;

import io.mycat.machine.StateMachine;
import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import io.mycat.mysql.state.PacketProcessStateTemplete;
import io.mycat.net2.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.mysql.ServerStatus;
import io.mycat.mysql.packet.MySQLPacket;

/**
 * 行数据传送状态
 *
 * @author ynfeng
 */
public class BackendComQueryRowState extends PacketProcessStateTemplete {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryRowState.class);
    public static final BackendComQueryRowState INSTANCE = new BackendComQueryRowState();


    private BackendComQueryRowState() {
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
        if (type == (byte) 0xfe) {
            mySQLBackendConnection.getProtocolStateMachine().setNextState(BackendIdleState.INSTANCE);
            mySQLBackendConnection.startTransfer(mySQLBackendConnection.getMySQLFrontConnection(), mySQLBackendConnection.getDataBuffer());
            interruptIterate();
            return false;
        }
        return false;
    }
}
