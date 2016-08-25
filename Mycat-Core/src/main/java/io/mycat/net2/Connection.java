package io.mycat.net2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wuzh
 */
public abstract class Connection implements ClosableConnection {
    public static Logger LOGGER = LoggerFactory.getLogger(Connection.class);
    protected String host;
    protected int port;
    protected int localPort;
    protected long id;

    public enum State {
        connecting, connected, closing, closed, failed
    }

    private State state = State.connecting;

    // 连接的方向，in表示是客户端连接过来的，out表示自己作为客户端去连接对端Sever
    public enum Direction {
        in, out
    }

    private Direction direction = Direction.in;

    protected final SocketChannel channel;

    private SelectionKey processKey;
    private static final int OP_NOT_READ = ~SelectionKey.OP_READ;
    private static final int OP_NOT_WRITE = ~SelectionKey.OP_WRITE;
    private ConDataBuffer readDataBuffer;
    private ConDataBuffer writeDataBuffer;
    protected boolean isClosed;
    protected boolean isSocketClosed;
    protected long startupTime;
    protected long lastReadTime;
    protected long lastWriteTime;
    protected int netInBytes;
    protected int netOutBytes;
    protected int pkgTotalSize;
    protected int pkgTotalCount;
    private long idleTimeout;
    private long lastPerfCollectTime;
    @SuppressWarnings("rawtypes")
    protected NIOHandler handler;
    private ReactorBufferPool myBufferPool;

    public Connection(SocketChannel channel) {
        this.channel = channel;
        this.isClosed = false;
        this.startupTime = TimeUtil.currentTimeMillis();
        this.lastReadTime = startupTime;
        this.lastWriteTime = startupTime;
        this.lastPerfCollectTime = startupTime;
    }

    public void resetPerfCollectTime() {
        netInBytes = 0;
        netOutBytes = 0;
        pkgTotalCount = 0;
        pkgTotalSize = 0;
        lastPerfCollectTime = TimeUtil.currentTimeMillis();
    }

