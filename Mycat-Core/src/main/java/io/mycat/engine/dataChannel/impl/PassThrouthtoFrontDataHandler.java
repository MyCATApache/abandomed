package io.mycat.engine.dataChannel.impl;

import java.io.IOException;

import io.mycat.buffer.MycatByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.engine.dataChannel.DataHandler;
import io.mycat.engine.dataChannel.TransferMode;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.state.IdleState;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.states.NoReadAndWriteState;
import io.mycat.net2.states.ReadWaitingState;
import io.mycat.net2.states.WriteWaitingState;

/**
 * 透传到前端数据处理器
 * @author yanjunli
 *
 */
public class PassThrouthtoFrontDataHandler implements DataHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PassThrouthtoFrontDataHandler.class);

	public static final PassThrouthtoFrontDataHandler INSTANCE = new PassThrouthtoFrontDataHandler();

	private PassThrouthtoFrontDataHandler(){}

	/*
	 * 透传时,进行前后端状态同步
	 */
	@Override
	public void transfer(MySQLConnection in, boolean isTransferLastPacket, boolean isAllFinish) throws IOException {
		MySQLBackendConnection backendConn = (MySQLBackendConnection)in;
		MycatByteBuffer backendDataBuffer = backendConn.getDataBuffer();

		MySQLFrontConnection  frontConn = backendConn.getMySQLFrontConnection();
		backendConn.setNextNetworkState(NoReadAndWriteState.INSTANCE);
		//后端状态有可能是上一个包的状态, 这种情况出现在 最后一个包无法解析出报文类型的情况,最后一个包的前几个字节不传输.
		frontConn.setNextState(backendConn.getNextState());
		frontConn.networkDriverMachine(WriteWaitingState.INSTANCE);

		MycatByteBuffer frontdatabuffer = frontConn.getDataBuffer();
    	frontConn.setDataBuffer(backendDataBuffer);

		frontConn.setWriteCompleteListener(() -> {
        	frontdatabuffer.clear();
        	frontConn.getDataBuffer().compact();
        	frontConn.setDataBuffer(frontdatabuffer);
        	frontConn.clearCurrentPacket();

        	if(TransferMode.SHORT_HALF_PACKET.equals(backendConn.getDirectTransferMode())){
        		backendConn.clearCurrentPacket();
        	}
        	if(isAllFinish){ //透传全部完成
        		frontConn.setNextState(IdleState.INSTANCE)
        				  .setNextNetworkState(ReadWaitingState.INSTANCE)
        				  .setPassthrough(false)
        				  .setDirectTransferMode(TransferMode.NONE);
        		backendConn.setNextState(IdleState.INSTANCE)
        		           .setPassthrough(false)
         				   .setDirectTransferMode(TransferMode.NONE)
         				   .networkDriverMachine(ReadWaitingState.INSTANCE);
        	}else{
        		frontConn.setNextNetworkState(NoReadAndWriteState.INSTANCE);
        		backendConn.networkDriverMachine(ReadWaitingState.INSTANCE);
        	}
        });
	}
}
