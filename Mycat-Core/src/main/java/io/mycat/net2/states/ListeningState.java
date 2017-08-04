package io.mycat.net2.states;

import java.io.IOException;

import io.mycat.common.State;
import io.mycat.common.StateMachine;

import io.mycat.net2.Connection;
import io.mycat.net2.NetSystem;

public class ListeningState implements State {
	
    public static final ListeningState INSTANCE = new ListeningState();


    private ListeningState() {
    }

	@Override
	public boolean handle(StateMachine context, Connection conn, Object attachment) throws IOException {
		NetSystem.getInstance().addConnection(conn);
        conn.setDataBuffer(conn.getMycatByteBufferAllocator().allocate());
        return conn.init();
	}

}

