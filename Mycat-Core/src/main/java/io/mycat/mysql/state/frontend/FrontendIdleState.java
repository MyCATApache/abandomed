package io.mycat.mysql.state.frontend;


import io.mycat.front.MySQLFrontConnection;
import io.mycat.machine.StateMachine;
import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import io.mycat.mysql.state.PacketProcessStateTemplete;
import io.mycat.net2.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 空闲状态
 *
 * @author ynfeng
 */
public class FrontendIdleState extends PacketProcessStateTemplete {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendIdleState.class);

    public static final FrontendIdleState INSTANCE = new FrontendIdleState();

    private FrontendIdleState() {
    }


    @Override
    public boolean stopProcess() {
        return true;
    }

    @Override
    public boolean handleShortHalfPacket(Connection connection, Object attachment, int packetStartPos) {
        return false;
    }

    @Override
    public boolean handleLongHalfPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) {
        return false;
    }

    @Override
    public boolean handleFullPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) {
        LOGGER.debug("Frontend in FrontendIdleState");
        MySQLFrontConnection mySQLFrontConnection = (MySQLFrontConnection) connection;
        switch (type) {
            case MySQLPacket.COM_QUERY:
                LOGGER.debug("Frontend receive a COM_QUERY in FrontendIdleState");
                mySQLFrontConnection.getProtocolStateMachine().setNextState(FrontendComQueryState.INSTANCE);
                return true;
            case MySQLPacket.COM_QUIT:
                LOGGER.debug("Frontend receive a COM_QUIT in FrontendIdleState");
                mySQLFrontConnection.getProtocolStateMachine().setNextState(FrontendCloseState.INSTANCE);
                return true;
            default:
                break;
        }
        return false;
    }
}
