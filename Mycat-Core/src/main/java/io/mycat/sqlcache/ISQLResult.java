package io.mycat.sqlcache;

import java.io.IOException;

/**
 * SQLResult 存放 实现接口
 * @author zagnix
 * @create 2016-11-18 16:46
 */
public interface ISQLResult {

    /**
     * 该将sql row的二进制数据，存放到Cache中
     * @param data
     * @return
     * @throws IOException
     */
    public long put(byte[] data) throws IOException;

    /**
     * 获取一条sql row的二进制数据
     * @return
     * @throws IOException
     */
    public byte[] next() throws IOException;

    /**
     * 根据index得到一条SQL row的二进制数据
     * @param index
     * @return
     * @throws IOException
     */
    public byte[] get(long index) throws IOException;


    /**
     * SQL结果集条数
     * @return
     */
    public long size();

    /**
     * 是否没有缓存SQL结果集
     * @return
     */
    public boolean isEmpty() ;

    /**
     * Cache是否已经满了
     * @return
     */
    public boolean isFull();

    /**
     * 将SQL结果集刷入磁盘
     */
    public void flush();

    /**
     * 删除数据页和索引页文件
     * @throws IOException
     */
    public void removeAll() throws IOException;

    /**
     * 将所有内存映射文件unmap
     * @throws IOException
     */
    public void recycle() throws IOException;


    /**
     * 复位读的位置0，即从头开始读取SQL结果集
     */
    public void reset();
}
