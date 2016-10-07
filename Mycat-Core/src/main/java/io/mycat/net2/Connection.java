package io.mycat.net2;

import io.mycat.util.StringUtil;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wuzh
 */
public abstract class Connection implements ClosableConnection {
	public static final int STATE_CONNECTING=0;
	public static final int STATE_IDLE=1;
	public static final int STATE_CLOSING=-1;
	public static final int STATE_CLOSED=-2;
	
    public static Logger LOGGER = LoggerFactory.getLogger(Connection.class);
    protected String host;
    protected int port;
    protected int localPort;
    protected long id;
    private String  reactor;
    private Object attachement;
    private int state = STATE_CONNECTING;

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
        return (this.state != STATE_CONNECTING && state!=STATE_CLOSING && state!=STATE_CLOSED );
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



  	public void setNIOReactor(String reactorName) {
		this.reactor = reactorName;
	}

	@SuppressWarnings("unchecked")
    public void register(Selector selector, ReactorBufferPool myBufferPool) throws IOException {
        processKey = channel.register(selector, SelectionKey.OP_READ, this);
        NetSystem.getInstance().addConnection(this);
       // boolean isLinux=GenelUtil.isLinuxSystem();
        //String maprFileName=isLinux? "/dev/zero":id+".rtmp";
        //String mapwFileName=isLinux? "/dev/zero":id+".wtmp";
        String maprFileName=id+".rtmp";
        String mapwFileName=id+".wtmp";
        LOGGER.info("connection bytebuffer mapped "+maprFileName);
        this.readDataBuffer =new MappedFileConDataBuffer(maprFileName); // 2 ,3 
        writeDataBuffer=new MappedFileConDataBuffer3(mapwFileName);
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
    	final NetSystem nets = NetSystem.getInstance();
    	final ConDataBuffer buffer = this.writeDataBuffer;
    	final int written = buffer.transferTo(this.channel);
    	final int remains = buffer.writingPos() - buffer.readPos();
    	netOutBytes += written;
		nets.addNetOutBytes(written);
    	// trace-protocol
    	// @author little-pan
    	// @since 2016-09-29
    	if(nets.getNetConfig().isTraceProtocol()){
    		final String hexs = StringUtil.dumpAsHex(buffer, buffer.readPos() - written, written);
    		LOGGER.info("C#{}B#{}: last writed = {} bytes, remain to write = {} bytes, written bytes\n{}", 
    			getId(), buffer.hashCode(), written, remains, hexs);
    	}
    	return (remains == 0);
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

    public Object getAttachement() {
		return attachement;
	}
	public void setAttachement(Object attachement) {
		this.attachement = attachement;
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

    public void setState(int newState) {
        this.state = newState;
    }

    /**
     * 异步读取数据,only nio thread call
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
	protected void asynRead() throws IOException {
    	final ConDataBuffer buffer = readDataBuffer;
    	if(LOGGER.isDebugEnabled()){
    		LOGGER.debug("C#{}B#{} ready to read data", getId(), buffer.hashCode());
    	}
        if (isClosed()) {
        	LOGGER.debug("Connection closed: ignore");
            return;
        }
        final int got =  buffer.transferFrom(channel);
        if(LOGGER.isDebugEnabled()){
        	LOGGER.debug("C#{}B#{} can read {} bytes", getId(), buffer.hashCode(), got);
        }
        switch (got) {
            case 0: {
                // 如果空间不够了，继续分配空间读取
                if (readDataBuffer.isFull()) {
                    // @todo extends
                }
                break;
            }
            case -1: {
            	close("client closed");
                break;
            }
            default: {// readed some bytes
            	// trace network protocol stream
            	final NetSystem nets = NetSystem.getInstance();
            	if(nets.getNetConfig().isTraceProtocol()){
            		final int offset = buffer.readPos(), length = buffer.writingPos() - offset;
            		final String hexs= StringUtil.dumpAsHex(buffer, offset, length);
            		LOGGER.info("C#{}B#{}: last readed = {} bytes, total readed = {} bytes, buffer bytes\n{}", 
            			getId(), buffer.hashCode(), got, length, hexs);
            	}
                // 子类负责解析报文并处理
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

	public int getState() {
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
    public String getReactor()
    {
    	return this.reactor;
    }
	public boolean belongsActor(String reacotr) {
		return reactor.equals(reacotr);
	}
  
}
