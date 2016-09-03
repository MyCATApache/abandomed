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
 
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.DHSource;
import io.mycat.backend.mysql.MySQLBackendConnectionFactory;
import io.mycat.backend.mysql.MySQLDataSource;
import io.mycat.backend.mysql.MySQLMSReplicatSet;
import io.mycat.beans.DataHostConfig;
import io.mycat.mysql.DefaultSQLCommandHandler;
import io.mycat.mysql.MySQLFrontendConnectionFactory;
import io.mycat.net2.ExecutorUtil;
import io.mycat.net2.NIOAcceptor;
import io.mycat.net2.NIOConnector;
import io.mycat.net2.NIOReactorPool;
import io.mycat.net2.NameableExecutor;
import io.mycat.net2.NamebleScheduledExecutor;
import io.mycat.net2.NetSystem;
import io.mycat.net2.SharedBufferPool;
import io.mycat.net2.SystemConfig;
/**
 * 
 * @author wuzhihui
 *
 */
public class MycatCore {
    private static final Logger LOGGER = LoggerFactory.getLogger(MycatCore.class);
    public static final int PORT = 8066;

    public static final String MOCK_HOSTNAME = "host1";

    public static final String MOCK_SCHEMA = "mysql";


    static {
       
    }

    private void init()
    {
    	
         
    }
    public static void main(String[] args) throws IOException {
        // Business Executor ，用来执行那些耗时的任务
        NameableExecutor businessExecutor = ExecutorUtil.create("BusinessExecutor", 10);
        // 定时器Executor，用来执行定时任务
        NamebleScheduledExecutor timerExecutor = ExecutorUtil.createSheduledExecute("Timer", 5);

        
        SharedBufferPool sharedPool = new SharedBufferPool(1024 * 1024 * 100, 1024);
        new NetSystem(sharedPool, businessExecutor, timerExecutor);
        // Reactor pool
        NIOReactorPool reactorPool = new NIOReactorPool("Reactor Pool", 5, sharedPool);
        SQLEngineCtx.INSTANCE().initReactorMap(reactorPool.getAllReactors());
        NIOConnector connector = new NIOConnector("NIOConnector", reactorPool);
        connector.start();
        NetSystem.getInstance().setConnector(connector);
        NetSystem.getInstance().setNetConfig(new SystemConfig());
        MySQLBackendConnectionFactory bakcMySQLFactory=new MySQLBackendConnectionFactory();
        SQLEngineCtx.INSTANCE().setBackendMySQLConFactory(bakcMySQLFactory);
        MySQLFrontendConnectionFactory frontFactory = new MySQLFrontendConnectionFactory();
        NIOAcceptor server = new NIOAcceptor("Server", "0.0.0.0", PORT, frontFactory, reactorPool);
        server.start();
        // server started
        
        LOGGER.info(server.getName() + " is started and listening on " + server.getPort());
        DefaultSQLCommandHandler defaultCmdHandler=new DefaultSQLCommandHandler();
    	SQLEngineCtx.INSTANCE().setDefaultMySQLCmdHandler(defaultCmdHandler);
    	
        DataHostConfig config = new  DataHostConfig("host1", "127.0.0.1", 3306,  "root", "123456","mysql");
        config.setMaxCon(10);
        DataHostConfig[] configs={config};
        MySQLMSReplicatSet mysqlRepSet=new MySQLMSReplicatSet("mysql1",configs);
        SQLEngineCtx.INSTANCE().addDHReplicatSet(mysqlRepSet);
    }
}
