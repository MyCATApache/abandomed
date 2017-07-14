package io.mycat.net2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import io.mycat.memalloc.MyCatMemoryAllocator;

/**
 * 基于ByteBuffer
 *
 * @author yanjunli
 */
public class DirectConDataBuffer implements ConDataBuffer {
    private final ByteBuffer byteBuffer;
    private final int totalSize;

    /* socket 出发可读事件时, writerbuffer 已经全部 write 完毕.   */
    private int readPos;           // 最后一次缓冲区position的位置  
    private int writePos;          // 最后一次缓冲区position的位置
    /*
     * 最后写出到缓冲区的结束位置  透传模式和非透传模式下位置并不相同.
     * 透传模式下, lastWritePos 位于  0--readPos;之间
     * 非透传模式下,lastWritePos 位于 writePos--totalSize 之间
     */
    private int lastWritePos;      //默认处于非透传模式 

    public DirectConDataBuffer(int size) {
        this.totalSize = size;
        this.byteBuffer =
                MyCatMemoryAllocator.getINSTANCE().directBuffer(totalSize).nioBuffer(0, totalSize);
        this.readPos = 0;
        this.writePos = 0; 
        this.lastWritePos = 0;
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
        /* 设置 输出缓冲的的位置  */
        lastWritePos = writePos;
        byteBuffer.position(readPos);
        return readed;
    }
    
    @Override
    public int transferTo(SocketChannel socketChanel) throws IOException {
        final ByteBuffer byteBuffer = this.byteBuffer;
        // lastWritePos -- writePos  之间的数据
        byteBuffer.position(lastWritePos);
        byteBuffer.limit(writePos);
        int writed = socketChanel.write(byteBuffer);
        this.lastWritePos += writed;
        return writed;
    }
    
    public int transferToWithDirectTransferMode(SocketChannel socketChanel) throws IOException{
    	final ByteBuffer byteBuffer = this.byteBuffer;
        // 传输   位于  0--readPos之间之间的数据, 处于透传模式时,需要先设置一下 lastWritePos
        byteBuffer.position(lastWritePos);
        byteBuffer.limit(readPos);
        int writed = socketChanel.write(byteBuffer);
        this.lastWritePos += writed;
        return writed;
    }

    @Override
    public byte getByte(int index){
        final ByteBuffer byteBuffer = this.byteBuffer;
        return byteBuffer.get(index);
    }
    
    @Override
    public void putBytes(byte[] bytes) {
    	byteBuffer.position(writePos);
    	byteBuffer.limit(totalSize);
    	byteBuffer.put(bytes);
    	writePos = writePos + bytes.length;
    }
	@Override
	public void putBytes(byte[] src, int offset, int length) {
		// TODO Auto-generated method stub
		
	}
    
	@Override
	public void putByte(byte buf){
    	byteBuffer.position(writePos);
    	byteBuffer.limit(totalSize);
		byteBuffer.put(buf);
		writePos++;
	}

    @Override
    public boolean isFull() throws IOException {
        return readPos == totalSize;
    }

    @Override
    public void recycle() {
    	if (byteBuffer.isDirect()) {
    		System.out.println("byteBuffer recycle please fix it!!!!!!");
         }
    }

	public int getReadPos() {
		return readPos;
	}

	public void setReadPos(int readPos) {
		this.readPos = readPos;
		this.byteBuffer.position(readPos);
	}

	public int getWritePos() {
		return writePos;
	}

	public void setWritePos(int writePos) {
		this.writePos = writePos;
	}

	public int getLastWritePos() {
		return lastWritePos;
	}

	public void setLastWritePos(int lastWritePos) {
		this.lastWritePos = lastWritePos;
	}


	@Override
	public byte getByte() {
		readPos++;
		return byteBuffer.get();
	}

	@Override
	public void getBytes(byte[] ab) {
		byteBuffer.get(ab);
		readPos +=ab.length;
	}
	
	@Override
	public void compact() {
		
//		currProcessPos = byteBuffer.position();
//		currProcessLimit --- readPos   这个区间是未处理的命令.
//      writePos  和   lastWritePos  之间不需要处理.		 
		/** 未处理完的命令   */
		byteBuffer.position(readPos);
		byteBuffer.limit(writePos);
		byteBuffer.compact();
		
		int position = byteBuffer.position();
		readPos = 0;
		writePos = position;
		lastWritePos = position;
	}

	@Override
	public int getTotalSize() {
		return totalSize;
	}

	@Override
	public void clear() {
		byteBuffer.clear();
        writePos = 0;
        readPos = 0;
        lastWritePos = 0;
	}
}
