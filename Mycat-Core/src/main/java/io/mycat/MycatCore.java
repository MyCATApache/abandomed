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
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnectionFactory;
import io.mycat.backend.MySQLReplicatSet;
import io.mycat.beans.MySQLRepBean;
import io.mycat.beans.SchemaBean;
import io.mycat.engine.impl.NormalSchemaSQLCommandHandler;
import io.mycat.engine.impl.PartionSchemaSQLCommandHandler;
import io.mycat.front.MySQLFrontendConnectionFactory;
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
        final SystemConfig sysconfig = new SystemConfig();
        sysconfig.setTraceProtocol(true);
        NetSystem.getInstance().setNetConfig(sysconfig);
        MySQLBackendConnectionFactory bakcMySQLFactory=new MySQLBackendConnectionFactory();
        SQLEngineCtx.INSTANCE().setBackendMySQLConFactory(bakcMySQLFactory);
        MySQLFrontendConnectionFactory frontFactory = new MySQLFrontendConnectionFactory();
        NIOAcceptor server = new NIOAcceptor("Server", "0.0.0.0", PORT, frontFactory, reactorPool);
        server.start();
        // server started
        
        LOGGER.info(server.getName() + " is started and listening on " + server.getPort());
        NormalSchemaSQLCommandHandler normalSchemaSQLCmdHandler=new NormalSchemaSQLCommandHandler();
    	SQLEngineCtx.INSTANCE().setNormalSchemaSQLCmdHandler(normalSchemaSQLCmdHandler);
    	PartionSchemaSQLCommandHandler partionSchemaSQLCmdHandler=new PartionSchemaSQLCommandHandler();
    	SQLEngineCtx.INSTANCE().setPartionSchemaSQLCmdHandler(partionSchemaSQLCmdHandler);
    	URL datasourceURL = ConfigLoader.class.getResource("/datasource.xml");
    	List<MySQLRepBean> mysqlRepBeans = ConfigLoader.loadMySQLRepBean(datasourceURL.toString());
        for(final MySQLRepBean repBean : mysqlRepBeans)
        {
        	MySQLReplicatSet mysqlRepSet = new MySQLReplicatSet(repBean,0);
            SQLEngineCtx.INSTANCE().addMySQLReplicatSet(mysqlRepSet);	
        }
        URL schemaURL=ConfigLoader.class.getResource("/schema.xml");
        List<SchemaBean>  schemaBeans=ConfigLoader.loadSheamBeans(schemaURL.toString());
        for(SchemaBean schemaBean:schemaBeans)
        {
        	SQLEngineCtx.INSTANCE().addSchemaBean(schemaBean);
        }
    }
}
