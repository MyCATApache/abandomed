package io.mycat.net2.states;

import java.io.IOException;

import io.mycat.common.State;
import io.mycat.common.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;
import io.mycat.net2.states.network.TRY_READ_RESULT;


public class ReadState implements State {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadState.class);
    public static final ReadState INSTANCE = new ReadState();

    private ReadState() {
    }

    @Override
    public boolean handle(StateMachine context, Connection connection, Object attachment)
            throws IOException {
        TRY_READ_RESULT res;
        res = connection.try_read_network();
        LOGGER.debug(connection.getClass().getSimpleName() + " read from network result " + res + "." + connection);
        switch (res) {
            case READ_NO_DATA_RECEIVED:
                connection.getNetworkStateMachine().setNextState(ReadWaitingState.INSTANCE);
                break;
            case READ_DATA_RECEIVED:   /* 数据读取完成,开始解析命令 */
                connection.getNetworkStateMachine().setNextState(ParseCmdState.INSTANCE);
                break;
            case READ_ERROR:
                connection.getNetworkStateMachine().setNextState(ClosingState.INSTANCE);
                break;
            case READ_MEMORY_ERROR: /* Failed to allocate more memory */
                connection.getNetworkStateMachine().setNextState(ClosingState.INSTANCE);
                break;
        }
        return true;
    }

}
