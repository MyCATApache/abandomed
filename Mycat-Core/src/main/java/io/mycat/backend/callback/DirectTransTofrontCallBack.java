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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.BackConnectionCallback;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.engine.ErrorCode;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.Connection;
import io.mycat.net2.ConnectionException;
/**
 * direct transfer bakend data to front connection
 * must attach (setAttachement) front connection on backend connection 
 * @author wuzhihui
 *
 */
public class DirectTransTofrontCallBack  implements BackConnectionCallback{
	 private static final Logger LOGGER = LoggerFactory.getLogger(DirectTransTofrontCallBack.class);
	 
	 
	@Override
	public void handleResponse(MySQLBackendConnection source, ConDataBuffer dataBuffer, byte packageType, int pkgStartPos,
			int pkgLen) throws IOException {
		 LOGGER.debug("Direct Trans To front CallBack."+source);
	        MySQLFrontConnection frontCon=getFrontCon(source);
	        frontCon.getWriteDataBuffer().putBytes(dataBuffer.getBytes(pkgStartPos, pkgLen));
        	frontCon.enableWrite(false);
	        source.setNextStatus(packageType);
	       
	        if (source.getState()== Connection.STATE_IDLE)
	        {
	        	LOGGER.debug("realese bakend connedtion "+source);
	        	source.release();
	        }
		
	}
	private MySQLFrontConnection getFrontCon(MySQLBackendConnection source)
	{
		return (MySQLFrontConnection) source.getAttachement();
	}

	@Override
	public void connectionError(ConnectionException e, MySQLBackendConnection conn) {
	getFrontCon(conn).failure(e.getCode(), e.toString());
		
	}
	@Override
	public void connectionAcquired(MySQLBackendConnection conn) {
		LOGGER.info("connection acquired "+conn);
		//执行异步任务（如果有，比如發送SQL語句給後端）
		getFrontCon(conn).executePendingTask();
		
	}
	@Override
	public void connectionClose(MySQLBackendConnection conn, String reason) {
		LOGGER.info("connection closed "+conn);
		if(conn.getState()!=Connection.STATE_CONNECTING)
		{
		getFrontCon(conn).getSession().removeBackCon(conn);
		}
		
	}
	@Override
	public void handlerError(Exception e, MySQLBackendConnection conn) {
		getFrontCon(conn).failure(ErrorCode.ERR_FOUND_EXCEPION, e.toString());
		
	}

}
