package io.mycat.net2.states;

import java.io.IOException;

import io.mycat.machine.State;
import io.mycat.machine.StateMachine;
import io.mycat.mysql.MySQLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;

public class ParseCmdState implements State {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParseCmdState.class);
    public static final ParseCmdState INSTANCE = new ParseCmdState();

    private ParseCmdState() {
    }

    @Override
    public boolean handle(StateMachine context, Connection conn, Object object)
            throws IOException {
        LOGGER.debug("Current conn in ParseCmdState. conn is " + conn.getClass());
        ((MySQLConnection) conn).getProtocolStateMachine().driveState(null);
        if (conn.getNetworkStateMachine().getWriteRemaining() == 0) {
            conn.getNetworkStateMachine().setNextState(ReadWaitingState.INSTANCE);
        }
        return false;
    }
}
