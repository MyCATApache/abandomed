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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.engine.SQLEngineCtx;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.ConnectionException;
import io.mycat.net2.NIOHandler;
/**
 * front mysql connection handler (main NIO Handler)
 * @author wuzhihui
 *
 */
public class MySQLFrontConnectionHandler implements NIOHandler<MySQLFrontConnection> {
  
    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLFrontConnectionHandler.class);
    private final CheckUserLoginResponseCallback loginCmdHandler=new CheckUserLoginResponseCallback();
    public MySQLFrontConnectionHandler()
    {
    	DefaultSQLCommandHandler defaultCmdHandler=new DefaultSQLCommandHandler();
    	SQLEngineCtx.INSTANCE().setDefaultMySQLCmdHandler(defaultCmdHandler);

    }
   
 

	@Override
    public void onConnected(MySQLFrontConnection con) throws IOException {
        LOGGER.debug("onConnected", con);
        con.getSession().changeCmdHandler(con, loginCmdHandler);
        con.sendAuthPackge();
    }

    @Override
    public void onConnectFailed(MySQLFrontConnection con, ConnectionException e) {
        LOGGER.debug("onConnectFailed", con);
    }

    @Override
    public void onClosed(MySQLFrontConnection con, String reason) {
        LOGGER.debug("onClosed", con);
    }

	@Override
    public void handleReadEvent(final MySQLFrontConnection fronCon) throws IOException{
    	ConDataBuffer dataBuffer=fronCon.getReadDataBuffer();
        LOGGER.debug("handle", fronCon);
        int offset = dataBuffer.readPos(), length = 0, limit = dataBuffer.writingPos();
   	// 读取到了包头和长度
		//是否讀完一個報文
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
			dataBuffer.seReadingPos(offset);
			
			LOGGER.info("received pkg ,offset: "+pkgStartPos+" length: "+length+" type: "+packetType+" cur total length: "+limit);
			fronCon.getSession().getCurCmdHandler().processCmd(fronCon, dataBuffer, packetType, pkgStartPos, length);
			
		}
		
    }



	@Override
	public void onHandlerError(MySQLFrontConnection con, Exception e) {
		 con.close(e.toString());
		
	}

  
   

   

}
