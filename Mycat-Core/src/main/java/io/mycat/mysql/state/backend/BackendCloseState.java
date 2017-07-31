package io.mycat.mysql.state.backend;


import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.state.AbstractMysqlConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 关闭状态
 *
 * @author ynfeng
 */
public class BackendCloseState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendCloseState.class);

    public static final BackendCloseState INSTANCE = new BackendCloseState();

    private BackendCloseState() {
    }


    @Override
    public boolean handle(MySQLConnection mySQLConnection, Object attachment) {
        return false;
    }
}
