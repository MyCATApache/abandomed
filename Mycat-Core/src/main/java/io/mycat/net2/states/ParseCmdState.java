package io.mycat.net2.states;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;

public class ParseCmdState implements ConnState{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ParseCmdState.class);
    public static final ParseCmdState INSTANCE = new ParseCmdState();

    private ParseCmdState() {
    }

	@Override
	public boolean handler(Connection conn, SelectionKey processKey, SocketChannel channel, Selector selector)
			throws IOException {
		LOGGER.debug("Current conn in ParseCmdState. conn is "+conn.getClass());
		conn.getHandler().handleReadEvent(conn);
		if(conn.getNextConnState()!=null){
			conn.setConnState(conn.getNextConnState());
			conn.setNextConnState(null);
		}
		return true;
	}
}
