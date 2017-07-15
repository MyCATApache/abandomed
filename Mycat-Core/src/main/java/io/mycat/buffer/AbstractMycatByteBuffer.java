package io.mycat.buffer;


/**
 * Created by ynfeng on 2017/7/5.
 */
public abstract class AbstractMycatByteBuffer implements MycatByteBuffer {
    private int writeIndex;
    private int readIndex;

    public AbstractMycatByteBuffer() {
        this.writeIndex = 0;
        this.readIndex = 0;
    }

    /**
     * 从指定位置读取整型，符合mysql协议中的整型数据类型
     *
     * @param index  位置
     * @param length 长度
     * @return 值
     */
    abstract long getInt(int index, int length);

    /**
     * 从指定位置读取bytes
     *
     * @param index  位置
     * @param length 长度
     * @return 字节数组
     */
    abstract byte[] getBytes(int index, int length);

    /**
     * 从指定位置读取byte
     *
     * @param index 位置
     * @return 字节
     */
    abstract byte getByte(int index);

    /**
     * 从指定位置开始写入字节数组
     *
     * @param index 位置
     * @param bytes 字节数组
     */
    abstract void putBytes(int index, byte[] bytes);

    /**
     * 向指定位置写入字节
     *
     * @param index
     * @param val
     */
    abstract void putByte(int index, byte val);

    /**
     * 获取lenenc占用的字节长度
     *
     * @param lenenc 值
     * @return 长度
     */
    private int getLenencBytes(int lenenc) {
        if (lenenc < 251) {
            return 1;
        } else if (lenenc >= 251 && lenenc < (1 << 16)) {
            return 3;
        } else if (lenenc >= (1 << 16) && lenenc < (1 << 24)) {
            return 4;
        } else {
            return 9;
        }
    }

    @Override
    public boolean hasRemaining() {
        return readIndex < writeIndex;
    }

    @Override
    public int remaining() {
        return writeIndex - readIndex;
    }

    @Override
    public int freeBytes() {
        return capacity() - writeIndex;
    }

    @Override
    public int writeIndex() {
        return writeIndex;
    }

    @Override
    public int readIndex() {
        return readIndex;
    }

    @Override
    public void writeIndex(int writeIndex) {
        this.writeIndex = writeIndex;
    }

    @Override
    public void readIndex(int readIndex) {
        this.readIndex = readIndex;
    }

    @Override
    public long getFixInt(int index, int length) {
        return getInt(index, length);
    }

    @Override
    public long readFixInt(int length) {
        long val = getInt(readIndex, length);
        this.readIndex += length;
        return val;
    }

    @Override
    public long getLenencInt(int index) {
        long len = getInt(index, 1) & 0xff;
        if (len == 0xfc) {
            return getInt(index + 1, 2);
        } else if (len == 0xfd) {
            return getInt(index + 1, 3);
        } else if (len == 0xfe) {
            return getInt(index + 1, 8);
        } else {
            return getInt(index, 1);
        }
    }

    @Override
    public long readLenencInt() {
        int index = readIndex;
        long len = getInt(index, 1) & 0xff;
        if (len < 251) {
            this.readIndex += 1;
            return getInt(index, 1);
        } else if (len == 0xfc) {
            this.readIndex += 2;
            return getInt(index + 1, 2);
        } else if (len == 0xfd) {
            this.readIndex += 3;
            return getInt(index + 1, 3);
        } else {
            this.readIndex += 8;
            return getInt(index + 1, 8);
        }
    }

    @Override
    public String getFixString(int index, int length) {
        byte[] bytes = getBytes(index, length);
        return new String(bytes);
    }

    @Override
    public String readFixString(int length) {
        byte[] bytes = getBytes(readIndex, length);
        readIndex += length;
        return new String(bytes);
    }

    @Override
    public String getLenencString(int index) {
        int strLen = (int) getLenencInt(index);
        int lenencLen = getLenencBytes(strLen);
        byte[] bytes = getBytes(index + lenencLen, strLen);
        return new String(bytes);
    }

