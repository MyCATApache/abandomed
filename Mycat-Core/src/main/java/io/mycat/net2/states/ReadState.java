package io.mycat.net2.states;

import java.io.IOException;

import io.mycat.machine.State;
import io.mycat.machine.StateMachine;
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
    public boolean handle(StateMachine context, Connection conn, Object attachment)
            throws IOException {
        TRY_READ_RESULT res;
        LOGGER.debug("Current conn in ReadState. conn is " + conn.getClass() + ",buffer is" + conn.getDataBuffer());
        res = conn.try_read_network();     /* writePos 指针向前移动   */
        switch (res) {
            case READ_NO_DATA_RECEIVED:
                conn.getNetworkStateMachine().setNextState(ReadWaitingState.INSTANCE);
                break;
            case READ_DATA_RECEIVED:   /* 数据读取完成,开始解析命令 */
                conn.getNetworkStateMachine().setNextState(ParseCmdState.INSTANCE);
                break;
            case READ_ERROR:
                conn.getNetworkStateMachine().setNextState(ClosingState.INSTANCE);
                break;
            case READ_MEMORY_ERROR: /* Failed to allocate more memory */
                conn.getNetworkStateMachine().setNextState(ClosingState.INSTANCE);
                break;
        }
        return true;
    }

}
