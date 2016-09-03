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

import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import io.mycat.backend.BackendConnection;
import io.mycat.engine.FrontConnection;
import io.mycat.engine.NoneBlockTask;
import io.mycat.engine.UserSession;
/**
 * front mysql connection
 * @author wuzhihui
 *
 */
public class MySQLFrontConnection extends MySQLConnection implements FrontConnection{
	private final UserSession<MySQLFrontConnection> session;
	private final ArrayList<NoneBlockTask> todoTasks=new ArrayList<NoneBlockTask>(2);
public MySQLFrontConnection(SocketChannel channel) {
		super(channel);
		session=new UserSession<MySQLFrontConnection>();
	}

   @SuppressWarnings("unchecked")
public void executePendingTask()
   {
	   this.ensureInActorThread();
	   if(todoTasks.isEmpty())
	   {
		   return;
	   }
	   NoneBlockTask task= todoTasks.remove(0);
	  try {
		task.execute();
	} catch (Exception e) {
		this.getHandler().onHandlerError(this, e);
	}
   }
   public void addTodoTask(NoneBlockTask task)
   {
	   this.ensureInActorThread();
	   todoTasks.add(task);
   }

   /**
    * 从sesshion中获取可能的后端连接
    * @param schema
    * @return
    */
   public BackendConnection findBackCon(String schema)
   {
	   return session.getBackendCons().get(schema);
   }
   
   public void removeBackCon(BackendConnection con)
   {
	   this.ensureInActorThread();
	   BackendConnection delted= session.getBackendCons().remove(con.getSchema());
	   if(delted==null)
	   {
		   LOGGER.warn("remove back con ,but not found ! "+this);
	   }else if(delted!=con)
	   {
		   LOGGER.warn("remove back con ,but not same con ,may be a BUG!! "+this);
	   }
	   
   }
   
   public void addBackCon(BackendConnection con)
   {
	   this.ensureInActorThread();
	   BackendConnection existCon=session.getBackendCons().get(con.getSchema());
	   if(existCon==con)
	   {
		   LOGGER.warn("add exist connection to session ,may be a BUG!! "+this);
	   }else if(existCon!=null)
	   {
		   throw new RuntimeException("add a duplicate back connection to session,schema: "+con.getSchema());
	   }
	   LOGGER.debug("add new backend con to session "+this);
	   session.getBackendCons().put(con.getSchema(), con);
		   
			   
   }
	@Override
	public UserSession<MySQLFrontConnection> getSession() {
		return this.session;
	}

}
