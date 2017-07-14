package io.mycat.net2;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * connection used data buffer  
 * @author wuzhihui
 *
 */
public interface ConDataBuffer {

	/**
	 * read data from socketChnnell 
	 * @param socketChanel
	 * @return readed bytes
	 */
	public int transferFrom(SocketChannel socketChanel ) throws IOException;
	
	/**
	 * transfert inner datas to this socket
	 * @param socketChanel
	 * @return transferd data
	 */
	public int transferTo(SocketChannel socketChanel) throws IOException;
	
	/**
	 * 透传模式 传输数据
	 * @param socketChanel
	 * @return
	 * @throws IOException
	 */
	public int transferToWithDirectTransferMode(SocketChannel socketChanel) throws IOException;
	
	
	/**
	 * 缓冲区总大小
	 * @return
	 */
	public int getTotalSize();
	
	/**
	 * 获取 writepos
	 * @return
	 */
	public int getWritePos();
	
	/**
	 * 设置 write
	 * @param writePos
	 */
	public void setWritePos(int writePos);
	
	public int getReadPos();

	public void setReadPos(int readPos);

	
	public int getLastWritePos();

	public void setLastWritePos(int lastWritePos);
	
	/**
	 * read one byte from inner buffer
	 * @param index
	 * @return
	 */
	public byte getByte(int index);
	
	public byte getByte();
	
	public void getBytes(byte[] ab);
	
	/**
	 * put bytes to inner datas from buf
	 */
	public void putBytes(byte[] buf) ;
	
	public void putBytes(byte[] src,int offset,int length);
	
	/**
	 * put bytes to inner data from buf
	 */
	public void putByte(byte buf) ;
	
	/**
	 * buffer 压缩
	 */
	public void compact();
	
	public void clear();
	
	public boolean isFull() throws IOException;
	
	public void recycle();
	
}
