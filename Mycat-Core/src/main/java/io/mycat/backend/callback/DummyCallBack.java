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
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.ConnectionException;
/**
 * callback witch do nothing 
 * @author wuzhihui
 *
 */
public class DummyCallBack  implements BackConnectionCallback{
	 private static final Logger LOGGER = LoggerFactory.getLogger(DummyCallBack.class);
	 
	 
	@Override
	public void handleResponse(MySQLBackendConnection source, ConDataBuffer dataBuffer, byte packageType, int pkgStartPos,
			int pkgLen) throws IOException {
		LOGGER.warn("rereived not processed response  "+source);
		
	}
	

	@Override
	public void connectionError(ConnectionException e, MySQLBackendConnection conn) {
		LOGGER.warn("connection error "+conn,e);
		
	}
	@Override
	public void connectionAcquired(MySQLBackendConnection conn) {
		LOGGER.info("connection acquired "+conn);
		 
		
	}
	@Override
	public void connectionClose(MySQLBackendConnection conn, String reason) {
		LOGGER.info("connection closed ,reason:"+reason+ "  "+conn);
		 
		
	}
	@Override
	public void handlerError(Exception e, MySQLBackendConnection conn) {
		LOGGER.warn("hanlder error "+conn,e);
		 
		
	}

}
