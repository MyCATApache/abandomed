package io.mycat.net2.states;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;
import io.mycat.net2.NetSystem;

public class ListeningState implements NetworkState {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ListeningState.class);
    public static final ListeningState INSTANCE = new ListeningState();


    private ListeningState() {
    }

	@Override
	public boolean handler(Connection conn) throws IOException {
		LOGGER.debug("Current conn in ListeningState. conn is "+conn.getClass());
		NetSystem.getInstance().addConnection(conn);
        conn.setDataBuffer(conn.getMycatByteBufferAllocator().allocate());
        return conn.init();
	}

}

