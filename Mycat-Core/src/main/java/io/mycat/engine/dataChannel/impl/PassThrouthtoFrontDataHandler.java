package io.mycat.engine.dataChannel.impl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.engine.dataChannel.DataHandler;
import io.mycat.engine.dataChannel.TransferMode;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.MySQLConnection;
import io.mycat.net2.states.NoReadAndWriteState;
import io.mycat.net2.states.ReadWaitingState;
import io.mycat.net2.states.WriteWaitingState;

/**
 * 透传到前端数据处理器
 *
 * @author yanjunli
 */
public class PassThrouthtoFrontDataHandler implements DataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PassThrouthtoFrontDataHandler.class);

    public static final PassThrouthtoFrontDataHandler INSTANCE = new PassThrouthtoFrontDataHandler();

    private PassThrouthtoFrontDataHandler() {
    }

    /*
     * 透传时,进行前后端状态同步
     */
    @Override
    public void transfer(MySQLConnection in, boolean isTransferLastPacket, boolean transferFinish, boolean isAllFinish) throws IOException {

        LOGGER.debug("current in  PassThrouthtoFrontDataHandler ");

        MySQLBackendConnection backendConn = (MySQLBackendConnection) in;
        MycatByteBuffer backendDataBuffer = backendConn.getDataBuffer();

        MySQLFrontConnection frontConn = backendConn.getMySQLFrontConnection();

        //透传到前端,注册前端可写事件,取消后端读写事件 实现流量匹配
        backendConn.getNetworkStateMachine().setNextState(NoReadAndWriteState.INSTANCE);
        frontConn.getNetworkStateMachine().setNextState(WriteWaitingState.INSTANCE)
                .driveState();

        //同步前后端状态
        frontConn.getProtocolStateMachine().setNextState(backendConn.getProtocolStateMachine().getNextState());
//		if(frontConn.getNextNetworkState() == null ) {
//			frontConn.setNextNetworkState(NoReadAndWriteState.INSTANCE);
//		}

        //将buffer 后端buffer 共享给前端
        MycatByteBuffer frontdatabuffer = frontConn.getDataBuffer();
        frontConn.setDataBuffer(backendDataBuffer);

        //透传完成后,的后置处理
        frontConn.setWriteCompleteListener(() -> {
            frontdatabuffer.clear();
            frontConn.getDataBuffer().compact();
            frontConn.setDataBuffer(frontdatabuffer);
            frontConn.clearCurrentPacket();
            //  这里需要特殊处理, 如果是短半包,需要清空当前包指针
            if (TransferMode.SHORT_HALF_PACKET.equals(backendConn.getDirectTransferMode())) {
                backendConn.clearCurrentPacket();
            }
            if (transferFinish) { //当前透传方向 传输完成
                frontConn.getNetworkStateMachine().setNextState(ReadWaitingState.INSTANCE);
                frontConn.setDirectTransferMode(TransferMode.NONE);
                backendConn.setDirectTransferMode(TransferMode.NONE);
                backendConn.getNetworkStateMachine().setNextState(ReadWaitingState.INSTANCE);
                backendConn.getNetworkStateMachine().driveState();
            } else {
                frontConn.getNetworkStateMachine().setNextState(NoReadAndWriteState.INSTANCE);
                backendConn.getNetworkStateMachine().setNextState(ReadWaitingState.INSTANCE);
                backendConn.getNetworkStateMachine().driveState();
            }

            if (isAllFinish) {  //透传全部完成
                frontConn.setPassthrough(false);
                backendConn.setPassthrough(false);
            }
        });
    }
}
