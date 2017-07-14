package io.mycat.net2.states;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.engine.dataChannel.TransferMode;
import io.mycat.net2.Connection;
import io.mycat.net2.states.network.TRANSMIT_RESULT;

public class WriteState implements ConnState {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(WriteState.class);
    public static final WriteState INSTANCE = new WriteState();

    private WriteState() {
    }

	@Override
	public boolean handler(Connection conn, SelectionKey processKey, SocketChannel channel, Selector selector)
			throws IOException {
		TRANSMIT_RESULT result = conn.write();
		switch(result){
		case TRANSMIT_COMPLETE:
			if(TransferMode.NORMAL.equals(conn.getDirectTransferMode())){
				if(conn.getTmpWriteBytes()!=null){  //说明还有数据没有传输完成,保持当前状态,继续传输数据.
					LOGGER.debug("Current conn in WriteState. tmpWriteBytes is not null conn is "+conn.getClass());
					return true;
				}
			}
			LOGGER.debug("Current conn in WriteState  TRANSMIT_COMPLETE. conn is "+conn.getClass()); 
			conn.setConnState(NewCmdState.INSTANCE);
			return true;
		case TRANSMIT_INCOMPLETE:
			LOGGER.debug("Current conn in WriteState TRANSMIT_INCOMPLETE conn is "+conn.getClass());
			return true;  //没有传输完成,继续保持当前状态,继续传输
		case TRANSMIT_HARD_ERROR: /* 连接断开了... */
			LOGGER.debug("Current conn in WriteState TRANSMIT_HARD_ERROR conn is "+conn.getClass());
			conn.setConnState(ClosingState.INSTANCE);
			return true;
		case TRANSMIT_SOFT_ERROR:  /* socket write buffer pool is full */
			LOGGER.debug("Current conn in WriteState ,socket write buffer pool is full. conn is "+conn.getClass());
			return false;
		}
		return true;
	}

}
