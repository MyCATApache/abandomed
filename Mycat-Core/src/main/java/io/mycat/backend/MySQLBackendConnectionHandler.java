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
package io.mycat.backend;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.callback.LoginRespCallback;
import io.mycat.mysql.MySQLConnection;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.ConnectionException;
import io.mycat.net2.NIOHandler;
/**
 * backend mysql NIO handler (only one for all backend mysql connections)
 * @author wuzhihui
 *
 */
public class MySQLBackendConnectionHandler implements NIOHandler<MySQLBackendConnection> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLBackendConnectionHandler.class);

    @Override
    public void onConnected(MySQLBackendConnection con) throws IOException {
    	LoginRespCallback loginRepsCallback=new LoginRespCallback(LOGGER,con.getUserCallback());
    	con.setUserCallback(loginRepsCallback);
        // con.asynRead();
    }

    @Override
    public void handleReadEvent(MySQLBackendConnection con) throws IOException{
    	ConDataBuffer dataBuffer=con.getReadDataBuffer();
        int offset = dataBuffer.readPos(), length = 0, limit = dataBuffer.writingPos();
   	// 循环收到的报文处理
		while(true)
		{
			if(!MySQLConnection.validateHeader(offset, limit))
			{
				return; 
			}
			length = MySQLConnection.getPacketLength(dataBuffer, offset);
			if(length+offset>limit)
			{
				 LOGGER.info("Not whole package :length "+length+" cur total length "+limit);
				return;
			}
			// 解析报文类型
			byte packetType = dataBuffer.getByte(offset+MySQLConnection.msyql_packetHeaderSize);
			 
			int pkgStartPos=offset;
			offset += length;
			dataBuffer.setReadingPos(offset);
			
			LOGGER.info("received pkg ,length "+length+" type "+packetType+" cur total length "+limit);
			con.getUserCallback().handleResponse(con, dataBuffer, packetType, pkgStartPos, length);
	        }
			
			
    }

   

    @Override
    public void onClosed(MySQLBackendConnection source, String reason) {
    	source.getUserCallback().connectionClose(source, reason);
    }

    

	@Override
	public void onConnectFailed(MySQLBackendConnection con, ConnectionException e) {
		con.getUserCallback().connectionError(e, con);
		
	}

	@Override
	public void onHandlerError(MySQLBackendConnection con, Exception e) {
		con.getUserCallback().handlerError( e,con);
		
	}

  }
