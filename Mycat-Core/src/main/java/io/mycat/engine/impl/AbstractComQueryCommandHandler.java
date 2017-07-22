package io.mycat.engine.impl;

import java.io.IOException;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.backend.MySQLBackendConnectionFactory;
import io.mycat.backend.MySQLDataSource;
import io.mycat.backend.MySQLReplicatSet;
import io.mycat.beans.DNBean;
import io.mycat.engine.SQLCommandHandler;
import io.mycat.front.MySQLFrontConnection;

/**
 * ComQuery命令处理器基类
 *
 * @author ynfeng
 */
public abstract class AbstractComQueryCommandHandler implements SQLCommandHandler {

    protected MySQLBackendConnection getBackendFrontConnection(MySQLFrontConnection mySQLFrontConnection) throws IOException {
//        // 直接透传（默认）获取连接池
//        MySQLBackendConnection existCon = null;
//        UserSession session = mySQLFrontConnection.getSession();
//        ArrayList<MySQLBackendConnection> allBackCons = session.getBackendCons();
//        if (!allBackCons.isEmpty()) {
//            existCon = allBackCons.get(0);
//        }
//        if (existCon == null || existCon.isClosed()) {
//            if (existCon != null) {
//                session.removeBackCon(existCon);
//            }
            final DNBean dnBean = mySQLFrontConnection.getMycatSchema().getDefaultDN();
            final String replica = dnBean.getMysqlReplica();
            final SQLEngineCtx ctx = SQLEngineCtx.INSTANCE();
            final MySQLReplicatSet repSet = ctx.getMySQLReplicatSet(replica);
            final MySQLDataSource datas = repSet.getCurWriteDH();
//
//            /**
//             * 如果该sql对应后端db，没有连接池，则创建连接池部分
//             */
//        MySQLBackendConnection existCon = datas.getConnection(mySQLFrontConnection.getReactor(), dnBean.getDatabase(), true, mySQLFrontConnection, null);
//        }
        MySQLBackendConnection con = new MySQLBackendConnectionFactory().make(datas, mySQLFrontConnection.getReactor(), dnBean.getDatabase(), mySQLFrontConnection, null);
                mySQLFrontConnection.setBackendConnection(con);
        return con;
    }
}
