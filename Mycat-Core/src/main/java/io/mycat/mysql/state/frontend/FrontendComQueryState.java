package io.mycat.mysql.state.frontend;


import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.engine.dataChannel.TransferMode;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.machine.StateMachine;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import io.mycat.mysql.state.backend.BackendComQueryResponseState;
import io.mycat.net2.Connection;
import io.mycat.net2.states.NoReadAndWriteState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InvalidObjectException;

/**
 * 查询状态
 *
 * @author ynfeng
 */
public class FrontendComQueryState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendComQueryState.class);
    public static final FrontendComQueryState INSTANCE = new FrontendComQueryState();

    private FrontendComQueryState() {
    }

    @Override
    public boolean handle(StateMachine context, Connection connection, Object attachment) throws IOException {
        MySQLFrontConnection frontCon = (MySQLFrontConnection) connection;
        LOGGER.debug("Frontend in FrontendComQueryState");
        MySQLBackendConnection backendConnection = frontCon.getBackendConnection();
        frontCon.getNetworkStateMachine().setNextState(NoReadAndWriteState.INSTANCE);
        if (backendConnection == null) {   //往往新建立连接时,后端连接会是空
            backendConnection = getBackendFrontConnection(frontCon);
            MySQLBackendConnection finalBackendConnection = backendConnection;
            frontCon.setPassthrough(true); // 开启透传模式
            frontCon.addTodoTask(() -> {
                frontCon.getProtocolStateMachine().setNextState(BackendComQueryResponseState.INSTANCE);
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

                SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToBackend(frontCon, true, true, false);
            });
        } else {

            if (frontCon.isPassthrough()) {  //如果房钱已经处于透传模式.
                processPacketHeader(frontCon);
            } else {
                frontCon.setPassthrough(true);
                backendConnection.setPassthrough(true);
            }

            MycatByteBuffer dataBuffer = frontCon.getDataBuffer();
            switch (frontCon.getDirectTransferMode()) {
                case COMPLETE_PACKET:
                    //设置当前包结束位置
                    dataBuffer.writeLimit(frontCon.getCurrentPacketLength());
//            	if(frontCon.getCurrentPacketLength()==dataBuffer.writeIndex()){  // 查询报文结束，触发透传    这里没有必要判断了。注释掉
                    frontCon.getProtocolStateMachine().setNextState(BackendComQueryResponseState.INSTANCE);
                    SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToBackend(frontCon, true, true, false);
//            	}
                    break;
                case LONG_HALF_PACKET:
                    dataBuffer.writeLimit(dataBuffer.writeIndex()); //设置当前包结束位置
                    if (frontCon.getCurrentPacketLength() == dataBuffer.writeIndex()) {  //当前报文传递完成
                        frontCon.getProtocolStateMachine().setNextState(BackendComQueryResponseState.INSTANCE);
                        SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToBackend(frontCon, true, true, false);
                    } else {
                        frontCon.setCurrentPacketStartPos(frontCon.getCurrentPacketStartPos() - dataBuffer.writeIndex());
                        frontCon.setCurrentPacketLength(frontCon.getCurrentPacketLength() - dataBuffer.writeIndex());
                        //当前半包透传
                        SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToBackend(frontCon, true, false, false);
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
}
