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
package io.mycat.front;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.beans.SchemaBean;
import io.mycat.engine.NoneBlockTask;
import io.mycat.engine.UserSession;
import io.mycat.mysql.MySQLConnection;

/**
 * front mysql connection
 *
 * @author wuzhihui
 */
public class MySQLFrontConnection extends MySQLConnection {
    private final UserSession session;
    private SchemaBean mycatSchema;
    private MySQLBackendConnection backendConnection;
    private final ArrayList<NoneBlockTask> todoTasks = new ArrayList<NoneBlockTask>(2);

    public MySQLFrontConnection(SocketChannel channel) {
        super(channel);
        session = new UserSession();
    }
    
    @Override
    public boolean init() throws IOException {
    	setProcessKey(channel.register(getSelector(), SelectionKey.OP_CONNECT, this));
    	driveState();
    	return true;
    }

    /**
     * 客戶端連接或者設置Schema的時候，需要調用此方法,確定是連接到了普通的Schema還是分片Schema
     *
     * @param schema
     * @throws IOException
     */
    public boolean setFrontSchema(String schema) throws IOException {
        //TODO 此处应该重新实现
        SchemaBean mycatSchema = null;
        if (schema != null) {

            mycatSchema = SQLEngineCtx.INSTANCE().getMycatSchema(schema);
            if (mycatSchema == null) {

                return false;
            }
            this.mycatSchema = mycatSchema;
            if (!mycatSchema.isNormalSchema()) {
                this.session.changeCmdHandler(SQLEngineCtx.INSTANCE().getPartionSchemaSQLCmdHandler());
            } else {
                session.changeCmdHandler(SQLEngineCtx.INSTANCE().getNomalSchemaSQLCmdHandler());
            }
            return true;
        } else {
            // 默认设置为不分片的SQL处理器
            session.changeCmdHandler(SQLEngineCtx.INSTANCE().getNomalSchemaSQLCmdHandler());
            return true;
        }
    }

    public void executePendingTask() {
        if (todoTasks.isEmpty()) {
            return;
        }
        NoneBlockTask task = todoTasks.remove(0);
        try {
            task.execute();
        } catch (Exception e) {
        	
        }
    }

    public void addTodoTask(NoneBlockTask task) {

        todoTasks.add(task);
    }

    public UserSession getSession() {
        return this.session;
    }

    public SchemaBean getMycatSchema() {
        return mycatSchema;
    }

    public void setMycatSchema(SchemaBean mycatSchema) {
        this.mycatSchema = mycatSchema;
    }

    public MySQLBackendConnection getBackendConnection() {
        return backendConnection;
    }

    public void setBackendConnection(MySQLBackendConnection backendConnection) {
        this.backendConnection = backendConnection;
    }
}
