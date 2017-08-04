package io.mycat.net2.states;

import io.mycat.machine.State;
import io.mycat.machine.StateMachine;
import io.mycat.net2.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Created by ynfeng on 2017/8/4.
 */
public class NoWriteState implements State {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoReadAndWriteState.class);
    public static final NoWriteState INSTANCE = new NoWriteState();

    @Override
    public boolean handle(StateMachine context, Connection connection, Object attachment) throws IOException {
        SelectionKey processKey = connection.getProcessKey();
        try {
            processKey.interestOps(processKey.interestOps() & Connection.OP_NOT_WRITE);
            return false;
        } catch (Exception e) {
            LOGGER.warn("disable write fail " + e);
        }
        return false;
    }
}
