package io.mycat.mysql.state.frontend;

import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import io.mycat.net2.states.ReadWaitingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


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
     * @param mySQLConnection
     * @param attachment
     */
    @Override
    public boolean handle(MySQLConnection mySQLConnection, Object attachment) throws IOException {
        LOGGER.debug("Frontend in FrontendConnectingState");
        MySQLFrontConnection mySQLFrontConnection = (MySQLFrontConnection) mySQLConnection;
        boolean returnflag = false;
        try {
            mySQLFrontConnection.sendAuthPackge();
            mySQLFrontConnection.setWriteCompleteListener(() -> {
                mySQLFrontConnection.clearCurrentPacket();
                mySQLFrontConnection.getDataBuffer().clear();
                mySQLFrontConnection.setNextNetworkState(ReadWaitingState.INSTANCE);
            });
            mySQLFrontConnection.setNextState(FrontendHandshakeState.INSTANCE);
            returnflag = false;
        } catch (Throwable e) {
            LOGGER.warn("frontend FrontendInitialState error", e);
            mySQLFrontConnection.setNextState(FrontendCloseState.INSTANCE);
            returnflag = true;
        }
        return returnflag;
    }
}
