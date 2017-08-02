package io.mycat.mysql.state.frontend;

import io.mycat.front.MySQLFrontConnection;
import io.mycat.machine.StateMachine;
import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import io.mycat.net2.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 初始状态
 *
 * @author ynfeng
 */
public class FrontendInitialState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendInitialState.class);

    public static final FrontendInitialState INSTANCE = new FrontendInitialState();

    private FrontendInitialState() {
    }

    /**
     * 前端连接在初始状态下不做任何动作，转到至连接中状态
     */
    @Override
    public boolean handle(StateMachine stateMachine, Connection connection, Object attachment) {
        LOGGER.debug("Frontend in FrontendConnectingState");
        MySQLFrontConnection mySQLFrontConnection  = (MySQLFrontConnection)connection;
        mySQLFrontConnection.getProtocolStateMachine().setNextState(FrontendConnectingState.INSTANCE);
        return true;
    }
}
