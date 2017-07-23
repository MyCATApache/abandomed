package io.mycat.backend.callback;

import java.io.IOException;

import io.mycat.buffer.MycatByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.mysql.state.ComQueryResponseState;
import io.mycat.net2.states.ReadWaitingState;
import io.mycat.net2.states.WriteWaitingState;

@Deprecated
public class BackendComQueryCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection conn, MycatByteBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {

    }
}
