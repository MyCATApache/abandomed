package io.mycat.backend.callback;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.ServerStatus;
import io.mycat.mysql.packet.MySQLMessage;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.mysql.state.ComQueryColumnDefState;
import io.mycat.mysql.state.IdleState;
import io.mycat.net2.ConDataBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BackendComQueryResponseCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryResponseCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection mySQLBackendConnection, ConDataBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
        MySQLFrontConnection mySQLFrontConnection = mySQLBackendConnection.getMySQLFrontConnection();
        if (packageType == MySQLPacket.OK_PACKET || packageType == MySQLPacket.ERROR_PACKET) {
            //多结果集时会返回OK包，此时服务器的状态应该已经不是多结果集的状态了
            MySQLMessage mySQLMessage = new MySQLMessage(dataBuffer.getBytes(pkgStartPos, pkgLen));
            mySQLMessage.position(7);
            int serverStatus = mySQLMessage.readUB2();
            mySQLBackendConnection.setServerStatus(serverStatus);
            if ((serverStatus & ServerStatus.SERVER_STATUS_IN_TRANS) != 0) {
                LOGGER.debug("后端连接,处于事务中，不能回收!{}", mySQLBackendConnection);
            } else {
                LOGGER.debug("后端连接，不在事务中，可以回收！{}",mySQLBackendConnection);
            }
            mySQLFrontConnection.setShareBuffer(mySQLBackendConnection.getReadDataBuffer());
            mySQLFrontConnection.setDirectTransferParams(mySQLBackendConnection.getCurrentPacketType(), mySQLBackendConnection.getCurrentPacketStartPos(), mySQLBackendConnection.getCurrentPacketLength());
            mySQLBackendConnection.setNextState(IdleState.INSTANCE);
            mySQLBackendConnection.getWriteDataBuffer().clear();
            mySQLBackendConnection.clearCurrentPacket();
            mySQLBackendConnection.setDirectTransferMode(false);
        } else {
            mySQLBackendConnection.setNextState(ComQueryColumnDefState.INSTANCE);
            mySQLFrontConnection.setDirectTransferParams(
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketLength());
        }
    }
}
