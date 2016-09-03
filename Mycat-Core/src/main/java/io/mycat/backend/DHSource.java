/*
 * Copyright (c) 2013, OpenCloudDB/MyCAT and/or its affiliates. All rights reserved.
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
package io.mycat.backend;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.SQLEngineCtx;
import io.mycat.beans.DataHostConfig;
/**
 * 数据节点（数据库）的连接池
 * @author wuzhihui
 *
 */
public abstract class DHSource {
    public static final Logger LOGGER = LoggerFactory.getLogger(DHSource.class);
    private final String name;
    private final int size;
    private final DataHostConfig config;
    private final ConMap conMap = new ConMap();
    private volatile long heartbeatRecoveryTime;

    public DHSource(DataHostConfig config) {
        this.size = config.getMaxCon();
        this.config = config;
        this.name = config.getHostName();
       
    }

    public boolean isMyConnection(BackendConnection con) {
        return (con.getPool() == this);
    }

       public int getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

   public long getExecuteCount() {
        long executeCount = 0;
        for (ConQueue queue : conMap.getAllConQueue()) {
            executeCount += queue.getExecuteCount();
        }
        return executeCount;
    }

    public long getExecuteCountForSchema(String schema) {
        return conMap.getSchemaConQueue(schema).getExecuteCount();

    }

    public int getActiveCountForSchema(String schema) {
        return conMap.getActiveCountForSchema(schema, this);
    }

    public int getIdleCountForSchema(String schema) {
        ConQueue queue = conMap.getSchemaConQueue(schema);
        int total = 0;
        total += queue.getAutoCommitCons().size() + queue.getManCommitCons().size();
        return total;
    }

    public boolean initSource() {
        int initSize = this.config.getMinCon();
        LOGGER.info("init backend myqsl source ,create connections total " + initSize + " for " + config);

        // long start=System.currentTimeMillis();
        // long timeOut=start+5000*1000L;
        Set<String> reactos= SQLEngineCtx.INSTANCE().getReactorMap().keySet();
        Iterator<String> itor=reactos.iterator();
        for (int i = 0; i < initSize; i++) {
            try {
            	String actorName=null;
            	if(!itor.hasNext())
            	{
            		itor=reactos.iterator();
            	}
            	actorName=itor.next();
            	this.createNewConnection(actorName, this.config.getDefaultSchema());
            } catch (Exception e) {
                LOGGER.warn(" init connection error.", e);
            }
        }
        LOGGER.info("init source finished");
        return true;
    }
    public int getActiveCount() {
        return this.conMap.getActiveCountForDs(this);
    }

    public void clearCons(String reason) {
        this.conMap.clearConnections(reason, this);
    }

    private BackendConnection takeCon(BackendConnection conn, final Object attachment, String schema) {

        conn.borrow();
        ConQueue queue = conMap.getSchemaConQueue(schema);
        queue.incExecuteCount();
        return conn;
    }

    private BackendConnection createNewConnection(String reactor, final String schema) throws IOException {
        // aysn create connection
        BackendConnection con = createNewConnectionOnReactor(reactor,schema);
        this.conMap.getSchemaConQueue(schema).getAutoCommitCons().add(con);
        return con;
    }

    public BackendConnection getConnection(String reactor,String schema, boolean autocommit, final Object attachment)
            throws IOException {
        BackendConnection con = this.conMap.tryTakeCon(reactor,schema, autocommit);
        if (con != null) {
            takeCon(con, // handler,
                    attachment, schema);
            return con;
        } else {
            int activeCons = this.getActiveCount();// 当前最大活动连接
            if (activeCons + 1 > size) {// 下一个连接大于最大连接数
                LOGGER.error("the max activeConnnections size can not be max than maxconnections");
                throw new IOException("the max activeConnnections size can not be max than maxconnections");
            } else { // create connection
                LOGGER.info(
                        "no ilde connection in pool,create new connection for " + this.name + " of schema " + schema);
                return createNewConnection(// handler,
                		reactor, schema);
            }
        }

    }

    private void returnCon(BackendConnection c) {
        // c.setAttachment(null);
        c.borrow();
        // c.setLastTime(TimeUtil.currentTimeMillis());
        ConQueue queue = this.conMap.getSchemaConQueue(c.getSchema());

        boolean ok = false;
        // if (c.isAutocommit()) {
        // ok = queue.getAutoCommitCons().offer(c);
        // } else {
        ok = queue.getManCommitCons().offer(c);
        // }
        if (!ok) {

            LOGGER.warn("can't return to pool ,so close con " + c);
            // c.close("can't return to pool ");
        }
    }

    public void releaseChannel(BackendConnection c) {
        returnCon(c);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("release channel " + c);
        }
    }

    public void connectionClosed(BackendConnection conn) {
        ConQueue queue = this.conMap.getSchemaConQueue(conn.getSchema());
        if (queue != null) {
            queue.removeCon(conn);
        }

    }

    public abstract BackendConnection createNewConnectionOnReactor(String reactor,String schema) throws IOException;

    public long getHeartbeatRecoveryTime() {
        return heartbeatRecoveryTime;
    }

    public void setHeartbeatRecoveryTime(long heartbeatRecoveryTime) {
        this.heartbeatRecoveryTime = heartbeatRecoveryTime;
    }

    public DataHostConfig getConfig() {
        return config;
    }
}