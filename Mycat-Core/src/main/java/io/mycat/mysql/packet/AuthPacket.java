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
package io.mycat.mysql.packet;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import io.mycat.buffer.MycatByteBuffer;
import io.mycat.mysql.Capabilities;
import io.mycat.net2.ConDataBuffer;
import io.mycat.util.BufferUtil;
import io.mycat.util.StreamUtil;

/**
 * From client to server during initial handshake.
 * 
 * <pre>
 * Bytes                        Name
 * -----                        ----
 * 4                            client_flags
 * 4                            max_packet_size
 * 1                            charset_number
 * 23                           (filler) always 0x00...
 * n (Null-Terminated String)   user
 * n (Length Coded Binary)      scramble_buff (1 + x bytes)
 * n (Null-Terminated String)   databasename (optional)
 * 
 * &#64;see http://forge.mysql.com/wiki/MySQL_Internals_ClientServer_Protocol#Client_Authentication_Packet
 * </pre>
 * 
 * @author mycat
 */
public class AuthPacket extends MySQLPacket {
    private static final byte[] FILLER = new byte[23];

    public long clientFlags;
    public long maxPacketSize;
    public int charsetIndex;
    public byte[] extra;// from FILLER(23)
    public String user;
    public byte[] password;
    public String database;

    public void read(MycatByteBuffer byteBuffer) throws IOException {
        packetLength = (int) byteBuffer.readFixInt(3);
        packetId = byteBuffer.readByte();
        clientFlags = byteBuffer.readFixInt(4);
        maxPacketSize =byteBuffer.readFixInt(4);
        charsetIndex = byteBuffer.readByte();
        // read extra
        int current = byteBuffer.readIndex();
//        int len = (int) mm.readLength();
//        if (len > 0 && len < FILLER.length) {
//            byte[] ab = new byte[len];
//            System.arraycopy(mm.bytes(), mm.position(), ab, 0, len);
//            this.extra = ab;
//        }
//        mm.position(current + FILLER.length);
//        user = mm.readStringWithNull();
//        password = mm.readBytesWithLength();
//        if (((clientFlags & Capabilities.CLIENT_CONNECT_WITH_DB) != 0) && mm.hasRemaining()) {
//            database = mm.readStringWithNull();
//        }
    }

  
    public void write(ByteBuffer  buffer,int pkgSize) {
        BufferUtil.writeUB3(buffer, pkgSize);
        buffer.put(packetId);
        BufferUtil.writeUB4(buffer, clientFlags);
        BufferUtil.writeUB4(buffer, maxPacketSize);
        buffer.put((byte) charsetIndex);
        buffer.put(FILLER);
        if (user == null) {
            buffer.put((byte) 0);
        } else {
            byte[] userData = user.getBytes();
            BufferUtil.writeWithNull(buffer, userData);
        }
        if (password == null) {
            buffer.put((byte) 0);
        } else {
            BufferUtil.writeWithLength(buffer, password);
        }
        if (database == null) {
            buffer.put((byte) 0);
        } else {
            byte[] databaseData = database.getBytes();
            BufferUtil.writeWithNull(buffer, databaseData);
        }

    }

    @Override
    public int calcPacketSize() {
        int size = 32;//4+4+1+23;
        size += (user == null) ? 1 : user.length() + 1;
        size += (password == null) ? 1 : BufferUtil.getLength(password);
        size += (database == null) ? 1 : database.length() + 1;
        return size;
    }

    @Override
    protected String getPacketInfo() {
        return "MySQL Authentication Packet";
    }

}
