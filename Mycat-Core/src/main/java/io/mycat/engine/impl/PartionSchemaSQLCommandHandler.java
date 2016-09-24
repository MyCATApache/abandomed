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
package io.mycat.engine.impl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.engine.ErrorCode;
import io.mycat.engine.SQLCommandHandler;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.net2.ConDataBuffer;

/**
 * 分片的Schema對應的 SQL Command Handler
 * @author wuzhihui
 *
 */
public class PartionSchemaSQLCommandHandler extends AbstractSchemaSQLCommandHandler{
	private static final Logger LOGGER = LoggerFactory.getLogger(PartionSchemaSQLCommandHandler.class);
	public PartionSchemaSQLCommandHandler() {
		// TODO Auto-generated constructor stub
	}


	@Override
	public void executeSetSQL(MySQLFrontConnection frontCon, ConDataBuffer dataBuffer, byte packageType,
			int pkgStartPos, int pkgLen, String sql) throws IOException {
		frontCon.writeErrMessage(ErrorCode.ER_BAD_DB_ERROR, "Not implemented Yet,Welcome to be a Commiter ,Leader want you !!");
	}

	@Override
	public void executeShowSQL(MySQLFrontConnection frontCon, ConDataBuffer dataBuffer, byte packageType,
			int pkgStartPos, int pkgLen, String sql) throws IOException {
		frontCon.writeErrMessage(ErrorCode.ER_BAD_DB_ERROR, "Not implemented Yet,Welcome to be a Commiter ,Leader want you !!");
		
	}

	@Override
	public void executeSelectSQL(MySQLFrontConnection frontCon, ConDataBuffer dataBuffer, byte packageType,
			int pkgStartPos, int pkgLen, String sql) throws IOException {
		frontCon.writeErrMessage(ErrorCode.ER_BAD_DB_ERROR, "Not implemented Yet,Welcome to be a Commiter ,Leader want you !!");
		
	}

}
