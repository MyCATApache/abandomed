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
package io.mycat.mysql;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.mysql.packet.ErrorPacket;
import io.mycat.mysql.packet.HandshakePacket;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.Connection;
import io.mycat.util.RandomUtil;
/**
 * Mysql connection 
 * @author wuzhihui
 *
 */
public  class MySQLConnection extends Connection {
	
	protected final static Logger LOGGER = LoggerFactory.getLogger(MySQLConnection.class);
	
	public static final int CMD_QUERY_STATUS 	 = 11;
    public static final int RESULT_WAIT_STATUS 	 = 21;
    public static final int RESULT_INIT_STATUS 	 = 22;
    public static final int RESULT_FETCH_STATUS	 = 23;
    public static final int RESULT_HEADER_STATUS = 24;
    public static final int RESULT_FAIL_STATUS 	 = 29;
    
    public final static int msyql_packetHeaderSize = 4;
	public final static int mysql_packetTypeSize   = 1;
	
	protected String user;
	protected String password;
	protected String charset;
	protected int 	 charsetIndex;
    protected byte[] seed;
    
	public MySQLConnection(SocketChannel channel) {
		super(channel);
	}
	
	public static final boolean validateHeader(final long offset, final long position) {
		//return offset + msyql_packetHeaderSize + mysql_packetTypeSize <= position;
		// ------------------------------------------------------------------------
		// fixbug: can't pass when an empty packet comes, so we should exclude "mysql_packetTypeSize"
		// @author little-pan
		// @since 2016-09-29
		return (position >= (offset + msyql_packetHeaderSize));
	}
	
	/**
	 * 获取报文长度
	 * 
	 * @param buffer
	 *			报文buffer
	 * @param offset
	 *			buffer解析位置偏移量
	 * @param position
	 *			buffer已读位置偏移量
	 * @return 报文长度(Header长度+内容长度)
	 * @throws IOException 
	 */
	public static final int getPacketLength(ConDataBuffer buffer, int offset) throws IOException {
		int length = buffer.getByte(offset) & 0xff;
		length |= (buffer.getByte(++offset) & 0xff) << 8;
		length |= (buffer.getByte(++offset) & 0xff) << 16;
		return length + msyql_packetHeaderSize;
	}

	protected int getServerCapabilities() {
	        int flag = 0;
	        flag |= Capabilities.CLIENT_LONG_PASSWORD;
	        flag |= Capabilities.CLIENT_FOUND_ROWS;
	        flag |= Capabilities.CLIENT_LONG_FLAG;
	        flag |= Capabilities.CLIENT_CONNECT_WITH_DB;
	        // flag |= Capabilities.CLIENT_NO_SCHEMA;
	        // boolean usingCompress = MycatServer.getInstance().getConfig()
	        // .getSystem().getUseCompression() == 1;
	        // if (usingCompress) {
	        // flag |= Capabilities.CLIENT_COMPRESS;
	        // }
	        flag |= Capabilities.CLIENT_ODBC;
	        flag |= Capabilities.CLIENT_LOCAL_FILES;
	        flag |= Capabilities.CLIENT_IGNORE_SPACE;
	        flag |= Capabilities.CLIENT_PROTOCOL_41;
	        flag |= Capabilities.CLIENT_INTERACTIVE;
	        // flag |= Capabilities.CLIENT_SSL;
	        flag |= Capabilities.CLIENT_IGNORE_SIGPIPE;
	        flag |= Capabilities.CLIENT_TRANSACTIONS;
	        // flag |= ServerDefs.CLIENT_RESERVED;
	        flag |= Capabilities.CLIENT_SECURE_CONNECTION;
	        return flag;
	    }

	    public void sendAuthPackge() throws IOException {
	        // 生成认证数据
	        byte[] rand1 = RandomUtil.randomBytes(8);
	        byte[] rand2 = RandomUtil.randomBytes(12);

	        // 保存认证数据
	        byte[] seed = new byte[rand1.length + rand2.length];
	        System.arraycopy(rand1, 0, seed, 0, rand1.length);
	        System.arraycopy(rand2, 0, seed, rand1.length, rand2.length);
	        this.seed = seed;

	        // 发送握手数据包
	        HandshakePacket hs = new HandshakePacket();
	        hs.packetId = 0;
	        hs.protocolVersion = Versions.PROTOCOL_VERSION;
	        hs.serverVersion = Versions.SERVER_VERSION;
	        hs.threadId = id;
	        hs.seed = rand1;
	        hs.serverCapabilities = getServerCapabilities();
	        // hs.serverCharsetIndex = (byte) (charsetIndex & 0xff);
	        hs.serverStatus = 2;
	        hs.restOfScrambleBuff = rand2;
	        this.writeMsqlPackage(hs);
	        // asynread response
	       // this.asynRead();
	    }

	public void failure(final int errno, final String info){
		try {
			LOGGER.warn("errno = {}, info = {}, {}", errno, info, this);
			writeErrMessage(errno, info);
		} catch (final IOException e) {
			this.close(e.toString());
		}
	}

  
	public void writeMsqlPackage(MySQLPacket pkg) throws IOException
	{
		int pkgSize=pkg.calcPacketSize();
		ByteBuffer buf=getWriteDataBuffer().beginWrite(pkgSize+MySQLPacket.packetHeaderSize);
		pkg.write(buf,pkgSize);
		getWriteDataBuffer().endWrite(buf);
		this.enableWrite(true);
	}
    
    public void writeErrMessage(int errno, String info) throws IOException {
        ErrorPacket err = new ErrorPacket();
        err.packetId = 1;
        err.errno = errno;
        err.message = info.getBytes();
       this.writeMsqlPackage(err);
    }
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

   
    public String getCharset() {
		return charset;
	}
	public void setCharset(int charsetIndex,String charsetName)
    {
    	this.charsetIndex=charsetIndex;
    	this.charset=charsetName;
    }
}
