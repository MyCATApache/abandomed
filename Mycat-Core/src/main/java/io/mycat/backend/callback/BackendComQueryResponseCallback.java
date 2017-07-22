package io.mycat.backend.callback;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.mysql.ServerStatus;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.mysql.state.ComQueryColumnDefState;
import io.mycat.mysql.state.IdleState;

public class BackendComQueryResponseCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryResponseCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection mySQLBackendConnection, MycatByteBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
    	LOGGER.debug(mySQLBackendConnection.getClass().getSimpleName() + "  in  BackendComQueryResponseCallback");
        switch(mySQLBackendConnection.getDirectTransferMode()){
        case COMPLETE_PACKET:
        	if (packageType == MySQLPacket.OK_PACKET || packageType == MySQLPacket.ERROR_PACKET) {
                //多结果集时会返回OK包，此时服务器的状态应该已经不是多结果集的状态了
//                MySQLMessage mySQLMessage = new MySQLMessage(dataBuffer);
//                mySQLMessage.position(mySQLBackendConnection.getCurrentPacketStartPos());
                int serverStatus = 0;
                mySQLBackendConnection.setServerStatus(serverStatus);
                if ((serverStatus & ServerStatus.SERVER_STATUS_IN_TRANS) != 0) {
                    LOGGER.debug("后端连接,处于事务中，不能回收!{}", mySQLBackendConnection);
                } else {
                    LOGGER.debug("后端连接，不在事务中，可以回收！{}",mySQLBackendConnection);
                }
                mySQLBackendConnection.setNextState(IdleState.INSTANCE);
                SQLEngineCtx.INSTANCE().getDataTransferChannel()
            	.transferToFront(mySQLBackendConnection, true, true);
            } else {
                mySQLBackendConnection.setNextState(ComQueryColumnDefState.INSTANCE);
            }
        	break;
        case LONG_HALF_PACKET:
        	if(packageType == MySQLPacket.OK_PACKET || packageType == MySQLPacket.ERROR_PACKET){
    			//当前半包不透传
    			SQLEngineCtx.INSTANCE().getDataTransferChannel()
            	.transferToFront(mySQLBackendConnection, false, false);
    		}else{
    			//当前半包透传
    			SQLEngineCtx.INSTANCE().getDataTransferChannel()
            	.transferToFront(mySQLBackendConnection, true, false);
    		}
        	break;
        case SHORT_HALF_PACKET:
        	//当前半包不透传
			SQLEngineCtx.INSTANCE().getDataTransferChannel()
        	.transferToFront(mySQLBackendConnection, false, false);
			break;
        case NONE:
        	break;
        }
    }
}
