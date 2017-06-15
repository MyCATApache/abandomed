package io.mycat.mysql.state;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * 连接中状态
 *
 * @author ynfeng
 */
public class ConnectingState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectingState.class);
    public static final ConnectingState INSTANCE = new ConnectingState();

    private ConnectingState() {
    }

    /**
     * 向客户端响应握手包
     *
     * @param mySQLFrontConnection
     * @param attachment
     */
    @Override
    protected void frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
        LOGGER.debug("Frontend in ConnectingState");
        try {
            mySQLFrontConnection.sendAuthPackge();
            mySQLFrontConnection.setNextState(HandshakeState.INSTANCE);
        } catch (IOException e) {
            LOGGER.warn("frontend InitialState error", e);
            mySQLFrontConnection.changeState(CloseState.INSTANCE, "register error");
            throw new StateException(e);
        }
    }

    @Override
    protected void backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {

    }
}
