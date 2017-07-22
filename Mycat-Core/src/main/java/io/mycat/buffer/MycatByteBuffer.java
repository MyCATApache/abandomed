package io.mycat.buffer;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Mycat网络通信专用的buffer,封装了Mysql协议中的数据类型的读写
 * 非线程安全的类
 * <p>
 * writeIndex表示写位置
 * readIndex表示读位置
 * capacity表示总容量
 * <p>
 * Created by ynfeng on 2017/7/4.
 */
public interface MycatByteBuffer {

    /**
     * 将readIndex和writeIndex之间的数据写出
     * 影响readIndex
     *
     * @param socketChannel 目标channel
     * @return 写出的字节数
     */
    int transferToChannel(SocketChannel socketChannel) throws IOException;

    /**
     * 从channel中读取数据
     * 影响writeIndex
     *
     * @param socketChannel 源channel
     * @return 读出的字节数
     */
    int transferFromChannel(SocketChannel socketChannel) throws IOException;

    /**
     * 在position位置做标记
     *
     * @return
     */
    void mark(int position);

    /**
     * 将readIndex复位至mark标记过的位置
     */
    void reset();

    /**
     * 将readIndex向后跳step个位置
     *
     * @param step 跳多长
     */
    void skip(int step);

    /**
     * 压缩空间
     *
     * @return 本身的实例
     */
    MycatByteBuffer compact();

    /**
     * 清空
     */
    void clear();

    /**
     * 是否还有未读的数据
     *
     * @return true表示有，false表示没有
     */
    boolean hasReadableBytes();

    /**
     * 获取未读的数据长度
     *
     * @return 未读长度
     */
    int readableBytes();

    /**
     * 获取可写的数据长度
     *
     * @return 可写长度
     */
    int writableBytes();

    /**
     * 获取容量
     *
     * @return 容量
     */
    int capacity();

    /**
     * 获取当前的写位置
     *
     * @return 当前的写位置
     */
    int writeIndex();
    
    
    /**
     * 获取当前写出指针位置
     * @return
     */
    int writeLimit();
    
    /**
     * 设置当前写指针位置
     * @param currentWriteIndex
     */
    void writeLimit(int writeLimit);

    /**
     * 获取当前的读位置
     *
     * @return 当前的读位置
     */
    int readIndex();

    /**
     * 设置读位置
     *
     * @param writeIndex 读位置
     */
    void writeIndex(int writeIndex);

    /**
     * 设置写位置
     *
     * @param readIndex 写位置
     */
    void readIndex(int readIndex);

    /**
     * 读取Protocol::FixedLengthInteger,详见Mysql协议
     * 不影响读写位置
     *
     * @param index  起始位置
     * @param length 长度
     * @return 值
     */
    long getFixInt(int index, int length);

    /**
     * 从当前的readIndex开始，读取 1 byte Protocol::FixedLengthInteger，详见mysql协议
     * 影响readIndex位置
     *
     * @param length 长度
     * @return 值
     */
    long readFixInt(int length);

    /**
     * 读取Protocol::LengthEncodedInteger，详见mysql协议
     * 不影响读写位置
     *
     * @param index 起始位置
     * @return 值
     */
    long getLenencInt(int index);

    /**
     * 从当前readIndex读取Protocol::LengthEncodedInteger，详见mysql协议
     * 影响readIndex
     *
     * @return 值
     */
    long readLenencInt();

    /**
     * 读取Protocol::FixedLengthString，详见mysql协议
     * 不影响读写位置
     *
     * @param index  起始位置
     * @param length 长度
     * @return 值
     */
    String getFixString(int index, int length);

    /**
     * 从当前readIndex读取Protocol::LengthEncodedInteger,详见mysql协议
     * 影响readIdex
     *
     * @param length 长度
     * @return 值
     */
    String readFixString(int length);

    /**
     * 读取Protocol::LengthEncodedString,详见mysql协议
     * 不影响读写位置
     *
     * @param index 起始位置
     * @return 值
     */
    String getLenencString(int index);

    /**
     * 从当前readIndex读取Protocol::LengthEncodedString,详见mysql协议
     * 影响readIndex
     *
     * @return 值
     */
    String readLenencString();

    /**
     * 读取Protocol::VariableLengthString ，详见mysql协议
     * 不影响读写位置
     *
     * @param index  起始位置
     * @param length 长度
     * @return 值
     */
    String getVarString(int index, int length);

    /**
     * 从当前readIndex读取Protocol::VariableLengthString,详见mysql协议
     * 影响readIndex
     *
     * @param length 长度
     * @return 值
     */
    String readVarString(int length);

    /**
     * 读取Protocol::NulTerminatedString,详见mysql协议
     * 不影响读写指针
     *
     * @param index 开始位置
     * @return 值
     */
    String getNULString(int index);

    /**
     * 从当前readIndex读取Protocol::NulTerminatedString,详见mysql协议
     * 影响readIndex
     *
     * @return 值
     */
    String readNULString();

    /**
     * 在index位置放入length长度的Protocol::FixedLengthInteger，详见mysql协议
     * 不影响读写指针
     *
     * @param index  位置
     * @param length 长度
     * @param val    值
     * @return 本身的实例
     */
    MycatByteBuffer putFixInt(int index, int length, long val);

    /**
     * 从writeIndex开始，写入length长度的Protocol::FixedLengthInteger，详见mysql协议
     * 影响writeIndex
     *
     * @param length 长度
     * @param val    值
     * @return 本身的实例
     */
    MycatByteBuffer writeFixInt(int length, long val);

