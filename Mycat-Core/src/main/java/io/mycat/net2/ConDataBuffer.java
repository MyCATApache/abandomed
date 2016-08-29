package io.mycat.net2;

import java.io.IOException;
import java.nio.ByteBuffer;
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
	 * put bytes to inner datas from buf
	 * @param index ,from where to put
	 * @param buf
	 */
	public void putBytes(ByteBuffer buf) throws IOException;
	
	/**
	 * put bytes to inner datas from buf
	 */
	public void putBytes(byte[] buf) throws IOException;
	
	/**
	 * begin write ,return a write buffer ,with requreid length
	 * this buffer should not shared and reused
	 * @param length
	 * @return
	 */
	public ByteBuffer beginWrite(int length) throws IOException;
	
	 /**
	  * end write ,must with beginWrite
	  * @param buffer
	 * @throws IOException 
	  */
	public void endWrite(ByteBuffer buffer) throws IOException;
	/**
	 * read one byte from inner buffer
	 * @param index
	 * @return
	 */
	public byte getByte(int index) throws IOException;
	/**
	 * read bytes from inner buffer
	 * 
	 */
	public ByteBuffer getBytes(int index,int length) throws IOException;
	
	/**
	 * transfert inner datas to this socket
	 * @param socketChanel
	 * @return transferd data
	 */
	public int transferTo(SocketChannel socketChanel) throws IOException;
	
	/**
	 * cur writing start pos  (valid writeble data is from  writing pos -> totalSize)
	 * @return
	 * @throws IOException 
	 */
	public int writingPos() throws IOException;
	/**
	 * cur reading start pos ,(valid readable data is from readPos -> writing pos)
	 * @return
	 */
	public int readPos();
	/**
	 * total length 
	 * @return
	 */
	public int totalSize();
	
	public void setWritingPos(int writingPos) throws IOException;
	
	public void seReadingPos(int readingPos);
	
	public boolean isFull() throws IOException;
	
	public void recycle();
	
}
