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
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.mysql.Capabilities;
import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.packet.AuthPacket;
import io.mycat.mysql.packet.HandshakePacket;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.net2.Connection;
import io.mycat.util.SecurityUtil;
/**
 * backend mysql connection
 * @author wuzhihui
 *
 */

public class MySQLBackendConnection extends MySQLConnection {

    public static final Logger LOGGER = LoggerFactory.getLogger(MySQLBackendConnection.class);
    private final boolean fromSlaveDB;
    private BackConnectionCallback  userCallback;
    private MySQLDataSource dataSource;
    private boolean borrowed;
    private String schema;
    private HandshakePacket handshake;

    private boolean authenticated;

    private long clientFlags;

    private long maxPacketSize = 16 * 1024 * 1024;

    private int charsetIndex;

    private MySQLDataSource datasource;
    
    public MySQLBackendConnection(MySQLDataSource datasource,SocketChannel channel) {
        super(channel);
        this.datasource=datasource;
        this.fromSlaveDB=datasource.isSlaveNode();
        this.clientFlags = initClientFlags();
    }

    private static long initClientFlags() {
        int flag = 0;
        flag |= Capabilities.CLIENT_LONG_PASSWORD;
        flag |= Capabilities.CLIENT_FOUND_ROWS;
        flag |= Capabilities.CLIENT_LONG_FLAG;
        flag |= Capabilities.CLIENT_CONNECT_WITH_DB;
        // flag |= Capabilities.CLIENT_NO_SCHEMA;
        boolean usingCompress = false;
        if (usingCompress) {
            flag |= Capabilities.CLIENT_COMPRESS;
        }
        flag |= Capabilities.CLIENT_ODBC;
        flag |= Capabilities.CLIENT_LOCAL_FILES;
        flag |= Capabilities.CLIENT_IGNORE_SPACE;
        flag |= Capabilities.CLIENT_PROTOCOL_41;
        flag |= Capabilities.CLIENT_INTERACTIVE;
        // flag |= Capabilities.CLIENT_SSL;
        flag |= Capabilities.CLIENT_IGNORE_SIGPIPE;
        flag |= Capabilities.CLIENT_TRANSACTIONS;
        // flag |= Capabilities.CLIENT_RESERVED;
        flag |= Capabilities.CLIENT_SECURE_CONNECTION;
        // client extension
        flag |= Capabilities.CLIENT_MULTI_STATEMENTS;
        flag |= Capabilities.CLIENT_MULTI_RESULTS;
        return flag;
    }

    public void setNextStatus(byte packetType) {
        int status = this.getState();
        switch (status) {
        case Connection.STATE_IDLE:
            if (packetType == MySQLPacket.COM_QUERY) {
                this.setState(RESULT_INIT_STATUS);
                LOGGER.debug("DB status: Result Init");
            } else if (packetType == MySQLPacket.COM_QUIT) {
            	this.setState(Connection.STATE_IDLE);
            }
            break;
        case RESULT_INIT_STATUS:
            if (packetType == MySQLPacket.OK_PACKET) {
                this.setState(Connection.STATE_IDLE);
                LOGGER.debug("DB status: IDLE");
            } else if (packetType == MySQLPacket.ERROR_PACKET) {
                this.setState(Connection.STATE_IDLE);
                LOGGER.debug("DB status: IDLE");
            } else {
                this.setState(RESULT_HEADER_STATUS);
                LOGGER.debug("DB status: Result Header");
            }
            break;
        case RESULT_HEADER_STATUS:
            if (packetType == MySQLPacket.EOF_PACKET) {
                this.setState(RESULT_FETCH_STATUS);
                LOGGER.debug("DB status: Fetch Row");
            } else if (packetType == MySQLPacket.ERROR_PACKET) {
                this.setState(Connection.STATE_IDLE);
                LOGGER.debug("DB status: IDLE");
            } else {
                LOGGER.debug("DB status: Result Field");
            }
            break;
        case RESULT_FETCH_STATUS:
            if (packetType == MySQLPacket.EOF_PACKET) {
                this.setState(Connection.STATE_IDLE);
                LOGGER.debug("DB status: IDLE");
            } else if (packetType == MySQLPacket.ERROR_PACKET) {
                this.setState(Connection.STATE_IDLE);
                LOGGER.debug("DB status: IDLE");
            } else {
                LOGGER.debug("DB status: Reading row");
            }   
            break;
        case RESULT_FAIL_STATUS:
            if (packetType == MySQLPacket.EOF_PACKET) {
                this.setState(Connection.STATE_IDLE);
            }
            break;
        default:
            LOGGER.warn("Error connected status.", status);
            break;
        }
    }

  

    public BackConnectionCallback  getUserCallback() {
        return userCallback;
    }


	public void setUserCallback(BackConnectionCallback conCallback) {
        this.userCallback = conCallback;
    }

    public MySQLDataSource getPool() {
        return dataSource;
    }

    public void setPool(MySQLDataSource pool) {
        this.dataSource = pool;
    }

   
    public HandshakePacket getHandshake() {
        return handshake;
    }

    public void setHandshake(HandshakePacket handshake) {
        this.handshake = handshake;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public void authenticate() throws IOException {
        AuthPacket packet = new AuthPacket();
        packet.packetId = 1;
        packet.clientFlags = clientFlags;
        packet.maxPacketSize = maxPacketSize;
        packet.charsetIndex = this.charsetIndex;
        packet.user = user;
        try {
            packet.password = passwd(password, handshake);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
        packet.database = schema;
        System.out.println("auth "+user+" :"+password+"@"+schema);

        // write to connection
        this.writeMsqlPackage(packet);

    }

    private static byte[] passwd(String pass, HandshakePacket hs) throws NoSuchAlgorithmException {
        if (pass == null || pass.length() == 0) {
            return null;
        }
        byte[] passwd = pass.getBytes();
        int sl1 = hs.seed.length;
        int sl2 = hs.restOfScrambleBuff.length;
        byte[] seed = new byte[sl1 + sl2];
        System.arraycopy(hs.seed, 0, seed, 0, sl1);
        System.arraycopy(hs.restOfScrambleBuff, 0, seed, sl1, sl2);
        return SecurityUtil.scramble411(passwd, seed);
    }


    public boolean isBorrowed() {
        return false;
    }



    public boolean isFromSlaveDB() {
        return this.fromSlaveDB;
    }

    public MySQLDataSource getDatasource() {
        return datasource;
    }

    public void setDatasource(MySQLDataSource datasource) {
        this.datasource = datasource;
    }

    public void release() {
        this.datasource.releaseChannel(this);
        this.borrowed=false;
    }

	public void setBorrowed(boolean borrowed) {
		this.borrowed = borrowed;
	}
	 public String getSchema() {
	        return schema;
	    }

	    public void setSchema(String schema) {
	        this.schema = schema;
	    }
	 
 
}
