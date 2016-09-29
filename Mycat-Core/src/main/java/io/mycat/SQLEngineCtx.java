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
package io.mycat;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnectionFactory;
import io.mycat.backend.MySQLReplicatSet;
import io.mycat.beans.SchemaBean;
import io.mycat.engine.SQLCommandHandler;
import io.mycat.net2.NIOReactor;

/**
 * SQL execute context ,not for specail sql data,but for gloabl env ,
 * @author wuzhihui
 *
 */
public class SQLEngineCtx {
	
	protected final static Logger LOGGER = LoggerFactory.getLogger(SQLEngineCtx.class);
	
	final static SQLEngineCtx instance;
	static {
		instance = new SQLEngineCtx();
	}
	
	/**
	 * 不分片的Schema對應的SQL处理器
	 */
	private SQLCommandHandler normalSchemaSQLCmdHandler;
	private SQLCommandHandler partionSchemaSQLCmdHandler;
	
	private  MySQLBackendConnectionFactory backendMySQLConFactory;
	
	/**
	 * 系统中所有的NIO Reactor
	 */
	private Map<String,NIOReactor> reactorMap=new HashMap<String,NIOReactor>();

/**
 * 系统中所有MySQLReplicatSet的Map
 */
private Map<String,MySQLReplicatSet> msqlRepSetMap=new HashMap<String,MySQLReplicatSet>();

/**
 * 系统中所有SchemaBean的Map
 */
private Map<String,SchemaBean> mycatSchemaMap=new HashMap<String,SchemaBean>();


public  SQLCommandHandler getNomalSchemaSQLCmdHandler()
{
	return normalSchemaSQLCmdHandler;
	
}
public  SQLCommandHandler getPartionSchemaSQLCmdHandler()
{
	return this.partionSchemaSQLCmdHandler;
	
}
protected void initReactorMap(NIOReactor[] reactors)
{
	for(NIOReactor r:reactors)
	{
		reactorMap.put(r.getName(), r);
	}
}
public NIOReactor findReactor(String name)
{
	return reactorMap.get(name);
}
public Map<String, NIOReactor> getReactorMap() {
	return reactorMap;
}


protected void setNormalSchemaSQLCmdHandler(SQLCommandHandler normalSchemaSQLCmdHandler) {
	this.normalSchemaSQLCmdHandler = normalSchemaSQLCmdHandler;
}
protected void setPartionSchemaSQLCmdHandler(SQLCommandHandler partionSchemaSQLCmdHandler) {
	this.partionSchemaSQLCmdHandler = partionSchemaSQLCmdHandler;
}
public static SQLEngineCtx INSTANCE()
{
	return instance;
}

public MySQLReplicatSet getMySQLReplicatSet(String repsetName)
{
	return this.msqlRepSetMap.get(repsetName);
}
public MySQLBackendConnectionFactory getBackendMySQLConFactory() {
	return backendMySQLConFactory;
}
protected void setBackendMySQLConFactory(MySQLBackendConnectionFactory backendMySQLConFactory) {
	this.backendMySQLConFactory = backendMySQLConFactory;
}

 	protected void addMySQLReplicatSet(final MySQLReplicatSet repSet) {
 		final String repSetName = repSet.getName();
 		LOGGER.debug("add MySQL replicatSet: {} = {}", repSetName, repSet);
 		this.msqlRepSetMap.put(repSetName, repSet);
 	}
 
 protected void addSchemaBean(SchemaBean schemaBean)
 {
	 this.mycatSchemaMap.put(schemaBean.getName(), schemaBean);
 }
 
 public SchemaBean getMycatSchema(String schema)
 {
 	return this.mycatSchemaMap.get(schema);
 }
}
