package io.mycat.backend.callback;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.mysql.state.ComQueryColumnDefState;
import io.mycat.net2.ConDataBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BackendComQueryResponseCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryResponseCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection conn, ConDataBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
        if(packageType !=  MySQLPacket.ERROR_PACKET){
            conn.setNextState(ComQueryColumnDefState.INSTANCE);
        } else {
            //TODO 透传结束
        }

    }
}
