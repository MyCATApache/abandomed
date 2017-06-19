package io.mycat.backend.callback;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.mysql.state.CloseState;
import io.mycat.mysql.state.IdleState;
import io.mycat.net2.ConDataBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 认证响应处理器
 *
 * @author ynfeng
 */
public class AuthenticateRespCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticateRespCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection conn, ConDataBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
        if (packageType == MySQLPacket.ERROR_PACKET) {
            conn.changeState(CloseState.INSTANCE, null);
        } else if (packageType == MySQLPacket.OK_PACKET) {
            conn.getReadDataBuffer().clear();
            conn.getWriteDataBuffer().clear();
            conn.clearCurrentPacket();
            conn.setNextState(IdleState.INSTANCE);
            MySQLFrontConnection mySQLFrontConnection = conn.getMySQLFrontConnection();
            if (mySQLFrontConnection != null) {
                mySQLFrontConnection.executePendingTask();
            }
        }

    }
}
