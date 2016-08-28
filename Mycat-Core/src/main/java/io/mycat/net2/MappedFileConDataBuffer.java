package io.mycat.net2;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
/**
 * mapped file used to store bytes
 * @author wuzhihui
 *
 */
public class MappedFileConDataBuffer implements ConDataBuffer {
	private FileChannel channel;
	private MappedByteBuffer mapBuf;
	private RandomAccessFile randomFile;
	private int readPos;
	private int totalSize;
	public MappedFileConDataBuffer(String fileName) throws IOException
	{
		randomFile = new RandomAccessFile(fileName, "rw");
		totalSize=1024*1024*5;
		randomFile.setLength(totalSize);
		channel = randomFile.getChannel();
		mapBuf=channel.map(FileChannel.MapMode.READ_WRITE, 0, totalSize);
		
	}
	@Override
	public int transferFrom(SocketChannel socketChanel) throws IOException {
		int position=mapBuf.position();
		int tranfered=(int) channel.transferFrom(socketChanel,position,totalSize-position);
		mapBuf.position(position+tranfered);
		return tranfered;
	}

	@Override
	public void putBytes(ByteBuffer buf) throws IOException {
		int position=mapBuf.position();
		int writed=channel.write(buf, position);
		if(buf.hasRemaining())
		{
			throw new IOException("can't write whole buf ,writed "+writed+" remains "+buf.remaining());
		}
		mapBuf.position(position+writed);
		
	}
	@Override
	public void putBytes(byte[] buf) throws IOException {
		this.mapBuf.put(buf);
		
	}

	@Override
	public int transferTo(SocketChannel socketChanel) throws IOException {
//		int oldPos=mapBuf.position();
//		int oldLimit=mapBuf.limit();
//		//int writed=(int) channel.transferTo(readPos, position-readPos, socketChanel);
//		mapBuf.position(readPos);
//		mapBuf.limit(oldPos);
//		
//		int writed=socketChanel.write(this.mapBuf);
//		mapBuf.position(oldPos);
//		mapBuf.limit(oldLimit);
//		this.readPos+=writed;
    int writeEnd=mapBuf.position();
	int writed=(int) channel.transferTo(readPos, writeEnd-readPos, socketChanel);
	this.readPos+=writed;
		return writed;
	}

	@Override
	public int writingPos() {
		return mapBuf.position();
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
	public void setWritingPos(int writingPos) {
		mapBuf.position(writingPos);
	}

	@Override
	public void seReadingPos(int readingPos) {
		this.readPos=readingPos;
	}
	@Override
	public boolean isFull() {
		return mapBuf.position()==this.totalSize;
	}
	@Override
	public void recycle() {
		System.out.println("warining ,not implemented recyled ,Leader.us tell you :please fix it ");
		try {
			randomFile.close();
		} catch (IOException e) {
			 
		}
	}
	@Override
	public byte getByte(int index) {
		return mapBuf.get(index);
	}
	@Override
	public ByteBuffer getBytes(int index,int length) throws IOException {
		int oldPos=mapBuf.position();
		mapBuf.position(index);
		ByteBuffer copyBuf=mapBuf.slice();
		copyBuf.limit(length);
		mapBuf.position(oldPos);
		return copyBuf;
		
	}
	@Override
	public ByteBuffer beginWrite(int length) {
		ByteBuffer copyBuf=mapBuf.slice();
		copyBuf.limit(length);
		return copyBuf;
	}
	
	@Override
	public void endWrite(ByteBuffer buffer) {
		mapBuf.position(mapBuf.position()+buffer.limit());
		
	}
	

}
