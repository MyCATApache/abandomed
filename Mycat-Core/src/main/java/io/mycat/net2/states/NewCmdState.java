package io.mycat.net2.states;

import java.io.IOException;

import io.mycat.common.State;
import io.mycat.common.StateMachine;

import io.mycat.net2.Connection;

/**
 * @author yanjunli
 */
public class NewCmdState implements State {

    public static final NewCmdState INSTANCE = new NewCmdState();


    private NewCmdState() {
    }

    @Override
    public boolean handle(StateMachine context, Connection connection, Object attachment) throws IOException {
        Connection.NetworkStateMachine networkStateMachine = ((Connection.NetworkStateMachine) context);
        if (networkStateMachine.getBuffer().writableBytes() < (networkStateMachine.getBuffer().capacity() / 2)) {
            networkStateMachine.getBuffer().compact();
        }
        networkStateMachine.setNextState(ReadWaitingState.INSTANCE);
        if (networkStateMachine.getDest() != connection) {
            networkStateMachine.getDest().getNetworkStateMachine().setNextState(ReadWaitingState.INSTANCE);
        }
        return true;
    }

}
