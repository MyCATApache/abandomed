package io.mycat.net2;

import java.io.IOException;
/**
 * NIOHandler是无状态的，多个连接共享一个，用于处理连接的事件，每个方法需要不阻塞，尽快返回结果
 * 
 * @author wuzh
 */
public interface NIOHandler<T extends Connection> {

	/**
	 * 连接建立成功的通知事件
	 * 
	 * @param con
	 *            当前连接
	 */
	public void onConnected(T con) throws IOException;

	/**
	 * 连接失败
	 * 
	 * @param con
	 *            失败的连接
	 * @param e
	 *            连接异常
	 */
	public void onConnectFailed(T con, ConnectionException e);
	
	/**
	 * 连接关闭通知
	 * @param con
	 * @throws IOException
	 */
	public void onClosed(T con,String reason);

	/**
	 * 收到数据需要处理
	 * 
	 * @param con
	 *            当前连接
	 */
	void handleReadEvent(T con) throws IOException;
	
	/**
	 * 数据处理过程中发生意外错误
	 * @param con
	 * @param e
	 */
	void onHandlerError(T con,Exception e);

}