package io.mycat.net2.states;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;

public class ClosedState implements NetworkState {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClosingState.class);
    public static final ClosedState INSTANCE = new ClosedState();


    private ClosedState() {
    }
	
	@Override
	public boolean handler(Connection conn)
			throws IOException {
		LOGGER.debug("Current conn in closedState. conn is "+conn.getClass());
		return false;
	}

}
