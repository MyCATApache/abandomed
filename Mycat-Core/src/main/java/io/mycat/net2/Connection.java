package io.mycat.net2;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import io.mycat.buffer.MycatByteBuffer;
import io.mycat.buffer.MycatByteBufferAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.WriteCompleteListener;
import io.mycat.engine.dataChannel.TransferMode;
import io.mycat.net2.states.ClosedState;
import io.mycat.net2.states.ClosingState;
import io.mycat.net2.states.ConnState;
import io.mycat.net2.states.ListeningState;
import io.mycat.net2.states.WriteWaitingState;
import io.mycat.net2.states.network.TRANSMIT_RESULT;
import io.mycat.net2.states.network.TRY_READ_RESULT;

/**
 * @author wuzh
 */
public abstract class Connection implements ClosableConnection {
	
    public static Logger LOGGER = LoggerFactory.getLogger(Connection.class);
    protected String host;
    protected int port;
    protected int localPort;
    protected long id;
    private String  reactor;
    private Object attachement;

    // 连接的方向，in表示是客户端连接过来的，out表示自己作为客户端去连接对端Sever
    public enum Direction {
        in, out
    }

    private Direction direction = Direction.in;

    protected final SocketChannel channel;

    private SelectionKey processKey;
    public static final int OP_NOT_READ = ~SelectionKey.OP_READ;
    public static final int OP_NOT_WRITE = ~SelectionKey.OP_WRITE;
    private MycatByteBuffer dataBuffer;
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
    protected NIOHandler handler;
    
    private ConnState connState = ListeningState.INSTANCE; 
    private ConnState nextConnState;  /** which state to go into after finishing current write */    
    
    private Selector selector;
    private MycatByteBufferAllocator mycatByteBufferAllocator;
    private WriteCompleteListener writeCompleteListener;
    
    private byte[] tmpWriteBytes;  // 该字段用于 在非透传模式下,net buffer 大小小于写出报文大小时,临时记录报文数据.用于分批 写出到 net buffer
    private int tmplastwritePos;   // 写缓冲区可用时, tmpWriteBytes 上次写到 net buffer 缓冲区中的位置
    private TransferMode directTransferMode = TransferMode.NORMAL;
    
    
        
    public Connection(SocketChannel channel) {
        this.channel = channel;
        this.isClosed = false;
        this.startupTime = TimeUtil.currentTimeMillis();
        this.lastReadTime = startupTime;
        this.lastWriteTime = startupTime;
        this.lastPerfCollectTime = startupTime;
    }
    
	public void connDriverMachine(){
		connDriverMachine(null);
	}
    
    /**
     * 状态机可以由内部驱动,也可以由外部驱动
     * 内部驱动设置state,外部驱动设置 nextstate
     */
    public void connDriverMachine(ConnState states){
    	
    	if(states!=null){
    		this.connState = states;
    	}

    	try {
    		/* 循环方式,替代递归 */
    		while(connState.handler(this, processKey, channel, selector)){}
		} catch (Exception e) {
			LOGGER.error("server error", e);
			close(" close connection!");
		}
    }
    
    /*
     * 处理新的命令
     */
    public TRY_READ_RESULT try_read_network() {
    	final MycatByteBuffer buffer = dataBuffer;
		int got = 0;
		try {
			got = buffer.transferFromChannel(channel);
			
			switch (got) {
	            case 0: {
	            	return TRY_READ_RESULT.READ_NO_DATA_RECEIVED;
	            }
	            case -1:{
	            	return TRY_READ_RESULT.READ_ERROR;
	            }
	            default: {
	            	return TRY_READ_RESULT.READ_DATA_RECEIVED;
	            }
			}
		} catch (Exception e) {
			LOGGER.error("read netwok data error !!", e);
			return TRY_READ_RESULT.READ_ERROR;
		}
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

    
    public void setHandler(NIOHandler handler) {
        this.handler = handler;

    }

	@SuppressWarnings("rawtypes")
    public NIOHandler getHandler() {
        return this.handler;
    }

 
    public boolean isConnected() {
        return (!this.connState.equals(ClosedState.INSTANCE) && !connState.equals(ClosingState.INSTANCE) && !connState.equals(ListeningState.INSTANCE));
    }

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
        if (dataBuffer != null) {
            try {
                mycatByteBufferAllocator.recyle(dataBuffer);
            } catch (Exception e) {
                e.printStackTrace(); }
            dataBuffer = null;
        }
    }

  	public void setNIOReactor(String reactorName) {
		this.reactor = reactorName;
	}
  	
  	/**
  	 * 如果 报文长度大于缓冲区长度,分段发送
  	 * @param data
  	 * @throws IOException
  	 */
    public void write(byte[] data) throws IOException{
        //TODO 是否有BUG?连续多次调用,造成tmpWriteBytes被覆盖，进而丢失数据？
    	int remains = dataBuffer.writableBytes();
    	if(remains >= data.length){
    		dataBuffer.writeBytes(data);
    	}else{
    		tmpWriteBytes = data;
       	}
    	this.directTransferMode = TransferMode.NORMAL;
    	setNextConnState(WriteWaitingState.INSTANCE);
    }
  	
