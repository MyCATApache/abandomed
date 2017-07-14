package io.mycat.mysql.state;


import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.backend.callback.BackendComQueryCallback;
import io.mycat.engine.impl.FrontendComQueryCommandHandler;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.net2.ConDataBuffer;

/**
 * 查询状态
 *
 * @author ynfeng
 */
public class ComQueryState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComQueryState.class);
    public static final ComQueryState INSTANCE = new ComQueryState();

    private FrontendComQueryCommandHandler frontendComQueryCommandHandler = new FrontendComQueryCommandHandler();
    private BackendComQueryCallback backendComQueryCallback = new BackendComQueryCallback();

    private ComQueryState() {
    }

    @Override
    protected void frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment) {
    	LOGGER.debug("Frontend in ComQueryState");
        try {
            frontendComQueryCommandHandler.processCmd(
                    mySQLFrontConnection,
                    mySQLFrontConnection.getDataBuffer(),
                    mySQLFrontConnection.getCurrentPacketType(),
                    mySQLFrontConnection.getCurrentPacketStartPos(),
                    mySQLFrontConnection.getCurrentPacketLength()
            );
        } catch (Throwable e) {
            LOGGER.warn("frontend ComQueryState error", e);
            mySQLFrontConnection.changeState(CloseState.INSTANCE, "program error");
            throw new StateException(e);
        }
    }

    @Override
    protected void backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) {
    	LOGGER.debug("Backend in ComQueryState");
        try {
            backendComQueryCallback.handleResponse(
                    mySQLBackendConnection,
                    mySQLBackendConnection.getDataBuffer(),
                    mySQLBackendConnection.getCurrentPacketType(),
                    mySQLBackendConnection.getCurrentPacketStartPos(),
                    mySQLBackendConnection.getCurrentPacketLength()
            );
        } catch (IOException e) {
            LOGGER.warn("frontend ComQueryState error", e);
            mySQLBackendConnection.changeState(CloseState.INSTANCE, "program error");
            throw new StateException(e);
        }
    }
}
