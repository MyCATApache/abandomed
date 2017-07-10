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

    protected DirectFixBuffer(ByteBuffer byteBuffer, int capacity) {
        this.byteBuffer = byteBuffer;
        this.capacity = capacity;
    }

    @Override
    public int transferToChannel(SocketChannel socketChannel) throws IOException {
        int oldPosition = byteBuffer.position();
        int oldLimit = byteBuffer.limit();
        byteBuffer.position(readIndex());
        byteBuffer.limit(writeIndex());
        int write = socketChannel.write(byteBuffer);
        byteBuffer.limit(oldLimit);
        byteBuffer.position(oldPosition);
        readIndex(readIndex() + write);
        return write;
    }

    @Override
    public int transferFromChannel(SocketChannel socketChannel) throws IOException {
        int oldPosition = byteBuffer.position();
        int oldLimit = byteBuffer.limit();
        byteBuffer.position(writeIndex());
        byteBuffer.limit(capacity);
        int read = socketChannel.read(byteBuffer);
        byteBuffer.limit(oldLimit);
        byteBuffer.position(oldPosition);
        writeIndex(writeIndex() + read);
        return read;
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
        int oldPosition = byteBuffer.position();
        int oldLimit = byteBuffer.limit();
        byteBuffer.limit(capacity);
        byteBuffer.position(index);
        long rv = 0;
        for (int i = 0; i < length; i++) {
            byte b = byteBuffer.get();
            rv |= (((long) b) & 0xFF) << (i * 8);
        }
        byteBuffer.limit(oldLimit);
        byteBuffer.position(oldPosition);
        return rv;
    }

    @Override
    byte[] getBytes(int index, int length) {
        int oldPosition = byteBuffer.position();
        int oldLimit = byteBuffer.limit();
        byteBuffer.limit(length + index);
        byteBuffer.position(index);
        byte[] bytes = new byte[length];
        byteBuffer.get(bytes);
        byteBuffer.limit(oldLimit);
        byteBuffer.position(oldPosition);
        return bytes;
    }

    @Override
    byte getByte(int index) {
        int oldPosition = byteBuffer.position();
        byteBuffer.position(index);
        byte b = byteBuffer.get();
        byteBuffer.position(oldPosition);
        return b;
    }

    @SuppressWarnings("Duplicates")
    @Override
    void putBytes(int index, byte[] bytes) {
        int oldPosition = byteBuffer.position();
        int oldLimit = byteBuffer.limit();
        byteBuffer.position(index);
        byteBuffer.limit(capacity);
        byteBuffer.put(bytes);
        byteBuffer.limit(oldLimit);
        byteBuffer.position(oldPosition);
    }

    @SuppressWarnings("Duplicates")
    @Override
    void putByte(int index, byte val) {
   int oldPosition = byteBuffer.position();
        int oldLimit = byteBuffer.limit();
        byteBuffer.position(index);
        byteBuffer.limit(capacity);
        byteBuffer.put(val);
        byteBuffer.limit(oldLimit);
        byteBuffer.position(oldPosition);
    }
}
