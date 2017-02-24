package io.mycat.net2;

import io.mycat.memalloc.MyCatMemoryAllocator;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author zagnix
 * @create 2017-01-19 11:41
 */

public class ByteBufConDataBuffer implements ConDataBuffer {

    private  ByteBuf buffer = null;
    private final int initCapacity;
    private final int maxCapacity;

    public ByteBufConDataBuffer(final int initCapacity,final int maxCapacity){
        this.initCapacity = initCapacity;
        this.maxCapacity = maxCapacity;
        this.buffer = MyCatMemoryAllocator.getINSTANCE().directBuffer(initCapacity);
    }

    @Override
    public int transferFrom(SocketChannel socketChanel) throws IOException {
            /* int size=-1,len = 0;
            int pkglen =0;

            while ((size=buffer.writeBytes(socketChanel,512))>0){
                len +=size;
            }
            */
        //System.out.println("transferFrom1 read index = " + buffer.readerIndex() + "write index = " + buffer.writerIndex());
        System.out.println("writable bytes :" + buffer.writableBytes());

        int len = buffer.writeBytes(socketChanel,buffer.writableBytes());
        //System.out.println("transferFrom2 read index = " + buffer.readerIndex() + "write index = " + buffer.writerIndex());
        return len;

    }

    @Override
    public void putBytes(ByteBuffer buf) throws IOException {
        buffer.writeBytes(buf);
    }

    @Override
    public void putBytes(byte[] buf) throws IOException {
        buffer.writeBytes(buf);
    }

    @Override
    public ByteBuffer beginWrite(int length) throws IOException {
        System.out.println("beginWrite1 read index = " + buffer.readerIndex() + "write index = " + buffer.writerIndex());
        ByteBuffer byteBuffer =ByteBuffer.allocateDirect(length); //buffer.internalNioBuffer(buffer.writerIndex(),length);
        System.out.println("beginWrite2 read index = " + buffer.readerIndex() + "write index = " + buffer.writerIndex());
        return byteBuffer;
    }

    @Override
    public void endWrite(ByteBuffer src) throws IOException {
        buffer.writeBytes(src);
    }

    @Override
    public byte getByte(int index) throws IOException {
        return buffer.getByte(index);
    }

    @Override
    public ByteBuffer getBytes(int index, int length) throws IOException {
        buffer.readerIndex(index);
        buffer.writerIndex(index+length);
        ByteBuffer byteBuffer = buffer.nioBuffer(0,length);
        buffer.readBytes(byteBuffer);
        return byteBuffer;
    }

    @Override
    public int transferTo(SocketChannel socketChanel) throws IOException {
       // System.out.println("transferTo1 read index = " + buffer.readerIndex() + "write index = " + buffer.writerIndex());
        int len = buffer.readBytes(socketChanel,buffer.readableBytes());
        //System.out.println("transferTo2 read index = " + buffer.readerIndex() + "write index = " + buffer.writerIndex());
        return len;

    }

    @Override
    public int writingPos() throws IOException {
        return buffer.writerIndex();
    }

    @Override
    public int readPos() {
        return buffer.readerIndex();
    }

    @Override
    public int totalSize() {
        return maxCapacity;
    }

    @Override
    public void setWritingPos(int writingPos) throws IOException {
            buffer.setIndex(buffer.readerIndex(),writingPos);
    }

    @Override
    public void setReadingPos(int readingPos) {
        buffer.setIndex(readingPos,buffer.writerIndex());
    }

    @Override
    public boolean isFull() throws IOException {
        return !buffer.isWritable();
    }

    @Override
    public void recycle() {
        //buffer.release();
    }

    public ByteBuf getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuf buffer) {
        this.buffer = buffer;
    }

    public int getInitCapacity() {
        return initCapacity;
    }


    public int getMaxCapacity() {
        return maxCapacity;
    }

}
