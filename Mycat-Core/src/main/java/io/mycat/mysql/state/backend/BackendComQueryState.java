package io.mycat.mysql.state.backend;


import java.io.IOException;

import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.net2.states.ReadWaitingState;
import io.mycat.net2.states.WriteWaitingState;

/**
 * 查询状态
 *
 * @author ynfeng
 */
public class BackendComQueryState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryState.class);
    public static final BackendComQueryState INSTANCE = new BackendComQueryState();

    private BackendComQueryState() {
    }

    @Override
    public boolean handle(MySQLConnection mySQLConnection, Object attachment) throws IOException {
        LOGGER.debug("Backend in FrontendComQueryState");
        MySQLBackendConnection conn = (MySQLBackendConnection) mySQLConnection;
        MycatByteBuffer writeBuffer = conn.getDataBuffer();
        if (conn.isPassthrough()) {    //如果处于透传模式下,需要从共享buffer 获取数据
            conn.setDataBuffer(conn.getShareBuffer());
        }
        /* 这里还没有办法区分是前端状态机驱动还是后端状态机驱动  */
        conn.setNextNetworkState(WriteWaitingState.INSTANCE)
                .networkDriverMachine();
        conn.setWriteCompleteListener(() -> {
            conn.setNextState(BackendComQueryResponseState.INSTANCE)
                    .setDataBuffer(writeBuffer)
                    .setNextNetworkState(ReadWaitingState.INSTANCE)
                    .getShareBuffer().compact();   //透传buffer 使用compact
            conn.getDataBuffer().clear();     //非透传buffer 使用 clear
        });
        return false;
    }
}
