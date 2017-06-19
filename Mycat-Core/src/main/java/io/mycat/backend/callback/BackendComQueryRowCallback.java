package io.mycat.backend.callback;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.mysql.state.ComQueryColumnDefState;
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
            LOGGER.debug("backend com query response complete change to idle state");
            mySQLFrontConnection.setShareBuffer(mySQLBackendConnection.getReadDataBuffer());
            mySQLFrontConnection.setDirectTransferParams(
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketLength());

            mySQLBackendConnection.setDirectTransferMode(false);
            mySQLBackendConnection.clearCurrentPacket();
            mySQLBackendConnection.getWriteDataBuffer().clear();
            mySQLBackendConnection.setNextState(IdleState.INSTANCE);
        } else {
            mySQLFrontConnection.setDirectTransferParams(
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketLength());
        }
    }
}
