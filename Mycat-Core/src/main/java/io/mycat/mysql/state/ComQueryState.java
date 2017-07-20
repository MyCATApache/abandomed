package io.mycat.mysql.state;


import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.engine.dataChannel.TransferMode;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.net2.states.NoReadAndWriteState;
import io.mycat.net2.states.ReadWaitingState;
import io.mycat.net2.states.WriteWaitingState;

/**
 * 查询状态
 *
 * @author ynfeng
 */
public class ComQueryState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComQueryState.class);
    public static final ComQueryState INSTANCE = new ComQueryState();

//    private FrontendComQueryStateHandler frontendComQueryStateHandler = new FrontendComQueryStateHandler();
//    private BackendComQueryCallback backendComQueryCallback = new BackendComQueryCallback();

    private ComQueryState() {
    }

    @Override
    protected boolean frontendHandle(MySQLFrontConnection frontCon, Object attachment)throws IOException {
    	LOGGER.debug("Frontend in ComQueryState");
        MySQLBackendConnection backendConnection = frontCon.getBackendConnection();
        frontCon.setNextNetworkState(NoReadAndWriteState.INSTANCE);
        frontCon.setNextState(ComQueryResponseState.INSTANCE);
        frontCon.setPassthrough(true); // 开启透传模式
        if (backendConnection == null) {
            backendConnection = getBackendFrontConnection(frontCon);
            MySQLBackendConnection finalBackendConnection = backendConnection;
            frontCon.addTodoTask(() -> {

            	/*
            	 * 1. 开启透传模式
            	 * 2. 同步状态机状态
            	 */
            	
                finalBackendConnection.setPassthrough(true)//开启透传模式
                					  .setDirectTransferMode(TransferMode.COMPLETE_PACKET) //透传方式整包透传
                					  .setCurrentPacketLength(frontCon.getCurrentPacketLength())
                					  .setCurrentPacketStartPos(frontCon.getCurrentPacketStartPos())
                					  .setCurrentPacketType(frontCon.getCurrentPacketType());
                
                SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToBackend(frontCon, true, true);
            });
        } else {
        	
        	backendConnection.setPassthrough(true)//开启透传模式
							  .setDirectTransferMode(TransferMode.COMPLETE_PACKET) //透传方式整包透传
							  .setCurrentPacketLength(frontCon.getCurrentPacketLength())
							  .setCurrentPacketStartPos(frontCon.getCurrentPacketStartPos())
							  .setCurrentPacketType(frontCon.getCurrentPacketType())
							  .setShareBuffer(frontCon.getDataBuffer());//

        	SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToBackend(frontCon, true, true);
        }
        return false;
    }

    @Override
    protected boolean backendHandle(MySQLBackendConnection conn, Object attachment)throws IOException {
    	LOGGER.debug("Backend in ComQueryState");
        MycatByteBuffer writeBuffer = conn.getDataBuffer();
        if(conn.isPassthrough()){    //如果处于透传模式下,需要从共享buffer 获取数据
        	conn.setDataBuffer(conn.getShareBuffer());
        }
        /* 这里还没有办法区分是前端状态机驱动还是后端状态机驱动  */
        conn.networkDriverMachine(WriteWaitingState.INSTANCE);
        conn.setWriteCompleteListener(() -> {
            conn.setNextState(ComQueryResponseState.INSTANCE)
            	.setDataBuffer(writeBuffer)
            	.setNextNetworkState(ReadWaitingState.INSTANCE)
            	.getShareBuffer().compact();   //透传buffer 使用compact
            conn.getDataBuffer().clear();     //非透传buffer 使用 clear
        });
        return false;
    }
}
