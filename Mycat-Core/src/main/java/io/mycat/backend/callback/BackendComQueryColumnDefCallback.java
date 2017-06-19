package io.mycat.backend.callback;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.mysql.state.ComQueryRowState;
import io.mycat.net2.ConDataBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BackendComQueryColumnDefCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryColumnDefCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection mySQLBackendConnection, ConDataBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
        MySQLFrontConnection mySQLFrontConnection = mySQLBackendConnection.getMySQLFrontConnection();
        if(packageType ==  MySQLPacket.EOF_PACKET){
            mySQLBackendConnection.setNextState(ComQueryRowState.INSTANCE);
            mySQLFrontConnection.setDirectTransferParams(
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketLength());
        } else {
            mySQLFrontConnection.setWriteDataBuffer(mySQLBackendConnection.getReadDataBuffer());
            mySQLFrontConnection.setDirectTransferParams(
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketLength());
        }
    }
}
