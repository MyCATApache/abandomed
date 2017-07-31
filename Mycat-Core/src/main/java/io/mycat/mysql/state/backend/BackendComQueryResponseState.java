package io.mycat.mysql.state.backend;


import java.io.IOException;

import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.mysql.ServerStatus;
import io.mycat.mysql.packet.MySQLPacket;

/**
 * COM_QUERY响应
 *
 * @author ynfeng
 */
public class BackendComQueryResponseState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendComQueryResponseState.class);
    public static final BackendComQueryResponseState INSTANCE = new BackendComQueryResponseState();

    private BackendComQueryResponseState() {
    }

    @Override
    public boolean handle(MySQLConnection mySQLConnection, Object attachment) throws IOException {
        LOGGER.debug("Backend in BackendComQueryResponseState");
        MySQLBackendConnection mySQLBackendConnection = (MySQLBackendConnection) mySQLConnection;
        boolean returnflag = false;
        try {
            processPacketHeader(mySQLBackendConnection);
            byte packageType;
            switch (mySQLBackendConnection.getDirectTransferMode()) {
                case COMPLETE_PACKET:
                    packageType = mySQLBackendConnection.getCurrentPacketType();
                    if (packageType == MySQLPacket.OK_PACKET || packageType == MySQLPacket.ERROR_PACKET) {
                        //多结果集时会返回OK包，此时服务器的状态应该已经不是多结果集的状态了
//                    MySQLMessage mySQLMessage = new MySQLMessage(dataBuffer);
//                    mySQLMessage.position(mySQLBackendConnection.getCurrentPacketStartPos());
                        //TODO serverStatus开始位置计算并不精确
                        int serverStatus = (int) mySQLBackendConnection.getDataBuffer().getFixInt(
                                mySQLBackendConnection.getCurrentPacketStartPos() + 7,
                                2
                        );
                        mySQLBackendConnection.setServerStatus(serverStatus);
                        if ((serverStatus & ServerStatus.SERVER_STATUS_IN_TRANS) != 0) {
                            LOGGER.debug("后端连接,处于事务中，不能回收!{}", mySQLBackendConnection);
                        } else {
                            LOGGER.debug("后端连接，不在事务中，可以回收！{}", mySQLBackendConnection);
                        }
                        mySQLBackendConnection.getDataBuffer().writeLimit(mySQLBackendConnection.getCurrentPacketLength());
                        mySQLBackendConnection.setNextState(BackendIdleState.INSTANCE);
                        SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(mySQLBackendConnection, true, true, true);
                        returnflag = false;  //触发透传,退出当前状态机
                    } else if (packageType == MySQLPacket.REQUEST_FILE_FIELD_COUNT) {
//                        mySQLBackendConnection.setNextState(FrontendComLoadState.INSTANCE);
                        SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(mySQLBackendConnection, true, true, false);
                    } else {
                        mySQLBackendConnection.setNextState(BackendComQueryColumnDefState.INSTANCE);
                        mySQLBackendConnection.getDataBuffer().writeLimit(mySQLBackendConnection.getCurrentPacketLength());
                        returnflag = true;   //状态机自驱进入下一个状态
                    }
                    break;
                case LONG_HALF_PACKET:
                case SHORT_HALF_PACKET:
                    SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(mySQLBackendConnection, false, false, false);
                case NONE:
                    returnflag = false;
                    break;
            }
        } catch (IOException e) {
            LOGGER.error("Backend BackendComQueryResponseState error", e);
            mySQLBackendConnection.setNextState(BackendCloseState.INSTANCE);
            returnflag = false;
        }
        return returnflag;
    }
}
