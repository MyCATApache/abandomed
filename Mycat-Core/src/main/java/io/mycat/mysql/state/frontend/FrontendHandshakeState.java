package io.mycat.mysql.state.frontend;


import io.mycat.front.MySQLFrontConnection;
import io.mycat.machine.StateMachine;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import io.mycat.net2.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 握手状态
 *
 * @author ynfeng
 */
public class FrontendHandshakeState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendHandshakeState.class);
    public static final FrontendHandshakeState INSTANCE = new FrontendHandshakeState();

    private FrontendHandshakeState() {
    }

    /**
     * 接收客户端的握手响应包,转至认证状态
     */
    @Override
    public boolean handle(StateMachine context, Connection connection, Object attachment) {
        LOGGER.debug("Frontend in FrontendHandshakeState");
        MySQLFrontConnection mySQLFrontConnection = (MySQLFrontConnection) connection;
        mySQLFrontConnection.getProtocolStateMachine().setNextState(FrontendAuthenticatingState.INSTANCE);
        return true;
    }

}
