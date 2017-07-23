package io.mycat.net2.states;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;

public class ReadWaitingState implements NetworkState {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ReadWaitingState.class);
    public static final ReadWaitingState INSTANCE = new ReadWaitingState();

    private ReadWaitingState() {
    }

	@Override
	public boolean handler(Connection conn)throws IOException {
		LOGGER.debug("Current conn in ReadWaitingState. conn is "+conn.getClass()); 
	    boolean needWakeup = false;
        try {
        	conn.getProcessKey().interestOps(conn.getProcessKey().interestOps() & Connection.OP_NOT_WRITE);
        	conn.getProcessKey().interestOps(conn.getProcessKey().interestOps() | SelectionKey.OP_READ);
            needWakeup = true;
        } catch (Exception e) {
            LOGGER.warn("enable read fail " + e);
            conn.setNextNetworkState(ClosingState.INSTANCE);
        }
        if (needWakeup) {
        	conn.getProcessKey().selector().wakeup();
        	conn.setNextNetworkState(ReadState.INSTANCE);   //继续解析剩下的数据
        }
	   return false;
	}

}
