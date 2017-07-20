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
public class PassThrouthtoBackendDataHandler implements DataHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PassThrouthtoBackendDataHandler.class);

	public static final PassThrouthtoBackendDataHandler INSTANCE = new PassThrouthtoBackendDataHandler();

	private PassThrouthtoBackendDataHandler(){}

	/*
	 * 透传时,进行前后端状态同步
	 */
	@Override
	public void transfer(MySQLConnection in, boolean isTransferLastPacket, boolean isAllFinish) throws IOException {
		MySQLFrontConnection frontCoxn = (MySQLFrontConnection)in;

		TransferMode mode = in.getDirectTransferMode();
		MycatByteBuffer frontDataBuffer = frontCoxn.getDataBuffer();
		
		switch(mode){
		case SHORT_HALF_PACKET:
			frontDataBuffer.writeLimit(in.getCurrentPacketLength());  //短半包的情况下,currentPacketLength 为 上一个包的结束位置.
			break;
		case LONG_HALF_PACKET:
			if(isTransferLastPacket){  //当前半包可以透传  当前半包可以透传时,需要调整 start length
				int halflength = in.getCurrentPacketStartPos() < 0?
						frontDataBuffer.writeIndex():
										(frontDataBuffer.writeIndex()-in.getCurrentPacketStartPos());
    			in.setCurrentPacketStartPos(in.getCurrentPacketStartPos() - frontDataBuffer.writeIndex());
				in.setCurrentPacketLength(in.getCurrentPacketLength() - halflength);
    		}else{  //当前半包不透传
    			frontDataBuffer.writeLimit(in.getCurrentPacketStartPos()); //短半包的情况下,currentPacketStartPos 为 上一个包的结束位置.
    		}
			break;
		case NONE:
			break;
		case COMPLETE_PACKET:
			break;
		}

		MySQLBackendConnection  backendCoxn = frontCoxn.getBackendConnection();
		//后端状态有可能是上一个包的状态, 这种情况出现在 最后一个包无法解析出报文类型的情况,最后一个包的前几个字节不传输.
		backendCoxn.setNextState(frontCoxn.getNextState());
		backendCoxn.networkDriverMachine(WriteWaitingState.INSTANCE);

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
        	if(isAllFinish){ //透传全部完成
        		backendCoxn.setNextNetworkState(ReadWaitingState.INSTANCE);
        	}else{
        		backendCoxn.setNextNetworkState(NoReadAndWriteState.INSTANCE);
        		frontCoxn.networkDriverMachine(ReadWaitingState.INSTANCE);
        	}
        });
	}
}
