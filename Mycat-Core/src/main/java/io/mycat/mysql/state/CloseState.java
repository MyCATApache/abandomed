package io.mycat.mysql.state;


import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 关闭状态
 *
 * @author ynfeng
 */
public class CloseState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloseState.class);

    public static final CloseState INSTANCE = new CloseState();

    private CloseState() {
    }

    @Override
    protected void frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
        LOGGER.debug("Frontend in CloseState");
        mySQLFrontConnection.close((String)attachment);
    }

    @Override
    protected void backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {

    }
}
