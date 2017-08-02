package io.mycat.net2.states;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import io.mycat.machine.State;
import io.mycat.machine.StateMachine;
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
        LOGGER.debug("Current conn in NoReadAndWriteState. conn is " + conn.getClass());
        SelectionKey processKey = conn.getProcessKey();
        boolean needWakeup = false;
        try {

            processKey.interestOps(processKey.interestOps() & Connection.OP_NOT_READ);
            processKey.interestOps(processKey.interestOps() & Connection.OP_NOT_WRITE);
            needWakeup = true;
        } catch (Exception e) {
            LOGGER.warn("enable read fail " + e);
        }
        if (needWakeup) {
            processKey.selector().wakeup();
        }
        return false;
    }

}

