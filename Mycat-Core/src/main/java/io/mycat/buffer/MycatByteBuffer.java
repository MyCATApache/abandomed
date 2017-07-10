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
     * 清空
     */
    void clear();

    /**
     * 是否还有未读的数据
     *
     * @return true表示有，false表示没有
     */
    boolean hasRemaining();

    /**
     * 获取未读的数据长度
     *
     * @return 未读长度
     */
    int remaining();

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
     * @param index 起始位置
     * @return 值
     */
    long readLenencInt(int index);

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
     * @param val   值
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
}
