package io.mycat.mysql.state;


import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.backend.callback.BackendComQueryRowCallback;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.ServerStatus;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.states.NoReadAndWriteState;
import io.mycat.net2.states.ReadWaitingState;
import io.mycat.net2.states.WriteWaitingState;

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
//        MySQLBackendConnection mySQLBackendConnection = mySQLFrontConnection.getBackendConnection();
//        byte packageType = mySQLFrontConnection.getCurrentPacketType();
//        if (packageType == MySQLPacket.EOF_PACKET) {
//            if ((mySQLBackendConnection.getServerStatus() & ServerStatus.SERVER_MORE_RESULTS_EXISTS) == 0) {
//                LOGGER.debug("frontend com query response complete change to idle state");
//                ConDataBuffer writeBuffer = mySQLFrontConnection.getWriteDataBuffer();
//                mySQLFrontConnection.setWriteDataBuffer(mySQLFrontConnection.getShareBuffer());
//                mySQLFrontConnection.connDriverMachine(WriteWaitingState.INSTANCE);
//                mySQLFrontConnection.setNextConnState(ReadWaitingState.INSTANCE);
//                mySQLFrontConnection.setWriteCompleteListener(() -> {
//                    mySQLFrontConnection.clearCurrentPacket();
////                    mySQLFrontConnection.setDirectTransferMode(false);
//                    mySQLFrontConnection.getWriteDataBuffer().clear();
//                    mySQLFrontConnection.getReadDataBuffer().clear();
//                    mySQLFrontConnection.setWriteDataBuffer(writeBuffer);
//                    mySQLFrontConnection.setNextState(IdleState.INSTANCE);
//                });
//            }
//            else {
//                LOGGER.debug("frontend com query response state have multi result");
//                mySQLFrontConnection.setNextState(ComQueryResponseState.INSTANCE);
//
//            }
//        }
    }

    @Override
    protected void backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {
    	LOGGER.debug("Backend in ComQueryRowState");
        try {
            backendComQueryRowCallback.handleResponse(
                    mySQLBackendConnection,
                    mySQLBackendConnection.getDataBuffer(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketStartPos(),
                    mySQLBackendConnection.getCurrentPacketLength()
            );
//            mySQLBackendConnection.getMySQLFrontConnection().driveState(attachment);
        } catch (IOException e) {
            LOGGER.warn("frontend ComQueryRowState error", e);
            mySQLBackendConnection.changeState(CloseState.INSTANCE, "program error");
            throw new StateException(e);
        }
    }
}
