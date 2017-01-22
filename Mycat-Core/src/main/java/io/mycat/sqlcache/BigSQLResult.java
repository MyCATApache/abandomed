package io.mycat.sqlcache;


import io.mycat.sqlcache.impl.mmap.MappedSQLResult;

import java.io.IOException;
import java.util.Iterator;

import static com.google.common.hash.Hashing.murmur3_32;

/**
 * SQL大结果集缓存
 *
 * @author zagnix
 * @version 1.0
 * @create 2016-12-27 18:48
 */

public class BigSQLResult implements Iterator {

    private ISQLResult sqlResult;
    private String cacheDir;

    public BigSQLResult(LocatePolicy locatePolicy, String sql, int pageSize){
        /**
         * Core - DirectMemory
         * Normal - mmap
         */
        this.cacheDir = "sqlcache/";
        String sqlkey = ""+murmur3_32().hashUnencodedChars(sql);

        if (locatePolicy.equals(LocatePolicy.Normal)){
            try {
                sqlResult = new MappedSQLResult(cacheDir,sqlkey,pageSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if(locatePolicy.equals(LocatePolicy.Core)){

        }
    }

    /**
     * 添加一条sql二进制数据到Cache存储中
     * @param data
     */
    public void put(byte [] data){
        try {
            sqlResult.put(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 数据是否为空
     * @return
     */
    public boolean isEmpty(){
        return sqlResult.isEmpty();
    }

    /**
     * 还有下一条sql结果集？
     * @return
     */
    public boolean hasNext() {
        return !sqlResult.isEmpty();
    }

    /**
     * 取下一条sql结果集
     *
     * @return
     */
    public byte[] next()  {
        try {
            return sqlResult.next();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Iterator interface
     */
    public void remove() {
        //TODO
    }

    /**
     * 主动将内存映射文件的数据刷到磁盘中
     */
    public void flush(){
        sqlResult.flush();
    }

    /**
     * sql 结果集大小
     * @return
     */
    public long size(){
        return sqlResult.size();
    }

    /**
     * 从 PageLRUCache中移除Page，并执行unmap操作，并删除对于文件
     */
    public void removeAll(){
        try {
            sqlResult.removeAll();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从 PageLRUCache中移除Page，并执行unmap操作，但不删除文件
     * 下次运行时候可以读取文件内容
     */
    public void recycle(){
        try {
            sqlResult.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 复位读位置为0，从头开始读取sql结果集
     */
    public void reset(){
        sqlResult.reset();
    }
}
