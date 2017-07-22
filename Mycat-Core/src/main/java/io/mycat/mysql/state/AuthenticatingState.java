package io.mycat.mysql.state;


import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.engine.ErrorCode;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.AuthPacket;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.net2.states.ClosingState;
import io.mycat.net2.states.ReadWaitingState;
import io.mycat.util.CharsetUtil;

/**
 * 认证状态
 *
 * @author ynfeng
 */
public class AuthenticatingState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatingState.class);
    public static final AuthenticatingState INSTANCE = new AuthenticatingState();
     
    private static final byte[] AUTH_OK = new byte[]{7, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0};

    private AuthenticatingState() {
    }

    /**
     * 对握手响应包进行认证
     *
     * @param mySQLFrontConnection
     * @param attachment
     */
    @Override
    protected boolean frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
        try {
            LOGGER.debug("Frontend in AuthenticatingState");
            AuthPacket auth = new AuthPacket();
            auth.read(mySQLFrontConnection.getDataBuffer());

            // Fake check user
            LOGGER.debug("Check user name. " + auth.user);
            if (!auth.user.equals("root")) {
                LOGGER.debug("User name error. " + auth.user);
                mySQLFrontConnection.failure(ErrorCode.ER_ACCESS_DENIED_ERROR, "Access denied for user '" + auth.user + "'");
                mySQLFrontConnection.setNextState(CloseState.INSTANCE);
                return true;
            }

            // Fake check password
            LOGGER.debug("Check user password. " + new String(auth.password));

            // check schema
            LOGGER.debug("Check database. " + auth.database);

            return success(mySQLFrontConnection, auth);
        } catch (Throwable e) {
            LOGGER.warn("Frontend AuthenticatingState error:", e);
            mySQLFrontConnection.setNextState(CloseState.INSTANCE);
            return true;
        }
    }
    
    private boolean success(MySQLFrontConnection con, AuthPacket auth) throws IOException {
        LOGGER.debug("Login success");
        // 设置字符集编码
        int charsetIndex = (auth.charsetIndex & 0xff);
        final String charset = CharsetUtil.getCharset(charsetIndex);
        if (charset == null) {
            final String errmsg = "Unknown charsetIndex:" + charsetIndex;
            LOGGER.warn(errmsg);
            con.writeErrMessage(ErrorCode.ER_UNKNOWN_CHARACTER_SET, errmsg);
            con.setNextState(CloseState.INSTANCE);
            return true;
        }
        LOGGER.debug("charset = {}, charsetIndex = {}", charset, charsetIndex);
        con.setCharset(charsetIndex, charset);

        if (!con.setFrontSchema(auth.database)) {
            final String errmsg = "No Mycat Schema defined: " + auth.database;
            LOGGER.debug(errmsg);
            con.writeErrMessage(ErrorCode.ER_BAD_DB_ERROR,"42000".getBytes(), errmsg);
            
            con.setWriteCompleteListener(() -> {
            	con.setNextState(CloseState.INSTANCE);
                con.setNextNetworkState(ClosingState.INSTANCE);
            });
        } else {
            con.write(AUTH_OK);
            con.setWriteCompleteListener(() -> {
                con.setNextState(IdleState.INSTANCE);
                con.clearCurrentPacket();
            	con.getDataBuffer().clear();
                con.setNextNetworkState(ReadWaitingState.INSTANCE);
            });
        }
        return false;
    }

    /**
     *
     *
     * @param mySQLBackendConnection
     * @param attachment
     */
    @Override
    protected boolean backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {
        LOGGER.debug("Backend in AuthenticatingState");
        boolean returnflag = false;
        try {
        	processPacketHeader(mySQLBackendConnection);
        	byte packageType = mySQLBackendConnection.getCurrentPacketType();
        	if (packageType == MySQLPacket.ERROR_PACKET) {
        		mySQLBackendConnection.setNextState(CloseState.INSTANCE);
        		returnflag = true;   //当前状态机进入 CloseState 状态
            } else if (packageType == MySQLPacket.OK_PACKET) {
            	mySQLBackendConnection.getDataBuffer().clear();
            	mySQLBackendConnection.setNextState(IdleState.INSTANCE);
                MySQLFrontConnection mySQLFrontConnection = mySQLBackendConnection.getMySQLFrontConnection();
                if (mySQLFrontConnection != null) {
                    mySQLFrontConnection.executePendingTask();
                }else{
                	mySQLBackendConnection.clearCurrentPacket();
                	mySQLBackendConnection.setNextNetworkState(ReadWaitingState.INSTANCE);
                }
                returnflag = false;  //当前状态机退出
            }
        } catch (Throwable e) {
            LOGGER.warn("Backend AuthenticatingState error:", e);
            mySQLBackendConnection.setNextState(CloseState.INSTANCE);
            returnflag = true;
        }
        return returnflag;
    }
}
