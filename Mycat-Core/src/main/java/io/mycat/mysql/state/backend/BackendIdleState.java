package io.mycat.mysql.state.backend;


import java.io.IOException;

import io.mycat.machine.StateMachine;
import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import io.mycat.mysql.state.PacketProcessStateTemplete;
import io.mycat.net2.Connection;
import io.mycat.net2.states.network.TRY_READ_RESULT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.mysql.packet.MySQLPacket;

/**
 * 空闲状态
 *
 * @author ynfeng
 */
public class BackendIdleState extends PacketProcessStateTemplete {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendIdleState.class);

    public static final BackendIdleState INSTANCE = new BackendIdleState();

    private BackendIdleState() {
    }

    @Override
    public boolean handleShortHalfPacket(Connection connection, Object attachment, int packetStartPos) throws IOException {
        return false;
    }

    @Override
    public boolean handleLongHalfPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException {
        LOGGER.debug("Backend in IdleState long half packet");
        return handleFullPacket(connection, attachment, packetStartPos, packetLen, type);
    }

    @Override
    public boolean handleFullPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException {
        MySQLBackendConnection mySQLBackendConnection = (MySQLBackendConnection) connection;
        LOGGER.debug("Backend in IdleState");
        switch (type) {
            case MySQLPacket.COM_QUERY:
                LOGGER.debug("Backend receive a COM_QUERY in IdleState");
                mySQLBackendConnection.getDataBuffer().packetIterator().fallback();
                mySQLBackendConnection.getProtocolStateMachine().setNextState(BackendComQueryState.INSTANCE);
                return true;
            default:
                break;
        }
        return false;
    }
}
