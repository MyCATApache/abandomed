package io.mycat.sqlcache.impl.mmap;


import io.mycat.sqlcache.MyCatBufferPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * 内存映射文件实现data存储
 *
 * @author zagnix
 * @version 1.0
 * @create 2016-12-27 18:52
 */

public class MappedMemFileBufferPage extends MyCatBufferPage {
    private final static Logger logger =
            LoggerFactory.getLogger(MappedMemFileBufferPage.class);
    private String pageName = null;
    private long pageIndex = 0L;
    private static Method mappedByteBufferCleaner = null;
    private static Method mappedByteBufferCleanerClean = null;

    static {
        try {
            mappedByteBufferCleaner =
                    Class.forName("java.nio.DirectByteBuffer").getMethod("cleaner");
            mappedByteBufferCleaner.setAccessible(true);
            mappedByteBufferCleanerClean =
                    Class.forName("sun.misc.Cleaner").getMethod("clean");
            mappedByteBufferCleanerClean.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    /**
     * 创建 一个MappedByteBufferPage对象
     *
     * @param mappedByteBuffer
     * @param file
     * @param index
     * @param cacheTTL
     */
    public MappedMemFileBufferPage(MappedByteBuffer mappedByteBuffer, String file, long index, long cacheTTL){
        super(mappedByteBuffer.load(),cacheTTL);
        this.pageName = file;
        this.pageIndex = index;
    }


    /**
     *
     * @param position
     * @param length
     * @return
     */
    public byte[] getBytes(int position, int length) {
        ByteBuffer buf = this.getLocalByteBuffer(position);
        byte[] data = new byte[length];
        buf.get(data);
        return data;
    }

    /**
     * ByteBuffer  slice from Thread Local
     * @param position
     * @param limit
     * @return
     */
    public ByteBuffer slice(int position, int limit) {
        ByteBuffer buffer = this.threadLocalByteBuffer.get();
        buffer.limit(position+limit);
        buffer.position(position);
        return buffer.slice();
    }

    /**
     * ByteBuffer from Thread Local
     *
     * @param position
     * @return
     */
    public ByteBuffer getLocalByteBuffer(int position) {
        ByteBuffer buf = this.threadLocalByteBuffer.get();
        buf.position(position);
        return buf;
    }


    /**
     * 将pageName的数据刷入磁盘
     */
    public void flush() {
        synchronized(this) {
            if (this.recycled) return;
            if (dirty) {
                MappedByteBuffer mappedByteBuffer =
                        (MappedByteBuffer)threadLocalByteBuffer.getByteBuffer();
                mappedByteBuffer.force();
                dirty = false;
            }
        }

    }

    /**
     * 返回 页 号
     * @return
     */
    public long getPageIndex() {
        return this.pageIndex;
    }

    /**
     * 设置 页 为  dirty
     * @param dirty
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * 回收页，关闭文件pageName
     * @throws IOException
     */
    public void recycle() throws IOException{

        synchronized(this) {
            if (this.recycled) return;
            flush();
            MappedByteBuffer srcBuf =
                    (MappedByteBuffer)threadLocalByteBuffer.getByteBuffer();
            unmap(srcBuf);
            this.threadLocalByteBuffer = null;
            this.recycled = true;
        }

    }

    /**
     * 解文件内存映射
     *
     * @param buffer
     */
    private void unmap(final MappedByteBuffer buffer) {
        if(buffer == null)
            return;

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                try {
                    if (mappedByteBufferCleaner != null &&
                            mappedByteBufferCleanerClean != null &&
                            buffer.isDirect()) {
                        Object cleaner = mappedByteBufferCleaner.invoke(buffer);
                        mappedByteBufferCleanerClean.invoke(cleaner);
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
                return null;
            }
        });
    }


    public boolean isRecycled() {
        return false;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public void setPageIndex(long pageIndex) {
        this.pageIndex = pageIndex;
    }
}
