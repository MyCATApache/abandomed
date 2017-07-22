package io.mycat.sqlcache;

import static com.google.common.hash.Hashing.murmur3_32;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.backend.MySQLDataSource;
import io.mycat.backend.MySQLReplicatSet;
import io.mycat.backend.callback.SQLResCacheHintHandler;
import io.mycat.beans.DNBean;
import io.mycat.bigmem.console.LocatePolicy;
import io.mycat.bigmem.sqlcache.BigSQLResult;
import io.mycat.bigmem.sqlcache.CacheImp;
import io.mycat.bigmem.sqlcache.IDataLoader;
import io.mycat.bigmem.sqlcache.IRemoveKeyListener;
import io.mycat.bigmem.sqlcache.Keyer;
import io.mycat.engine.UserSession;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.CommandPacket;
import io.mycat.mysql.packet.MySQLMessage;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.net2.states.WriteWaitingState;

/**
 * SQL 结果集缓存服务
 *
 * @author zagnix
 * @version 1.0
 * @create 2017-01-17 17:23
 */

public class SQLResultsCacheService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SQLResultsCacheService.class);
    private final CacheImp<String, BigSQLResult> sqlResultCacheImp;

    static class InitSQLResultsCacheService {
        public final static SQLResultsCacheService sqlResultsCacheService = new SQLResultsCacheService();
    }

    private SQLResultsCacheService() {
        sqlResultCacheImp = new CacheImp<String, BigSQLResult>();
    }

    /**
     * 将sql结果集缓存起来
     *
     * @param hintSQLInfo  Hint SQL相关信息
     * @param bigSQLResult sql结果集缓存
     * @param loader       结果集load or reload 接口
     * @param listener     key被移除时，调用的接口
     */
    public void cacheSQLResult(HintSQLInfo hintSQLInfo, BigSQLResult bigSQLResult, IDataLoader<String, BigSQLResult> loader,
                               IRemoveKeyListener<String, BigSQLResult> listener) {
        /**
         * cache-time=xxx auto-refresh=true access-count=5000
         */
        String key = "" + murmur3_32().hashUnencodedChars(hintSQLInfo.getExecSQL());

        Keyer<String, BigSQLResult> keyer = new Keyer<String, BigSQLResult>();
        keyer.setSql(hintSQLInfo.getExecSQL());
        keyer.setKey(key);
        keyer.setValue(bigSQLResult);
        keyer.setCacheTTL(Integer.valueOf(hintSQLInfo.getParamsKv().get("cache-time")));
        keyer.setAccessCount(Integer.valueOf(hintSQLInfo.getParamsKv().get("access-count")));
        // keyer.setAutoRefresh(Boolean.valueOf(hintSQLInfo.getParamsKv().get("auto-refresh")));
        keyer.setRemoveKeyListener(listener);
        keyer.setiDataLoader(loader);
        sqlResultCacheImp.put(key, bigSQLResult, keyer);
    }

    /**
     * 获取sql语句，已经缓存的结果集
     *
     * @param sql sql 语句
     * @return
     */
    public BigSQLResult getSQLResult(String sql) {
        String key = "" + murmur3_32().hashUnencodedChars(sql);
        return sqlResultCacheImp.get(key);
    }


    /**
     * 获取sql语句，已经缓存的结果集
     *
     * @param sql sql 语句
     */
    public void remove(String sql) {

        String key = "" + murmur3_32().hashUnencodedChars(sql);
        sqlResultCacheImp.remove(key);
    }


    /**
     * 处理Select Hint SQL 结果集函数
     *
     * @param frontCon
     * @param hintSQLInfo
     * @param mySQLMessage
     * @return
     * @throws IOException
     */
    public boolean processHintSQL(MySQLFrontConnection frontCon, HintSQLInfo hintSQLInfo, MySQLMessage mySQLMessage) throws IOException {

        /**
         * SQL Cache 结果集缓存框架实现：
         * 1.根据解析处理的sql，查询SQLCache是否已经缓存了。缓存了，则直接从Cache中获取结果集发给client端即可
         * 2.没有缓存的情况则将sql执行发送的后端Mysql数据库，执行并发查询，返回的结果集，分二部走
         *          1>. 直接透传给Client
         *          2>. 缓存到MyCat本地
         * 3.根据Hint注解的属性，决定异步拉去后端的结果集，替换掉旧的数据集
         */
        BigSQLResult sqlResultCache = getSQLResult(hintSQLInfo.getExecSQL());

        if (sqlResultCache != null) {
            LOGGER.error(hintSQLInfo.getExecSQL() + ":====>>>> Use Local Cache SQL Resuls");
            sqlResultCacheDirectClient(frontCon, sqlResultCache);
            return true;
        } else {
            /**从后端拉取数据进行缓存*/
            sqlResultCache =
                    new BigSQLResult(LocatePolicy.Normal, hintSQLInfo.getExecSQL(), 32 * 1024 * 1024/**TODO*/);
        }

        /**
         * 1. 改写sql语句，去掉前面的注释
         */
        CommandPacket command = new CommandPacket();
        command.packetId = 0;
        command.command = MySQLPacket.COM_QUERY;

        /**
         * 2. 获取后端DB连接池，执行rewrite sql
         */
        MySQLBackendConnection existCon = null;
        UserSession session = frontCon.getSession();
        ArrayList<MySQLBackendConnection> allBackCons = session.getBackendCons();
        if (!allBackCons.isEmpty()) {
            existCon = allBackCons.get(0);
        }
        if (existCon == null || existCon.isClosed()) {
            if (existCon != null) {
                session.removeBackCon(existCon);
            }
            if (frontCon.getMycatSchema() == null) {
                frontCon.writeErrMessage(1450, "No schema selected");
                return false;
            }

            final DNBean dnBean = frontCon.getMycatSchema().getDefaultDN();
            final String replica = dnBean.getMysqlReplica();
            final SQLEngineCtx ctx = SQLEngineCtx.INSTANCE();
            final MySQLReplicatSet repSet = ctx.getMySQLReplicatSet(replica);
            final MySQLDataSource datas = repSet.getCurWriteDH();
            /**
             * 如果该sql对应后端db，没有连接池，则创建连接池部分
             */
            final MySQLBackendConnection newCon =
                    datas.getConnection(frontCon.getReactor(), dnBean.getDatabase(), true, frontCon, new SQLResCacheHintHandler(hintSQLInfo, sqlResultCache));

//            /**
//             * 执行sql语句
//             */
//            frontCon.addTodoTask(() -> {
//                /**
//                 * 将数据写到后端连接池中
//                 */
//                command.arg = hintSQLInfo.getExecSQL().getBytes(newCon.getCharset());
//                newCon.getWriteDataBuffer().putBytes(command.write(newCon));
//                newCon.enableWrite(false);
//                /**
//                 * 新建立的连接放到连接池中
//                 */
//                session.addBackCon(newCon);
//
//            });
        } else {
            /**
             * 否则直接写到后端即可
             */
            command.arg = hintSQLInfo.getExecSQL().getBytes(existCon.getCharset());
            LOGGER.debug(" here has bug , please fix it ");  //TODO existCon.getWriteDataBuffer().putBytes(command.write(existCon));
            existCon.setNextNetworkState(WriteWaitingState.INSTANCE);
            /**设置后端连接池结果集处理handler,sqlResultCache缓存结果集类*/
            existCon.setUserCallback(new SQLResCacheHintHandler(hintSQLInfo, sqlResultCache));
        }

        return true;
    }

    /**
     * 处理Local Cache结果集,直接返回给Front Connection端
     *
     * @param frontConn
     * @param sqlResultCache
     */
    private void sqlResultCacheDirectClient(MySQLFrontConnection frontConn, BigSQLResult sqlResultCache) throws IOException {
//        sqlResultCache.reset();
//        int filedCount = 0;
//        int status = Connection.STATE_IDLE;
//        while (sqlResultCache.hasNext()){
//            byte [] datas = sqlResultCache.next();
//            byte packetType = datas[MySQLConnection.msyql_packetHeaderSize];
//            ByteBuffer packetBuffer = ByteBuffer.wrap(datas);
//            frontConn.getWriteDataBuffer().putBytes(packetBuffer);
//            frontConn.enableWrite(false);
//
//          if (false) {
//              switch (status) {
//                  case Connection.STATE_IDLE:
//                      if (packetType == MySQLPacket.COM_QUERY) {
//                          status = Connection.STATE_IDLE;
//                      } else if (packetType == MySQLPacket.COM_QUIT) {
//                          status = (Connection.STATE_IDLE);
//                      } else if (sqlResultCache != null) {
//                          /**step 1: Result Set Header Packet 列的数目*/
//                          status = RESULT_HEADER_STATUS;
//                          ResultSetHeaderPacket resultSetHeaderPacket = new ResultSetHeaderPacket();
//                          resultSetHeaderPacket.read(packetBuffer);
//                          filedCount = resultSetHeaderPacket.fieldCount;
//                          LOGGER.error("step 1 =====>  DB status: Result Filed Count " + filedCount);
//
//                      }
//                      break;
//                  case RESULT_HEADER_STATUS:
//                      if (packetType == MySQLPacket.EOF_PACKET) {
//                          LOGGER.error("step 3 : EOF Packet,marker—end of field packets");
//                          status = RESULT_FETCH_STATUS;
//                      } else if (packetType == MySQLPacket.ERROR_PACKET) {
//                          status = (Connection.STATE_IDLE);
//                      } else if (sqlResultCache != null) {
//                          /**Step 2: Field Packets,列的描述信息*/
//
//                          FieldPacket fieldPacket = new FieldPacket();
//                          fieldPacket.read(packetBuffer);
//                          //frontConn.getChannel().write(packetBuffer);
//
//                          LOGGER.error("Step 2: Field Packets");
//
//                      }
//                      break;
//                  case RESULT_FETCH_STATUS:
//                      if (packetType == MySQLPacket.EOF_PACKET) {
//                          /**Step 5:EOF Packet: marker---end of row data packets*/
//                          LOGGER.error("Step 5:EOF Packet: marker---end of row data packets");
//                          status = Connection.STATE_IDLE;
//                          break;
//                      } else if (packetType == MySQLPacket.ERROR_PACKET) {
//                          status = (Connection.STATE_IDLE);
//                      } else if (sqlResultCache != null) {
//                          /**Step 4: Row Data Packet{多个}: row contents 一行对应一个row packet*/
//
//                          LOGGER.error("Step 4: Row Data Packet");
//                          RowDataPacket rowDataPacket = new RowDataPacket(filedCount);
//                          rowDataPacket.read(packetBuffer);
//                          for (int i = 0; i < filedCount; i++) {
//                              byte[] src = rowDataPacket.fieldValues.get(i);
//                              if (src != null)
//                                  LOGGER.error("filed value: " + new String(src));
//                          }
//                      }
//                      break;
//                  default:
//                      LOGGER.warn("Error connected status.", status);
//                      break;
//              }
//          }
//        }
    }

    /**
     * @return
     */
    public static SQLResultsCacheService getInstance() {
        return InitSQLResultsCacheService.sqlResultsCacheService;
    }

}
