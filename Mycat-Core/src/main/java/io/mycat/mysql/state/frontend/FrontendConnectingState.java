package io.mycat.mysql.state.frontend;

import io.mycat.front.MySQLFrontConnection;
import io.mycat.machine.StateMachine;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import io.mycat.net2.Connection;
import io.mycat.net2.states.ReadWaitingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * 连接中状态
 *
 * @author ynfeng
 */
public class FrontendConnectingState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendConnectingState.class);
    public static final FrontendConnectingState INSTANCE = new FrontendConnectingState();

    private FrontendConnectingState() {
    }

    /**
     * 向客户端响应握手包
     *
     */
    @Override
    public boolean handle(StateMachine context, Connection connection, Object attachment) {
        LOGGER.debug("Frontend in FrontendConnectingState");
        MySQLFrontConnection mySQLFrontConnection = (MySQLFrontConnection) connection;
        boolean returnflag = false;
        try {
            mySQLFrontConnection.sendAuthPackge();
            mySQLFrontConnection.setWriteCompleteListener(() -> {
                mySQLFrontConnection.clearCurrentPacket();
                mySQLFrontConnection.getDataBuffer().clear();
                mySQLFrontConnection.getNetworkStateMachine().setNextState(ReadWaitingState.INSTANCE);
            });
            mySQLFrontConnection.getProtocolStateMachine().setNextState(FrontendHandshakeState.INSTANCE);
            returnflag = false;
        } catch (Throwable e) {
            LOGGER.warn("frontend FrontendInitialState error", e);
            mySQLFrontConnection.getProtocolStateMachine().setNextState(FrontendCloseState.INSTANCE);
            returnflag = true;
        }
        return returnflag;
    }
}
