package io.mycat.mysql.state;


import io.mycat.backend.MySQLBackendConnection;
import io.mycat.backend.callback.BackendComQueryCallback;
import io.mycat.backend.callback.BackendComQueryRowCallback;
import io.mycat.engine.impl.FrontendComQueryCommandHandler;
import io.mycat.front.MySQLFrontConnection;
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

    @Override
    protected void frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
        LOGGER.debug("Frontend in ComQueryRowState");
        byte packageType = mySQLFrontConnection.getCurrentPacketType();
        if (packageType == MySQLPacket.EOF_PACKET) {
            LOGGER.debug("frontend com query response complete change to idle state");
            ConDataBuffer writeBuffer = mySQLFrontConnection.getWriteDataBuffer();
            mySQLFrontConnection.setWriteDataBuffer(mySQLFrontConnection.getShareBuffer());
            mySQLFrontConnection.enableWrite(true);
            mySQLFrontConnection.setWriteCompleteListener(() -> {
                mySQLFrontConnection.clearCurrentPacket();
                mySQLFrontConnection.getWriteDataBuffer().clear();
                mySQLFrontConnection.setWriteDataBuffer(writeBuffer);
                mySQLFrontConnection.setNextState(IdleState.INSTANCE);
            });
        } else {
            //TODO 透传结束
        }
    }

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
            MySQLFrontConnection mySQLFrontConnection = mySQLBackendConnection.getMySQLFrontConnection();
            mySQLFrontConnection.setShareBuffer(mySQLBackendConnection.getReadDataBuffer());
            mySQLFrontConnection.setDirectTransferParams(
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketLength());
            mySQLBackendConnection.clearCurrentPacket();
            mySQLFrontConnection.driveState(attachment);
        } catch (IOException e) {
            LOGGER.warn("frontend ComQueryRowState error", e);
            mySQLBackendConnection.changeState(CloseState.INSTANCE, "program error");
            throw new StateException(e);
        }
    }
}
