package io.mycat.mysql.state;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.net2.NetSystem;
import io.mycat.net2.states.ReadWaitingState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * 连接中状态
 *
 * @author ynfeng
 */
public class ConnectingState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectingState.class);
    public static final ConnectingState INSTANCE = new ConnectingState();

    private ConnectingState() {
    }

    /**
     * 向客户端响应握手包
     *
     * @param mySQLFrontConnection
     * @param attachment
     */
    @Override
    protected boolean frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment)throws IOException {
        LOGGER.debug("Frontend in ConnectingState");
        boolean returnflag = false;
        try {
            mySQLFrontConnection.sendAuthPackge();
            mySQLFrontConnection.setWriteCompleteListener(()->{
            	mySQLFrontConnection.clearCurrentPacket();
            	mySQLFrontConnection.getDataBuffer().clear();
            	mySQLFrontConnection.setNextNetworkState(ReadWaitingState.INSTANCE);
            });
            mySQLFrontConnection.setNextState(HandshakeState.INSTANCE);
            returnflag = false;
        } catch (Throwable e) {
            LOGGER.warn("frontend InitialState error", e);
            mySQLFrontConnection.setNextState(CloseState.INSTANCE);
            returnflag = true;
        }
        return returnflag;
    }

    /**
     * 连接后端服务器
     *
     * @param mySQLBackendConnection
     * @param attachment
     */
    @Override
    protected boolean backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment)throws IOException {
        LOGGER.debug("Backend in ConnectingState");
        mySQLBackendConnection.setNextState(HandshakeState.INSTANCE);
        NetSystem.getInstance().getConnector().postConnect(mySQLBackendConnection);
        return false;
    }
}
