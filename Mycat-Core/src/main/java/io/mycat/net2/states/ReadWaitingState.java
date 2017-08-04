package io.mycat.net2.states;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import io.mycat.machine.State;
import io.mycat.machine.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;

public class ReadWaitingState implements State {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadWaitingState.class);
    public static final ReadWaitingState INSTANCE = new ReadWaitingState();

    private ReadWaitingState() {
    }

    @Override
    public boolean handle(StateMachine context, Connection connection, Object attachment) throws IOException {
        try {
            Connection.NetworkStateMachine networkStateMachine = ((Connection.NetworkStateMachine) context);
            connection.getProcessKey().interestOps(connection.getProcessKey().interestOps() & Connection.OP_NOT_WRITE);
            connection.getProcessKey().interestOps(connection.getProcessKey().interestOps() | SelectionKey.OP_READ);
            connection.getNetworkStateMachine().setNextState(ReadState.INSTANCE);
            if (connection != networkStateMachine.getDest()) {
                networkStateMachine.getDest().getNetworkStateMachine().setNextState(NoWriteState.INSTANCE);
            }
            connection.getProcessKey().selector().wakeup();
        } catch (Exception e) {
            LOGGER.warn("enable read fail ", e);
            connection.getNetworkStateMachine().setNextState(ClosingState.INSTANCE);
        }
        return false;
    }

}
