package io.mycat.mysql.state;


import io.mycat.backend.MySQLBackendConnection;
import io.mycat.backend.callback.BackendComQueryColumnDefCallback;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.MySQLPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 列定义传送状态
 *
 * @author ynfeng
 */
public class ComQueryColumnDefState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComQueryColumnDefState.class);
    public static final ComQueryColumnDefState INSTANCE = new ComQueryColumnDefState();
    private BackendComQueryColumnDefCallback backendComQueryColumnDefCallback = new BackendComQueryColumnDefCallback();


    private ComQueryColumnDefState() {
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
        LOGGER.debug("Frontend in ComQueryColumnDefState");
        byte packageType = mySQLFrontConnection.getCurrentPacketType();
        if (packageType == MySQLPacket.EOF_PACKET) {
            mySQLFrontConnection.setNextState(ComQueryRowState.INSTANCE);
        }

        if (attachment != null && ((Boolean) attachment == true)) {
            //表示后端后到的半包需要进行一次透传，压制后端的读，实现前后端流量匹配
            mySQLFrontConnection.getBackendConnection().disableRead();
            mySQLFrontConnection.setWriteCompleteListener(() -> {
                //写完成后开启后端的读
                mySQLFrontConnection.getBackendConnection().enableRead();
            });
            mySQLFrontConnection.enableWrite(true);
        }
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {
        LOGGER.debug("Backend in ComQueryColumnDefState");
        try {
            backendComQueryColumnDefCallback.handleResponse(
                    mySQLBackendConnection,
                    mySQLBackendConnection.getReadDataBuffer(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketStartPos(),
                    mySQLBackendConnection.getCurrentPacketLength()
            );
            mySQLBackendConnection.getMySQLFrontConnection().driveState(attachment);
        } catch (IOException e) {
            LOGGER.warn("Backend ComQueryColumnDefState error", e);
            mySQLBackendConnection.changeState(CloseState.INSTANCE, "program error");
            throw new StateException(e);
        }
    }
}
