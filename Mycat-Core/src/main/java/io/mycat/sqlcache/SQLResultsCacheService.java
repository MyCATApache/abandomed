package io.mycat.sqlcache;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.backend.MySQLDataSource;
import io.mycat.backend.MySQLReplicatSet;
import io.mycat.backend.callback.SQLResCacheHintHandler;
import io.mycat.beans.DNBean;
import io.mycat.engine.UserSession;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;


/**
 * SQL 结果集缓存服务
 *
 * @author zagnix
 * @version 1.0
 * @create 2017-01-17 17:23
 */

public class SQLResultsCacheService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SQLResultsCacheService.class);
    static class InitSQLResultsCacheService{
        public  final static SQLResultsCacheService sqlResultsCacheService = new SQLResultsCacheService();
    }

    private SQLResultsCacheService(){
    }



    /**
     *  处理Select Hint SQL 结果集函数
     * @param frontCon
     * @param hintSQLInfo
     * @param mySQLMessage
     * @return
     * @throws IOException
     */
    public boolean processHintSQL(MySQLFrontConnection frontCon,HintSQLInfo hintSQLInfo, MySQLMessage mySQLMessage) throws IOException {



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
        UserSession session=frontCon.getSession();
        ArrayList<MySQLBackendConnection> allBackCons = session.getBackendCons();
        if (!allBackCons.isEmpty()) {
            existCon = allBackCons.get(0);
        }
        if (existCon == null || existCon.isClosed()) {
            if (existCon != null) {
                session.removeBackCon(existCon);
            }
            if (frontCon.getMycatSchema() == null){
                frontCon.writeErrMessage(1450, "No schema selected");
                return false;
            }

            final DNBean dnBean = frontCon.getMycatSchema().getDefaultDN();
            final String replica   = dnBean.getMysqlReplica();
            final SQLEngineCtx ctx = SQLEngineCtx.INSTANCE();
            final MySQLReplicatSet repSet = ctx.getMySQLReplicatSet(replica);
            final MySQLDataSource datas   = repSet.getCurWriteDH();
            /**
             * 如果该sql对应后端db，没有连接池，则创建连接池部分
             */
            final MySQLBackendConnection newCon =
                    datas.getConnection(frontCon.getReactor(), dnBean.getDatabase(), true, null);

            /**很关键的设置前端front 与 backend session*/
            newCon.setAttachement(frontCon);

            /**设置后端连接池结果集处理handler,sqlResultCache缓存结果集类*/
            newCon.setUserCallback(new SQLResCacheHintHandler());

            /**
             * 执行sql语句
             */
            frontCon.addTodoTask(() -> {
                /**
                 * 将数据写到后端连接池中
                 */
                command.arg = hintSQLInfo.getExecSQL().getBytes(newCon.getCharset());
                newCon.getWriteDataBuffer().putBytes(command.write(newCon));
                newCon.enableWrite(false);
                /**
                 * 新建立的连接放到连接池中
                 */
                session.addBackCon(newCon);

            });
        } else {
            /**
             * 否则直接写到后端即可
             */
            command.arg = hintSQLInfo.getExecSQL().getBytes(existCon.getCharset());
            existCon.getWriteDataBuffer().putBytes(command.write(existCon));
            existCon.enableWrite(false);
            /**设置后端连接池结果集处理handler,sqlResultCache缓存结果集类*/
            existCon.setUserCallback(new SQLResCacheHintHandler());
        }

        return true;
    }

    /**
     *
     * @return
     */
    public static SQLResultsCacheService getInstance()
    {
        return InitSQLResultsCacheService.sqlResultsCacheService;
    }

}
