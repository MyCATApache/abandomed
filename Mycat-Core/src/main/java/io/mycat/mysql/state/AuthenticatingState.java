package io.mycat.mysql.state;


import io.mycat.backend.MySQLBackendConnection;
import io.mycat.backend.callback.AuthenticateRespCallback;
import io.mycat.front.CheckUserLoginResponseCallback;
import io.mycat.front.MySQLFrontConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 认证状态
 *
 * @author ynfeng
 */
public class AuthenticatingState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatingState.class);
    public static final AuthenticatingState INSTANCE = new AuthenticatingState();
    private final CheckUserLoginResponseCallback loginCmdHandler = new CheckUserLoginResponseCallback();
    private final AuthenticateRespCallback authenticateRespCallback = new AuthenticateRespCallback();

    private AuthenticatingState() {
    }

    /**
     * 对握手响应包进行认证
     *
     * @param mySQLFrontConnection
     * @param attachment
     */
    @Override
    protected void frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
        try {
            LOGGER.debug("Frontend in AuthenticatingState");
            loginCmdHandler.processCmd(
                    mySQLFrontConnection,
                    mySQLFrontConnection.getDataBuffer(),
                    mySQLFrontConnection.getCurrentPacketType(),
                    mySQLFrontConnection.getCurrentPacketStartPos(),
                    mySQLFrontConnection.getCurrentPacketLength()
            );
        } catch (Throwable e) {
            LOGGER.warn("Frontend AuthenticatingState error:", e);
            mySQLFrontConnection.changeState(CloseState.INSTANCE, "program err:" + e.toString());
            throw new StateException(e);
        }
    }

    /**
     *
     *
     * @param mySQLBackendConnection
     * @param attachment
     */
    @Override
    protected void backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {
        LOGGER.debug("Backend in AuthenticatingState");
        try {
            authenticateRespCallback.handleResponse(
                    mySQLBackendConnection,
                    mySQLBackendConnection.getDataBuffer(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketStartPos(),
                    mySQLBackendConnection.getCurrentPacketLength()
            );
        } catch (Throwable e) {
            LOGGER.warn("Backend AuthenticatingState error:", e);
            mySQLBackendConnection.changeState(CloseState.INSTANCE, "program err:" + e.toString());
            throw new StateException(e);
        }
    }
}
