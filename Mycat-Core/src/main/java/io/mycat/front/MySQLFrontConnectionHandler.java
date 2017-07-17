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

import io.mycat.buffer.MycatByteBuffer;
import io.mycat.mysql.state.CloseState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.mysql.MySQLConnection;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.ConnectionException;
import io.mycat.net2.NIOHandler;
import io.mycat.net2.NetSystem;
import io.mycat.util.StringUtil;

/**
 * Front mysql connection handler (main NIO Handler).
 *
 * @author wuzhihui
 */
public class MySQLFrontConnectionHandler implements NIOHandler<MySQLFrontConnection> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLFrontConnectionHandler.class);

    @Override
    public void onConnected(MySQLFrontConnection con) throws IOException {
        LOGGER.debug("onConnected(): {}", con);
        con.driveState();
    }

    @Override
    public void onConnectFailed(MySQLFrontConnection con, ConnectionException e) {
        LOGGER.debug("onConnectFailed(): {}", con);
    }

    @Override
    public void onClosed(MySQLFrontConnection con, String reason) {
        LOGGER.debug("onClosed(): {}", con);
    }

    @Override
    public void handleReadEvent(final MySQLFrontConnection cnxn) throws IOException {
        LOGGER.debug("handleReadEvent(): {}", cnxn);
        final MycatByteBuffer buffer = cnxn.getDataBuffer();
        int offset = cnxn.getCurrentPacketStartPos();
        int limit = buffer.writeIndex();
        int length = MySQLConnection.getPacketLength(buffer, offset);
        final byte packetType = buffer.getByte(offset + MySQLConnection.msyql_packetHeaderSize);
        final int pkgStartPos = offset;
        final NetSystem nets = NetSystem.getInstance();
        if (nets.getNetConfig().isTraceProtocol()) {
//            final String hexs = StringUtil.dumpAsHex(buffer, pkgStartPos, length);
//            LOGGER.info("C#{}B#{} received a packet: offset = {}, length = {}, type = {}, cur total length = {}, packet bytes\n{}",
//                    cnxn.getId(), buffer.hashCode(), pkgStartPos, length, packetType, limit, hexs);
        }
        offset += length;
        cnxn.setCurrentPacketLength(length);
        cnxn.setCurrentPacketStartPos(pkgStartPos);
        cnxn.setCurrentPacketType(packetType);
        cnxn.driveState();
    }

    @Override
    public void onHandlerError(final MySQLFrontConnection con, final Exception e) {
        LOGGER.warn("onHandlerError(): connect = {}, error = {}", con, e);

    }

}
