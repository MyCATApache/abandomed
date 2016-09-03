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

import io.mycat.backend.BackendConnection;
import io.mycat.net2.Connection;

/**
 * 异步SQL命令，SQL返回的结果通过SQLCmdRespCallback来进行处理
 * @author wuzhihui
 *
 * @param <T>
 * @param <C>
 * @param <S>
 */
public class SQLCmd<T extends Connection, C extends BackendConnection ,S> {
public static int SUCCESS=1;
public static int ERROR=-1;
public static int RUNNING=0;
public static int TIMEOUT=-2;
private final String sql;
private final T con;
private final SQLCmdRespCallback<S> callBack;
private volatile int finishedType;
/**
 * 是否是前端用户发出的命令还是Mycat自己发出的命令
 */
private final boolean cmdFromUser;
public SQLCmd(String sql, T con, SQLCmdRespCallback<S> callBack, boolean cmdFromUser) {
	super();
	this.sql = sql;
	this.con = con;
	this.callBack = callBack;
	this.cmdFromUser = cmdFromUser;
}

public int getFinishedType() {
	return finishedType;
}

public void setFinishedType(int finishedType) {
	this.finishedType = finishedType;
}

public String getSql() {
	return sql;
}
public T getCon() {
	return con;
}
public SQLCmdRespCallback<S> getCallBack() {
	return callBack;
}
public boolean isCmdFromUser() {
	return cmdFromUser;
}


}
