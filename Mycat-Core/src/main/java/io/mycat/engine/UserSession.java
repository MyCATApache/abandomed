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
package io.mycat.engine;

import java.util.HashMap;
import java.util.Map;

import io.mycat.backend.BackendConnection;
import io.mycat.net2.Connection;

/**
 * user session ,mean's a mysql client session or pg client session
 * @author wuzhihui
 *
 */
public class UserSession<T extends Connection> {
	
private SQLCommandHandler<T> curCmdHandler;	
public void changeCmdHandler(T frontCon, SQLCommandHandler<T> newCmdHandler)
{
	frontCon.ensureInActorThread();
	this.curCmdHandler=newCmdHandler;
}

public SQLCommandHandler<T> getCurCmdHandler() {
	return curCmdHandler;
}

/**
 * 当前Session所关联的所有后端连接（包括正在使用以及使用过还没释放的连接），key是連接所在的schema，
 */
private final java.util.Map<String, BackendConnection> backendCons=new HashMap<String,BackendConnection>(8);

public  Map<String,BackendConnection> getBackendCons() {
	return backendCons;
}



	

}
