package io.mycat.backend.callback;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.bigmem.util.ByteUtil;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.ServerStatus;
import io.mycat.mysql.packet.MySQLMessage;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.mysql.state.ComQueryColumnDefState;
import io.mycat.mysql.state.ComQueryResponseState;
import io.mycat.mysql.state.IdleState;
import io.mycat.net2.ConDataBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BackendComQueryRowCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryRowCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection mySQLBackendConnection, ConDataBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
        MySQLFrontConnection mySQLFrontConnection = mySQLBackendConnection.getMySQLFrontConnection();
        if (packageType == MySQLPacket.EOF_PACKET) {
            MySQLMessage mySQLMessage = new MySQLMessage(dataBuffer.getBytes(pkgStartPos, pkgLen));
            mySQLMessage.position(7);
            int serverStatus = mySQLMessage.readUB2();
            mySQLBackendConnection.setServerStatus(serverStatus);
            //检查后面还有没有结果集
            if ((mySQLBackendConnection.getServerStatus() & ServerStatus.SERVER_MORE_RESULTS_EXISTS) == 0) {
                LOGGER.debug("backend com query response complete change to idle state");
                mySQLFrontConnection.setShareBuffer(mySQLBackendConnection.getReadDataBuffer());
                mySQLFrontConnection.setDirectTransferParams(
                        mySQLBackendConnection.getCurrentPacketType(),
                        mySQLBackendConnection.getCurrentPacketStartPos(),
                        mySQLBackendConnection.getCurrentPacketLength());

                mySQLBackendConnection.setDirectTransferMode(false);
                mySQLBackendConnection.clearCurrentPacket();
                mySQLBackendConnection.getWriteDataBuffer().clear();
                mySQLBackendConnection.setNextState(IdleState.INSTANCE);
            } else {
                LOGGER.debug("backend com query response state have multi result");
                mySQLBackendConnection.setNextState(ComQueryResponseState.INSTANCE);
                mySQLFrontConnection.setDirectTransferParams(
                        mySQLBackendConnection.getCurrentPacketType(),
                        mySQLBackendConnection.getCurrentPacketStartPos(),
                        mySQLBackendConnection.getCurrentPacketLength());
                mySQLBackendConnection.setCurrentPacketStartPos(pkgStartPos + pkgLen);
            }
        } else {
            mySQLFrontConnection.setDirectTransferParams(
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketLength());
        }
    }
}
