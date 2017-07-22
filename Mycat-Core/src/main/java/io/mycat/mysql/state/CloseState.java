package io.mycat.mysql.state;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.net2.states.ClosingState;

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
    protected boolean frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
        LOGGER.debug("Frontend in CloseState");
        mySQLFrontConnection.setNextNetworkState(ClosingState.INSTANCE);
        return false;
    }

    @Override
    protected boolean backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {
    	return false;
    }
}
