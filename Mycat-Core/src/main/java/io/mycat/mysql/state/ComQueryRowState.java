package io.mycat.mysql.state;


import io.mycat.backend.MySQLBackendConnection;
import io.mycat.backend.callback.BackendComQueryCallback;
import io.mycat.backend.callback.BackendComQueryRowCallback;
import io.mycat.engine.impl.FrontendComQueryCommandHandler;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.ServerStatus;
import io.mycat.mysql.packet.MySQLMessage;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.net2.ConDataBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 行数据传送状态
 *
 * @author ynfeng
 */
public class ComQueryRowState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComQueryRowState.class);
    public static final ComQueryRowState INSTANCE = new ComQueryRowState();
    private BackendComQueryRowCallback backendComQueryRowCallback = new BackendComQueryRowCallback();


    private ComQueryRowState() {
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
        LOGGER.debug("Frontend in ComQueryRowState");
        MySQLBackendConnection mySQLBackendConnection = mySQLFrontConnection.getBackendConnection();
        byte packageType = mySQLFrontConnection.getCurrentPacketType();
        if (packageType == MySQLPacket.EOF_PACKET) {
            if ((mySQLBackendConnection.getServerStatus() & ServerStatus.SERVER_MORE_RESULTS_EXISTS) == 0) {
                LOGGER.debug("frontend com query response complete change to idle state");
                ConDataBuffer writeBuffer = mySQLFrontConnection.getWriteDataBuffer();
                mySQLFrontConnection.setWriteDataBuffer(mySQLFrontConnection.getShareBuffer());
                mySQLFrontConnection.enableWrite(true);
                mySQLFrontConnection.setWriteCompleteListener(() -> {
                    mySQLFrontConnection.clearCurrentPacket();
                    mySQLFrontConnection.setDirectTransferMode(false);
                    mySQLFrontConnection.getWriteDataBuffer().clear();
                    mySQLFrontConnection.getReadDataBuffer().clear();
                    mySQLFrontConnection.setWriteDataBuffer(writeBuffer);
                    mySQLFrontConnection.clearCurrentPacket();
                    mySQLFrontConnection.setNextState(IdleState.INSTANCE);
                });
            } else {
                LOGGER.debug("frontend com query response state have multi result");
                mySQLFrontConnection.setNextState(ComQueryResponseState.INSTANCE);
                mySQLFrontConnection.getBackendConnection().disableRead();
                mySQLFrontConnection.setWriteCompleteListener(() -> {
                    //写完成后开启后端的读
                    mySQLFrontConnection.getBackendConnection().enableRead();
                });
                mySQLFrontConnection.enableWrite(true);
            }
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
        LOGGER.debug("Backend in ComQueryRowState");
        try {
            backendComQueryRowCallback.handleResponse(
                    mySQLBackendConnection,
                    mySQLBackendConnection.getReadDataBuffer(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketStartPos(),
                    mySQLBackendConnection.getCurrentPacketLength()
            );
            mySQLBackendConnection.getMySQLFrontConnection().driveState(attachment);
        } catch (IOException e) {
            LOGGER.warn("frontend ComQueryRowState error", e);
            mySQLBackendConnection.changeState(CloseState.INSTANCE, "program error");
            throw new StateException(e);
        }
    }
}
