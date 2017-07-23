package io.mycat.mysql.state;


import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.MySQLPacket;

/**
 * 列定义传送状态
 *
 * @author ynfeng
 */
public class ComQueryColumnDefState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComQueryColumnDefState.class);
    public static final ComQueryColumnDefState INSTANCE = new ComQueryColumnDefState();


    private ComQueryColumnDefState() {
    }

    @Override
    protected boolean frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) { 
        LOGGER.debug("Frontend in ComQueryColumnDefState");
        return false;
    }

    @Override
    protected boolean backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {        
        LOGGER.debug("Backend in ComQueryColumnDefState");
        boolean returnflag = false;
        try {
        	//  如果当前状态数据报文可能有多个，需要透传
        	while(true){
        		processPacketHeader(mySQLBackendConnection);
        		MycatByteBuffer dataBuffer = mySQLBackendConnection.getDataBuffer();
            	byte packageType;
            	switch(mySQLBackendConnection.getDirectTransferMode()){
                case COMPLETE_PACKET:
                	packageType = mySQLBackendConnection.getCurrentPacketType();
                	//设置当前包结束位置
                	dataBuffer.writeLimit(mySQLBackendConnection.getCurrentPacketLength());
                	if (packageType ==  MySQLPacket.EOF_PACKET) {
                		mySQLBackendConnection.setNextState(ComQueryRowState.INSTANCE);
                		return true;
                    }
                	break;
                case LONG_HALF_PACKET:
                	packageType = mySQLBackendConnection.getCurrentPacketType();
                	if(packageType ==  MySQLPacket.EOF_PACKET){
                		//短半包的情况下,currentPacketStartPos 为 上一个包的结束位置,当前半包不透传,不需要设置
//                		dataBuffer.writeLimit(mySQLBackendConnection.getCurrentPacketStartPos());
            			//当前半包不透传
            			SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(mySQLBackendConnection, false,false, false);
            		}else{
            			dataBuffer.writeLimit(dataBuffer.writeIndex()); //设置当前包结束位置
						mySQLBackendConnection.setCurrentPacketStartPos(mySQLBackendConnection.getCurrentPacketStartPos() - dataBuffer.writeIndex());
						mySQLBackendConnection.setCurrentPacketLength(mySQLBackendConnection.getCurrentPacketLength() - dataBuffer.writeIndex());
            			//当前半包透传
            			SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(mySQLBackendConnection, true,false, false);
            		}
                	return false;
                case SHORT_HALF_PACKET:
                	//短半包的情况下,currentPacketLength 为 上一个包的结束位置,当前半包不透传,不需要设置
//                	dataBuffer.writeLimit(mySQLBackendConnection.getCurrentPacketLength());
                	//当前半包不透传
        			SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(mySQLBackendConnection, false,false, false);
        			return false;
                case NONE:
                	break;
                }
        	}
        	
        } catch (IOException e) {
            LOGGER.warn("Backend ComQueryColumnDefState error", e);
            mySQLBackendConnection.setNextState(CloseState.INSTANCE);
            returnflag = false;
        }
        return returnflag;
    }
}
