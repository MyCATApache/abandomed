package io.mycat.net2.states;

import java.io.IOException;
import java.nio.ByteBuffer;

import io.mycat.buffer.MycatByteBuffer;
import io.mycat.machine.State;
import io.mycat.machine.StateMachine;
import io.mycat.mysql.MySQLConnection;
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
    public boolean handle(StateMachine context, Connection conn, Object attachment)
            throws IOException {
        LOGGER.debug("Current conn in WriteState. conn is " + conn.getClass());
        Connection.NetworkStateMachine networkStateMachine = (Connection.NetworkStateMachine) context;
        MycatByteBuffer byteBuffer = networkStateMachine.getBuffer();
        TRANSMIT_RESULT result = conn.write(byteBuffer, networkStateMachine.getWriteRemaining());
        switch (result) {
            case TRANSMIT_COMPLETE:
                LOGGER.debug("Current conn in WriteState  TRANSMIT_COMPLETE. conn is " + conn.getClass() + " buffer is " + byteBuffer);
                networkStateMachine.setWriteRemaining(0);
                conn.getNetworkStateMachine().setNextState(NewCmdState.INSTANCE);
                return true;
            case TRANSMIT_INCOMPLETE:
                LOGGER.debug("Current conn in WriteState TRANSMIT_INCOMPLETE conn is " + conn.getClass());
                int updateRemaining = networkStateMachine.getWriteRemaining() - (networkStateMachine.getWriteRemaining() - byteBuffer.readableBytes());
                networkStateMachine.setWriteRemaining(updateRemaining);
                return true;  //没有传输完成,继续保持当前状态,继续传输
            case TRANSMIT_HARD_ERROR: /* 连接断开了... */
                LOGGER.debug("Current conn in WriteState TRANSMIT_HARD_ERROR conn is " + conn.getClass());
                conn.getNetworkStateMachine().setNextState(ClosingState.INSTANCE);
                return true;
            case TRANSMIT_SOFT_ERROR:  /* socket write buffer pool is full */
                LOGGER.debug("Current conn in WriteState ,socket write buffer pool is full. conn is " + conn.getClass());
                return false;
        }
        return true;
    }

}