    /**
     * 在index位置放入Protocol::LengthEncodedInteger,详见msyql协议
     * 不影响读写指针
     *
     * @param index 位置
     * @param val   值
     * @return 本身的实例
     */
    MycatByteBuffer putLenencInt(int index, long val);

    /**
     * 从writeIndex开始，写入Protocol::LengthEncodedInteger,详见mysql协议
     * 影响writeIndex
     *
     * @param val 值
     * @return 本身的实例
     */
    MycatByteBuffer writeLenencInt(long val);

    /**
     * 在index位置放入Protocol::FixedLengthString,详见mysql协议
     * 不影响读写指针
     *
     * @param index 位置
     * @param val   值
     * @return 本身的实例
     */
    MycatByteBuffer putFixString(int index, String val);

    /**
     * 从writeIndex开始，写入Protocol::FixedLengthString,详见mysql协议
     * 影响writeIndex
     *
     * @param val 值
     * @return 本身的实例
     */
    MycatByteBuffer writeFixString(String val);

    /**
     * 在指定位置放入Protocol::LengthEncodedString,详见mysql协议
     * 不影响读写指针
     *
     * @param index 位置
     * @param val   值
     * @return 本身的实例
     */
    MycatByteBuffer putLenencString(int index, String val);

    /**
     * 在指定位置写入Protocol::LengthEncodedString,详见mysql协议
     * 影响writeIndex
     *
     * @param val 值
     * @return 本身的实例
     */
    MycatByteBuffer writeLenencString(String val);

    /**
     * 在index位置放入Protocol::VariableLengthString，详见mysql协议
     * 不影响读写指针
     *
     * @param index 位置
     * @param val   值
     * @return 本身的实例
     */
    MycatByteBuffer putVarString(int index, String val);

    /**
     * 从writeIndex开始，写入VariableLengthString,详见mysql协议
     * 影响writeIndex
     *
     * @param val 值
     * @return 本身的实例
     */
    MycatByteBuffer writeVarString(String val);

    /**
     * 在index位置放入Protocol::NulTerminatedString，详见mysql协议
     * 不影响读写指针
     *
     * @param index 位置
     * @param val   值
     * @return 本身的实例
     */
    MycatByteBuffer putNULString(int index, String val);

    /**
     * 从writeIndex开始，写入Protocol::NulTerminatedString,详见mysql协议
     * 影响writeIndex
     *
     * @param val 值
     * @return 本身的实例
     */
    MycatByteBuffer writeNULString(String val);

    /**
     * 从指定位置获取bytes
     * 不影响读写指针
     *
     * @param index  位置
     * @param length 长度
     * @return 本身的实例
     */
    byte[] getBytes(int index, int length);

    /**
     * 从指定位置读取bytes
     * 影响readIndex
     *
     * @param length 长度
     * @return 本身的实例
     */
    byte[] readBytes(int length);

    /**
     * 从指定位置获取byte
     * 不影响读写指针
     *
     * @param index 位置
     * @return 值
     */
    byte getByte(int index);

    /**
     * 从readIndex读取byte
     * 影响readIndex
     *
     * @return 值
     */
    byte readByte();

    /**
     * 从指定位置获取lenenc形式的byte数组
     *
     * @param index 位置
     * @return byte数组
     */
    byte[] getLenencBytes(int index);

    /**
     * 从readIndex位置开始读取Lenenc形式的byte数组
     *
     * @return byte数组
     */
    byte[] readLenencBytes();

    /**
     * 在指定位置写入bytes
     * 不影响读写指针
     *
     * @param index 位置
     * @param bytes 字节数组
     * @return 本身的实例
     */
    MycatByteBuffer putBytes(int index, byte[] bytes);

    /**
     * 在指定位置写入length长度的bytes
     * 不影响读写指针
     *
     * @param index  位置
     * @param length 长度
     * @param bytes  字节数组
     * @return 本身的实例
     */
    MycatByteBuffer putBytes(int index, int length, byte[] bytes);

    /**
     * 在指定位置放入lenenc形式的byte数组
     *
     * @param index 位置
     * @param bytes byte数组
     * @return 本身的实例
     */
    MycatByteBuffer putLenencBytes(int index,byte[] bytes);

    /**
     * 在指定的位置放入byte,
     * 不影响读写指针
     *
     * @param index 位置
     * @param val   值
     * @return 本身的实例
     */
    MycatByteBuffer putByte(int index, byte val);

    /**
     * 从writeIndex位置写入lenenc形式的byte数组
     *
     * @param bytes
     * @return
     */
    MycatByteBuffer writeLenencBytes(byte[] bytes);

    /**
     * 在writeIndex位置定入byte,
     * 影响writeIndex
     *
     * @param val 值
     * @return 本身的实例
     */
    MycatByteBuffer writeByte(byte val);

    /**
     * 从writeIndex位置开始写入bytes
     * 影响writeIndex
     *
     * @param bytes 字节数组
     * @return 本身的实例
     */
    MycatByteBuffer writeBytes(byte[] bytes);

    /**
     * 从writeIndex位置开始写入length长度的bytes
     * 影响writeIndex
     *
     * @param length 长度
     * @param bytes  字节数组
     * @return 本身的实例
     */
    MycatByteBuffer writeBytes(int length, byte[] bytes);
}
