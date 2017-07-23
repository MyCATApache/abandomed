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
 * @author yanjunli
 *
 */
public class PassThrouthtoBackendDataHandler implements DataHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PassThrouthtoBackendDataHandler.class);

	public static final PassThrouthtoBackendDataHandler INSTANCE = new PassThrouthtoBackendDataHandler();

	private PassThrouthtoBackendDataHandler(){}

	/*
	 * 透传时,进行前后端状态同步
	 */
	@Override
	public void transfer(MySQLConnection in, boolean isTransferLastPacket,boolean transferFinish, boolean isAllFinish) throws IOException {
		
		LOGGER.debug("current in  PassThrouthtoBackendDataHandler ");
		
		MySQLFrontConnection frontCoxn = (MySQLFrontConnection)in;

		MycatByteBuffer frontDataBuffer = frontCoxn.getDataBuffer();


		MySQLBackendConnection  backendCoxn = frontCoxn.getBackendConnection();
		//后端状态有可能是上一个包的状态, 这种情况出现在 最后一个包无法解析出报文类型的情况,最后一个包的前几个字节不传输.
		backendCoxn.setNextState(frontCoxn.getNextState());
		backendCoxn.setNextNetworkState(WriteWaitingState.INSTANCE)
				   .networkDriverMachine();

		MycatByteBuffer backdatabuffer = backendCoxn.getDataBuffer();
		backendCoxn.setDataBuffer(frontDataBuffer);

		backendCoxn.setWriteCompleteListener(() -> {
			backdatabuffer.clear();
        	backendCoxn.getDataBuffer().compact();
        	backendCoxn.setDataBuffer(backdatabuffer);
        	backendCoxn.clearCurrentPacket();
        	
        	if(TransferMode.SHORT_HALF_PACKET.equals(frontCoxn.getDirectTransferMode())){
        		frontCoxn.clearCurrentPacket();
        	}
        	if(transferFinish){ //透传全部完成
        		backendCoxn.setNextNetworkState(ReadWaitingState.INSTANCE);
        	}else{
        		backendCoxn.setNextNetworkState(NoReadAndWriteState.INSTANCE);
        		frontCoxn.setNextNetworkState(ReadWaitingState.INSTANCE)
        			     .networkDriverMachine();
        	}
        	
        	if(isAllFinish){  //透传全部完成
        		frontCoxn.setPassthrough(false);
        		backendCoxn.setPassthrough(false);
        	}
        });
	}
}
