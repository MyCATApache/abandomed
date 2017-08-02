package io.mycat.net2;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.NetworkChannel;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 存放当前所有连接的信息，包括客户端和服务端等，以及Network部分所使用共用对象
 *
 * @author wuzhih
 *
 */
public class NetSystem {
	public static final int RUNNING = 0;
	public static final int SHUTING_DOWN = -1;
	// private static final Logger LOGGER = Logger.getLogger("NetSystem");
	private static NetSystem INSTANCE;
	// 用来执行那些耗时的任务
	private final NameableExecutor executor;
	// 用来执行定时任务
	private final NamebleScheduledExecutor timer;
	private final ConcurrentMap<Long, Connection> allConnections;
	private long netInBytes;
	private long netOutBytes;
	private SystemConfig netConfig;
	private NIOConnector connector;

	public static NetSystem getInstance() {
		return INSTANCE;
	}

	public NetSystem(NameableExecutor executor, NamebleScheduledExecutor timer)
			throws IOException {
		this.executor = executor;
		this.timer = timer;
		this.allConnections = new ConcurrentHashMap<Long, Connection>();
		INSTANCE = this;
	}

	public NIOConnector getConnector() {
		return connector;
	}

	public void setConnector(NIOConnector connector) {
		this.connector = connector;
	}

	public int getWriteQueueSize() {
		int total = 0;
//		for (Connection con : allConnections.values()) {
			//total += con.getWriteQueue().size();
//		}

		return total;

	}

	public SystemConfig getNetConfig() {
		return netConfig;
	}

	public void setNetConfig(SystemConfig netConfig) {
		this.netConfig = netConfig;
	}

	public NameableExecutor getExecutor() {
		return executor;
	}

	public NamebleScheduledExecutor getTimer() {
		return timer;
	}

	public long getNetInBytes() {
		return netInBytes;
	}

	public void addNetInBytes(long bytes) {
		netInBytes += bytes;
	}

	public long getNetOutBytes() {
		return netOutBytes;
	}

	public void addNetOutBytes(long bytes) {
		netOutBytes += bytes;
	}

	/**
	 * 添加一个连接到系统中被监控
	 *
	 * @param c
	 */
	public void addConnection(Connection c) {
		allConnections.put(c.getId(), c);
	}

	public ConcurrentMap<Long, Connection> getAllConnectios() {
		return allConnections;
	}

	/**
	 * 定时执行该方法，回收部分资源。
	 */
	public void checkConnections() {
		Iterator<Entry<Long, Connection>> it = allConnections.entrySet().iterator();
		while (it.hasNext()) {
			Connection c = it.next().getValue();

			// 删除空连接
			if (c == null) {
				it.remove();
				continue;
			}

			// 清理已关闭连接，否则空闲检查。
			if (c.isClosed()) {
				c.cleanup();
				it.remove();
			} else {
				c.idleCheck();
			}
		}
	}

	public void removeConnection(Connection con) {
		this.allConnections.remove(con.getId());

	}

	public void setSocketParams(Connection con, boolean isFrontChannel) throws IOException {
		int sorcvbuf = 0;
		int sosndbuf = 0;
		int soNoDelay = 0;
		if (isFrontChannel) {
			sorcvbuf = netConfig.getFrontsocketsorcvbuf();
			sosndbuf = netConfig.getFrontsocketsosndbuf();
			soNoDelay = netConfig.getFrontSocketNoDelay();
		} else {
			sorcvbuf = netConfig.getBacksocketsorcvbuf();
			sosndbuf = netConfig.getBacksocketsosndbuf();
			soNoDelay = netConfig.getBackSocketNoDelay();
		}
		NetworkChannel channel = con.getChannel();
		channel.setOption(StandardSocketOptions.SO_RCVBUF, sorcvbuf);
		channel.setOption(StandardSocketOptions.SO_SNDBUF, sosndbuf);
		channel.setOption(StandardSocketOptions.TCP_NODELAY, soNoDelay == 1);
		channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

	}

}
