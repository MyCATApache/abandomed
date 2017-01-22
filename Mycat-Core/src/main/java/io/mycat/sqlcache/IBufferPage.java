package io.mycat.sqlcache;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Cache Page接口
 *
 * @author zagnix
 * @version 1.0
 * @create 2016-12-27 18:49
 */

public interface IBufferPage {

    /**
     *  get bytes from Thread Local
     *
     * @param position
     * @param length
     * @return
     */
    public byte[] getBytes(int position, int length);

    /**
     *  ByteBuffer slice from Thread Local
     *
     * @param position
     * @param limit
     * @return
     */
    public ByteBuffer slice(int position, int limit);

    /**
     * get ByteBuffer from Thread Local
     *
     * @param position
     * @return
     */
    public ByteBuffer getLocalByteBuffer(int position);


    /**
     * 将数据刷到磁盘
     */
    public void flush();


    /**
     * 页号
     *
     * @return
     */
    long getPageIndex();


    /**
     * 设置页有数据被写入了，属于’脏页‘
     *
     * @param dirty
     */
    void setDirty(boolean dirty);


    /**
     * 页回收
     */
    public void recycle() throws IOException;

    /**
     * 页是否被回收了
     *
     * @return
     */
    boolean isRecycled();
}
