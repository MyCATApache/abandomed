package io.mycat.mysql.state.backend;


import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.mysql.packet.HandshakePacket;
import io.mycat.net2.states.ReadWaitingState;
import io.mycat.util.CharsetUtil;

/**
 * 握手状态
 *
 * @author ynfeng
 */
public class BackendHandshakeState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendHandshakeState.class);
    public static final BackendHandshakeState INSTANCE = new BackendHandshakeState();

    private BackendHandshakeState() {
    }

    /**
     * 向服务器响应握手包
     *
     * @param mySQLConnection
     * @param attachment
     */
    @Override
    public boolean handle(MySQLConnection mySQLConnection, Object attachment) {
        MySQLBackendConnection mySQLBackendConnection = (MySQLBackendConnection) mySQLConnection;
        LOGGER.debug("Backend in FrontendHandshakeState");
        boolean returnflag;
        try {
            processHandShakePacket(mySQLBackendConnection);
            mySQLBackendConnection.authenticate();
            mySQLBackendConnection.setNextState(BackendAuthenticatingState.INSTANCE);
            mySQLBackendConnection.setWriteCompleteListener(() -> {
                mySQLBackendConnection.clearCurrentPacket();
                mySQLBackendConnection.getDataBuffer().clear();
                mySQLBackendConnection.setNextNetworkState(ReadWaitingState.INSTANCE);
            });

            returnflag = false;
        } catch (Throwable e) {
            LOGGER.warn("frontend FrontendInitialState error", e);
            mySQLBackendConnection.setNextState(BackendCloseState.INSTANCE);
            returnflag = true;
        }
        return returnflag;
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
