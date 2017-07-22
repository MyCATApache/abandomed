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
package io.mycat.util;

import io.netty.buffer.ByteBuf;

/**
 * @author mycat
 */
public class BufUtil {

    public static final void writeUB2(ByteBuf buffer, int i) {
    	buffer.writeByte((byte) (i & 0xff));
        buffer.writeByte((byte) (i >>> 8));
    }

    public static final void writeUB3(ByteBuf buffer, int i) {
        buffer.writeByte((byte) (i & 0xff));
        buffer.writeByte((byte) (i >>> 8));
        buffer.writeByte((byte) (i >>> 16));
    }

    public static final void writeInt(ByteBuf buffer, int i) {
        buffer.writeByte((byte) (i & 0xff));
        buffer.writeByte((byte) (i >>> 8));
        buffer.writeByte((byte) (i >>> 16));
        buffer.writeByte((byte) (i >>> 24));
    }

    public static final void writeFloat(ByteBuf buffer, float f) {
        writeInt(buffer, Float.floatToIntBits(f));
    }

    public static final void writeUB4(ByteBuf buffer, long l) {
        buffer.writeByte((byte) (l & 0xff));
        buffer.writeByte((byte) (l >>> 8));
        buffer.writeByte((byte) (l >>> 16));
        buffer.writeByte((byte) (l >>> 24));
    }

    public static final void writeLong(ByteBuf buffer, long l) {
        buffer.writeByte((byte) (l & 0xff));
        buffer.writeByte((byte) (l >>> 8));
        buffer.writeByte((byte) (l >>> 16));
        buffer.writeByte((byte) (l >>> 24));
        buffer.writeByte((byte) (l >>> 32));
        buffer.writeByte((byte) (l >>> 40));
        buffer.writeByte((byte) (l >>> 48));
        buffer.writeByte((byte) (l >>> 56));
    }

    public static final void writeDouble(ByteBuf buffer, double d) {
        writeLong(buffer, Double.doubleToLongBits(d));
    }

    public static final void writeLength(ByteBuf buffer, long l) {
        if (l < 251) {
            buffer.writeByte((byte) l);
        } else if (l < 0x10000L) {
            buffer.writeByte((byte) 252);
            writeUB2(buffer, (int) l);
        } else if (l < 0x1000000L) {
            buffer.writeByte((byte) 253);
            writeUB3(buffer, (int) l);
        } else {
            buffer.writeByte((byte) 254);
            writeLong(buffer, l);
        }
    }

    public static final void writeWithNull(ByteBuf buffer, byte[] src) {
        buffer.writeBytes(src);
        buffer.writeByte((byte) 0);
    }

    public static final void writeWithLength(ByteBuf buffer, byte[] src) {
        int length = src.length;
        if (length < 251) {
            buffer.writeByte((byte) length);
        } else if (length < 0x10000L) {
            buffer.writeByte((byte) 252);
            writeUB2(buffer, length);
        } else if (length < 0x1000000L) {
            buffer.writeByte((byte) 253);
            writeUB3(buffer, length);
        } else {
            buffer.writeByte((byte) 254);
            writeLong(buffer, length);
        }
        buffer.writeBytes(src);
    }

    public static final void writeWithLength(ByteBuf buffer, byte[] src, byte nullValue) {
        if (src == null) {
            buffer.writeByte(nullValue);
        } else {
            writeWithLength(buffer, src);
        }
    }

    public static final int getLength(long length) {
        if (length < 251) {
            return 1;
        } else if (length < 0x10000L) {
            return 3;
        } else if (length < 0x1000000L) {
            return 4;
        } else {
            return 9;
        }
    }

    public static final int getLength(byte[] src) {
        int length = src.length;
        if (length < 251) {
            return 1 + length;
        } else if (length < 0x10000L) {
            return 3 + length;
        } else if (length < 0x1000000L) {
            return 4 + length;
        } else {
            return 9 + length;
        }
    }

}