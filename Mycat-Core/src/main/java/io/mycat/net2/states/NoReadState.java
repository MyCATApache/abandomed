package io.mycat.net2.states;

import io.mycat.common.State;
import io.mycat.common.StateMachine;
import io.mycat.net2.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Created by ynfeng on 2017/8/4.
 */
public class NoReadState implements State {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoReadAndWriteState.class);
    public static final NoReadState INSTANCE = new NoReadState();

    private NoReadState() {
    }

    @Override
    public boolean handle(StateMachine context, Connection connection, Object attachment) throws IOException {
        SelectionKey processKey = connection.getProcessKey();
        try {
            processKey.interestOps(processKey.interestOps() & Connection.OP_NOT_READ);
            return false;
        } catch (Exception e) {
            LOGGER.warn("disable read fail " + e);
        }
        return false;
    }
}
