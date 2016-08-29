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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

import sun.misc.Unsafe;
import sun.nio.ch.FileChannelImpl;
/**
 * mapped file used to store bytes
 * @author wuzhihui
 *
 */
public class MappedFileConDataBuffer3 implements ConDataBuffer {
	public static final Unsafe unsafe;
	public static final Method mmap;
	public static final Method unmmap;
	public static final int BYTE_ARRAY_OFFSET;

	private final long addr;

	private FileChannel channel2;
	private RandomAccessFile randomFile;
	private int readPos;
	private int totalSize;
	static {
		try {
			Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
			singleoneInstanceField.setAccessible(true);
			unsafe = (Unsafe) singleoneInstanceField.get(null);
			mmap = getMethod(FileChannelImpl.class, "map0", int.class, long.class, long.class);
			mmap.setAccessible(true);
			unmmap = getMethod(FileChannelImpl.class, "unmap0", long.class, long.class);
			unmmap.setAccessible(true);
			BYTE_ARRAY_OFFSET = unsafe.arrayBaseOffset(byte[].class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public MappedFileConDataBuffer3(String fileName) throws IOException
	{
		randomFile = new RandomAccessFile(fileName, "rw");
		totalSize=1024*1024*5;
		if(!fileName.startsWith("/dev/zero"))
		{
			randomFile.setLength(totalSize);	
		}
		channel2 = randomFile.getChannel();
	   try {
		addr = (long) mmap.invoke(channel2, 1, 0, totalSize);
	} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
		throw new IOException(e);
	}
     	}
	@Override
	public int transferFrom(SocketChannel socketChanel) throws IOException {
		int position=(int) this.channel2.position();
		int tranfered=(int) channel2.transferFrom(socketChanel,position,totalSize-position);
		channel2.position(position+tranfered);
		return tranfered;
	}

	@Override
	public void putBytes(ByteBuffer buf) throws IOException {
		int offset=(int) channel2.position();
		System.out.println("put bytes "+buf.position()+" limit:"+buf.limit()+ " writePos "+offset);
		for(int i=0;i<buf.limit();i++)
		{
			unsafe.putByte(offset + addr+i, buf.get(i));
		}
		channel2.position(offset+buf.limit());
	
	}
	@Override
	public void putBytes(byte[] buf) throws IOException {
		putBytes(ByteBuffer.wrap(buf));
		
	}

	@Override
	public int transferTo(SocketChannel socketChanel) throws IOException {
		int writeEnd=(int) this.channel2.position();
	int writed=(int) channel2.transferTo(readPos, writeEnd-readPos, socketChanel);
	this.readPos+=writed;
		return writed;
	}

	@Override
	public int writingPos() throws IOException {
 
			return (int) channel2.position();
		 
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
		this.channel2.position(writingPos);
	}

	@Override
	public void seReadingPos(int readingPos) {
		this.readPos=readingPos;
	}
	@Override
	public boolean isFull() throws IOException {
		return channel2.position()==this.totalSize;
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
		return unsafe.getByte(index + addr);
	}
	@Override
	public ByteBuffer getBytes(int index,int length) throws IOException {
		ByteBuffer dst=ByteBuffer.allocate(length);
        for(int i=0;i<length;i++)
        {
        	dst.put(unsafe.getByte(index +i+ addr));
        }
		dst.flip();
		return dst;
		
	}
	@Override
	public ByteBuffer beginWrite(int length) {
		ByteBuffer copyBuf=ByteBuffer.allocate(length);
		return copyBuf;
	}
	
	@Override
	public void endWrite(ByteBuffer buffer) throws IOException {
 
			//System.out.println("end write 1 ,buf[ "+buffer.position()+","+buffer.limit()+"] writePos "+channel2.position()+" read pos "+this.readPos);
				this.putBytes(buffer);
			//System.out.println("end write 2,total "+buffer.limit()+" writePos "+channel2.position()+" read pos "+this.readPos);
		 
		
		 
		
	}
	private static Method getMethod(Class<?> cls, String name, Class<?>... params) throws Exception {
		Method m = cls.getDeclaredMethod(name, params);
		m.setAccessible(true);
		return m;
	}

	protected void unmap() throws Exception {
		unmmap.invoke(null, addr, this.totalSize());
	
	}


}
