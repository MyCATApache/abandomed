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

public class BackendComQueryResponseCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryResponseCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection mySQLBackendConnection, ConDataBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
        MySQLFrontConnection mySQLFrontConnection = mySQLBackendConnection.getMySQLFrontConnection();
        if (packageType != MySQLPacket.ERROR_PACKET) {
            mySQLBackendConnection.setNextState(ComQueryColumnDefState.INSTANCE);
            mySQLFrontConnection.setDirectTransferParams(
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketLength());

        } else {
            mySQLFrontConnection.setShareBuffer(mySQLBackendConnection.getReadDataBuffer());
            mySQLFrontConnection.setDirectTransferParams(mySQLBackendConnection.getCurrentPacketType(), mySQLBackendConnection.getCurrentPacketStartPos(), mySQLBackendConnection.getCurrentPacketLength());
            mySQLBackendConnection.setNextState(IdleState.INSTANCE);
            mySQLBackendConnection.getWriteDataBuffer().clear();
            mySQLBackendConnection.clearCurrentPacket();
            mySQLBackendConnection.setDirectTransferMode(false);
        }
    }
}
