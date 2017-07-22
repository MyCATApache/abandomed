package io.mycat.backend.callback;

import io.mycat.backend.BackConnectionCallback;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.net2.ConnectionException;

/**
 * 只处理响应的适配器
 *
 * @author ynfeng
 */
public abstract class ResponseCallbackAdapter implements BackConnectionCallback {
    @Override
    public void connectionError(ConnectionException e, MySQLBackendConnection conn) {

    }

    @Override
    public void connectionAcquired(MySQLBackendConnection conn) {

    }

    @Override
    public void connectionClose(MySQLBackendConnection conn, String reason) {

    }

    @Override
    public void handlerError(Exception e, MySQLBackendConnection conn) {

    }
}
