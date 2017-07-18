package io.mycat.buffer;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by ynfeng on 2017/7/7.
 */
public class MycatByteBufferTest {
    private static final int CHUNK_SIZE = 1024 * 1024 * 512;
    private MycatByteBufferAllocator allocator = new DirectFixBufferAllocator(CHUNK_SIZE);

    @Test
    public void testIntReadWrite() {
        MycatByteBuffer buffer = allocator.allocate();
        Assert.assertTrue(allocator.getChunkSize() == CHUNK_SIZE);
        //fix int test
        buffer.putFixInt(0, 1, 100);
        Assert.assertTrue(buffer.getFixInt(0, 1) == 100);
        buffer.putFixInt(1, 2, 200);
        Assert.assertTrue(buffer.getFixInt(1, 2) == 200);
        Assert.assertTrue(buffer.writeIndex() == 0);
        Assert.assertTrue(buffer.readIndex() == 0);
        buffer.clear();

        //对读写指针的影响测试
        buffer.writeFixInt(1, 100);
        Assert.assertTrue(buffer.writeIndex() == 1);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.readFixInt(1) == 100);
        Assert.assertTrue(buffer.readIndex() == 1);
        buffer.clear();

        //Lenenc int test
        buffer.putLenencInt(0, 1024);
        Assert.assertTrue(buffer.getLenencInt(0) == 1024);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.writeIndex() == 0);
        buffer.clear();

        buffer.putLenencInt(0, 1 << 17);
        Assert.assertTrue(buffer.getLenencInt(0) == 1 << 17);
        buffer.clear();

        buffer.putLenencInt(0, 1 << 25);
        Assert.assertTrue(buffer.getLenencInt(0) == 1 << 25);
        buffer.clear();

        buffer.writeLenencInt(1);
        Assert.assertTrue(buffer.readLenencInt() == 1);
        buffer.clear();

        buffer.writeLenencInt(1 << 17);
        Assert.assertTrue(buffer.readLenencInt() == 1 << 17);
        buffer.clear();

        buffer.writeLenencInt(1 << 25);
        Assert.assertTrue(buffer.readLenencInt() == 1 << 25);
        buffer.clear();

        buffer.writeLenencInt(2048);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.writeIndex() == 3);
        Assert.assertTrue(buffer.readLenencInt() == 2048);
        Assert.assertTrue(buffer.readIndex() == 2);
        allocator.recyle(buffer);
    }

    @Test
    public void testStringReadWrite() {
        MycatByteBuffer buffer = allocator.allocate();
        //Fix String test
        buffer.putFixString(0, "hello");
        Assert.assertTrue(buffer.writeIndex() == 0);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.getFixString(0, 5).equals("hello"));
        buffer.clear();

        buffer.writeFixString("hello");
        Assert.assertTrue(buffer.writeIndex() == 5);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.readFixString(5).equals("hello"));
        Assert.assertTrue(buffer.readIndex() == 5);
        buffer.clear();

        //Lenenc String test
        buffer.putLenencString(0, "hello");
        Assert.assertTrue(buffer.writeIndex() == 0);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.getLenencString(0).equals("hello"));
        buffer.clear();

        buffer.writeLenencString("hello");
        Assert.assertTrue(buffer.writeIndex() == 6);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.readLenencString().equals("hello"));
        Assert.assertTrue(buffer.readIndex() == 6);
        buffer.clear();

        //Var String test
        buffer.putVarString(0, "hello");
        Assert.assertTrue(buffer.writeIndex() == 0);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.getVarString(0, 5).equals("hello"));
        buffer.clear();

        buffer.writeVarString("hello");
        Assert.assertTrue(buffer.writeIndex() == 5);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.readVarString(5).equals("hello"));
        Assert.assertTrue(buffer.readIndex() == 5);
        buffer.clear();


        //NUL String test
        buffer.putNULString(0, "hello");
        Assert.assertTrue(buffer.writeIndex() == 0);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.getNULString(0).equals("hello"));
        buffer.clear();

        buffer.writeNULString("hello");
        Assert.assertTrue(buffer.readableBytes() == 6);
        Assert.assertTrue(buffer.writableBytes() == CHUNK_SIZE - 6);
        Assert.assertTrue((buffer.writeIndex() == 6));
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.readNULString().equals("hello"));
        Assert.assertTrue(buffer.readIndex() == 6);
        Assert.assertFalse(buffer.hasReadableBytes());
        buffer.clear();

        String str = randomString(1 << 17);
        buffer.putLenencString(0, str);
        Assert.assertTrue(buffer.getLenencString(0).equals(str));
        buffer.clear();

        str = randomString(1 << 25);
        buffer.putLenencString(0, str);
        Assert.assertTrue(buffer.getLenencString(0).equals(str));
        buffer.clear();

        str = randomString(1 << 15);
        buffer.putLenencString(0, str);
        Assert.assertTrue(buffer.getLenencString(0).equals(str));
        buffer.clear();

        allocator.recyle(buffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRecyleException() {
        MycatByteBuffer buffer = new DirectFixBuffer(ByteBuffer.allocateDirect(10), 10);
        allocator.recyle(buffer);
    }

    @Test
    public void testTransfer() throws IOException {
        MycatByteBuffer buffer = allocator.allocate();
        SocketChannel socketChannel = new FakeSocketChannel(null);
        buffer.transferFromChannel(socketChannel);
        Assert.assertTrue(buffer.readableBytes() == CHUNK_SIZE);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.writeIndex() == CHUNK_SIZE);
        Assert.assertTrue(buffer.writableBytes() == 0);

        buffer.transferToChannel(socketChannel);
        Assert.assertFalse(buffer.hasReadableBytes());
        Assert.assertTrue(buffer.readIndex() == CHUNK_SIZE);
        Assert.assertNotNull(((DirectFixBuffer) buffer).getByteBuffer());
        allocator.recyle(buffer);
    }

    @Test
    public void testCompact() {
        MycatByteBuffer buffer = allocator.allocate();
        buffer.writeFixString("hello world");
        Assert.assertTrue(buffer.readFixString(6).equals("hello "));
        buffer.compact();
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.writeIndex() == 5);
        Assert.assertTrue(buffer.readFixString(5).equals("world"));
        Assert.assertTrue(buffer.readableBytes() == 0);
        allocator.recyle(buffer);
    }

    @Test
    public void testMark() {
        MycatByteBuffer buffer = allocator.allocate();
        buffer.mark(100);
        buffer.reset();
        Assert.assertTrue(buffer.readIndex() == 100);
    }

    @Test
    public void testBytesReadWrite() {
        MycatByteBuffer buffer = allocator.allocate();
        buffer.putBytes(0, "hello".getBytes());
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.writeIndex() == 0);
        Assert.assertTrue(new String(buffer.getBytes(0, 5)).equals("hello"));
        buffer.clear();

        buffer.writeBytes("hello".getBytes());
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.writeIndex() == 5);
        Assert.assertTrue(new String(buffer.readBytes(5)).equals("hello"));
        Assert.assertTrue(buffer.readIndex() == 5);
        buffer.clear();

        allocator.recyle(buffer);
    }

    @Test
    public void testByteReadWrite() {
        MycatByteBuffer buffer = allocator.allocate();
        buffer.putByte(0, (byte) 100);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.writeIndex() == 0);
        Assert.assertTrue(buffer.getByte(0) == 100);
        buffer.clear();

        buffer.writeByte((byte) 100);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.writeIndex() == 1);
        Assert.assertTrue(buffer.readByte() == 100);
        Assert.assertTrue(buffer.readIndex() == 1);

        allocator.recyle(buffer);
    }

    @Test
    public void testSkip(){
        MycatByteBuffer buffer = allocator.allocate();
        buffer.skip(10);
        Assert.assertTrue(buffer.readIndex() == 10);
        allocator.recyle(buffer);
    }

    @Test
    public void testLenencBytesReadWrite(){
        MycatByteBuffer buffer = allocator.allocate();
        buffer.putLenencBytes(0,"hello".getBytes());
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.writeIndex() == 0);
        Assert.assertTrue(new String(buffer.getLenencBytes(0)).equals("hello"));
        buffer.clear();

        buffer.writeLenencBytes("hello".getBytes());
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.writeIndex() == 6);
        Assert.assertTrue(new String(buffer.readLenencBytes()).equals("hello"));
        Assert.assertTrue(buffer.readIndex() == 6);
        allocator.recyle(buffer);
    }

    private String randomString(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) ThreadLocalRandom.current().nextInt(33, 128);
        }
        return new String(bytes);
    }


    class FakeSocketChannel extends SocketChannel {

        public FakeSocketChannel() {
            super(null);
        }

        /**
         * Initializes a new instance of this class.
         *
         * @param provider The provider that created this channel
         */
        protected FakeSocketChannel(SelectorProvider provider) {
            super(provider);
        }

        @Override
        public SocketChannel bind(SocketAddress local) throws IOException {
            return null;
        }

        @Override
        public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
            return null;
        }

        @Override
        public <T> T getOption(SocketOption<T> name) throws IOException {
            return null;
        }

        @Override
        public Set<SocketOption<?>> supportedOptions() {
            return null;
        }

        @Override
        public SocketChannel shutdownInput() throws IOException {
            return null;
        }

        @Override
        public SocketChannel shutdownOutput() throws IOException {
            return null;
        }

        @Override
        public Socket socket() {
            return null;
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public boolean isConnectionPending() {
            return false;
        }

        @Override
        public boolean connect(SocketAddress remote) throws IOException {
            return false;
        }

        @Override
        public boolean finishConnect() throws IOException {
            return false;
        }

        @Override
        public SocketAddress getRemoteAddress() throws IOException {
            return null;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            return dst.capacity() - dst.position();
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            return 0;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            return src.capacity() - src.position();
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            return 0;
        }

        @Override
        public SocketAddress getLocalAddress() throws IOException {
            return null;
        }

        @Override
        protected void implCloseSelectableChannel() throws IOException {

        }

        @Override
        protected void implConfigureBlocking(boolean block) throws IOException {

        }
    }
}
