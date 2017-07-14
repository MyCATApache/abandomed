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

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;

/**
 * user session ,mean's a mysql client session or pg client session
 * @author wuzhihui
 *
 */
public class UserSession  {
	 private static final Logger LOGGER = LoggerFactory.getLogger(UserSession.class);	
private SQLCommandHandler curCmdHandler;	
private ArrayList<MySQLBackendConnection> backConLst=new  ArrayList<MySQLBackendConnection>();

public void changeCmdHandler(SQLCommandHandler newCmdHandler)
{
	
	this.curCmdHandler=newCmdHandler;
}

public SQLCommandHandler getCurCmdHandler() {
	return curCmdHandler;
}




public void removeBackCon(MySQLBackendConnection con)
{
	 if(this.backConLst.remove(con))
	 {
		 LOGGER.warn("remove back con ,but not found ! "+this); 
	 }
		 
	   
}

public void addBackCon(MySQLBackendConnection con)
{
	 if(this.backConLst.contains(con))
	 {
		 throw new RuntimeException("add a duplicate back connection to session,schema: "+con.getSchema());
	 }else
	 {
		 backConLst.add(con);
	 }
		   
			   
}

public  ArrayList<MySQLBackendConnection> getBackendCons() {
	return backConLst;
}

}
