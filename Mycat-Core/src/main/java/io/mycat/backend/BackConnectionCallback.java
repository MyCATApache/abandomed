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

import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.ConnectionException;
/**
 * 后端数据库的事件处理回调接口
 * @author wuzhihui
 *
 */
public interface BackConnectionCallback  {

    /**
     * 无法获取连接
     * 
     * @param e
     * @param conn
     */
     void connectionError(ConnectionException e, MySQLBackendConnection conn);

    /**
     * 已获得有效连接的响应处理
     */
    void connectionAcquired(MySQLBackendConnection conn);

    /**
     * 收到数据包的响应处理
     */
    void handleResponse(MySQLBackendConnection conn,ConDataBuffer dataBuffer,byte packageType,int pkgStartPos,int pkgLen) throws IOException ;

    /**
     * on connetion close event
     */
    void connectionClose(MySQLBackendConnection conn, String reason);
    
    /**
     * 处理数据的过程中发生错误
     * @param e
     * @param conn
     */
    void handlerError(Exception e,MySQLBackendConnection conn);
 

}