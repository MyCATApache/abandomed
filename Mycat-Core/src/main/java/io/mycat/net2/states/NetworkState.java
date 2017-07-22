package io.mycat.net2.states;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import io.mycat.net2.Connection;
/**
 *   因为状态机内部状态转换可能转换几个状态后,才会进入到 外部调用模块指定的 nextState
 *   状态机内部状态转换 直接设置 conn.setState
 *   外部驱动状态机进入下一个状态使用 conn.setNextState
 * @author yanjunli
 *
 */
public interface NetworkState {
	
	/**
	 * 状态机处理
	 * @param conn
	 * @return  true 状态机继续执行,false 状态机停止,等待读写事件
	 * @throws IOException
	 */
	boolean handler(Connection conn)throws IOException;

}

