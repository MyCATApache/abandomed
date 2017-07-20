package io.mycat.backend.callback;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.mysql.packet.HandshakePacket;
import io.mycat.mysql.state.AuthenticatingState;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.states.ReadWaitingState;
import io.mycat.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 握手响应处理器
 *
 * @author ynfeng
 */
public class HandshakeRespCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandshakeRespCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection conn, MycatByteBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
//        processHandShakePacket(conn, dataBuffer);
//        conn.authenticate();
//        conn.setWriteCompleteListener(()->{
//        	conn.clearCurrentPacket();
//        	conn.getDataBuffer().clear();
//        	conn.setNextNetworkState(ReadWaitingState.INSTANCE);
//        });
    }
}
