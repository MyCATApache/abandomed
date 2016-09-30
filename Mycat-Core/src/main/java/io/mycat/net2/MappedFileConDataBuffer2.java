/*
 * Copyright (c) 2016, OpenCloudDB/MyCAT and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software;Designed and Developed mainly by many Chinese
 * opensource volunteers. you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 2 only, as published by the
 * Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Any questions about this component can be directed to it's project Web address
 * https://code.google.com/p/opencloudb/.
 *
 */
package io.mycat.net2;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
/**
 * mapped file used to store bytes
 * @author wuzhihui
 *
 */
public class MappedFileConDataBuffer2 implements ConDataBuffer {
	private FileChannel channel;
	private RandomAccessFile randomFile;
	private int readPos;
	private int totalSize;
	public MappedFileConDataBuffer2(String fileName) throws IOException
	{
		randomFile = new RandomAccessFile(fileName, "rw");
		totalSize=1024*1024*5;
		randomFile.setLength(totalSize);
		if(!fileName.startsWith("/dev/null"))
		{
			randomFile.setLength(totalSize);	
		}
		channel = randomFile.getChannel();
		
	}
	@Override
	public int transferFrom(SocketChannel socketChanel) throws IOException {
		int position=(int) this.channel.position();
		int tranfered=(int) channel.transferFrom(socketChanel,position,totalSize-position);
		channel.position(position+tranfered);
		return tranfered;
	}

	@Override
	public void putBytes(ByteBuffer buf) throws IOException {
		//buf.flip();
		int writed=channel.write(buf);
		if(buf.hasRemaining()||writed==0)
		{
			throw new IOException("can't write whole buf ,writed "+writed+" remains "+buf.remaining());
		}
	
	}
	@Override
	public void putBytes(byte[] buf) throws IOException {
		int writed=channel.write(ByteBuffer.wrap(buf));
		if(writed!=buf.length)
		{
			throw new java.lang.RuntimeException("not write complete ");
		}
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
		int writeEnd=(int) this.channel.position();
	int writed=(int) channel.transferTo(readPos, writeEnd-readPos, socketChanel);
	this.readPos+=writed;
	//System.out.println("transferTo ,writed  "+writed+" read "+readPos+" pos " + "writepos "+writeEnd);
		return writed;
	}

	@Override
	public int writingPos() {
		try {
			return (int) channel.position();
		} catch (IOException e) {
			 
			e.printStackTrace();
		}
		return -1;
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
		this.channel.position(writingPos);
	}

	@Override
	public void setReadingPos(int readingPos) {
		this.readPos=readingPos;
	}
	@Override
	public boolean isFull() throws IOException {
		return channel.position()==this.totalSize;
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
	public byte getByte(int index) throws IOException {
		ByteBuffer dst=ByteBuffer.allocate(1);
		this.channel.read(dst, index);
		return dst.get(0);
	}
	@Override
	public ByteBuffer getBytes(int index,int length) throws IOException {
		ByteBuffer dst=ByteBuffer.allocate(length);
		channel.read(dst,index);
		dst.flip();
		return dst;
		
	}
	@Override
	public ByteBuffer beginWrite(int length) {
		ByteBuffer copyBuf=ByteBuffer.allocate(length);
		//copyBuf.limit(length);
		return copyBuf;
	}
	
	@Override
	public void endWrite(ByteBuffer buffer) throws IOException {
 
			//System.out.println("end write 1 ,buf[ "+buffer.position()+","+buffer.limit()+"] writePos "+channel.position()+" read pos "+this.readPos);
			buffer.flip();
			//int writed=channel.write(buffer);
			channel.write(buffer);
			//System.out.println("end write 2,total "+writed+" writePos "+channel.position()+" read pos "+this.readPos);
		 
		
		 
		
	}
	

}
