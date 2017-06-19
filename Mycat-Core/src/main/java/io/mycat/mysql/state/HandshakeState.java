package io.mycat.mysql.state;


import io.mycat.backend.MySQLBackendConnection;
import io.mycat.backend.callback.HandshakeRespCallback;
import io.mycat.front.CheckUserLoginResponseCallback;
import io.mycat.front.MySQLFrontConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 握手状态
 *
 * @author ynfeng
 */
public class HandshakeState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandshakeState.class);
    public static final HandshakeState INSTANCE = new HandshakeState();
    private final HandshakeRespCallback handshakeRespCallback = new HandshakeRespCallback();

    private HandshakeState() {
    }

    /**
     * 接收客户端的握手响应包,转至认证状态
     *
     * @param mySQLFrontConnection
     * @param attachment
     */
    @Override
    protected void frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
        LOGGER.debug("Frontend in HandshakeState");
        mySQLFrontConnection.changeState(AuthenticatingState.INSTANCE, attachment);
    }

    /**
     * 向服务器响应握手包
     *
     * @param mySQLBackendConnection
     * @param attachment
     */
    @Override
    protected void backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {
        LOGGER.debug("Backend in HandshakeState");
        try {
            handshakeRespCallback.handleResponse(
                    mySQLBackendConnection,
                    mySQLBackendConnection.getReadDataBuffer(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketStartPos(),
                    mySQLBackendConnection.getCurrentPacketLength()
            );
        } catch (Throwable e) {
            LOGGER.warn("frontend InitialState error", e);
            mySQLBackendConnection.changeState(CloseState.INSTANCE, "auth error.");
            throw new StateException(e);
        }
    }
}
