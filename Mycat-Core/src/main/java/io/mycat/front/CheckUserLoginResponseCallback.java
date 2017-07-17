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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.engine.ErrorCode;
import io.mycat.engine.SQLCommandHandler;
import io.mycat.mysql.packet.AuthPacket;
import io.mycat.mysql.state.CloseState;
import io.mycat.mysql.state.IdleState;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.states.ClosingState;
import io.mycat.net2.states.ReadWaitingState;
import io.mycat.util.CharsetUtil;

/**
 * Util class used for backend mysql connnection login
 *
 * @author wuzhihui
 */
public class CheckUserLoginResponseCallback implements SQLCommandHandler {
    private static final byte[] AUTH_OK = new byte[]{7, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0};
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckUserLoginResponseCallback.class);

    public void processCmd(MySQLFrontConnection con, ConDataBuffer dataBuffer, byte packageType, int pkgStartPos,
                           int pkgLen) throws IOException {
        AuthPacket auth = new AuthPacket();
        auth.read(dataBuffer);

        // Fake check user
        LOGGER.debug("Check user name. " + auth.user);
        if (!auth.user.equals("root")) {
            LOGGER.debug("User name error. " + auth.user);
            con.failure(ErrorCode.ER_ACCESS_DENIED_ERROR, "Access denied for user '" + auth.user + "'");
            con.setNextState(CloseState.INSTANCE);
            return;
        }

        // Fake check password
        LOGGER.debug("Check user password. " + new String(auth.password));

        // check schema
        LOGGER.debug("Check database. " + auth.database);

        success(con, auth);

    }

    private void success(MySQLFrontConnection con, AuthPacket auth) throws IOException {
        LOGGER.debug("Login success");
        // 设置字符集编码
        int charsetIndex = (auth.charsetIndex & 0xff);
        final String charset = CharsetUtil.getCharset(charsetIndex);
        if (charset == null) {
            final String errmsg = "Unknown charsetIndex:" + charsetIndex;
            LOGGER.warn(errmsg);
            con.writeErrMessage(ErrorCode.ER_UNKNOWN_CHARACTER_SET, errmsg);
            con.setNextState(CloseState.INSTANCE);
            return;
        }
        LOGGER.debug("charset = {}, charsetIndex = {}", charset, charsetIndex);
        con.setCharset(charsetIndex, charset);

        if (!con.setFrontSchema(auth.database)) {
            final String errmsg = "No Mycat Schema defined: " + auth.database;
            LOGGER.debug(errmsg);
            con.writeErrMessage(ErrorCode.ER_BAD_DB_ERROR,"42000".getBytes(), errmsg);
            
            con.setWriteCompleteListener(() -> {
            	con.setNextState(CloseState.INSTANCE);
                con.setNextNetworkState(ClosingState.INSTANCE);
            });
        } else {
            con.clearCurrentPacket();
            con.write(AUTH_OK);
            con.setWriteCompleteListener(() -> {
                con.setNextState(IdleState.INSTANCE);
                con.clearCurrentPacket();
            	con.getDataBuffer().clear();
                con.setNextNetworkState(ReadWaitingState.INSTANCE);
            });
        }
    }
}
