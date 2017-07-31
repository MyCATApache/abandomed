package io.mycat.mysql.state.backend;


import io.mycat.machine.StateMachine;
import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import io.mycat.net2.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.net2.states.ReadWaitingState;

/**
 * 认证状态
 *
 * @author ynfeng
 */
public class BackendAuthenticatingState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendAuthenticatingState.class);
    public static final BackendAuthenticatingState INSTANCE = new BackendAuthenticatingState();

    private static final byte[] AUTH_OK = new byte[]{7, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0};

    private BackendAuthenticatingState() {
    }

    /**
     * @param attachment
     */
    @Override
    public boolean handle(StateMachine context, Connection connection, Object attachment) {
        MySQLBackendConnection mySQLBackendConnection = (MySQLBackendConnection) connection;
        LOGGER.debug("Backend in FrontendAuthenticatingState");
        boolean returnflag = false;
        try {
            processPacketHeader(mySQLBackendConnection);
            byte packageType = mySQLBackendConnection.getCurrentPacketType();
            if (packageType == MySQLPacket.ERROR_PACKET) {
                mySQLBackendConnection.getProtocolStateMachine().setNextState(BackendCloseState.INSTANCE);
                returnflag = true;   //当前状态机进入 FrontendCloseState 状态
            } else if (packageType == MySQLPacket.OK_PACKET) {
                mySQLBackendConnection.getDataBuffer().clear();
                mySQLBackendConnection.getProtocolStateMachine().setNextState(BackendIdleState.INSTANCE);
                MySQLFrontConnection mySQLFrontConnection = mySQLBackendConnection.getMySQLFrontConnection();
                if (mySQLFrontConnection != null) {
                    mySQLFrontConnection.executePendingTask();
                } else {
                    mySQLBackendConnection.clearCurrentPacket();
                    mySQLBackendConnection.getNetworkStateMachine().setNextState(ReadWaitingState.INSTANCE);
                }
                returnflag = false;  //当前状态机退出
            }
        } catch (Throwable e) {
            LOGGER.warn("Backend FrontendAuthenticatingState error:", e);
            mySQLBackendConnection.getProtocolStateMachine().setNextState(BackendCloseState.INSTANCE);
            returnflag = true;
        }
        return returnflag;
    }
}
