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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.MycatCore;
import io.mycat.SQLEngineCtx;
import io.mycat.backend.mysql.DirectTransTofrontCallBack;
import io.mycat.backend.mysql.MySQLBackendConnection;
import io.mycat.engine.SQLCommandHandler;
import io.mycat.mysql.packet.MySQLMessage;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.mysql.handler.SelectHandler;
import io.mycat.net2.mysql.parser.ServerParse;
/**
 * Default SQL Command Handler
 * @author wuzhihui
 *
 */
public class DefaultSQLCommandHandler implements  SQLCommandHandler<MySQLFrontConnection>{
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSQLCommandHandler.class);
	private final DirectTransTofrontCallBack directTransCallback=new DirectTransTofrontCallBack();
	@Override
	public void processCmd(MySQLFrontConnection frontCon, ConDataBuffer dataBuffer, byte packageType, int pkgStartPos,
			int pkgLen) throws IOException {
		switch(packageType)
		{
			case MySQLPacket.COM_QUIT:
	            LOGGER.info("Client quit.");
	            frontCon.close("quit packet");
	            break;
	         default:
	        	 LOGGER.warn("not processed request pkg .");
		}
		
		 // 取得语句
		ByteBuffer byteBuff=dataBuffer.getBytes(pkgStartPos, pkgLen);
	    MySQLMessage mm = new MySQLMessage(byteBuff);
	    mm.position(5);
	    String sql = null;
	    sql = mm.readString(frontCon.getCharset());
	    LOGGER.debug(sql);
	    // 执行查询
	    int rs = ServerParse.parse(sql);
	    int sqlType = rs & 0xff;

	    // 检查当前使用的DB
	    // String db = "";
	    // if (db == null
	    // && sqlType!=ServerParse.USE
	    // && sqlType!=ServerParse.HELP
	    // && sqlType!=ServerParse.SET
	    // && sqlType!=ServerParse.SHOW
	    // && sqlType!=ServerParse.KILL
	    // && sqlType!=ServerParse.KILL_QUERY
	    // && sqlType!=ServerParse.MYSQL_COMMENT
	    // && sqlType!=ServerParse.MYSQL_CMD_COMMENT) {
	    // writeErrMessage(ErrorCode.ERR_BAD_LOGICDB, "No MyCAT Database
	    // selected");
	    // return;
	    // }

	    switch (sqlType) {
	    case ServerParse.EXPLAIN:
	        LOGGER.debug("EXPLAIN");
	        break;
	    case ServerParse.SET:
	        LOGGER.debug("SET");
	        break;
	    case ServerParse.SHOW:
	        LOGGER.debug("SHOW");
	        break;
	    case ServerParse.SELECT:
	        LOGGER.debug("SELECT");
	        // SelectHandler.handle(sql, con, rs >>> 8);
	        break;
	    case ServerParse.START:
	        LOGGER.debug("START");
	        break;
	    case ServerParse.BEGIN:
	        LOGGER.debug("BEGIN");
	        break;
	    case ServerParse.SAVEPOINT:
	        LOGGER.debug("SAVEPOINT");
	        break;
	    case ServerParse.KILL:
	        LOGGER.debug("KILL");
	        break;
	    case ServerParse.KILL_QUERY:
	        LOGGER.debug("KILL_QUERY");
	        break;
	    case ServerParse.USE:
	        LOGGER.debug("USE");
	        break;
	    case ServerParse.COMMIT:
	        LOGGER.debug("COMMIT");
	        break;
	    case ServerParse.ROLLBACK:
	        LOGGER.debug("ROLLBACK");
	        break;
	    case ServerParse.HELP:
	        LOGGER.debug("HELP");
	        break;
	    case ServerParse.MYSQL_CMD_COMMENT:
	        LOGGER.debug("MYSQL_CMD_COMMENT");
	        break;
	    case ServerParse.MYSQL_COMMENT:
	        LOGGER.debug("MYSQL_COMMENT");
	        break;
	    case ServerParse.LOAD_DATA_INFILE_SQL:
	        LOGGER.debug("LOAD_DATA_INFILE_SQL");
	        break;
	    default:
	        LOGGER.debug("DEFAULT");
	    }
	    if (sql.equals("select @@version_comment limit 1")) {
	        SelectHandler.handle(sql, frontCon, rs >>> 8);
	       // return;
	    }
	        //直接透传（默认）
	        final String db="mysql";
	        MySQLBackendConnection existCon= (MySQLBackendConnection) frontCon.getSession().getBackendCons().get(db);
	        if(existCon==null||existCon.isClosed())
	        {
	        	if(existCon!=null)
	        	{
	        		frontCon.removeBackCon(existCon);
	        	}
	        	final MySQLBackendConnection newCon= (MySQLBackendConnection)SQLEngineCtx.INSTANCE().getDHReplicatSet("mysql1").getCurWriteDH()
	 	                .getConnection(frontCon.getReactor(),MycatCore.MOCK_SCHEMA, true, null);
	        	newCon.setAttachment(frontCon);//must in direct callback handler
	        	newCon.setUserCallback(directTransCallback);
	 	       frontCon.addTodoTask(()->
	 	    	  {newCon.getWriteDataBuffer().putBytes(dataBuffer.getBytes(pkgStartPos, pkgLen));newCon.enableWrite(false);frontCon.addBackCon(newCon);});
	        }else
	        {
	        	existCon.getWriteDataBuffer().putBytes(dataBuffer.getBytes(pkgStartPos, pkgLen));
	        	existCon.enableWrite(false);
	        }
	        
	       
	}

}
