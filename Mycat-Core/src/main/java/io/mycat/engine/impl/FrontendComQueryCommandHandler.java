package io.mycat.engine.impl;

import java.io.IOException;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.engine.dataChannel.TransferMode;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.state.ComQueryResponseState;
import io.mycat.net2.states.NoReadAndWriteState;

/**
 * 前端查询命令响应
 * <p>
 * 此类未根据sql类型做特殊处理，后面可根据实际情况，比如主从，获取对应的后端连接
 *
 * @author ynfeng
 */
public class FrontendComQueryCommandHandler extends AbstractComQueryCommandHandler {

    @Override
    public void processCmd(MySQLFrontConnection frontCon, MycatByteBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
        frontCon.setNextState(ComQueryResponseState.INSTANCE);
        MySQLBackendConnection backendConnection = frontCon.getBackendConnection();
        if (backendConnection == null) {
            backendConnection = getBackendFrontConnection(frontCon);
            MySQLBackendConnection finalBackendConnection = backendConnection;
            frontCon.addTodoTask(() -> {
                
                //开启透传模式
                finalBackendConnection.setDirectTransferMode(TransferMode.COMPLETE_PACKET); //整包透传
                finalBackendConnection.setCurrentPacketLength(frontCon.getCurrentPacketLength());
                finalBackendConnection.setCurrentPacketStartPos(frontCon.getCurrentPacketStartPos());
                finalBackendConnection.setCurrentPacketType(frontCon.getCurrentPacketType());

                finalBackendConnection.setShareBuffer(dataBuffer);//
                //TODO ???
//                finalBackendConnection.getShareBuffer().setLastWritePos(0);
//                finalBackendConnection.getShareBuffer().readIndex(frontCon.getCurrentPacketLength());
                finalBackendConnection.driveState();
            });
        } else {
        	
        	//开启透传模式
        	backendConnection.setDirectTransferMode(TransferMode.COMPLETE_PACKET); //整包透传
            backendConnection.setCurrentPacketLength(frontCon.getCurrentPacketLength());
            backendConnection.setCurrentPacketStartPos(frontCon.getCurrentPacketStartPos());
            backendConnection.setCurrentPacketType(frontCon.getCurrentPacketType());
        	
            backendConnection.setShareBuffer(dataBuffer);
            //TODO ???
//            backendConnection.getShareBuffer().setLastWritePos(0);
//            backendConnection.getShareBuffer().readIndex(frontCon.getCurrentPacketLength());
            backendConnection.driveState();
        }
        frontCon.setNextNetworkState(NoReadAndWriteState.INSTANCE);
    }
}
