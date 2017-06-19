package io.mycat.net2;

import io.mycat.memalloc.MyCatMemoryAllocator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * 基于ByteBuffer
 *
 * @author ynfeng
 */
public class DirectConDataBuffer implements ConDataBuffer {
    private final ByteBuffer byteBuffer;
    private final int totalSize;
    private int readPos;
    private int writePos;

    public DirectConDataBuffer(int size) {
        this.totalSize = size;
        this.byteBuffer =
                MyCatMemoryAllocator.getINSTANCE().directBuffer(totalSize).nioBuffer(0, totalSize);
        this.readPos = 0;
        this.writePos = 0;
    }

    @Override
    public int transferFrom(SocketChannel socketChanel) throws IOException {
        final ByteBuffer byteBuffer = this.byteBuffer;
        byteBuffer.position(writePos);
        byteBuffer.limit(totalSize);
        int readed = socketChanel.read(byteBuffer);
        if (readed != -1) {
            writePos += readed;
        }
        return readed;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void putBytes(ByteBuffer buf)
            throws IOException {
        buf.flip();
        final ByteBuffer byteBuffer = this.byteBuffer;
        int oldWritePos = writePos;
        prepareToWrite();
        byteBuffer.put(buf);
        writePos += (byteBuffer.position() - oldWritePos);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void putBytes(byte[] bytes)
            throws IOException {
        final ByteBuffer byteBuffer = this.byteBuffer;
        int oldWritePos = writePos;
        prepareToWrite();
        byteBuffer.put(bytes);
        writePos += (byteBuffer.position() - oldWritePos);
    }

    private void prepareToWrite() {
        byteBuffer.position(writePos);
        byteBuffer.limit(totalSize);
    }

    @Override
    public ByteBuffer beginWrite(int length) throws IOException {
        return MyCatMemoryAllocator.getINSTANCE().directBuffer(length).nioBuffer(0, length);
    }

    @Override
    public void endWrite(ByteBuffer buffer) throws IOException {
        this.putBytes(buffer);
    }

    @Override
    public byte getByte(int index) throws IOException {
        final ByteBuffer byteBuffer = this.byteBuffer;
        return byteBuffer.get(index);
    }

    @Override
    public ByteBuffer getBytes(int index, int length) throws IOException {
        final ByteBuffer byteBuffer = this.byteBuffer;
        int oldPos = byteBuffer.position();
        int oldLimit = byteBuffer.limit();
        byteBuffer.position(index);
        byteBuffer.limit(index + length);
        ByteBuffer newBuff = byteBuffer.slice();
        byteBuffer.position(oldPos);
        byteBuffer.limit(oldLimit);
        return newBuff;
    }

    @Override
    public int transferTo(SocketChannel socketChanel) throws IOException {
        final ByteBuffer byteBuffer = this.byteBuffer;
        prepareToRead();
        int writed = socketChanel.write(byteBuffer);
        this.readPos += writed;
        return writed;
    }

    private void prepareToRead() {
        byteBuffer.position(readPos);
        byteBuffer.limit(writePos);
    }


    @Override
    public int writingPos() throws IOException {
        return writePos;
    }

    @Override
    public int readPos() {
        return readPos;
    }

    @Override
    public int totalSize() {
        return totalSize;
    }

    @Override
    public void setWritingPos(int writingPos) throws IOException {
        this.writePos = writingPos;
    }

    @Override
    public void setReadingPos(int readingPos) {
        this.readPos = readingPos;
    }

    @Override
    public boolean isFull() throws IOException {
        return writePos == totalSize;
    }

    @Override
    public void recycle() {

    }

    @Override
    public void clear() {
        byteBuffer.clear();
        writePos = 0;
        readPos = 0;
    }
}
