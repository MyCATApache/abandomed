package io.mycat.backend.callback;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.mysql.packet.HandshakePacket;
import io.mycat.mysql.state.AuthenticatingState;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.states.ReadWaitingState;
import io.mycat.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 握手响应处理器
 *
 * @author ynfeng
 */
public class HandshakeRespCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandshakeRespCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection conn, ConDataBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
        processHandShakePacket(conn, dataBuffer);
        conn.authenticate();
        conn.setNextState(AuthenticatingState.INSTANCE);
        conn.setWriteCompleteListener(()->{
        	conn.clearCurrentPacket();
        	conn.getDataBuffer().clear();
        	conn.setNextConnState(ReadWaitingState.INSTANCE);
        });
    }

    private void processHandShakePacket(MySQLBackendConnection conn, final ConDataBuffer dataBuffer) {
        HandshakePacket packet = new HandshakePacket();
        packet.read(dataBuffer);
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
