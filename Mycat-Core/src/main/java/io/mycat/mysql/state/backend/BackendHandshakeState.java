package io.mycat.mysql.state.backend;


import io.mycat.mysql.state.PacketProcessStateTemplete;
import io.mycat.net2.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.mysql.packet.HandshakePacket;
import io.mycat.util.CharsetUtil;

import java.io.IOException;

/**
 * 握手状态
 *
 * @author ynfeng
 */
public class BackendHandshakeState extends PacketProcessStateTemplete {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendHandshakeState.class);
    public static final BackendHandshakeState INSTANCE = new BackendHandshakeState();

    private BackendHandshakeState() {
    }

    @Override
    public boolean handleShortHalfPacket(Connection connection, Object attachment, int packetStartPos) throws IOException {
        return false;
    }

    @Override
    public boolean handleLongHalfPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException {
        return false;
    }

    @Override
    public boolean handleFullPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException {
        MySQLBackendConnection mySQLBackendConnection = (MySQLBackendConnection) connection;
        LOGGER.debug("Backend in HandshakeState");
        try {
            processHandShakePacket(mySQLBackendConnection);
            mySQLBackendConnection.authenticate();
            mySQLBackendConnection.getProtocolStateMachine().setNextState(BackendAuthenticatingState.INSTANCE);
            interruptIterate();
            return false;
        } catch (Throwable e) {
            LOGGER.warn("Backend InitialState error", e);
            mySQLBackendConnection.getProtocolStateMachine().setNextState(BackendCloseState.INSTANCE);
            return true;
        }
    }

    private void processHandShakePacket(MySQLBackendConnection conn) {
        HandshakePacket packet = new HandshakePacket();
        packet.read(conn.getDataBuffer());
        conn.getDataBuffer().clear();
        conn.setHandshake(packet);
        // 设置字符集编码
        int charsetIndex = (packet.serverCharsetIndex & 0xff);
        String charset = CharsetUtil.getCharset(charsetIndex);
        if (charset != null) {
            conn.setCharset(charsetIndex, charset);
        } else {
            String errmsg = "Unknown charsetIndex:" + charsetIndex + " of " + conn;
            LOGGER.warn(errmsg);
            return;
        }
    }
}
