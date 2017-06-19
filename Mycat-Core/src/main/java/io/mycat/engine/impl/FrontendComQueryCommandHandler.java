package io.mycat.engine.impl;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.state.ComQueryResponseState;
import io.mycat.net2.ConDataBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 前端查询命令响应
 * <p>
 * 此类未根据sql类型做特殊处理，后面可根据实际情况，比如主从，获取对应的后端连接
 *
 * @author ynfeng
 */
public class FrontendComQueryCommandHandler extends AbstractComQueryCommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendComQueryCommandHandler.class);

    @Override
    public void processCmd(MySQLFrontConnection frontCon, ConDataBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
        frontCon.setNextState(ComQueryResponseState.INSTANCE);
        MySQLBackendConnection backendConnection = frontCon.getBackendConnection();
        if (backendConnection == null) {
            backendConnection = getBackendFrontConnection(frontCon);
            MySQLBackendConnection finalBackendConnection = backendConnection;
            frontCon.addTodoTask(() -> {
                finalBackendConnection.setShareBuffer(dataBuffer);
                finalBackendConnection.setDirectTransferParams(packageType, pkgStartPos, pkgLen);
                finalBackendConnection.driveState();
            });
        } else {
            backendConnection.setShareBuffer(dataBuffer);
            backendConnection.setDirectTransferParams(packageType, pkgStartPos, pkgLen);
            backendConnection.driveState();
        }
    }
}
