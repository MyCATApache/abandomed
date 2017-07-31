package io.mycat.mysql.state.frontend;


import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
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
     *
     * @param mySQLConnection
     * @param attachment
     */
    @Override
    public boolean handle(MySQLConnection mySQLConnection, Object attachment) {
        LOGGER.debug("Frontend in FrontendHandshakeState");
        mySQLConnection.setNextState(FrontendAuthenticatingState.INSTANCE);
        return true;
    }

}
