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
import java.nio.channels.SocketChannel;

import io.mycat.net2.Connection;
import io.mycat.net2.ConnectionFactory;
import io.mycat.net2.NIOHandler;
/**
 * create front mysql connection
 * @author wuzhihui
 *
 */
public class MySQLFrontendConnectionFactory extends ConnectionFactory {
	/**
	 * 全局共享的前段NIO Handler
	 */
	 private final MySQLFrontConnectionHandler handler = new MySQLFrontConnectionHandler();
    

	@Override
    protected Connection makeConnection(SocketChannel channel) throws IOException {
        MySQLFrontConnection con = new MySQLFrontConnection(channel);
        // con.setPrivileges(MycatPrivileges.instance());
        // con.setCharset("UTF-8");
        // con.setLoadDataInfileHandler(new ServerLoadDataInfileHandler(c));
        // c.setPrepareHandler(new ServerPrepareHandler(c));
        // con.setTxIsolation(sys.getTxIsolation());
        return con;
    }

    @Override
    protected NIOHandler<MySQLFrontConnection> getNIOHandler() {
        return handler;
    }

}
