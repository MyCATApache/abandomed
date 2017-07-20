package io.mycat.mysql.state;


import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 初始状态
 *
 * @author ynfeng
 */
public class InitialState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitialState.class);

    public static final InitialState INSTANCE = new InitialState();

    private InitialState() {
    }

    /**
     * 前端连接在初始状态下不做任何动作，转到至连接中状态
     *
     * @param mySQLFrontConnection
     * @param attachment
     */
    @Override
    protected boolean frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
        LOGGER.debug("Frontend in ConnectingState");
        mySQLFrontConnection.setNextState(ConnectingState.INSTANCE);
        return true;
    }

    /**
     * 后端连接在初始状态下不做任何动作，转到至连接中状态
     *
     * @param mySQLBackendConnection
     * @param attachment
     */
    @Override
    protected boolean backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {
        LOGGER.debug("Backend in ConnectingState");
        mySQLBackendConnection.setNextState(ConnectingState.INSTANCE);
        return true;
    }
}
