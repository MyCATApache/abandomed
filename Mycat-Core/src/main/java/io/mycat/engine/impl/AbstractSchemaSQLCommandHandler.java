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
import java.nio.ByteBuffer;
import java.util.ArrayList;

import io.mycat.sqlcache.HintSQLInfo;
import io.mycat.sqlcache.HintSQLParser;
import io.mycat.sqlcache.SQLResultsCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.backend.MySQLDataSource;
import io.mycat.backend.MySQLReplicatSet;
import io.mycat.backend.callback.DirectTransTofrontCallBack;
import io.mycat.beans.DNBean;
import io.mycat.beans.SchemaBean;
import io.mycat.engine.ErrorCode;
import io.mycat.engine.SQLCommandHandler;
import io.mycat.engine.UserSession;
import io.mycat.engine.sqlparser.SQLInfo;
import io.mycat.engine.sqlparser.ServerParse;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.MySQLMessage;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.mysql.packet.OkPacket;
import io.mycat.net2.ConDataBuffer;

/**
 * 抽象类，负责处理前段发来的 SQL Command
 * 
 * @author wuzhihui
 *
 */
public abstract class AbstractSchemaSQLCommandHandler implements SQLCommandHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSchemaSQLCommandHandler.class);
	protected final DirectTransTofrontCallBack directTransCallback = new DirectTransTofrontCallBack();
	@Override
	public void processCmd(MySQLFrontConnection frontCon, ConDataBuffer dataBuffer, byte packageType, int pkgStartPos,
			int pkgLen) throws IOException {
		switch (packageType) {
		case MySQLPacket.COM_QUIT:
			LOGGER.info("Client quit.");
			frontCon.close("quit packet");
			break;
		case MySQLPacket.COM_INIT_DB:
			// Implementation: use database;
			// @author little-pan
			// @since 2016-09-29
			final ByteBuffer byteBuff = dataBuffer.getBytes(pkgStartPos, pkgLen);
			initDb(frontCon, byteBuff);
			break;
		default:
			doSQLCommand(frontCon, dataBuffer, packageType, pkgStartPos, pkgLen);
		}
	}

	private void initDb(final MySQLFrontConnection frontCon, final ByteBuffer byteBuffer) throws IOException {
		final MySQLMessage mm = new MySQLMessage(byteBuffer);
		mm.position(5);
		final String db = mm.readString();
		// 1. check access etc
		LOGGER.debug("check access to schema: {}", db);
		// 2. set schema
		if (frontCon.setFrontSchema(db) == false) {
			frontCon.writeErrMessage(ErrorCode.ER_NO_DB_ERROR, "No schema defined: " + db);
			return;
		}
		frontCon.write(OkPacket.OK);
		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("C#{}B#{} init-db ok: schema = {}", frontCon.getId(), byteBuffer.hashCode(), db);
		}
	}
	
	private void doSQLCommand(MySQLFrontConnection frontCon, ConDataBuffer dataBuffer, byte packageType,
			int pkgStartPos, int pkgLen) throws IOException {
		{
			//取得语句
			ByteBuffer byteBuff = dataBuffer.getBytes(pkgStartPos, pkgLen);
			MySQLMessage mm = new MySQLMessage(byteBuff);
			mm.position(5);
			String sql = mm.readString(frontCon.getCharset());
			/**
			 * parser hit sql
			 * 需要改写sql语句，去掉前面的注释即可
			 * 首先判断是否
			 */
			HintSQLInfo hintSQL = HintSQLParser.parserHintSQL(sql);
			if (hintSQL != null && hintSQL.isCache()){
				/**
				 * 0.判断是select语句
				 * 1.改写sql语句，去掉前面的注释
				 * 2.发送sql语句到后端执行。做好backend connection的Handler设置工作
				 * 3.解析sql的结果集，决定是否异步缓存，一部分直接发送给您前端。
				 * 4.如果当前sql的结果集已经缓存了。直接从本地缓存中拉去结果集即可
				 */

				if (!SQLResultsCacheService.getInstance().processHintSQL(frontCon,hintSQL,mm)){
					frontCon.writeErrMessage(ErrorCode.ER_NO_DB_ERROR, "cache no implementation");
				}

				return;
			}

			// 执行查询
			SQLInfo sqlInf=new SQLInfo();
			int rs = ServerParse.parse(sql,sqlInf);
			int sqlType = rs & 0xff;
			switch (sqlType) {
			case ServerParse.EXPLAIN:
				LOGGER.debug("EXPLAIN");
				break;
			case ServerParse.SET:
				LOGGER.debug("SET");
				executeSetSQL(frontCon, dataBuffer, packageType,pkgStartPos, pkgLen,sql);
				break;
			case ServerParse.SHOW:
				LOGGER.debug("SHOW");
				executeShowSQL(frontCon, dataBuffer, packageType,pkgStartPos, pkgLen,sql);
				break;
			case ServerParse.SELECT:
				LOGGER.debug("SELECT");
				SchemaBean mycatSchema = frontCon.getMycatSchema();
				if (mycatSchema == null) {
					frontCon.writeErrMessage(ErrorCode.ER_NO_DB_ERROR, "No database selected");
					return;
				}

				executeSelectSQL(frontCon, dataBuffer, packageType,pkgStartPos, pkgLen,sql);

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
				String schema=sqlInf.getDb();
				if (!frontCon.setFrontSchema(schema)) {
					frontCon.writeErrMessage(ErrorCode.ER_NO_DB_ERROR, "No Mycat Schema defined :" +schema);
				} else {
					frontCon.write(OkPacket.OK);
				}

				return;
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

			
		}
	}
	public void passThroughSQL(MySQLFrontConnection frontCon, ConDataBuffer dataBuffer, int pkgStartPos, int pkgLen)
			throws IOException {
		// 直接透传（默认）获取连接池
		MySQLBackendConnection existCon = null;
		UserSession session=frontCon.getSession();
		ArrayList<MySQLBackendConnection> allBackCons = session.getBackendCons();
		if (!allBackCons.isEmpty()) {
			existCon = allBackCons.get(0);
		}
		if (existCon == null || existCon.isClosed()) {
			if (existCon != null) {
				session.removeBackCon(existCon);
			}
			if (frontCon.getMycatSchema() == null){
				frontCon.writeErrMessage(1450, "No schema selected");
				LOGGER.error("No schema selected");
				return ;
			}

			final DNBean dnBean = frontCon.getMycatSchema().getDefaultDN();
			final String replica   = dnBean.getMysqlReplica();
			final SQLEngineCtx ctx = SQLEngineCtx.INSTANCE();
			LOGGER.debug("select a replica: {}", replica);
			final MySQLReplicatSet repSet = ctx.getMySQLReplicatSet(replica);
			final MySQLDataSource datas   = repSet.getCurWriteDH();

			/**
			 * 如果该sql对应后端db，没有连接池，则创建连接池部分
			 */
			final MySQLBackendConnection newCon = 
					datas.getConnection(frontCon.getReactor(), dnBean.getDatabase(), true, null);

			/**很关键的设置前端front 与 backend session*/
			newCon.setAttachement(frontCon);

			/**设置后端连接池结果集处理handler*/
			newCon.setUserCallback(directTransCallback);

			/**
			 * 执行sql语句
			 */
			frontCon.addTodoTask(() -> {
				/**
				 * 将数据写到后端连接池中
				 */
				newCon.getWriteDataBuffer().putBytes(dataBuffer.getBytes(pkgStartPos,pkgLen));
				newCon.enableWrite(false);
				/**
				 * 新建立的连接放到连接池中
				 */
				session.addBackCon(newCon);
			});
		} else {
			/**
			 * 否则直接写到后端即可
			 */
			existCon.getWriteDataBuffer().putBytes(dataBuffer.getBytes(pkgStartPos, pkgLen));
			existCon.enableWrite(false);
			existCon.setUserCallback(directTransCallback);
		}
	}
	
	public abstract void executeSetSQL(MySQLFrontConnection frontCon, ConDataBuffer dataBuffer, byte packageType,
			int pkgStartPos, int pkgLen,String sql) throws IOException;
	
	public abstract void executeShowSQL(MySQLFrontConnection frontCon, ConDataBuffer dataBuffer, byte packageType,
			int pkgStartPos, int pkgLen,String sql) throws IOException;
	
	public abstract void executeSelectSQL(MySQLFrontConnection frontCon, ConDataBuffer dataBuffer, byte packageType,
			int pkgStartPos, int pkgLen,String sql) throws IOException;
}
