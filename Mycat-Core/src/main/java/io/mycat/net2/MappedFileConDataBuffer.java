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
	public int transferFrom(final SocketChannel socketChanel) throws IOException {
		final int position = mapBuf.position();
		final int count    = totalSize - position;
		final int tranfered= (int) channel.transferFrom(socketChanel, position, count);
		mapBuf.position(position + tranfered);
		// fixbug: transferFrom() always return 0 when client closed abnormally!
		// --------------------------------------------------------------------
		// So decide whether the connection closed or not by read()! 
		// @author little-pan
		// @since 2016-09-29
		if(tranfered == 0 && count > 0){
			return (socketChanel.read(mapBuf));
		}
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
    int writeEnd=mapBuf.position();
	int writed=(int) channel.transferTo(readPos, writeEnd-readPos, socketChanel);
	this.readPos+=writed;
	//System.out.println("transferTo ,writed  "+writed+" read "+readPos+" pos " + "writepos "+writeEnd);
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
	public void setReadingPos(int readingPos) {
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
		 mapBuf.position(mapBuf.position()+buffer.position());
		//System.out.println("end write ,total "+buffer.limit()+" writePos "+mapBuf.position()+" read pos "+this.readPos);
	}
	

}
