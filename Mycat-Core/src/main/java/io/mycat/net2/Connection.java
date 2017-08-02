package io.mycat.net2;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import io.mycat.machine.SimpleStateMachine;
import io.mycat.machine.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.WriteCompleteListener;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.buffer.MycatByteBufferAllocator;
import io.mycat.engine.dataChannel.TransferMode;
import io.mycat.machine.StateMachine;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.net2.states.ClosedState;
import io.mycat.net2.states.ClosingState;
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
    private String reactor;
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


    /**
     * which state to go into after finishing current write
     */

    private Selector selector;
    private MycatByteBufferAllocator mycatByteBufferAllocator;
    private WriteCompleteListener writeCompleteListener;

    private byte[] tmpWriteBytes;  // 该字段用于 在非透传模式下,net buffer 大小小于写出报文大小时,临时记录报文数据.用于分批 写出到 net buffer
    private int tmplastwritePos;   // 写缓冲区可用时, tmpWriteBytes 上次写到 net buffer 缓冲区中的位置
    private TransferMode directTransferMode = TransferMode.NONE;            //透传模式
    private boolean passthrough = false;   //是否透传

    private int currentPacketLength;
    private byte currentPacketType;
    private int currentPacketStartPos;

    private MycatByteBuffer shareBuffer;
    private final NetworkStateMachine networkStateMachine;

    public Connection(SocketChannel channel) {
        this.channel = channel;
        this.isClosed = false;
        this.startupTime = TimeUtil.currentTimeMillis();
        this.lastReadTime = startupTime;
        this.lastWriteTime = startupTime;
        this.lastPerfCollectTime = startupTime;
        this.networkStateMachine = new NetworkStateMachine(this, ListeningState.INSTANCE);
    }

    /**
     * 连接初始化模板方法
     */
    public boolean init() throws IOException {
        return false;
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
                case -1: {
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

    public boolean isConnected() {
        return (this.getNetworkStateMachine().getCurrentState() != ClosedState.INSTANCE &&
                this.getNetworkStateMachine().getCurrentState() != ClosingState.INSTANCE &&
                this.getNetworkStateMachine().getCurrentState() != ListeningState.INSTANCE);
    }

    public void close(String reason) {
        if (!isClosed) {
            closeSocket();
            this.cleanup();
            isClosed = true;
            NetSystem.getInstance().removeConnection(this);
            LOGGER.info("close connection,reason:" + reason + " ," + this.getClass());
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
                e.printStackTrace();
            }
            dataBuffer = null;
        }
    }

    public void setNIOReactor(String reactorName) {
        this.reactor = reactorName;
    }

    /*
     * Returns:
     *   TRANSMIT_COMPLETE   All done writing.
     *   TRANSMIT_INCOMPLETE More data readableBytes to write.
     *   TRANSMIT_SOFT_ERROR Can't write any more right now.
     *   TRANSMIT_HARD_ERROR Can't write (networkState is set to conn_closing)
     */
    public TRANSMIT_RESULT write(MycatByteBuffer buffer, int length) throws IOException {
        final NetSystem nets = NetSystem.getInstance();
        int written = 0;
        int remains = 0;
        written = buffer.transferToChannel(this.channel, length);
        remains = buffer.readableBytes();//buffer.getWritePos() - buffer.getLastWritePos();
        if (written > 0) {
            netOutBytes += written;
            nets.addNetOutBytes(written);
            if (remains > 0) {  // 还有数据没有写完
                return TRANSMIT_RESULT.TRANSMIT_INCOMPLETE;
            } else {
                return TRANSMIT_RESULT.TRANSMIT_COMPLETE;
            }
        } else if (written <= 0) {
            try {
                processKey.interestOps(processKey.interestOps() | SelectionKey.OP_WRITE);
            } catch (Exception e) {
                LOGGER.error("couldn't update write event", e);
                return TRANSMIT_RESULT.TRANSMIT_HARD_ERROR;
            }
            return TRANSMIT_RESULT.TRANSMIT_SOFT_ERROR;
        }
        return TRANSMIT_RESULT.TRANSMIT_COMPLETE;
    }

    /**
     * Transfer{@code length} bytes from {@code mycatByteBuffer} to {@code dest} {@link Connection}.
     *
     * @param dest
     * @param mycatByteBuffer
     * @param length
     */
    public void startTransfer(Connection dest, MycatByteBuffer mycatByteBuffer, int length) throws IOException {
        NetworkStateMachine networkStateMachine = dest.getNetworkStateMachine();
        if (networkStateMachine.getWriteRemaining() != 0) {
            throw new IOException("A transmission is in progress!");
        }
        networkStateMachine.setDest(dest);
        networkStateMachine.setSrc(this);
        networkStateMachine.setBuffer(mycatByteBuffer);
        networkStateMachine.setWriteRemaining(length);
        networkStateMachine.setNextState(WriteWaitingState.INSTANCE);
        networkStateMachine.driveState();
    }

    /**
     * Transfer all writable bytes from {@code mycatByteBuffer} to {@code dest} {@link Connection}
     */
    public void startTransfer(Connection dest, MycatByteBuffer mycatByteBuffer) throws IOException {
        this.startTransfer(dest, mycatByteBuffer, mycatByteBuffer.readableBytes());
    }

    /**
     * Transfer{@code length} bytes from {@code mycatByteBuffer} to self SocketChannel.
     */
    public void startTransfer(MycatByteBuffer mycatByteBuffer, int length) throws IOException {
        this.startTransfer(this, mycatByteBuffer, length);
    }

    /**
     * Transfer all writable bytes from {@code mycatByteBuffer} to self SocketChannel.
     *
     * @param mycatByteBuffer
     */
    public void startTransfer(MycatByteBuffer mycatByteBuffer) throws IOException {
        this.startTransfer(this, mycatByteBuffer);
    }


    public Object getAttachement() {
        return attachement;
    }

    public void setAttachement(Object attachement) {
        this.attachement = attachement;
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
        return "Connection [host=" + host + ",  port=" + port + ", id=" + id + ", state=" + networkStateMachine.getCurrentState() + ", direction="
                + direction + ", startupTime=" + startupTime + ", lastReadTime=" + lastReadTime + ", lastWriteTime="
                + lastWriteTime + "]";
    }

    public String getReactor() {
        return this.reactor;
    }

    public boolean belongsActor(String reacotr) {
        return reactor.equals(reacotr);
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

    public Connection setWriteCompleteListener(WriteCompleteListener writeCompleteListener) {
        this.writeCompleteListener = writeCompleteListener;
        return this;
    }

    public MycatByteBuffer getDataBuffer() {
        return dataBuffer;
    }

    public Connection setDataBuffer(MycatByteBuffer dataBuffer) {
        this.dataBuffer = dataBuffer;
        return this;
    }

    public TransferMode getDirectTransferMode() {
        return directTransferMode;
    }

    public Connection setDirectTransferMode(TransferMode directTransferMode) {
        this.directTransferMode = directTransferMode;
        return this;
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

    public boolean isPassthrough() {
        return passthrough;
    }

    public Connection setPassthrough(boolean passthrough) {
        this.passthrough = passthrough;
        return this;
    }

    public int getCurrentPacketLength() {
        return currentPacketLength;
    }

    public Connection setCurrentPacketLength(int currentPacketLength) {
        this.currentPacketLength = currentPacketLength;
        return this;
    }

    public byte getCurrentPacketType() {
        return currentPacketType;
    }

    public Connection setCurrentPacketType(byte currentPacketType) {
        this.currentPacketType = currentPacketType;
        return this;
    }

    public int getCurrentPacketStartPos() {
        return currentPacketStartPos;
    }

    public Connection setCurrentPacketStartPos(int currentPacketStartPos) {
        this.currentPacketStartPos = currentPacketStartPos;
        return this;
    }

    /**
     * /**
     * 清除关于当前包的记录状态
     */
    public void clearCurrentPacket() {
        this.currentPacketLength = 0;
        this.currentPacketType = MySQLPacket.COM_SLEEP;
        this.currentPacketStartPos = 0;
    }

    public MycatByteBuffer getShareBuffer() {
        return shareBuffer;
    }

    public Connection setShareBuffer(MycatByteBuffer shareBuffer) {
        this.shareBuffer = shareBuffer;
        return this;
    }

    public NetworkStateMachine getNetworkStateMachine() {
        return networkStateMachine;
    }

    public class NetworkStateMachine extends SimpleStateMachine {
        private int writeRemaining;
        private Connection src;
        private Connection dest;
        private MycatByteBuffer buffer;

        public NetworkStateMachine(Connection connection, State initialState) {
            super(connection, initialState);
        }

        public int getWriteRemaining() {
            return writeRemaining;
        }

        public void setWriteRemaining(int writeRemaining) {
            this.writeRemaining = writeRemaining;
        }

        public Connection getSrc() {
            return src;
        }

        public void setSrc(Connection src) {
            this.src = src;
        }

        public Connection getDest() {
            return dest;
        }

        public void setDest(Connection dest) {
            this.dest = dest;
        }

        public MycatByteBuffer getBuffer() {
            return buffer;
        }

        public void setBuffer(MycatByteBuffer buffer) {
            this.buffer = buffer;
        }
    }
}
