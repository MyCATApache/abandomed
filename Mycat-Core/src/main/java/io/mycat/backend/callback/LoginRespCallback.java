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
package io.mycat.backend.callback;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;

import io.mycat.backend.BackConnectionCallback;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.engine.ErrorCode;
import io.mycat.mysql.packet.EOFPacket;
import io.mycat.mysql.packet.ErrorPacket;
import io.mycat.mysql.packet.HandshakePacket;
import io.mycat.mysql.packet.OkPacket;
import io.mycat.mysql.packet.Reply323Packet;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.Connection;
import io.mycat.net2.ConnectionException;
import io.mycat.util.CharsetUtil;
import io.mycat.util.SecurityUtil;
/**
 * used for backend mysql login
 * @author wuzhihui
 *
 */
public class LoginRespCallback implements BackConnectionCallback{
   private 	  BackConnectionCallback userCallback;
   private Logger LOGGER;
	public LoginRespCallback(Logger LOGGER,BackConnectionCallback userCallback)
	{
		this.userCallback=userCallback;
		this.LOGGER=LOGGER;
	}
	public void handleResponse(MySQLBackendConnection source, ConDataBuffer dataBuffer, byte packageType, int pkgStartPos,
			int pkgLen) throws IOException {
		 switch (packageType) {
         case OkPacket.FIELD_COUNT:
             HandshakePacket packet = source.getHandshake();
             if (packet == null) {
             	ByteBuffer byteBuff=dataBuffer.getBytes(pkgStartPos, pkgLen);
                 processHandShakePacket(source,byteBuff);
                 // 发送认证数据包
                 source.authenticate();
                 break;
             }
             // 处理认证结果
             source.setState(Connection.STATE_IDLE);             
             source.setAuthenticated(true);
             //通知用戶回調函數
             source.setUserCallback(userCallback);
             userCallback.connectionAcquired(source);
             return;
         case ErrorPacket.FIELD_COUNT:
         	ByteBuffer byteBuff=dataBuffer.getBytes(pkgStartPos, pkgLen);
             ErrorPacket err = new ErrorPacket();
             err.read(byteBuff);
             String errMsg = new String(err.message);
             LOGGER.warn("can't connect to mysql server ,errmsg:" + errMsg + " " + source);
             source.close(errMsg);
           //通知用戶回調函數
             userCallback.connectionError(new ConnectionException(err.errno, errMsg),source);
              return;
         case EOFPacket.FIELD_COUNT:
         	byte pkgId=dataBuffer.getByte(pkgStartPos+3);
             auth323(source, pkgId);
             break;
         default:
             packet = source.getHandshake();
             if (packet == null) {
             	byteBuff=dataBuffer.getBytes(pkgStartPos, pkgLen);
                 processHandShakePacket(source, byteBuff);
                 // 发送认证数据包
                 source.authenticate();
                 break;
             } else {
            	
            	 String errmgs="Login failed Unknown Packet,package type "+packageType+" length "+pkgLen;
            	 LOGGER.warn(errmgs);
            	 source.close(errmgs);
            	//通知用戶回調函數
            	 userCallback.connectionError(new ConnectionException(-1, errmgs),source);
                  return;
             }

         }

	}
	   private void processHandShakePacket(MySQLBackendConnection source, final ByteBuffer byteBuf) {
	        // 设置握手数据包
	        HandshakePacket packet = new HandshakePacket();
	        packet.read(byteBuf);
	        source.setHandshake(packet);
	       // System.out.println(packet.threadId+" "+new String(packet.serverVersion)+" "+packet.packetLength+" "+packet.protocolVersion+" "+packet.serverCapabilities+" "+packet.serverCharsetIndex+" "+packet.serverStatus+" "+packet.serverStatus+ " "+Arrays.toString(packet.seed)+ " "+Arrays.toString(packet.restOfScrambleBuff));
	               // source.setThreadId(packet.threadId);

	        // 设置字符集编码
	        int charsetIndex = (packet.serverCharsetIndex & 0xff);
	        String charset = CharsetUtil.getCharset(charsetIndex);
	         if (charset != null) {
	         source.setCharset(charsetIndex,charset);
	         } else {
	        	 String errmsg="Unknown charsetIndex:" + charsetIndex+ " of "+source;
	        	 LOGGER.warn(errmsg);
	        	 userCallback.connectionError(new ConnectionException(ErrorCode.ER_UNKNOWN_CHARACTER_SET,errmsg),source);
        		 return;
	         }
	    }

	    private static void auth323(MySQLBackendConnection source, byte packetId) throws IOException {
	        // 发送323响应认证数据包
	        Reply323Packet r323 = new Reply323Packet();
	        r323.packetId = ++packetId;
	        String pass = source.getPassword();
	        if (pass != null && pass.length() > 0) {
	            byte[] seed = source.getHandshake().seed;
	            r323.seed = SecurityUtil.scramble323(pass, new String(seed)).getBytes();
	        }
	        source.writeMsqlPackage(r323);
	    }
		@Override
		public void connectionError(ConnectionException e, MySQLBackendConnection conn) {
			//通知用戶回調函數
			userCallback.connectionError(e,conn);
			
		}
		@Override
		public void connectionAcquired(MySQLBackendConnection conn) {
			//not called in this callback
			
		}

		@Override
		public void connectionClose(MySQLBackendConnection conn, String reason) {
			userCallback.connectionClose(conn, reason);
			
		}
		@Override
		public void handlerError(Exception e, MySQLBackendConnection conn) {
			userCallback.handlerError(e,conn);
			
		}

}
