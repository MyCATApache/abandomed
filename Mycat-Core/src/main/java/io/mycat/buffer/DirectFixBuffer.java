package io.mycat.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by ynfeng on 2017/7/7.
 */
public class DirectFixBuffer extends AbstractMycatByteBuffer {
    private ByteBuffer byteBuffer;
    private int capacity;
    private  int mark;

    protected DirectFixBuffer(ByteBuffer byteBuffer, int capacity) {
        this.byteBuffer = byteBuffer;
        this.capacity = capacity;
    }

    @Override
    public int transferToChannel(SocketChannel socketChannel) throws IOException {
        byteBuffer.limit(writeIndex());
        byteBuffer.position(readIndex());
        int write = socketChannel.write(byteBuffer);
        readIndex(readIndex() + write);
        return write;
    }

    @Override
    public int transferFromChannel(SocketChannel socketChannel) throws IOException {
        byteBuffer.limit(capacity());
        byteBuffer.position(writeIndex());
        int read = socketChannel.read(byteBuffer);
        writeIndex(writeIndex() + read);
        return read;
    }

    @Override
    public void mark(int position) {
        this.mark = position;
    }

    @Override
    public void reset() {
        this.mark = 0;
    }

    @Override
    public MycatByteBuffer compact() {
        byteBuffer.position(readIndex());
        byteBuffer.limit(writeIndex());
        byteBuffer.compact();
        writeIndex(writeIndex() - readIndex());
        readIndex(0);
        return this;
    }

    @Override
    public void clear() {
        byteBuffer.clear();
        writeIndex(0);
        readIndex(0);
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    long getInt(int index, int length) {
        byteBuffer.limit(index + length);
        byteBuffer.position(index);
        long rv = 0;
        for (int i = 0; i < length; i++) {
            byte b = byteBuffer.get();
            rv |= (((long) b) & 0xFF) << (i * 8);
        }
        return rv;
    }

    @Override
    byte[] getBytes(int index, int length) {
        byteBuffer.limit(length + index);
        byteBuffer.position(index);
        byte[] bytes = new byte[length];
        byteBuffer.get(bytes);
        return bytes;
    }

    @Override
    byte getByte(int index) {
        byteBuffer.limit(index + 1);
        byteBuffer.position(index);
        byte b = byteBuffer.get();
        return b;
    }

    @Override
    void putBytes(int index, byte[] bytes) {
        byteBuffer.limit(index + bytes.length);
        byteBuffer.position(index);
        byteBuffer.put(bytes);
    }

    @Override
    void putByte(int index, byte val) {
        byteBuffer.limit(index + 1);
        byteBuffer.position(index);
        byteBuffer.put(val);
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }
}
