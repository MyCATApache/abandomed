package io.mycat.mysql.state.frontend;


import io.mycat.common.StateMachine;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import io.mycat.net2.Connection;
import io.mycat.net2.states.ClosingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 关闭状态
 *
 * @author ynfeng
 */
public class FrontendCloseState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendCloseState.class);

    public static final FrontendCloseState INSTANCE = new FrontendCloseState();

    private FrontendCloseState() {
    }

    @Override
    public boolean handle(StateMachine stateMachine, Connection connection, Object attachment) {
        connection.getNetworkStateMachine().setNextState(ClosingState.INSTANCE);
        return false;
    }
}
