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
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * @author mycat
 */
public class MySQLMessage {
    public static final long NULL_LENGTH = -1;
    private static final byte[] EMPTY_BYTES = new byte[0];
    private final int length;
    private final ByteBuffer byteBuffer;

    public MySQLMessage(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        this.length = byteBuffer.capacity();
        
    }

    public int length() {
        return length;
    }

    public int position() {
        return byteBuffer.position();
    }

    // public byte[] bytes() {
    // return data;
    // }

    public void move(int i) {
    	byteBuffer.position(byteBuffer.position()+i);
    }

    public void position(int i) {
    	byteBuffer.position(i);
    }

    public boolean hasRemaining() {
        return  byteBuffer.hasRemaining();
    }

    public byte read(int i) {
        return byteBuffer.get(i);
    }

    public byte read() {
        return byteBuffer.get();
    }

    public int readUB2() {
        int i = read() & 0xff;
        i |= (read() & 0xff) << 8;
        return i;
    }

    public int readUB3() {
        int i = read() & 0xff;
        i |= (read() & 0xff) << 8;
        i |= (read() & 0xff) << 16;
        return i;
    }

    public long readUB4() {
        long l = (long) (read() & 0xff);
        l |= (long) (read() & 0xff) << 8;
        l |= (long) (read() & 0xff) << 16;
        l |= (long) (read() & 0xff) << 24;
        return l;
    }

    public int readInt() {
        int i = read() & 0xff;
        i |= (read() & 0xff) << 8;
        i |= (read() & 0xff) << 16;
        i |= (read() & 0xff) << 24;
        return i;
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public long readLong() {
        long l = (long) (read() & 0xff);
        l |= (long) (read() & 0xff) << 8;
        l |= (long) (read() & 0xff) << 16;
        l |= (long) (read() & 0xff) << 24;
        l |= (long) (read() & 0xff) << 32;
        l |= (long) (read() & 0xff) << 40;
        l |= (long) (read() & 0xff) << 48;
        l |= (long) (read() & 0xff) << 56;
        return l;
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public long readLength() {
        int length = read() & 0xff;
        switch (length) {
        case 251:
            return NULL_LENGTH;
        case 252:
            return readUB2();
        case 253:
            return readUB3();
        case 254:
            return readLong();
        default:
            return length;
        }
    }

   

    public byte[] readBytes() {
        int length = this.byteBuffer.remaining();
        if (length <= 0) {
        	 return EMPTY_BYTES;
        }
        return readBytes(length);
    }

    public byte[] readBytes(int length) {
    	byte[] ab = new byte[length];
        this.byteBuffer.get(ab);
        return ab;
    }

    public byte[] readBytesWithNull() {
    	 int length = this.byteBuffer.remaining();
         if (length <= 0) {
         	 return EMPTY_BYTES;
         }
        int offset = -1;
        int position=byteBuffer.position();
        int limit=byteBuffer.limit();
        for (int i = position; i < limit; i++) {
            if (read(i) == 0) {
                offset = i;
                break;
            }
        }
        switch (offset) {
        case -1:
            byte[] ab1 = new byte[limit - position];
            // System.arraycopy(b, position, ab1, 0, ab1.length);
            this.byteBuffer.get(ab1);
            return ab1;
        case 0:
        	byteBuffer.position(position+1);
            return EMPTY_BYTES;
        default:
            byte[] ab2 = new byte[offset - position];
            // System.arraycopy(b, position, ab2, 0, ab2.length);
            this.byteBuffer.get(ab2);
            //??position = offset + 1;
            byteBuffer.position(offset+1);
            return ab2;
        }
    }

    public byte[] readBytesWithLength() {
        int length = (int) readLength();
        if (length == NULL_LENGTH) {
            return null;
        }
        if (length <= 0) {
            return EMPTY_BYTES;
        }

        byte[] ab = new byte[length];
        this.byteBuffer.get(ab);
        return ab;
    }

    public String readString() throws IOException{
    	return readString("UTF-8");
    }

    public String readString(String charset) throws UnsupportedEncodingException {
    	int remains=byteBuffer.remaining();
        if (remains==0) {
            return null;
        }
        byte[] ab = new byte[remains];
        this.byteBuffer.get(ab);
        
        return new String(ab,charset);
    }

    public String readStringWithNull() throws IOException{
    	return readStringWithNull("UTF-8");
    	
    }

    public String readStringWithNull(String charset) throws UnsupportedEncodingException {
    	byte[] readed=readBytesWithNull();
    	return (EMPTY_BYTES==readed)?null:new String(readed,charset);
    }

    public String readStringWithLength() {
        try {
			return readStringWithLength("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
    }

    public String readStringWithLength(String charset) throws UnsupportedEncodingException {
    	int length = (int) readLength();
        if (length <= 0) {
            return null;
        }
        byte[] ab = new byte[length - this.position()];
        this.byteBuffer.get(ab);
        String s = new String(ab,charset);
        return s;
    }

    public java.sql.Time readTime() {
        move(6);
        int hour = read();
        int minute = read();
        int second = read();
        Calendar cal = getLocalCalendar();
        cal.set(0, 0, 0, hour, minute, second);
        return new Time(cal.getTimeInMillis());
    }

    public java.util.Date readDate() {
        byte length = read();
        int year = readUB2();
        byte month = read();
        byte date = read();
        int hour = read();
        int minute = read();
        int second = read();
        if (length == 11) {
            long nanos = readUB4();
            Calendar cal = getLocalCalendar();
            cal.set(year, --month, date, hour, minute, second);
            Timestamp time = new Timestamp(cal.getTimeInMillis());
            time.setNanos((int) nanos);
            return time;
        } else {
            Calendar cal = getLocalCalendar();
            cal.set(year, --month, date, hour, minute, second);
            return new java.sql.Date(cal.getTimeInMillis());
        }
    }

    public BigDecimal readBigDecimal() {
        String src = readStringWithLength();
        return src == null ? null : new BigDecimal(src);
    }

    public String toString() {
        return byteBuffer.toString();
    }

    private static final ThreadLocal<Calendar> localCalendar = new ThreadLocal<Calendar>();

    private static final Calendar getLocalCalendar() {
        Calendar cal = localCalendar.get();
        if (cal == null) {
            cal = Calendar.getInstance();
            localCalendar.set(cal);
        }
        return cal;
    }

}