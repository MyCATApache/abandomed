package io.mycat.net2.states;

import java.io.IOException;

import io.mycat.common.State;
import io.mycat.common.StateMachine;

import io.mycat.net2.Connection;

public class ClosedState implements State {
    public static final ClosedState INSTANCE = new ClosedState();


    private ClosedState() {
    }

    @Override
    public boolean handle(StateMachine context, Connection conn, Object attachment)
            throws IOException {
        return false;
    }
}
