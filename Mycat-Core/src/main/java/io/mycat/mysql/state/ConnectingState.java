package io.mycat.mysql.state;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.net2.NetSystem;
import io.mycat.net2.states.ReadWaitingState;

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
            mySQLFrontConnection.setNextState(HandshakeState.INSTANCE);
            mySQLFrontConnection.sendAuthPackge();
            mySQLFrontConnection.setWriteCompleteListener(()->{
            	mySQLFrontConnection.clearCurrentPacket();
            	mySQLFrontConnection.getDataBuffer().clear();
            	mySQLFrontConnection.setNextConnState(ReadWaitingState.INSTANCE);
            });
        } catch (Throwable e) {
            LOGGER.warn("frontend InitialState error", e);
            mySQLFrontConnection.changeState(CloseState.INSTANCE, "register error");
            throw new StateException(e);
        }
    }

    /**
     * 连接后端服务器
     *
     * @param mySQLBackendConnection
     * @param attachment
     */
    @Override
    protected void backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {
        LOGGER.debug("Backend in ConnectingState");
        mySQLBackendConnection.setNextState(HandshakeState.INSTANCE);
        NetSystem.getInstance().getConnector().postConnect(mySQLBackendConnection);
    }
}
