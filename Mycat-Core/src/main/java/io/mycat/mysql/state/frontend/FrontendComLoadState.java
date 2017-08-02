package io.mycat.mysql.state.frontend;

import java.io.IOException;

import io.mycat.machine.StateMachine;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import io.mycat.net2.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.SQLEngineCtx;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.MySQLConnection;
import io.mycat.net2.states.NoReadAndWriteState;

public class FrontendComLoadState extends AbstractMysqlConnectionState {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendComLoadState.class);
    public static final FrontendComLoadState INSTANCE = new FrontendComLoadState();

    private FrontendComLoadState() {
    }

    @Override
    public boolean handle(StateMachine stateMachine, Connection connection, Object attachment) throws IOException {
        LOGGER.debug("Frontend in FrontendComLoadState");
        MySQLFrontConnection frontCon = (MySQLFrontConnection) connection;
        boolean returnflag = false;
        try {
            frontCon.getNetworkStateMachine().setNextState(NoReadAndWriteState.INSTANCE);
            //  如果当前状态数据报文可能有多个，需要透传
            while (true) {
                processPacketHeader(frontCon);
                MycatByteBuffer dataBuffer = frontCon.getDataBuffer();
                switch (frontCon.getDirectTransferMode()) {
                    case COMPLETE_PACKET:
                        dataBuffer.writeLimit(frontCon.getCurrentPacketLength()); //设置当前包结束位置
                        break;
                    case LONG_HALF_PACKET:
                        dataBuffer.writeLimit(dataBuffer.writeIndex()); //设置当前包结束位置
                        frontCon.setCurrentPacketStartPos(frontCon.getCurrentPacketStartPos() - dataBuffer.writeIndex());
                        frontCon.setCurrentPacketLength(frontCon.getCurrentPacketLength() - dataBuffer.writeIndex());
                        //当前半包透传
                        SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToBackend(frontCon, true, false, false);
                        return false;
                    case SHORT_HALF_PACKET:
                        if ((dataBuffer.writeIndex() - dataBuffer.writeLimit()) == MySQLConnection.msyql_packetHeaderSize) {
                            dataBuffer.writeLimit(dataBuffer.writeIndex());
//                            frontCon.setNextState(BackendComQueryResponseState.INSTANCE);
                            SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToBackend(frontCon, true, true, false);
                            return false;
                        }
                        //短半包的情况下,currentPacketLength 为 上一个包的结束位置,当前半包不透传,不需要设置
//                	dataBuffer.writeLimit(mySQLBackendConnection.getCurrentPacketLength());
                        //当前半包不透传
                        SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToBackend(frontCon, false, false, false);
                        return false;
                    case NONE:
                        break;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Backend BackendComQueryColumnDefState error", e);
            frontCon.getProtocolStateMachine().setNextState(FrontendCloseState.INSTANCE);
            returnflag = false;
        }
        return returnflag;
    }
}
