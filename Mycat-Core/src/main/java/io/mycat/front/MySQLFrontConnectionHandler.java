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
package io.mycat.front;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.mysql.MySQLConnection;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.ConnectionException;
import io.mycat.net2.NIOHandler;
import io.mycat.net2.NetSystem;
import io.mycat.util.StringUtil;
/**
 * Front mysql connection handler (main NIO Handler).
 * 
 * @author wuzhihui
 *
 */
public class MySQLFrontConnectionHandler implements NIOHandler<MySQLFrontConnection> {
  
    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLFrontConnectionHandler.class);
    
    private final CheckUserLoginResponseCallback loginCmdHandler = new CheckUserLoginResponseCallback();
    
	@Override
    public void onConnected(MySQLFrontConnection con) throws IOException {
        LOGGER.debug("onConnected(): {}", con);
        con.getSession().changeCmdHandler(loginCmdHandler);
        con.sendAuthPackge();
    }

    @Override
    public void onConnectFailed(MySQLFrontConnection con, ConnectionException e) {
        LOGGER.debug("onConnectFailed(): {}", con);
    }

    @Override
    public void onClosed(MySQLFrontConnection con, String reason) {
        LOGGER.debug("onClosed(): {}", con);
    }

	@Override
    public void handleReadEvent(final MySQLFrontConnection cnxn) throws IOException{
        LOGGER.debug("handleReadEvent(): {}", cnxn);
        final ConDataBuffer buffer = cnxn.getReadDataBuffer();
        int offset = buffer.readPos(), limit = buffer.writingPos();
        // 读取到了包头和长度
		// 是否讀完一個報文
		for(; ; ) {
			if(!MySQLConnection.validateHeader(offset, limit)) {
				LOGGER.debug("C#{}B#{} validate protocol packet header: too short, ready to handle next the read event",
					cnxn.getId(), buffer.hashCode());
				return; 
			}
			int length = MySQLConnection.getPacketLength(buffer, offset);
			if((length + offset) > limit) {
				 LOGGER.debug("C#{}B#{} nNot a whole packet: required length = {} bytes, cur total length = {} bytes, "
				 	+ "ready to handle the next read event", cnxn.getId(), buffer.hashCode(), length, limit);
				return;
			}
			if(length == 4){
				// @todo handle empty packet
			}
			// 解析报文类型
			final byte packetType = buffer.getByte(offset + MySQLConnection.msyql_packetHeaderSize);
			final int pkgStartPos = offset;
			offset += length;
			buffer.setReadingPos(offset);
			// trace-protocol-packet
			// @author little-pan
			// @since 2016-09-29
			final NetSystem nets = NetSystem.getInstance();
			if(nets.getNetConfig().isTraceProtocol()){
				final String hexs = StringUtil.dumpAsHex(buffer, pkgStartPos, length);
				LOGGER.info("C#{}B#{} received a packet: offset = {}, length = {}, type = {}, cur total length = {}, packet bytes\n{}", 
					cnxn.getId(), buffer.hashCode(), pkgStartPos, length, packetType, limit, hexs);
			}
			cnxn.getSession().getCurCmdHandler().processCmd(cnxn, buffer, packetType, pkgStartPos, length);
		}
    }
	
	@Override
	public void onHandlerError(final MySQLFrontConnection con, final Exception e) {
		LOGGER.warn("onHandlerError(): connect = {}, error = {}", con, e);
		con.close(e.toString());
	}
	
}
