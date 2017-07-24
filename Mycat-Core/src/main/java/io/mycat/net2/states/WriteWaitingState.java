package io.mycat.net2.states;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;

public class WriteWaitingState implements NetworkState {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(WriteWaitingState.class);
    public static final WriteWaitingState INSTANCE = new WriteWaitingState();

    private WriteWaitingState() {
    }


	@Override
	public boolean handler(Connection conn)
			throws IOException {
		LOGGER.debug("Current conn in WriteWaitingState. conn is "+conn.getClass());
		boolean needWakeup = false;
		SelectionKey processKey = conn.getProcessKey();
        try {
        	processKey.interestOps(processKey.interestOps() & Connection.OP_NOT_READ);
    		processKey.interestOps(processKey.interestOps() | SelectionKey.OP_WRITE);
            needWakeup = true;
        } catch (Exception e) {
            LOGGER.warn("enable read fail " + e);
            conn.setNextNetworkState(ClosingState.INSTANCE);
        }
        if (needWakeup) {
            processKey.selector().wakeup();
            conn.setNextNetworkState(WriteState.INSTANCE);
        }
		return false;
	}

}

