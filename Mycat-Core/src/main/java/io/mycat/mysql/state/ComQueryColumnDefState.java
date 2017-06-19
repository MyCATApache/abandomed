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

    @Override
    protected void frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
        LOGGER.debug("Frontend in ComQueryColumnDefState");
        byte packageType = mySQLFrontConnection.getCurrentPacketType();
        if(packageType ==  MySQLPacket.EOF_PACKET){
            mySQLFrontConnection.setNextState(ComQueryRowState.INSTANCE);
        } else {
            //TODO 透传结束
        }
    }

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
            MySQLFrontConnection mySQLFrontConnection = mySQLBackendConnection.getMySQLFrontConnection();
            mySQLFrontConnection.setWriteDataBuffer(mySQLBackendConnection.getReadDataBuffer());
            mySQLFrontConnection.setDirectTransferParams(
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketLength());
            mySQLFrontConnection.driveState(attachment);
        } catch (IOException e) {
            LOGGER.warn("Backend ComQueryColumnDefState error", e);
            mySQLBackendConnection.changeState(CloseState.INSTANCE, "program error");
            throw new StateException(e);
        }
    }
}
