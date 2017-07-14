package io.mycat.net2.states;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;

public class ReadWaitingState implements ConnState {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ReadWaitingState.class);
    public static final ReadWaitingState INSTANCE = new ReadWaitingState();

    private ReadWaitingState() {
    }

	@Override
	public boolean handler(Connection conn,SelectionKey processKey,SocketChannel channel,Selector selector)throws IOException {
		LOGGER.debug("Current conn in ReadWaitingState. conn is "+conn.getClass()); 
	    boolean needWakeup = false;
        try {
        	processKey.interestOps(processKey.interestOps() & Connection.OP_NOT_WRITE);
 		    processKey.interestOps(processKey.interestOps() | SelectionKey.OP_READ);
            needWakeup = true;
        } catch (Exception e) {
            LOGGER.warn("enable read fail " + e);
        }
        if (needWakeup) {
            processKey.selector().wakeup();
        }
	   conn.setConnState(ReadState.INSTANCE);   //继续解析剩下的数据
	   return false;
	}

}
