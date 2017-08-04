package io.mycat.mysql.state.frontend;


import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.MySQLPacket;
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
    public static final FrontendIdleState INSTANCE = new FrontendIdleState();

    private FrontendIdleState() {
    }


    @Override
    public boolean handleShortHalfPacket(Connection connection, Object attachment, int packetStartPos) throws IOException {
        return false;
    }

    @Override
    public boolean handleLongHalfPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException {
        //与全包处理相同
        return handleFullPacket(connection, attachment, packetStartPos, packetLen, type);
    }

    @Override
    public boolean handleFullPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException {
        MySQLFrontConnection mySQLFrontConnection = (MySQLFrontConnection) connection;
        switch (type) {
            case MySQLPacket.COM_QUERY:
                //因为收到一个完整包，下一个状态不能迭代了，所以回退到上一个包，也就是本次的包，并立即中止迭代
                mySQLFrontConnection.getDataBuffer().packetIterator().fallback();
                mySQLFrontConnection.getProtocolStateMachine().setNextState(FrontendComQueryState.INSTANCE);
                interruptIterate();
                return true;
            case MySQLPacket.COM_QUIT:
                mySQLFrontConnection.getProtocolStateMachine().setNextState(FrontendCloseState.INSTANCE);
                return true;
            default:
                break;
        }
        return false;
    }
}
