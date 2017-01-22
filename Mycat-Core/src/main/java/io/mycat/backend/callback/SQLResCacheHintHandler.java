package io.mycat.backend.callback;

import io.mycat.backend.BackConnectionCallback;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.Connection;
import io.mycat.net2.ConnectionException;
import io.mycat.sqlcache.HintHandler;
import io.mycat.sqlcache.HintSQLInfo;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * SQL 结果集缓存 HintHandler
 *
 * @author zagnix
 * @create 2017-01-17 14:11
 */

public class SQLResCacheHintHandler implements BackConnectionCallback,HintHandler {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SQLResCacheHintHandler.class);
    private int filedCount = 0;
    private HintSQLInfo hintSQLInfo;
    public SQLResCacheHintHandler(){
        this.hintSQLInfo = hintSQLInfo;
    }

    @Override
    public boolean handle(String sql) {
        return false;
    }

    @Override
    public void connectionError(ConnectionException e, MySQLBackendConnection conn) {
        LOGGER.error("Hint SQL connectionError");

    }

    @Override
    public void connectionAcquired(MySQLBackendConnection conn) {
        LOGGER.error("Hint SQL connectionAcquired");
        ((MySQLFrontConnection)conn.getAttachement()).executePendingTask();
    }

    @Override
    public void handleResponse(MySQLBackendConnection conn, ConDataBuffer dataBuffer, byte packetType, int pkgStartPos, int pkgLen) throws IOException {
        //LOGGER.error("Hint SQL handleResponse");
        MySQLFrontConnection frontCon= (MySQLFrontConnection) conn.getAttachement();
        /**
         * 直接透传给前端client即可，
         * 缓存框架需要在这里将收到的数据写到缓存中
         */
        ByteBuffer packetBuffer = dataBuffer.getBytes(pkgStartPos,pkgLen);
        frontCon.getWriteDataBuffer().putBytes(packetBuffer);
        frontCon.enableWrite(false);
        conn.setNextStatus(packetType);

        if (conn.getState()== Connection.STATE_IDLE)
        {
            LOGGER.debug("realese bakend connedtion "+conn);
            conn.release();
        }
    }

    @Override
    public void connectionClose(MySQLBackendConnection conn, String reason) {
        LOGGER.error("Hint SQL connectionClose");

    }

    @Override
    public void handlerError(Exception e, MySQLBackendConnection conn) {
        LOGGER.error("Hint SQL handlerError");

    }
}
