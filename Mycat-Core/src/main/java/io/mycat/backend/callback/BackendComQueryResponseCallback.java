package io.mycat.backend.callback;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.mysql.ServerStatus;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.mysql.state.ComQueryColumnDefState;
import io.mycat.mysql.state.IdleState;

@Deprecated
public class BackendComQueryResponseCallback extends ResponseCallbackAdapter {

    @Override
    public void handleResponse(MySQLBackendConnection mySQLBackendConnection, MycatByteBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
    	
    }
}
