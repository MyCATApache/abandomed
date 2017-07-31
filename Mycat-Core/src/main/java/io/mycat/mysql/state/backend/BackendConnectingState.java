package io.mycat.mysql.state.backend;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import io.mycat.net2.NetSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * 连接中状态
 *
 * @author ynfeng
 */
public class BackendConnectingState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendConnectingState.class);
    public static final BackendConnectingState INSTANCE = new BackendConnectingState();

    private BackendConnectingState() {
    }

    /**
     * 连接后端服务器
     *
     * @param mySQLConnection
     * @param attachment
     */
    @Override
    public boolean handle(MySQLConnection mySQLConnection, Object attachment) throws IOException {
        LOGGER.debug("Backend in FrontendConnectingState");
        MySQLBackendConnection mySQLBackendConnection = (MySQLBackendConnection) mySQLConnection;
        mySQLBackendConnection.setNextState(BackendHandshakeState.INSTANCE);
        NetSystem.getInstance().getConnector().postConnect(mySQLBackendConnection);
        return false;
    }
}
