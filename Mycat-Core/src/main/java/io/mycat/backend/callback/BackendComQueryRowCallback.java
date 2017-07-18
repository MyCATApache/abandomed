package io.mycat.backend.callback;

import java.io.IOException;

import io.mycat.buffer.MycatByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.mysql.ServerStatus;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.mysql.state.ComQueryResponseState;
import io.mycat.mysql.state.IdleState;

public class BackendComQueryRowCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryRowCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection mySQLBackendConnection, MycatByteBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
        LOGGER.debug(mySQLBackendConnection.getClass().getSimpleName() + "  in  BackendComQueryRowCallback");
        switch (mySQLBackendConnection.getDirectTransferMode()) {
            case COMPLETE_PACKET:
                if (packageType == MySQLPacket.EOF_PACKET) {
//                MySQLMessage mySQLMessage = new MySQLMessage(dataBuffer);
//                mySQLMessage.position(mySQLBackendConnection.getCurrentPacketStartPos());
                    int serverStatus = 0;
                    mySQLBackendConnection.setServerStatus(serverStatus);
                    //检查后面还有没有结果集
                    if ((mySQLBackendConnection.getServerStatus() & ServerStatus.SERVER_MORE_RESULTS_EXISTS) == 0) {
                        LOGGER.debug("backend com query response complete change to idle state");
                        mySQLBackendConnection.setNextState(IdleState.INSTANCE);
                        SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(mySQLBackendConnection, true, true);
                    } else {
                        LOGGER.debug("backend com query response state have multi result");
                        mySQLBackendConnection.setNextState(ComQueryResponseState.INSTANCE);
                        SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(mySQLBackendConnection, true, false);
                    }
                }
                break;
            case LONG_HALF_PACKET:
                if (packageType == MySQLPacket.EOF_PACKET) {
                    //当前半包不透传
                    SQLEngineCtx.INSTANCE().getDataTransferChannel()
                            .transferToFront(mySQLBackendConnection, false, false);
                } else {
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
