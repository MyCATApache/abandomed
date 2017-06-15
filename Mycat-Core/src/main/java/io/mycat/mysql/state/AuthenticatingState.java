package io.mycat.mysql.state;


import io.mycat.backend.MySQLBackendConnection;
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

    private AuthenticatingState() {
    }

    @Override
    protected void frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
        try {
            LOGGER.debug("Frontend in AuthenticatingState");
            loginCmdHandler.processCmd(
                    mySQLFrontConnection,
                    mySQLFrontConnection.getReadDataBuffer(),
                    mySQLFrontConnection.getCurrentPacketType(),
                    mySQLFrontConnection.getCurrentPacketStartPos(),
                    mySQLFrontConnection.getCurrentPacketLength()
            );
        } catch (IOException e) {
            LOGGER.warn("Frontend ConnectingState error:", e);
            mySQLFrontConnection.changeState(CloseState.INSTANCE, "program err:" + e.toString());
            throw new StateException(e);
        }
    }

    @Override
    protected void backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {

    }
}
