package io.mycat.engine.dataChannel.impl;

import java.io.IOException;

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
		TransferMode mode = in.getDirectTransferMode();
		ConDataBuffer backendDataBuffer = backendConn.getDataBuffer();
		switch(mode){
		case SHORT_HALF_PACKET:
			backendDataBuffer.setReadPos(in.getCurrentPacketLength());  //设置读取位置
			break;
		case LONG_HALF_PACKET:
			if(isTransferLastPacket){  //当前半包可以透传  当前半包可以透传时,需要调整 start length
    			// packtype 不变,start = -13,length = length - 缓冲区中剩余长度.  limit = dataBuffer.writingPos();
				
				int halflength = in.getCurrentPacketStartPos() < 0?
									backendDataBuffer.getWritePos():
										(backendDataBuffer.getWritePos()-in.getCurrentPacketStartPos());
				
    			in.setCurrentPacketStartPos(in.getCurrentPacketStartPos() - backendDataBuffer.getWritePos());
    			in.setCurrentPacketLength(in.getCurrentPacketLength() - halflength);
    			backendDataBuffer.setReadPos(backendDataBuffer.getWritePos());  //设置读取位置
    		}else{  //当前半包不透传 
    			// packtype 不变,start = 0,length = packlength;
    			int packetLength = in.getCurrentPacketLength() - in.getCurrentPacketStartPos();
    			in.setCurrentPacketLength(packetLength);
    			in.setCurrentPacketStartPos(0);
    			backendDataBuffer.setReadPos(in.getCurrentPacketStartPos());  //设置读取位置
    		}
			
			break;
		case NORMAL:
			break;
		case COMPLETE_PACKET:
			backendDataBuffer.setReadPos(in.getCurrentPacketLength());  //设置读取位置
			break;
		}

		MySQLFrontConnection  frontConn = backendConn.getMySQLFrontConnection();  //TODO 放入到当前会话上下文中?
		backendConn.setNextConnState(NoReadAndWriteState.INSTANCE);
		//后端状态有可能是上一个包的状态, 这种情况出现在 最后一个包无法解析出报文类型的情况,这种情况最后一个包的前几个字节不传输.
		frontConn.setNextState(backendConn.getCurrentState());
		LOGGER.debug("Frontend in "+frontConn.getCurrentState().getClass().getSimpleName());
		
    	frontConn.connDriverMachine(WriteWaitingState.INSTANCE);
        ConDataBuffer frontdatabuffer = frontConn.getDataBuffer();
        
            	
    	//开启透传模式
    	frontConn.setDirectTransferMode(TransferMode.COMPLETE_PACKET); //整包透传
    	frontConn.setDataBuffer(backendDataBuffer);
        frontConn.getDataBuffer().setLastWritePos(0);

        frontConn.setWriteCompleteListener(() -> {
        	frontdatabuffer.clear();
        	frontConn.getDataBuffer().compact();
        	frontConn.setDataBuffer(frontdatabuffer);

            //写完成后开启后端的读
        	frontConn.getBackendConnection().connDriverMachine(ReadWaitingState.INSTANCE);
        	frontConn.clearCurrentPacket();
        	if(TransferMode.SHORT_HALF_PACKET.equals(mode)){
        		backendConn.clearCurrentPacket();
        	}
        	if(isAllFinish){
        		frontConn.setNextState(IdleState.INSTANCE);
        		frontConn.setNextConnState(ReadWaitingState.INSTANCE);
        	}else{
        		frontConn.setNextConnState(NoReadAndWriteState.INSTANCE);
        	}
        });
	}
}
