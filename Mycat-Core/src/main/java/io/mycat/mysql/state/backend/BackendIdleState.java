package io.mycat.mysql.state.backend;


import java.io.IOException;

import io.mycat.machine.StateMachine;
import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import io.mycat.net2.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.mysql.packet.MySQLPacket;

/**
 * 空闲状态
 *
 * @author ynfeng
 */
public class BackendIdleState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendIdleState.class);

    public static final BackendIdleState INSTANCE = new BackendIdleState();

    private BackendIdleState() {
    }

    @Override
    public boolean handle(StateMachine context, Connection connction, Object attachment) throws IOException {
        MySQLBackendConnection mySQLBackendConnection = (MySQLBackendConnection) connction;
        LOGGER.debug("Backend in FrontendIdleState");
        boolean returnflag = false;
        processPacketHeader(mySQLBackendConnection);
        switch (mySQLBackendConnection.getCurrentPacketType()) {
            case MySQLPacket.COM_QUERY:
                LOGGER.debug("Backend receive a COM_QUERY in FrontendIdleState");
                mySQLBackendConnection.getProtocolStateMachine().setNextState(BackendComQueryState.INSTANCE);
                returnflag = true;
                break;
            default:
                break;
        }
        return returnflag;
    }
}
