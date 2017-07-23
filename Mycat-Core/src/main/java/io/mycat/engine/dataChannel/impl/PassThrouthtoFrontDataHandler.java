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
import io.mycat.mysql.state.IdleState;
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
	public void transfer(MySQLConnection in, boolean isTransferLastPacket,boolean transferFinish, boolean isAllFinish) throws IOException {
		
		LOGGER.debug("current in  PassThrouthtoFrontDataHandler ");
		
		MySQLBackendConnection backendConn = (MySQLBackendConnection)in;
		MycatByteBuffer backendDataBuffer = backendConn.getDataBuffer();

		MySQLFrontConnection  frontConn = backendConn.getMySQLFrontConnection();
		
		//透传到前端,注册前端可写事件,取消后端读写事件 实现流量匹配
		backendConn.setNextNetworkState(NoReadAndWriteState.INSTANCE);
		frontConn.setNextNetworkState(WriteWaitingState.INSTANCE)
			     .networkDriverMachine();
		
		//同步前后端状态
		frontConn.setNextState(backendConn.getNextState());
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
        	if(TransferMode.SHORT_HALF_PACKET.equals(backendConn.getDirectTransferMode())){
        		backendConn.clearCurrentPacket();
        	}
        	if(transferFinish){ //当前透传方向 传输完成
        		frontConn .setNextNetworkState(ReadWaitingState.INSTANCE)
        				  .setDirectTransferMode(TransferMode.NONE);
        		backendConn.setDirectTransferMode(TransferMode.NONE)
         				   .setNextNetworkState(ReadWaitingState.INSTANCE)
         				   .networkDriverMachine();
        	}else{
        		frontConn.setNextNetworkState(NoReadAndWriteState.INSTANCE);
        		backendConn.setNextNetworkState(ReadWaitingState.INSTANCE)
        				   .networkDriverMachine();
        	}
        	
        	if(isAllFinish){  //透传全部完成
        		frontConn.setPassthrough(false);
        		backendConn.setPassthrough(false);
        	}
        });
	}
}
