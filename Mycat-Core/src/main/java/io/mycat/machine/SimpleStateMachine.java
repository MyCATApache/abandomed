package io.mycat.machine;

import io.mycat.net2.Connection;

import java.io.IOException;

/**
 * Created by ynfeng on 2017/7/31.
 */
public class SimpleStateMachine implements StateMachine {
    protected State state;
    private State nextState;
    private Connection connection;

    public SimpleStateMachine(Connection connection, State initialState) {
        this.state = initialState;
        this.connection = connection;
    }

    @Override
    public StateMachine setNextState(State state) {
        this.nextState = state;
        return this;
    }

    @Override
    public State getNextState() {
        return nextState;
    }

    @Override
    public void driveState(Object attachment) throws IOException {

        do {
            if (this.nextState != null) {
                this.state = nextState;
                this.nextState = null;
            }
        } while (this.state.handle(this, connection, attachment));
    }

    @Override
    public void driveState() throws IOException {
        driveState(null);
    }

    @Override
    public State getCurrentState() {
        return state;
    }
}