    /*
     * Returns:
     *   TRANSMIT_COMPLETE   All done writing.
     *   TRANSMIT_INCOMPLETE More data readableBytes to write.
     *   TRANSMIT_SOFT_ERROR Can't write any more right now.
     *   TRANSMIT_HARD_ERROR Can't write (connstate is set to conn_closing)
     */
    public TRANSMIT_RESULT write() throws IOException {
    	
    	final NetSystem nets = NetSystem.getInstance();
        final MycatByteBuffer buffer = this.dataBuffer;
        int written = 0;
        int remains = 0;
        if(TransferMode.NORMAL.equals(directTransferMode)){
        	/**
        	 * 在非透传模式下,如果 local buffer > net buffer 剩余长度,分批透传
        	 */
        	if(tmpWriteBytes!=null&&tmplastwritePos<(tmpWriteBytes.length-1)){
        		dataBuffer.compact();
        		int tempremains = tmpWriteBytes.length - tmplastwritePos;
        		int localbufferremains = dataBuffer.writableBytes();//dataBuffer.getTotalSize() - dataBuffer.getWritePos();
        		if(tempremains >= localbufferremains){
//        			dataBuffer.putBytes(tmpWriteBytes,tmplastwritePos,localbufferremains);
                    dataBuffer.writeBytes(localbufferremains,tmpWriteBytes);
        			tmplastwritePos += localbufferremains;
        		}else{
//        			dataBuffer.putBytes(tmpWriteBytes,tmplastwritePos,tempremains);
                    dataBuffer.writeBytes(tmpWriteBytes);
        			tmpWriteBytes = null;
        			tmplastwritePos = 0;
        		}
        	}
        	written = buffer.transferToChannel(this.channel);
        	remains = buffer.readableBytes() ;//buffer.getWritePos() - buffer.getLastWritePos();
        }else{
        	written = buffer.transferToChannel(this.channel);
        	remains = buffer.readableBytes();//buffer.getReadPos() - buffer.getLastWritePos();
        }
        
       	if(written > 0){
    		netOutBytes += written;
            nets.addNetOutBytes(written);
         // trace-protocol
            // @author little-pan
            // @since 2016-09-29
//            if (nets.getNetConfig().isTraceProtocol()) {
//                final String hexs = StringUtil.dumpAsHex(buffer, buffer.readPos() - written, written);
//                LOGGER.info(
//                        "C#{}B#{}: last writed = {} bytes, remain to write = {} bytes, written bytes\n{}",
//                        getId(), buffer.hashCode(), written, remains, hexs);
//            }
    		if (remains >0) {  // 还有数据没有写完
        		return TRANSMIT_RESULT.TRANSMIT_INCOMPLETE;
    		} else {
    			return TRANSMIT_RESULT.TRANSMIT_COMPLETE;
    		}
    	}else if(written <= 0){
    		try {
    			processKey.interestOps(processKey.interestOps() | SelectionKey.OP_WRITE);
			} catch (Exception e) {
				LOGGER.error("couldn't update write event",e);
				return TRANSMIT_RESULT.TRANSMIT_HARD_ERROR;
			}
    		return TRANSMIT_RESULT.TRANSMIT_SOFT_ERROR;
    	}
    	return TRANSMIT_RESULT.TRANSMIT_COMPLETE;
    }

    public Object getAttachement() {
		return attachement;
	}
	public void setAttachement(Object attachement) {
		this.attachement = attachement;
	}

    public void setConnState(ConnState newState) {
        this.connState = newState;
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

	public ConnState getConnState() {
        return connState;
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
        return "Connection [host=" + host + ",  port=" + port + ", id=" + id + ", state=" + connState + ", direction="
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

	public ConnState getNextConnState() {
		return nextConnState;
	}

	public void setNextConnState(ConnState nextConnState) {
		LOGGER.debug("nextConnState is change to "+nextConnState
					+"  current nextConnState is " +this.nextConnState + this.getClass().getSimpleName());
		this.nextConnState = nextConnState;
	}

	public Selector getSelector() {
		return selector;
	}

	public void setSelector(Selector selector) {
		this.selector = selector;
	}

	public SelectionKey getProcessKey() {
		return processKey;
	}

	public void setProcessKey(SelectionKey processKey) {
		this.processKey = processKey;
	}
	
	public WriteCompleteListener getWriteCompleteListener() {
		return writeCompleteListener;
	}

	public void setWriteCompleteListener(WriteCompleteListener writeCompleteListener) {
		this.writeCompleteListener = writeCompleteListener;
	}

	public MycatByteBuffer getDataBuffer() {
		return dataBuffer;
	}

	public void setDataBuffer(MycatByteBuffer dataBuffer) {
		this.dataBuffer = dataBuffer;
	}
	
	public TransferMode getDirectTransferMode() {
		return directTransferMode;
	}

	public void setDirectTransferMode(TransferMode directTransferMode) {
		this.directTransferMode = directTransferMode;
	}

	public byte[] getTmpWriteBytes() {
		return tmpWriteBytes;
	}

    public MycatByteBufferAllocator getMycatByteBufferAllocator() {
        return mycatByteBufferAllocator;
    }

    public void setMycatByteBufferAllocator(MycatByteBufferAllocator mycatByteBufferAllocator) {
        this.mycatByteBufferAllocator = mycatByteBufferAllocator;
    }
}