    public long getLastPerfCollectTime() {
        return lastPerfCollectTime;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getId() {
        return id;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isIdleTimeout() {
        return TimeUtil.currentTimeMillis() > Math.max(lastWriteTime, lastReadTime) + idleTimeout;

    }

    public SocketChannel getChannel() {
        return channel;
    }

    public long getStartupTime() {
        return startupTime;
    }

    public long getLastReadTime() {
        return lastReadTime;
    }

    public long getLastWriteTime() {
        return lastWriteTime;
    }

    public long getNetInBytes() {
        return netInBytes;
    }

    public long getNetOutBytes() {
        return netOutBytes;
    }

    private ByteBuffer allocate() {
        return NetSystem.getInstance().getBufferPool().allocate();
    }

    private final void recycle(ByteBuffer buffer) {
        NetSystem.getInstance().getBufferPool().recycle(buffer);
    }

    public void setHandler(NIOHandler<? extends Connection> handler) {
        this.handler = handler;

    }

    public ConDataBuffer getWriteDataBuffer() {
		return writeDataBuffer;
	}

	@SuppressWarnings("rawtypes")
    public NIOHandler getHandler() {
        return this.handler;
    }

 
    public boolean isConnected() {
        return (this.state == Connection.State.connected);
    }


    // public void write(byte[] src) {
    // try {
    // writeQueueLock.lock();
    // ByteBuffer buffer = this.allocate();
    // int offset = 0;
    // int remains = src.length;
    // while (remains > 0) {
    // int writeable = buffer.remaining();
    // if (writeable >= remains) {
    // // can write whole srce
    // buffer.put(src, offset, remains);
    // this.writeQueue.offer(buffer);
    // break;
    // } else {
    // // can write partly
    // buffer.put(src, offset, writeable);
    // offset += writeable;
    // remains -= writeable;
    // writeQueue.offer(buffer);
    // buffer = allocate();
    // continue;
    // }
    //
    // }
    // } finally {
    // writeQueueLock.unlock();
    // }
    // this.enableWrite(true);
    // }

    @SuppressWarnings("unchecked")
    public void close(String reason) {
        if (!isClosed) {
            closeSocket();
            this.cleanup();
            isClosed = true;
            NetSystem.getInstance().removeConnection(this);
            LOGGER.info("close connection,reason:" + reason + " ," + this.getClass());
            if (handler != null) {
                handler.onClosed(this, reason);
            }
        }
    }

    /**
     * asyn close (executed later in thread) 该函数使用多线程异步关闭
     * Connection，会存在并发安全问题，暂时注释
     * 
     * @param reason
     */
    // public void asynClose(final String reason) {
    // Runnable runn = new Runnable() {
    // public void run() {
    // Connection.this.close(reason);
    // }
    // };
    // NetSystem.getInstance().getTimer().schedule(runn, 1, TimeUnit.SECONDS);
    //
    // }

    public boolean isClosed() {
        return isClosed;
    }

    public void idleCheck() {
        if (isIdleTimeout()) {
            LOGGER.info(toString() + " idle timeout");
            close(" idle ");
        }
    }

    /**
     * 清理资源
     */

    protected void cleanup() {
    	// 清理资源占用
        if(readDataBuffer!=null)
        {
        	 readDataBuffer.recycle();
        	 readDataBuffer=null;
        }
        if(this.writeDataBuffer!=null)
        {
        	 writeDataBuffer.recycle();
        	 writeDataBuffer=null;
        }
       
       
       
    }



    @SuppressWarnings("unchecked")
    public void register(Selector selector, ReactorBufferPool myBufferPool) throws IOException {
        this.myBufferPool = myBufferPool;
        processKey = channel.register(selector, SelectionKey.OP_READ, this);
        NetSystem.getInstance().addConnection(this);
        this.readDataBuffer =new MappedFileConDataBuffer(id+".rtmp");
        writeDataBuffer=new MappedFileConDataBuffer(id+".wtmp");
        this.handler.onConnected(this);

    }

    public void doWriteQueue() {
        try {
            boolean noMoreData = write0();
            lastWriteTime = TimeUtil.currentTimeMillis();
            if (noMoreData) {
                if ((processKey.isValid() && (processKey.interestOps() & SelectionKey.OP_WRITE) != 0)) {
                    disableWrite();
                }

            } else {

                if ((processKey.isValid() && (processKey.interestOps() & SelectionKey.OP_WRITE) == 0)) {
                    enableWrite(false);
                }
            }

        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("caught err:", e);
            }
            close("err:" + e);
        }

    }

    public void write(byte[] data) throws IOException {
    	this.writeDataBuffer.putBytes(data);
    	this.enableWrite(true);
        
        
    }

    private boolean write0() throws IOException {
    	int written =this.writeDataBuffer.transferTo(this.channel);
    	System.out.println("writed  "+written);
		 netOutBytes += written;
        NetSystem.getInstance().addNetOutBytes(written);
       return (writeDataBuffer.readPos()==writeDataBuffer.writingPos());
       
    }

    private void disableWrite() {
        try {
            SelectionKey key = this.processKey;
            key.interestOps(key.interestOps() & OP_NOT_WRITE);
        } catch (Exception e) {
            LOGGER.warn("can't disable write " + e + " con " + this);
        }

    }

    public void enableWrite(boolean wakeup) {
        boolean needWakeup = false;
        try {
            SelectionKey key = this.processKey;
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            needWakeup = true;
        } catch (Exception e) {
            LOGGER.warn("can't enable write " + e);

        }
        if (needWakeup && wakeup) {
            processKey.selector().wakeup();
        }
    }

    public void disableRead() {

        SelectionKey key = this.processKey;
        key.interestOps(key.interestOps() & OP_NOT_READ);
    }

    public void enableRead() {

        boolean needWakeup = false;
        try {
            SelectionKey key = this.processKey;
            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
            needWakeup = true;
        } catch (Exception e) {
            LOGGER.warn("enable read fail " + e);
        }
        if (needWakeup) {
            processKey.selector().wakeup();
        }
    }

    public void setState(State newState) {
        this.state = newState;
    }

    /**
     * 异步读取数据,only nio thread call
     * 
     * @throws IOException
     */
    protected void asynRead() throws IOException {
        if (this.isClosed) {
            return;
        }
        int got =  (int) readDataBuffer.transferFrom(channel);
            switch (got) {
            case 0: {
                // 如果空间不够了，继续分配空间读取
                if (readDataBuffer.isFull()) {
                    //todo extends
                }
                break;
            }
            case -1: {
                break;
            }
            default: {// readed some bytes

                // 子类负责解析报文并处理
            	LOGGER.info("readed "+got);
                handler.handleReadEvent(this);
            }
            }
           }

        private void closeSocket() {

        if (channel != null) {
            boolean isSocketClosed = true;
            try {
                processKey.cancel();
                channel.close();
            } catch (Throwable e) {
            }
            boolean closed = isSocketClosed && (!channel.isOpen());
            if (!closed) {
                LOGGER.warn("close socket of connnection failed " + this);
            }

        }
    }

    public ConDataBuffer getReadDataBuffer() {
		return readDataBuffer;
	}

	public State getState() {
        return state;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Connection.Direction in) {
        this.direction = in;

    }

    public int getPkgTotalSize() {
        return pkgTotalSize;
    }

    public int getPkgTotalCount() {
        return pkgTotalCount;
    }

    @Override
    public String toString() {
        return "Connection [host=" + host + ",  port=" + port + ", id=" + id + ", state=" + state + ", direction="
                + direction + ", startupTime=" + startupTime + ", lastReadTime=" + lastReadTime + ", lastWriteTime="
                + lastWriteTime + "]";
    }

   
    public ReactorBufferPool getMyBufferPool() {
        return myBufferPool;
    }

}
