package io.mycat.mysql.state;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.HandshakePacket;
import io.mycat.net2.states.ReadWaitingState;
import io.mycat.util.CharsetUtil;

/**
 * 握手状态
 *
 * @author ynfeng
 */
public class HandshakeState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandshakeState.class);
    public static final HandshakeState INSTANCE = new HandshakeState();

    private HandshakeState() {
    }

    /**
     * 接收客户端的握手响应包,转至认证状态
     *
     * @param mySQLFrontConnection
     * @param attachment
     */
    @Override
    protected boolean frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
        LOGGER.debug("Frontend in HandshakeState");
        mySQLFrontConnection.setNextState(AuthenticatingState.INSTANCE);
        return true;
    }

    /**
     * 向服务器响应握手包
     *
     * @param mySQLBackendConnection
     * @param attachment
     */
    @Override
    protected boolean backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {
    	LOGGER.debug("Backend in HandshakeState");
    	boolean returnflag;
        try {
        	processHandShakePacket(mySQLBackendConnection);
        	mySQLBackendConnection.authenticate();
            mySQLBackendConnection.setNextState(AuthenticatingState.INSTANCE);
            mySQLBackendConnection.setWriteCompleteListener(()->{
            	mySQLBackendConnection.clearCurrentPacket();
            	mySQLBackendConnection.getDataBuffer().clear();
            	mySQLBackendConnection.setNextNetworkState(ReadWaitingState.INSTANCE);
            });
            
            returnflag = false;
        } catch (Throwable e) {
            LOGGER.warn("frontend InitialState error", e);
            mySQLBackendConnection.setNextState(CloseState.INSTANCE);
            returnflag = true;
        }
        return returnflag;
    }
    
    private void processHandShakePacket(MySQLBackendConnection conn) {
        HandshakePacket packet = new HandshakePacket();
        packet.read(conn.getDataBuffer());
        conn.getDataBuffer().clear();
        conn.setHandshake(packet);
        // 设置字符集编码
        int charsetIndex = (packet.serverCharsetIndex & 0xff);
        String charset = CharsetUtil.getCharset(charsetIndex);
        if (charset != null) {
            conn.setCharset(charsetIndex, charset);
        } else {
            String errmsg = "Unknown charsetIndex:" + charsetIndex + " of " + conn;
            LOGGER.warn(errmsg);
            return;
        }
    }
}
