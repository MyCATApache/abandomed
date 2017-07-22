package io.mycat.backend.callback;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.mysql.state.ComQueryRowState;

public class BackendComQueryColumnDefCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryColumnDefCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection mySQLBackendConnection, MycatByteBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
    	LOGGER.debug(mySQLBackendConnection.getClass().getSimpleName() + "  in  BackendComQueryColumnDefCallback");
    	switch(mySQLBackendConnection.getDirectTransferMode()){
        case COMPLETE_PACKET:
        	if (packageType ==  MySQLPacket.EOF_PACKET) {
        		mySQLBackendConnection.setNextState(ComQueryRowState.INSTANCE);
//                SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(mySQLBackendConnection, true, true);
            }
//        	else {
//                SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(mySQLBackendConnection, true, false);
//            }
        	break;
        case LONG_HALF_PACKET:
        	if(packageType ==  MySQLPacket.EOF_PACKET){
    			//当前半包不透传
    			SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(mySQLBackendConnection, false, false);
    		}else{
    			//当前半包透传
    			SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(mySQLBackendConnection, true, false);
    		}
        	break;
        case SHORT_HALF_PACKET:
        	//当前半包不透传
			SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(mySQLBackendConnection, false, false);
			break;
        case NONE:
        	break;
        }
    	
    }
}
