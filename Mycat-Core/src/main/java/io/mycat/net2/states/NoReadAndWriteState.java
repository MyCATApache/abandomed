package io.mycat.net2.states;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import io.mycat.common.State;
import io.mycat.common.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;

public class NoReadAndWriteState implements State {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoReadAndWriteState.class);
    public static final NoReadAndWriteState INSTANCE = new NoReadAndWriteState();

    private NoReadAndWriteState() {
    }

    @Override
    public boolean handle(StateMachine context, Connection conn, Object attachment)
            throws IOException {
        SelectionKey processKey = conn.getProcessKey();
        try {
            processKey.interestOps(processKey.interestOps() & Connection.OP_NOT_READ);
            processKey.interestOps(processKey.interestOps() & Connection.OP_NOT_WRITE);
            return true;
        } catch (Exception e) {
            LOGGER.warn("enable read fail " + e);
        }
        return false;
    }

}

