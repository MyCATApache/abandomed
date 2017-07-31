package io.mycat.mysql.state.frontend;


import io.mycat.SQLEngineCtx;
import io.mycat.engine.ErrorCode;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.packet.AuthPacket;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import io.mycat.mysql.state.backend.BackendIdleState;
import io.mycat.mysql.state.backend.BackendCloseState;
import io.mycat.net2.states.ClosingState;
import io.mycat.net2.states.ReadWaitingState;
import io.mycat.util.CharsetUtil;
import io.mycat.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 认证状态
 *
 * @author ynfeng
 */
public class FrontendAuthenticatingState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendAuthenticatingState.class);
    public static final FrontendAuthenticatingState INSTANCE = new FrontendAuthenticatingState();

    private static final byte[] AUTH_OK = new byte[]{7, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0};

    private FrontendAuthenticatingState() {
    }

    /**
     * 对握手响应包进行认证
     *
     * @param mySQLConnection
     * @param attachment
     */
    @Override
    public boolean handle(MySQLConnection mySQLConnection, Object attachment) {
        MySQLFrontConnection mySQLFrontConnection = (MySQLFrontConnection) mySQLConnection;
        try {
            LOGGER.debug("Frontend in FrontendAuthenticatingState");
            AuthPacket auth = new AuthPacket();
            auth.read(mySQLFrontConnection.getDataBuffer());

            // Fake check user
            LOGGER.debug("Check user name. " + auth.user);
            if (!auth.user.equals("root")) {
                LOGGER.debug("User name error. " + auth.user);
                mySQLFrontConnection.failure(ErrorCode.ER_ACCESS_DENIED_ERROR, "Access denied for user '" + auth.user + "'");
                mySQLFrontConnection.setNextState(BackendCloseState.INSTANCE);
                return true;
            }

            // Fake check password
            LOGGER.debug("Check user password. " + new String(auth.password));

            // check schema
            LOGGER.debug("Check database. " + auth.database);

            return success(mySQLFrontConnection, auth);
        } catch (Throwable e) {
            LOGGER.warn("Frontend FrontendAuthenticatingState error:", e);
            mySQLFrontConnection.setNextState(BackendCloseState.INSTANCE);
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
            con.setNextState(BackendCloseState.INSTANCE);
            return true;
        }
        LOGGER.debug("charset = {}, charsetIndex = {}", charset, charsetIndex);
        con.setCharset(charsetIndex, charset);

        String db = StringUtil.isEmpty(auth.database)
                ? SQLEngineCtx.INSTANCE().getDefaultMycatSchema().getName() : auth.database;
        if (!con.setFrontSchema(db)) {
            final String errmsg = "No Mycat Schema defined: " + auth.database;
            LOGGER.debug(errmsg);
            con.writeErrMessage(ErrorCode.ER_BAD_DB_ERROR, "42000".getBytes(), errmsg);

            con.setWriteCompleteListener(() -> {
                con.setNextState(BackendCloseState.INSTANCE);
                con.setNextNetworkState(ClosingState.INSTANCE);
            });
        } else {
            con.write(AUTH_OK);
            con.setWriteCompleteListener(() -> {
                con.setNextState(FrontendIdleState.INSTANCE);
                con.clearCurrentPacket();
                con.getDataBuffer().clear();
                con.setNextNetworkState(ReadWaitingState.INSTANCE);
            });
        }
        return false;
    }
}
