package io.mycat.mysql.state;

import io.mycat.mysql.MySQLConnection;

/**
 * mysql连接的状态接口
 *
 * @author ynfeng
 */
public interface MysqlConnectionState {
    void handle(MySQLConnection mySQLConnection,Object attachment);
}
