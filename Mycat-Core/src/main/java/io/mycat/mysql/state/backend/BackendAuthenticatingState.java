package io.mycat.mysql.state.backend;


import io.mycat.mysql.state.PacketProcessStateTemplete;
import io.mycat.net2.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.MySQLPacket;

import java.io.IOException;

/**
 * 认证状态
 *
 * @author ynfeng
 */
public class BackendAuthenticatingState extends PacketProcessStateTemplete {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendAuthenticatingState.class);
    public static final BackendAuthenticatingState INSTANCE = new BackendAuthenticatingState();

    private BackendAuthenticatingState() {
    }

    @Override
    public boolean handleShortHalfPacket(Connection connection, Object attachment, int packetStartPos) throws IOException {
        return false;
    }

    @Override
    public boolean handleLongHalfPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException {
        return false;
    }

    @Override
    public boolean handleFullPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException {
        MySQLBackendConnection mySQLBackendConnection = (MySQLBackendConnection) connection;
        LOGGER.debug("Backend in AuthenticatingState");
        try {
            if (type == MySQLPacket.ERROR_PACKET) {
                mySQLBackendConnection.getProtocolStateMachine().setNextState(BackendCloseState.INSTANCE);
            } else if (type == MySQLPacket.OK_PACKET) {
                mySQLBackendConnection.getProtocolStateMachine().setNextState(BackendIdleState.INSTANCE);
                MySQLFrontConnection mySQLFrontConnection = mySQLBackendConnection.getMySQLFrontConnection();
                if (mySQLFrontConnection != null) {
                    mySQLFrontConnection.executePendingTask();
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("BackendAuthenticatingState error:", e);
            mySQLBackendConnection.getProtocolStateMachine().setNextState(BackendCloseState.INSTANCE);
            return true;
        }
        return false;
    }
}
