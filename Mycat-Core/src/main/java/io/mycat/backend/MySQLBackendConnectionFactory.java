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
package io.mycat.backend;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import io.mycat.backend.callback.DummyCallBack;
import io.mycat.beans.MySQLBean;
import io.mycat.net2.NetSystem;
/**
 * bakcend mysql connection factory
 * @author wuzhihui
 *
 */
public class MySQLBackendConnectionFactory {
 
    private final MySQLBackendConnectionHandler nioHandler = new MySQLBackendConnectionHandler();
    private final DummyCallBack dummyCallBack=new DummyCallBack();
    public MySQLBackendConnection make(MySQLDataSource pool,String reactor, String schema) throws IOException {

    	MySQLBean dsc = pool.getConfig();
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);

        MySQLBackendConnection c = new MySQLBackendConnection(pool,channel);
        NetSystem.getInstance().setSocketParams(c, false);
        // 设置NIOHandlers
        c.setHandler(nioHandler);
        c.setHost(dsc.getIp());
        c.setPort(dsc.getPort());
        c.setUser(dsc.getUser());
        c.setPassword(dsc.getPassword());
        c.setSchema(schema);
        c.setPool(pool);
        c.setNIOReactor(reactor);
        c.setUserCallback(dummyCallBack);
        c.setIdleTimeout(NetSystem.getInstance().getNetConfig().getConIdleTimeout()*60*1000L);
        NetSystem.getInstance().getConnector().postConnect(c);
        return c;
    }
}
