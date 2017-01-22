package io.mycat.sqlcache.impl.mmap;



import io.mycat.sqlcache.IBufferPageFactory;
import io.mycat.sqlcache.MyCatBufferPage;
import io.mycat.sqlcache.PageLRUCache;
import io.mycat.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.nio.channels.FileChannel.MapMode.READ_WRITE;

/**
 *  Memroy Mapped Page Factory
 *
 * @author zagnix
 * @create 2016-11-18 16:46
 */

public class MappedBufferPageFactory implements IBufferPageFactory {
    private final static Logger logger =
            LoggerFactory.getLogger(MappedBufferPageFactory.class);
    private int pageSize;
    private String pageDirName;
    private File pageDir;
    private String page;


    private final Map<Long, ReentrantReadWriteLock> pageLockMap =
            new ConcurrentHashMap<Long,ReentrantReadWriteLock>();

    public static final String PAGE_FILE_NAME = "page";
    public static final String PAGE_FILE_SUFFIX = ".page";

    private final PageLRUCache pageLRUCache;
    private long cacheTTL = 0L;


    public MappedBufferPageFactory(int pageFileSize, String pageDir, long cacheTTL){

        /**
         * 页文件大小
         */
        this.pageSize = pageFileSize;
        /**
         * 页文件目录名
         */
        this.pageDirName = pageDir;
        /**
         * 页文件目录
         */
        this.pageDir = new File(this.pageDirName);
        this.cacheTTL = cacheTTL;


        if (!this.pageDir.exists()) {
            this.pageDir.mkdirs();
        }

        if (!this.pageDirName.endsWith(File.separator)) {
            this.pageDirName += File.separator;
        }
        /**
         * 页文件名前缀
         */
        this.page = this.pageDirName + PAGE_FILE_NAME + "-";
        this.pageLRUCache = new PageLRUCache();

    }

    /**
     * 查询page
     * @param index
     * @return
     * @throws IOException
     */
    public MyCatBufferPage acquirePage(long index) throws IOException {
        /**
         * 首先从Cache中查找对应的页
         */
        MyCatBufferPage myCatBufferPage = pageLRUCache.get(index);

        if (myCatBufferPage == null) {
            try {
                if (!pageLockMap.containsKey(index)) {
                    pageLockMap.put(index,new ReentrantReadWriteLock());
                }
                Lock writeLock = pageLockMap.get(index).writeLock();
                /**
                 * 加锁处理当前index
                 */
                try {
                    writeLock.lock();
                    /**
                     * 可能其他线程已经加入到了cache中了。
                     */
                    myCatBufferPage = pageLRUCache.get(index);

                    if (myCatBufferPage == null) {
                        /**
                         * 创建一个页，利用Java MappedByteBuffer 高性能内存映射
                         */
                        RandomAccessFile raf = null;
                        FileChannel channel = null;
                        try {
                            String pageName = this.getPageNameByIndex(index);
                            raf = new RandomAccessFile(pageName, "rw");
                            channel = raf.getChannel();

                            /**
                             * 文件映射 转换成 MappedByteBuffer
                             */
                            MappedByteBuffer mbb = channel.map(READ_WRITE,0,this.pageSize);

                            /**
                             * 转换成逻辑的映射页
                             */
                            myCatBufferPage = new MappedMemFileBufferPage(mbb, pageName, index,cacheTTL);

                            /**
                             * 缓存起来。。。lRU Cache.......
                             */
                            pageLRUCache.put(index, myCatBufferPage);

                            if (logger.isDebugEnabled()) {
                                logger.debug("Mapped page for " + pageName + " was just created and cached.");
                            }

                        } finally {
                            if (channel != null) channel.close();
                            if (raf != null) raf.close();
                        }
                    }
                }finally {
                    writeLock.unlock();
                }
            } finally {
                pageLockMap.remove(index);
            }
        }
        return myCatBufferPage;
    }

    /**
     * 根据索引生成页文件
     * @param index
     * @return
     */
    private String getPageNameByIndex(long index) {
        return this.page + index + PAGE_FILE_SUFFIX;
    }

    /**
     * 回收Page
     * @param index
     */
    public void releasePage(long index) {
        pageLRUCache.release(index);
    }

    /**
     * 通过page文件名,返回其page索引
     * @param pageName
     * @return
     */
    private long getIndexByPageName(String pageName) {
        int beginIndex = pageName.lastIndexOf('-');
        beginIndex += 1;
        int endIndex = pageName.lastIndexOf(PAGE_FILE_SUFFIX);
        String sIndex = pageName.substring(beginIndex, endIndex);
        long index = Long.parseLong(sIndex);
        return index;
    }


    /**
     * 根据索引删除页文件，调用者需要同步访问
     * @param index
     * @throws IOException
     */
    public void deletePage(long index) throws IOException {
        pageLRUCache.remove(index);
        String pageName = this.getPageNameByIndex(index);
        int count = 0;
        int maxRound = 10;
        boolean deleted = false;
        while(count < maxRound) {
            try {
                Utils.deleteFile(new File(pageName));
                deleted = true;
                break;
            } catch (IllegalStateException ex) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }
                count++;
                if (logger.isDebugEnabled()) {
                    logger.warn("fail to delete  " + pageName + ", tried round = " + count);
                }
            }
        }
        if (deleted) {
            if(logger.isDebugEnabled()) {
                logger.debug("Page " + pageName + " was just deleted.");
            }
        } else {
            logger.warn("fail to delete  " + pageName + " after max " + maxRound + " rounds of try, you may delete it manually.");
        }
    }


    /**
     * thread unsafe, caller need synchronization
     */
    public void flush() {
        Collection<MyCatBufferPage> cachedPages = pageLRUCache.getValues();
        for(MyCatBufferPage mappedPage : cachedPages) {
            ((MappedMemFileBufferPage)mappedPage).flush();
        }
    }

    /**
     * thread unsafe, caller need synchronization
     */
    public void deleteAllPages() throws IOException {
        pageLRUCache.removeAll();
        Set<Long> indexSet = getExistingPageIndexSet();
        this.deletePages(indexSet);
    }

    /**
     * thread unsafe, caller need synchronization
     */
    public void deletePages(Set<Long> indexes) throws IOException {
        if (indexes == null) return;
        for(long index : indexes) {
            this.deletePage(index);
        }
    }

    public Set<Long> getExistingPageIndexSet() {
        Set<Long> indexSet = new HashSet<Long>();
        File[] pageFiles = this.pageDir.listFiles();
        if (pageFiles != null && pageFiles.length > 0) {
            for(File pageFile : pageFiles) {
                String fileName = pageFile.getName();
                if (fileName.endsWith(PAGE_FILE_SUFFIX)) {
                    long index = this.getIndexByPageName(fileName);
                    indexSet.add(index);
                }
            }
        }
        return indexSet;
    }

    public void releaseCachedPages() throws IOException {
        pageLRUCache.removeAll();
    }


    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getPageDirName() {
        return pageDirName;
    }

    public void setPageDirName(String pageDirName) {
        this.pageDirName = pageDirName;
    }

    public File getPageDir() {
        return pageDir;
    }

    public void setPageDirFile(File pageDir) {
        this.pageDir = pageDir;
    }

    public String getPage() {
        return page;
    }

    public void setPageFile(String page) {
        this.page = page;
    }
}
