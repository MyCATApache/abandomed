package io.mycat.mysql.state;


import java.io.IOException;
import java.io.InvalidObjectException;

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
    protected boolean frontendHandle(final MySQLFrontConnection frontCon, Object attachment)throws IOException {
    	LOGGER.debug("Frontend in ComQueryState");
        MySQLBackendConnection backendConnection = frontCon.getBackendConnection();
        frontCon.setNextNetworkState(NoReadAndWriteState.INSTANCE);
        if (backendConnection == null) {   //往往新建立连接时,后端连接会是空
            backendConnection = getBackendFrontConnection(frontCon);
            MySQLBackendConnection finalBackendConnection = backendConnection;
            frontCon.setPassthrough(true); // 开启透传模式
            frontCon.addTodoTask(() -> {
            	frontCon.setNextState(ComQueryResponseState.INSTANCE);
            	/*
            	 * 1. 开启透传模式
            	 * 2. 同步状态机状态
            	 */
                //开启透传模式
            	finalBackendConnection.setPassthrough(true)
            						  .setDirectTransferMode(TransferMode.COMPLETE_PACKET) //透传方式整包透传
                					  .setCurrentPacketLength(frontCon.getCurrentPacketLength())
                					  .setCurrentPacketStartPos(frontCon.getCurrentPacketStartPos())
                					  .setCurrentPacketType(frontCon.getCurrentPacketType());
                
                SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToBackend(frontCon, true, true);
            });
        } else {

        	if(frontCon.isPassthrough()){  //如果房钱已经处于透传模式.
        		processPacketHeader(frontCon);
        	}else{
        		frontCon.setPassthrough(true);
        		backendConnection.setPassthrough(true);
        	}

    		MycatByteBuffer dataBuffer = frontCon.getDataBuffer();
        	switch(frontCon.getDirectTransferMode()){
            case COMPLETE_PACKET:
            	//设置当前包结束位置
            	dataBuffer.writeLimit(frontCon.getCurrentPacketLength());
//            	if(frontCon.getCurrentPacketLength()==dataBuffer.writeIndex()){  // 查询报文结束，触发透传    这里没有必要判断了。注释掉
            		frontCon.setNextState(ComQueryResponseState.INSTANCE);
            		SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToBackend(frontCon, true, true);
//            	}
            	break;
            case LONG_HALF_PACKET:
        			dataBuffer.writeLimit(dataBuffer.writeIndex()); //设置当前包结束位置
        			if(frontCon.getCurrentPacketLength()==dataBuffer.writeIndex()){  //当前报文传递完成
        				frontCon.setNextState(ComQueryResponseState.INSTANCE);
        				SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToBackend(frontCon, true, true);
        			}else{
        				frontCon.setCurrentPacketStartPos(frontCon.getCurrentPacketStartPos() - dataBuffer.writeIndex());
            			frontCon.setCurrentPacketLength(frontCon.getCurrentPacketLength() - dataBuffer.writeIndex());
            			//当前半包透传
            			SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToBackend(frontCon, true, false);
        			}
        			break;
            case SHORT_HALF_PACKET:
            	throw new InvalidObjectException("invalid packettype is SHORT_HALF_PACKET");
            	// 短半包 在 前端透传时，不会出现。
            	//当前半包不透传
//            	SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToBackend(frontCon, false, false);
            case NONE:
            	throw new InvalidObjectException("invalid packettype is NONE");
            }
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
        conn.setNextNetworkState(WriteWaitingState.INSTANCE)
        	.networkDriverMachine();
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
