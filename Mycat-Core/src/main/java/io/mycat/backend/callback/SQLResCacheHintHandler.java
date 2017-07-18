package io.mycat.backend.callback;

import io.mycat.backend.BackConnectionCallback;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.bigmem.sqlcache.BigSQLResult;
import io.mycat.bigmem.sqlcache.IDataLoader;
import io.mycat.bigmem.sqlcache.IRemoveKeyListener;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.net2.ConnectionException;
import io.mycat.sqlcache.*;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * SQL 结果集缓存 HintHandler
 *
 * @author zagnix
 * @create 2017-01-17 14:11
 */

public class SQLResCacheHintHandler implements BackConnectionCallback, HintHandler {
    private final BigSQLResult sqlResultCache;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SQLResCacheHintHandler.class);
    private int filedCount = 0;
    private IDataLoader<String, BigSQLResult> loader;
    private IRemoveKeyListener<String, BigSQLResult> listener;
    private HintSQLInfo hintSQLInfo;

    public SQLResCacheHintHandler(HintSQLInfo hintSQLInfo, BigSQLResult sqlResultCache) {
        this.hintSQLInfo = hintSQLInfo;
        this.sqlResultCache = sqlResultCache;
        this.listener = new HintSQLRemoveKeyListener<>();
        this.loader = new HintSQLDataLoader<>();
        this.sqlResultCache.reset();
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
//        ((MySQLFrontConnection)conn.getAttachement()).executePendingTask();
    }

    @Override
    public void handleResponse(MySQLBackendConnection conn, MycatByteBuffer dataBuffer, byte packetType, int pkgStartPos, int pkgLen) throws IOException {
//        //LOGGER.error("Hint SQL handleResponse");
//        MySQLFrontConnection frontCon = conn.getMySQLFrontConnection();
//        /**
//         * 直接透传给前端client即可，
//         * 缓存框架需要在这里将收到的数据写到缓存中
//         */
//        ByteBuffer packetBuffer = dataBuffer.getBytes(pkgStartPos, pkgLen);
//        frontCon.getWriteDataBuffer().putBytes(packetBuffer);
//        frontCon.enableWrite(false);
//        int status = conn.getState();
//        switch (status) {
//            case Connection.STATE_IDLE:
//                if (packetType == MySQLPacket.COM_QUERY) {
//                    conn.setState(Connection.STATE_IDLE);
//                } else if (packetType == MySQLPacket.COM_QUIT) {
//                    conn.setState(Connection.STATE_IDLE);
//                } else if (sqlResultCache != null) {
//                    /**step 1: Result Set Header Packet 列的数目*/
//                    LOGGER.error("step 1 =====>  DB status: Result Filed Count ");
//                    byte[] headers = new byte[packetBuffer.readableBytes()];
//                    packetBuffer.get(headers);
//                    sqlResultCache.put(headers);
//                    conn.setState(RESULT_HEADER_STATUS);
//                }
//                break;
//            case RESULT_HEADER_STATUS:
//                if (packetType == MySQLPacket.EOF_PACKET) {
//
//                    LOGGER.error("step 3 : EOF Packet,marker—end of field packets");
//                    byte[] fieldDescsEof = new byte[packetBuffer.readableBytes()];
//                    packetBuffer.get(fieldDescsEof);
//                    sqlResultCache.put(fieldDescsEof);
//
//                    conn.setState(RESULT_FETCH_STATUS);
//                } else if (packetType == MySQLPacket.ERROR_PACKET) {
//                    conn.setState(Connection.STATE_IDLE);
//                } else if (sqlResultCache != null) {
//                    /**Step 2: Field Packets,列的描述信息*/
//                    LOGGER.error("Step 2: Field Packets");
//                    byte[] fieldDescs = new byte[packetBuffer.readableBytes()];
//                    packetBuffer.get(fieldDescs);
//                    sqlResultCache.put(fieldDescs);
//                }
//                break;
//            case RESULT_FETCH_STATUS:
//                if (packetType == MySQLPacket.EOF_PACKET) {
//                    /**Step 5:EOF Packet: marker---end of row data packets*/
//                    LOGGER.error("Step 5:EOF Packet: marker---end of row data packets");
//                    byte[] rowDataEof = new byte[packetBuffer.readableBytes()];
//                    packetBuffer.get(rowDataEof);
//                    sqlResultCache.put(rowDataEof);
//                    /**将sqlResultCache插入CacheService中*/
//                    SQLResultsCacheService.getInstance().
//                            cacheSQLResult(hintSQLInfo, sqlResultCache, loader, listener);
//
//                    conn.setState(Connection.STATE_IDLE);
//                } else if (packetType == MySQLPacket.ERROR_PACKET) {
//                    conn.setState(Connection.STATE_IDLE);
//                } else if (sqlResultCache != null) {
//                    /**Step 4: Row Data Packet{多个}: row contents 一行对应一个row packet*/
//                    LOGGER.error("Step 4: Row Data Packet");
//                    byte[] rowDatas = new byte[packetBuffer.readableBytes()];
//                    packetBuffer.get(rowDatas);
//                    sqlResultCache.put(rowDatas);
//                }
//                break;
//            default:
//                LOGGER.warn("Error connected status.", status);
//                break;
//        }
//
//        if (conn.getState() == Connection.STATE_IDLE) {
//            LOGGER.debug("realese bakend connedtion " + conn);
//            conn.release();
//        }
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
