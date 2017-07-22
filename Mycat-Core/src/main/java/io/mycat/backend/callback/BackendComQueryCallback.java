package io.mycat.backend.callback;

import java.io.IOException;

import io.mycat.buffer.MycatByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.mysql.state.ComQueryResponseState;
import io.mycat.net2.states.ReadWaitingState;
import io.mycat.net2.states.WriteWaitingState;

public class BackendComQueryCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection conn, MycatByteBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
    	LOGGER.debug(conn.getClass().getSimpleName() + "  in  BackendComQueryCallback");
        MycatByteBuffer writeBuffer = conn.getDataBuffer();
        conn.setDataBuffer(conn.getShareBuffer());
        /* 这里还没有办法区分是前端状态机驱动还是后端状态机驱动  */
        conn.setNextNetworkState(WriteWaitingState.INSTANCE);
        conn.networkDriverMachine();
        conn.setWriteCompleteListener(() -> {
            conn.setNextState(ComQueryResponseState.INSTANCE);
            conn.setDataBuffer(writeBuffer);
            conn.setNextNetworkState(ReadWaitingState.INSTANCE);
            conn.getShareBuffer().compact();   //透传buffer 使用compact
            conn.getDataBuffer().clear();     //非透传buffer 使用 clear
        });
    }
}
