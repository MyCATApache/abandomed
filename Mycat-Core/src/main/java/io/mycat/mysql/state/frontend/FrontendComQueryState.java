package io.mycat.mysql.state.frontend;


import io.mycat.backend.MySQLBackendConnection;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.machine.StateMachine;
import io.mycat.mysql.state.PacketProcessStateTemplete;
import io.mycat.mysql.state.backend.BackendComQueryResponseState;
import io.mycat.net2.Connection;
import io.mycat.net2.states.NoReadAndWriteState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 查询状态
 *
 * @author ynfeng
 */
public class FrontendComQueryState extends PacketProcessStateTemplete {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendComQueryState.class);
    public static final FrontendComQueryState INSTANCE = new FrontendComQueryState();

    private FrontendComQueryState() {
    }

    @Override
    public boolean handleShortHalfPacket(Connection connection, Object attachment, int packetStartPos) throws IOException {
        return false;
    }

    @Override
    public boolean handleLongHalfPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException {
        LOGGER.debug("Frontend in FrontendComQueryState long half packet");
        return internalProcess(connection, false);
    }

    @Override
    public boolean handleFullPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException {
        LOGGER.debug("Frontend in FrontendComQueryState");
        return internalProcess(connection, true);
    }

    private boolean internalProcess(Connection connection, boolean isFullPacket) throws IOException {
        MySQLFrontConnection frontCon = (MySQLFrontConnection) connection;
        MySQLBackendConnection backendConnection = frontCon.getBackendConnection();
        if (backendConnection == null) {   //往往新建立连接时,后端连接会是空
            backendConnection = getBackendFrontConnection(frontCon);
            MySQLBackendConnection finalBackendConnection = backendConnection;
            frontCon.addTodoTask(() -> {
                frontCon.startTransfer(finalBackendConnection, frontCon.getDataBuffer());
                if (isFullPacket) {
                    //TODO 这种人工干预的内存清理操作要想办法自动处理
                    //TODO 另外前后端的流量匹配应该在网络状态机中自动完成
                    //TODO 网络状态机上下文中已经放置了传输时的两个连接和buffer
                    finalBackendConnection.getDataBuffer().clear();
                    finalBackendConnection.getProtocolStateMachine().setNextState(BackendComQueryResponseState.INSTANCE);
                    frontCon.getProtocolStateMachine().setNextState(FrontendIdleState.INSTANCE);
                }
            });
        } else {
            backendConnection.getDataBuffer().clear();
            frontCon.startTransfer(backendConnection, frontCon.getDataBuffer());
            if (isFullPacket) {
                backendConnection.getProtocolStateMachine().setNextState(BackendComQueryResponseState.INSTANCE);
                frontCon.getProtocolStateMachine().setNextState(FrontendIdleState.INSTANCE);
            }
        }
        return false;
    }

}
