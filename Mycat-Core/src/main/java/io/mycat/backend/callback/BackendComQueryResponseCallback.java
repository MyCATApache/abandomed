package io.mycat.backend.callback;

import java.io.IOException;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.buffer.MycatByteBuffer;

@Deprecated
public class BackendComQueryResponseCallback extends ResponseCallbackAdapter {

    @Override
    public void handleResponse(MySQLBackendConnection mySQLBackendConnection, MycatByteBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
    	
    }
}
