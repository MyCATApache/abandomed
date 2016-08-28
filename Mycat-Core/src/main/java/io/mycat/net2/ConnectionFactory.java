package io.mycat.net2;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;

/**
 * @author wuzh
 */
public abstract class ConnectionFactory {

	/**
	 * 创建一个具体的连接
	 * 
	 * @param channel
	 * @return Connection
	 * @throws IOException
	 */
	protected abstract Connection makeConnection(SocketChannel channel)
			throws IOException;

	/**
	 * NIOHandler是无状态的，多个连接共享一个，因此建议作为 Factory的私有变量
	 * 
	 * @return NIOHandler
	 */
	@SuppressWarnings("rawtypes")
	protected abstract NIOHandler getNIOHandler();

	@SuppressWarnings("unchecked")
	public Connection make(SocketChannel channel) throws IOException {
		channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		// 子类完成具体连接创建工作
		Connection c = makeConnection(channel);
		// 设置连接的参数
		NetSystem.getInstance().setSocketParams(c,true);
		// 设置NIOHandler
		c.setHandler(getNIOHandler());
		return c;
	}
}
