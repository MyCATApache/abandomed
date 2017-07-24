package io.mycat.mysql.state;


import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.ServerStatus;
import io.mycat.mysql.packet.MySQLPacket;

/**
 * 行数据传送状态
 *
 * @author ynfeng
 */
public class ComQueryRowState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComQueryRowState.class);
    public static final ComQueryRowState INSTANCE = new ComQueryRowState();


    private ComQueryRowState() {
    }

    @Override
    protected boolean frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
    	LOGGER.debug("Frontend in ComQueryRowState");
    	return true;
    }

    @Override
    protected boolean backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {
    	LOGGER.debug("Backend in ComQueryRowState");
    	boolean returnflag = false;
        try {

        	while(true){
        		processPacketHeader(mySQLBackendConnection);
        		MycatByteBuffer dataBuffer = mySQLBackendConnection.getDataBuffer();
            	byte packageType;
            	switch (mySQLBackendConnection.getDirectTransferMode()) {
    	            case COMPLETE_PACKET:
    	            	LOGGER.debug("Backend in ComQueryRowState  COMPLETE_PACKET  ");
    	            	dataBuffer.writeLimit(mySQLBackendConnection.getCurrentPacketLength());
    	            	packageType = mySQLBackendConnection.getCurrentPacketType();
    	                if (packageType == MySQLPacket.EOF_PACKET) {
    	                	//TODO 开始位置计算并不精确
    	                    int serverStatus = (int) dataBuffer.getFixInt(mySQLBackendConnection.getCurrentPacketStartPos()+7,2);
    	                    mySQLBackendConnection.setServerStatus(serverStatus);
    	                    //检查后面还有没有结果集
    	                    if ((mySQLBackendConnection.getServerStatus() & ServerStatus.SERVER_MORE_RESULTS_EXISTS) == 0) {
    	                        LOGGER.debug("backend com query response complete change to idle state");
    	                        mySQLBackendConnection.setNextState(IdleState.INSTANCE);
    	                        SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(mySQLBackendConnection, true,true, true);
    	                        return false;
    	                    } else {
    	                        LOGGER.debug("backend com query response state have multi result");
    	                        mySQLBackendConnection.setNextState(ComQueryResponseState.INSTANCE);
    	                        return true;
    	                    }
    	                }
    	                break;
    	            case LONG_HALF_PACKET:
    	            	LOGGER.debug("Backend in ComQueryRowState  LONG_HALF_PACKET  ");
    	            	packageType = mySQLBackendConnection.getCurrentPacketType();
    	                if (packageType == MySQLPacket.EOF_PACKET) {
    	                	//短半包的情况下,currentPacketStartPos 为 上一个包的结束位置.
//                    		dataBuffer.writeLimit(mySQLBackendConnection.getCurrentPacketStartPos());
    	                    //当前半包不透传
    	                    SQLEngineCtx.INSTANCE().getDataTransferChannel()
    	                            .transferToFront(mySQLBackendConnection, false,false,false);
    	                } else {
    	                	dataBuffer.writeLimit(dataBuffer.writeIndex());
    						mySQLBackendConnection.setCurrentPacketStartPos(mySQLBackendConnection.getCurrentPacketStartPos() - dataBuffer.writeIndex());
    						mySQLBackendConnection.setCurrentPacketLength(mySQLBackendConnection.getCurrentPacketLength() - dataBuffer.writeIndex());
    	                    //当前半包透传
    	                    SQLEngineCtx.INSTANCE().getDataTransferChannel()
    	                            .transferToFront(mySQLBackendConnection, true,false, false);
    	                }
    	                return false;
    	            case SHORT_HALF_PACKET:
    	            	LOGGER.debug("Backend in ComQueryRowState  SHORT_HALF_PACKET  ");
    	            	//短半包的情况下,currentPacketLength 为 上一个包的结束位置.
//                    	dataBuffer.writeLimit(mySQLBackendConnection.getCurrentPacketLength());
    	                //当前半包不透传
    	                SQLEngineCtx.INSTANCE().getDataTransferChannel()
    	                        .transferToFront(mySQLBackendConnection, false,false, false);
    	                return false;
    	            case NONE:
    	                break;
    	        }
        	}
        } catch (IOException e) {
            LOGGER.warn("frontend ComQueryRowState error", e);
            mySQLBackendConnection.setNextState(CloseState.INSTANCE);
            returnflag = false;
        }
        return returnflag;
    }
}
