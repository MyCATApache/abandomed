package io.mycat.net2.states;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.Connection;
import io.mycat.net2.DirectConDataBuffer;
import io.mycat.net2.NetSystem;

public class ListeningState implements ConnState {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ListeningState.class);
    public static final ListeningState INSTANCE = new ListeningState();


    private ListeningState() {
    }

	@Override
	public boolean handler(Connection conn,SelectionKey processKey,SocketChannel channel,Selector selector) throws IOException {
		LOGGER.debug("Current conn in ListeningState. conn is "+conn.getClass());
		NetSystem.getInstance().addConnection(conn);
		conn.setProcessKey(channel.register(selector, SelectionKey.OP_READ, conn));
        conn.setConnState(ReadState.INSTANCE);
        //TODO
        /**使用MyCatMemoryAllocator分配Direct Buffer,再进行SocketChannel通信时候，
         * 网络读写都会减少一次数据的拷贝,而使用FileChanel与SocketChannel数据交换时
         * 底层最终还是生成一个临时的Direct Buffer，用临时Direct Buffer写入或者读SocketChannel中
         * 后面考虑会使用netty中ByteBuf中的DirectBuffer进行网络IO通信。效率更高
         * */
//        conn.setReadDataBuffer(new DirectConDataBuffer(1024 ));
        conn.setDataBuffer(conn.getMycatByteBufferAllocator().allocate());
        //新的client进来后，处理Server发送Client的handshake init packet
        conn.getHandler().onConnected(conn);  //连接事件处理完成后,驱动状态机
        if(conn.getNextConnState()!=null){
			conn.setConnState(conn.getNextConnState());
			conn.setNextConnState(null);
			return true;
		}
        return false;
	}

}
