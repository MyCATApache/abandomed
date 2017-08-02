package io.mycat.backend.callback;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.buffer.MycatByteBuffer;

@Deprecated
public class BackendComQueryColumnDefCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryColumnDefCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection mySQLBackendConnection, MycatByteBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
    }
}
