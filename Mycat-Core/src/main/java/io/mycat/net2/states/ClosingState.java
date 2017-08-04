package io.mycat.net2.states;

import java.io.IOException;

import io.mycat.common.State;
import io.mycat.common.StateMachine;

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
