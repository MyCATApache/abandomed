package io.mycat.net2.states;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;

public class ParseCmdState implements NetworkState{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ParseCmdState.class);
    public static final ParseCmdState INSTANCE = new ParseCmdState();

    private ParseCmdState() {
    }

	@Override
	public boolean handler(Connection conn)
			throws IOException {
		LOGGER.debug("Current conn in ParseCmdState. conn is "+conn.getClass());
		conn.driveState(null);
		return true;
	}
}
