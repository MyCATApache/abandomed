package io.mycat.backend.callback;

import java.io.IOException;

import io.mycat.buffer.MycatByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;

@Deprecated
public class BackendComQueryRowCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryRowCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection mySQLBackendConnection, MycatByteBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {

    }
}