    @Override
    public String readLenencString() {
        int strLen = (int) getLenencInt(readIndex);
        int lenencLen = getLenencBytes(strLen);
        byte[] bytes = getBytes(readIndex + lenencLen, strLen);
        this.readIndex += strLen + lenencLen;
        return new String(bytes);
    }

    @Override
    public String getVarString(int index, int length) {
        return getFixString(index, length);
    }

    @Override
    public String readVarString(int length) {
        return readFixString(length);
    }

    @Override
    public String getNULString(int index) {
        int strLength = 0;
        int scanIndex = index;
        while (scanIndex < capacity()) {
            if (getByte(scanIndex++) == 0) {
                break;
            }
            strLength++;
        }
        byte[] bytes = getBytes(index, strLength - 1);
        return new String(bytes);
    }

    @Override
    public String readNULString() {
        String rv = getNULString(readIndex);
        readIndex += rv.getBytes().length + 1;
        return rv;
    }

    @Override
    public MycatByteBuffer putFixInt(int index, int length, long val) {
        int index0 = index;
        for (int i = 0; i < length; i++) {
            byte b = (byte) ((val >> (i * 8)) & 0xFF);
            putByte(index0++, b);
        }
        return this;
    }

    @Override
    public MycatByteBuffer writeFixInt(int length, long val) {
        putFixInt(writeIndex, length, val);
        this.writeIndex += length;
        return this;
    }

    @Override
    public MycatByteBuffer putLenencInt(int index, long val) {
        if (val < 251) {
            putByte(index, (byte) val);
        } else if (val >= 251 && val < (1 << 16)) {
            putByte(index, (byte) 0xfc);
            putFixInt(index + 1, 2, val);
        } else if (val >= (1 << 16) && val < (1 << 24)) {
            putByte(index, (byte) 0xfd);
            putFixInt(index + 1, 3, val);
        } else {
            putByte(index, (byte) 0xfe);
            putFixInt(index + 1, 8, val);
        }
        return this;
    }

    @Override
    public MycatByteBuffer writeLenencInt(long val) {
        if (val < 251) {
            putByte(writeIndex++, (byte) val);
        } else if (val >= 251 && val < (1 << 16)) {
            putByte(writeIndex++, (byte) 0xfc);
            putFixInt(writeIndex, 2, val);
            writeIndex += 2;
        } else if (val >= (1 << 16) && val < (1 << 24)) {
            putByte(writeIndex++, (byte) 0xfd);
            putFixInt(writeIndex, 3, val);
            writeIndex += 3;
        } else {
            putByte(writeIndex++, (byte) 0xfe);
            putFixInt(writeIndex, 8, val);
            writeIndex += 8;
        }
        return this;
    }

    @Override
    public MycatByteBuffer putFixString(int index, String val) {
        putBytes(index, val.getBytes());
        return this;
    }

    @Override
    public MycatByteBuffer writeFixString(String val) {
        putBytes(writeIndex, val.getBytes());
        writeIndex += val.getBytes().length;
        return this;
    }

    @Override
    public MycatByteBuffer putLenencString(int index, String val) {
        this.putLenencInt(index, val.getBytes().length);
        int lenencLen = getLenencBytes(val.getBytes().length);
        this.putFixString(index + lenencLen, val);
        return this;
    }

    @Override
    public MycatByteBuffer writeLenencString(String val) {
        putLenencString(writeIndex, val);
        int lenencLen = getLenencBytes(val.getBytes().length);
        this.writeIndex += lenencLen + val.getBytes().length;
        return this;
    }

    @Override
    public MycatByteBuffer putVarString(int index, String val) {
        putFixString(index, val);
        return this;
    }

    @Override
    public MycatByteBuffer writeVarString(String val) {
        return writeFixString(val);
    }

    @Override
    public MycatByteBuffer putNULString(int index, String val) {
        putFixString(index, val);
        putByte(index + 1 + val.getBytes().length, (byte) 0);
        return this;
    }

    @Override
    public MycatByteBuffer writeNULString(String val) {
        putNULString(writeIndex, val);
        writeIndex += val.getBytes().length + 1;
        return this;
    }
}
