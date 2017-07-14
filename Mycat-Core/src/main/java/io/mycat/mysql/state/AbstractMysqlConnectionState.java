package io.mycat.mysql.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.MySQLConnection;

/**
 * 状态基类，封装前后端状态处理分发
 *
 * @author ynfeng
 */
public abstract class AbstractMysqlConnectionState implements MysqlConnectionState {
	private static final Logger LOGGER = LoggerFactory.getLogger(ComQueryRowState.class);

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
