package io.mycat.net2.states;

import java.io.IOException;

import io.mycat.machine.State;
import io.mycat.machine.StateMachine;
import io.mycat.mysql.MySQLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.WriteCompleteListener;
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
        networkStateMachine.getBuffer().compact();
        networkStateMachine.setNextState(ReadWaitingState.INSTANCE);
        if (networkStateMachine.getDest() != connection) {
            networkStateMachine.getDest().getNetworkStateMachine().setNextState(ReadWaitingState.INSTANCE);
        }
        return true;
    }

}
