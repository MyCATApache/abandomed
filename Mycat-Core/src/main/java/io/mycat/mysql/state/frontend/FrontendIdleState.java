package io.mycat.mysql.state.frontend;


import io.mycat.front.MySQLFrontConnection;
import io.mycat.machine.StateMachine;
import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import io.mycat.net2.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 空闲状态
 *
 * @author ynfeng
 */
public class FrontendIdleState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendIdleState.class);

    public static final FrontendIdleState INSTANCE = new FrontendIdleState();

    private FrontendIdleState() {
    }

    @Override
    public boolean handle(StateMachine context, Connection connection, Object attachment) throws IOException {
        LOGGER.debug("Frontend in FrontendIdleState");
        MySQLFrontConnection mySQLFrontConnection = (MySQLFrontConnection) connection;
        processPacketHeader(mySQLFrontConnection);
        int packetType = mySQLFrontConnection.getCurrentPacketType();
        boolean returnflag = false;
        switch (packetType) {
            case MySQLPacket.COM_QUERY:
                LOGGER.debug("Frontend receive a COM_QUERY in FrontendIdleState");
                mySQLFrontConnection.getProtocolStateMachine().setNextState(FrontendComQueryState.INSTANCE);
                returnflag = true;
                break;
            case MySQLPacket.COM_QUIT:
                LOGGER.debug("Frontend receive a COM_QUIT in FrontendIdleState");
                mySQLFrontConnection.getProtocolStateMachine().setNextState(FrontendCloseState.INSTANCE);
                returnflag = true;
            default:
                break;
        }
        return returnflag;
    }
}
