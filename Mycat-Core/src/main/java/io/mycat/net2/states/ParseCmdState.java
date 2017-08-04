package io.mycat.net2.states;

import java.io.IOException;

import io.mycat.common.State;
import io.mycat.common.StateMachine;
import io.mycat.mysql.MySQLConnection;

import io.mycat.net2.Connection;

public class ParseCmdState implements State {
    public static final ParseCmdState INSTANCE = new ParseCmdState();

    private ParseCmdState() {
    }

    @Override
    public boolean handle(StateMachine context, Connection conn, Object object)
            throws IOException {
        ((MySQLConnection) conn).getProtocolStateMachine().driveState(null);
        if (conn.getNetworkStateMachine().getWriteRemaining() == 0) {
            conn.getNetworkStateMachine().setNextState(ReadWaitingState.INSTANCE);
        }
        return false;
    }
}
