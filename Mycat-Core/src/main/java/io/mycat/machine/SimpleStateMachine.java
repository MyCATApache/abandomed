package io.mycat.machine;

import io.mycat.net2.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by ynfeng on 2017/7/31.
 */
public class SimpleStateMachine implements StateMachine {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleStateMachine.class);
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
        boolean result;
        do {
            if (this.nextState != null) {
                this.state = nextState;
                this.nextState = null;
            }
            LOGGER.debug(connection.getClass().getSimpleName() + "'s " + this.getClass().getSimpleName() + " drive to " + state.getClass().getSimpleName());
            result = this.state.handle(this, connection, attachment);
        } while (result);
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
