package io.mycat.mysql.state;


import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.MySQLPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 空闲状态
 *
 * @author ynfeng
 */
public class IdleState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdleState.class);

    public static final IdleState INSTANCE = new IdleState();

    private IdleState() {
    }

    @Override
    protected void frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
        LOGGER.debug("Frontend in IdleState");
        int packetType = mySQLFrontConnection.getCurrentPacketType();
        switch (packetType) {
            case MySQLPacket.COM_QUERY:
                LOGGER.debug("Frontend receive a COM_QUERY in IdleState");
                mySQLFrontConnection.changeState(ComQueryState.INSTANCE, attachment);
                break;
            case MySQLPacket.COM_QUIT:
            	LOGGER.debug("Frontend receive a COM_QUIT in IdleState");
            	mySQLFrontConnection.changeState(CloseState.INSTANCE, attachment);
            default:
                break;
        }
    }

    @Override
    protected void backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {
        LOGGER.debug("Backend in IdleState");
        int packetType = mySQLBackendConnection.getCurrentPacketType();
        switch (packetType) {
            case MySQLPacket.COM_QUERY:
                LOGGER.debug("Backend receive a COM_QUERY in IdleState");
                mySQLBackendConnection.changeState(ComQueryState.INSTANCE, attachment);
                break;
            default:
                break;
        }
    }
}
