package io.mycat.net2.states;

import java.io.IOException;

import io.mycat.machine.State;
import io.mycat.machine.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;

public class ClosingState implements State {
    public static final ClosingState INSTANCE = new ClosingState();


    private ClosingState() {
    }

    @Override
    public boolean handle(StateMachine context, Connection conn, Object attachment)
            throws IOException {
        conn.close(" close connection!");
        return false;
    }

}
