package io.mycat.net2.states;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import io.mycat.common.State;
import io.mycat.common.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;

public class WriteWaitingState implements State {

    private static final Logger LOGGER = LoggerFactory.getLogger(WriteWaitingState.class);
    public static final WriteWaitingState INSTANCE = new WriteWaitingState();

    private WriteWaitingState() {
    }


    @Override
    public boolean handle(StateMachine context, Connection connection, Object attachment)
            throws IOException {
        SelectionKey processKey = connection.getProcessKey();
        Connection.NetworkStateMachine networkStateMachine = ((Connection.NetworkStateMachine) context);
        try {
            processKey.interestOps(processKey.interestOps() & Connection.OP_NOT_READ);
            processKey.interestOps(processKey.interestOps() | SelectionKey.OP_WRITE);
            connection.getNetworkStateMachine().setNextState(WriteState.INSTANCE);
            if (connection != networkStateMachine.getDest()) {
                networkStateMachine.getDest().getNetworkStateMachine().setNextState(NoReadState.INSTANCE);
            }
            processKey.selector().wakeup();
        } catch (Exception e) {
            LOGGER.warn("enable read fail " + e);
            connection.getNetworkStateMachine().setNextState(ClosingState.INSTANCE);
        }
        return false;
    }

}

