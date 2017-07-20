package io.mycat.mysql.state;

import java.io.IOException;

import io.mycat.mysql.MySQLConnection;

/**
 * mysql连接的状态接口
 *
 * @author ynfeng
 */
public interface MysqlConnectionState {
    boolean handle(MySQLConnection mySQLConnection,Object attachment)throws IOException;
}
