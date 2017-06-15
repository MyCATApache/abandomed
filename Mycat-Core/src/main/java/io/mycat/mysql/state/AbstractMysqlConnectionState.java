package io.mycat.mysql.state;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.MySQLConnection;

/**
 * 状态基类，封装前后端状态处理分发
 *
 * @author ynfeng
 */
public abstract class AbstractMysqlConnectionState implements MysqlConnectionState {

    @Override
    public void handle(MySQLConnection mySQLConnection, Object attachment) {
        if (mySQLConnection instanceof MySQLBackendConnection) {
            backendHandle((MySQLBackendConnection) mySQLConnection, attachment);
        } else if (mySQLConnection instanceof MySQLFrontConnection) {
            frontendHandle((MySQLFrontConnection) mySQLConnection, attachment);
        } else {
            throw new IllegalArgumentException("No such type of MySQLConnection:" + mySQLConnection.getClass());
        }
    }

    abstract protected void frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment);

    abstract protected void backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment);
}
