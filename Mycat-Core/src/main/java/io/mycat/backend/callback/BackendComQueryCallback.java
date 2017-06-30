package io.mycat.backend.callback;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.mysql.state.ComQueryResponseState;
import io.mycat.net2.ConDataBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BackendComQueryCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection conn, ConDataBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
        ConDataBuffer writeBuffer = conn.getWriteDataBuffer();
        conn.setWriteDataBuffer(conn.getShareBuffer());
        conn.enableWrite(true);
        conn.setWriteCompleteListener(() -> {
            conn.setNextState(ComQueryResponseState.INSTANCE);
            conn.setWriteDataBuffer(writeBuffer);
            conn.enableRead();
        });

    }
}
