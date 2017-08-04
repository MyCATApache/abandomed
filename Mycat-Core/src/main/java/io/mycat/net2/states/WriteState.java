package io.mycat.net2.states;

import java.io.IOException;

import io.mycat.buffer.MycatByteBuffer;
import io.mycat.common.State;
import io.mycat.common.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;
import io.mycat.net2.states.network.TRANSMIT_RESULT;

public class WriteState implements State {

    private static final Logger LOGGER = LoggerFactory.getLogger(WriteState.class);
    public static final WriteState INSTANCE = new WriteState();

    private WriteState() {
    }

    @Override
    public boolean handle(StateMachine context, Connection connection, Object attachment)
            throws IOException {
        Connection.NetworkStateMachine networkStateMachine = (Connection.NetworkStateMachine) context;
        MycatByteBuffer byteBuffer = networkStateMachine.getBuffer();
        TRANSMIT_RESULT result = connection.write(byteBuffer, networkStateMachine.getWriteRemaining());
        LOGGER.debug(connection.getClass().getSimpleName() + " write to network result " + result + "." + connection);
        switch (result) {
            case TRANSMIT_COMPLETE:
                networkStateMachine.setWriteRemaining(0);
                connection.getNetworkStateMachine().setNextState(NewCmdState.INSTANCE);
                return true;
            case TRANSMIT_INCOMPLETE:
                int updateRemaining = networkStateMachine.getWriteRemaining() - (networkStateMachine.getWriteRemaining() - byteBuffer.readableBytes());
                networkStateMachine.setWriteRemaining(updateRemaining);
                return true;  //没有传输完成,继续保持当前状态,继续传输
            case TRANSMIT_HARD_ERROR: /* 连接断开了... */
                connection.getNetworkStateMachine().setNextState(ClosingState.INSTANCE);
                return true;
            case TRANSMIT_SOFT_ERROR:  /* socket write buffer pool is full */
                return false;
        }
        return true;
    }

}
