package io.mycat.net2.states;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;

public class ClosingState implements ConnState {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ClosingState.class);
    public static final ClosingState INSTANCE = new ClosingState();


    private ClosingState() {
    }

	@Override
	public boolean handler(Connection conn, SelectionKey processKey, SocketChannel channel, Selector selector)
			throws IOException {
		LOGGER.debug("Current conn in ClosingState. conn is "+conn.getClass());
		conn.close(" close connection!");
		return false;
	}

}
