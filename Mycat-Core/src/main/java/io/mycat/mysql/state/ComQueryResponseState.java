package io.mycat.mysql.state;


import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.backend.callback.BackendComQueryResponseCallback;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.states.NoReadAndWriteState;
import io.mycat.net2.states.ReadWaitingState;
import io.mycat.net2.states.WriteState;
import io.mycat.net2.states.WriteWaitingState;

/**
 * COM_QUERY响应
 *
 * @author ynfeng
 */
public class ComQueryResponseState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComQueryResponseState.class);
    public static final ComQueryResponseState INSTANCE = new ComQueryResponseState();

    private BackendComQueryResponseCallback backendComQueryResponseCallback = new BackendComQueryResponseCallback();

    private ComQueryResponseState() {
    }

    @Override
    protected void frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
       LOGGER.debug("Frontend in ComQueryResponseState");
//        byte packageType = mySQLFrontConnection.getCurrentPacketType();
//        if(packageType == MySQLPacket.ERROR_PACKET || packageType == MySQLPacket.OK_PACKET){
//            ConDataBuffer writeBuffer = mySQLFrontConnection.getWriteDataBuffer();
//            mySQLFrontConnection.setWriteDataBuffer(mySQLFrontConnection.getShareBuffer());
//            mySQLFrontConnection.setNextState(IdleState.INSTANCE);
//            mySQLFrontConnection.setWriteCompleteListener(() -> {
//                mySQLFrontConnection.setCurrentPacketStartPos(0);
//                mySQLFrontConnection.getWriteDataBuffer().clear();
//                mySQLFrontConnection.getReadDataBuffer().clear();
//                mySQLFrontConnection.setWriteDataBuffer(writeBuffer);
//            });
//            mySQLFrontConnection.setNextConnState(WriteWaitingState.INSTANCE);
//        } else {
//            mySQLFrontConnection.setNextState(ComQueryColumnDefState.INSTANCE);
//        }
//
//        if (attachment != null && ((Boolean) attachment)) {
//        	passThrouthOnce(mySQLFrontConnection);
//        }
    }

    @Override
    protected void backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {
        LOGGER.debug("Backend in ComQueryResponseState");
        try {
        	backendComQueryResponseCallback.handleResponse(
                    mySQLBackendConnection,
                    mySQLBackendConnection.getDataBuffer(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketStartPos(),
                    mySQLBackendConnection.getCurrentPacketLength()
            );
        	// 如果需要透传给前端,再驱动前端状态机
        	// 透传分为全包透传和半包透传的情况
//        	if(MySQLConnection.COMPLETE_PACKET == transferMode||
//        			MySQLConnection.HALF_PACKET == transferMode){
//        		mySQLBackendConnection.getMySQLFrontConnection().driveState(attachment);
//        	}
        } catch (IOException e) {
            LOGGER.warn("Backend ComQueryResponseState error", e);
            mySQLBackendConnection.changeState(CloseState.INSTANCE, "program error");
            throw new StateException(e);
        }
    }
}
