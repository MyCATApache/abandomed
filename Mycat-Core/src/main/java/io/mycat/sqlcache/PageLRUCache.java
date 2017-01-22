package io.mycat.sqlcache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 通过LRU机制 Cache Buffer Page，提供高访问速度
 * @author zagnix
 * @create 2016-12-02 14:37
 */

public class PageLRUCache {
    private final static Logger logger = LoggerFactory.getLogger(PageLRUCache.class);

    public static final long DEFAULT_TTL = 5 * 1000;

    private final Map<Long,MyCatBufferPage> map;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    /**
     * 异步线程
     */
    private static final ExecutorService executorService
            = Executors.newCachedThreadPool();

    private final Set<Long> toDisk = new HashSet<Long>();

    public PageLRUCache() {
        map = new HashMap<Long,MyCatBufferPage>();
    }

    /**
     * 向Cache添加一个page，会触发Page Sweep操作
     * @param key
     * @param value
     * @param ttlInMilliSeconds
     */
    public void put(Long key, MyCatBufferPage value, long ttlInMilliSeconds) {
        Collection<MyCatBufferPage> values = null;
        try {
            writeLock.lock();
            values = pageSweep();
            if (values != null && values.contains(value)) {
                values.remove(value);
            }
            value.getLastAccessedTimestamp().set(System.currentTimeMillis());
            map.put(key, value);
        } finally {
            writeLock.unlock();
        }


        if (values != null && values.size() > 0) {
            executorService.execute(new Task(values));
        }
    }

    public void put(Long key,MyCatBufferPage value) {
        this.put(key, value, DEFAULT_TTL);
    }


    /**
     * 根据Page Index 得到对应的 Page
     * 同时会设置该Page的访问时间，和引用计数+1
     * @param key
     * @return
     */
    public MyCatBufferPage get(final Long key) {
        try {
            readLock.lock();
            MyCatBufferPage value = map.get(key);
            if (value != null) {
                value.getLastAccessedTimestamp().set(System.currentTimeMillis());
                value.getRefCount().incrementAndGet();
            }
            return map.get(key);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 将上次访问时间超过Cache TTL 和 引用数=0的Page
     * 存放到collection中，供put调用时候，异步从内存
     * 中刷到磁盘上
     * @return
     */

    private Collection<MyCatBufferPage> pageSweep() {
        Collection<MyCatBufferPage> values = null;
        toDisk.clear();
        Set<Long> keys = map.keySet();
        long currentTimeMillis = System.currentTimeMillis();

        for(Long key: keys) {
            MyCatBufferPage v = map.get(key);
            if (v.getRefCount().get() <= 0
                    && (currentTimeMillis - v.getLastAccessedTimestamp().get()) > v.getCacheTTL()) {
                toDisk.add(key);
            }
        }

        if (toDisk.size() > 0) {
            values = new HashSet<MyCatBufferPage>();
            for(Long key : toDisk) {
                MyCatBufferPage v = map.remove(key);
                values.add(v);
            }
        }

        return values;
    }

    /**
     * 引用计数减1
     * @param key
     */

    public void release(final Long key) {
        try {
            readLock.lock();
            MyCatBufferPage value = map.get(key);
            if (value != null) {
                value.getRefCount().decrementAndGet();
            }
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 缓存page的大小
     * @return
     */
    public int size() {
        try {
            readLock.lock();
            return map.size();
        } finally {
            readLock.unlock();
        }
    }


    /**
     * 将所有缓存在内存中Page 数据，刷到磁盘中去
     * @throws IOException
     */
    public void removeAll()  throws IOException {
        try {
            writeLock.lock();
            Collection<MyCatBufferPage> values = map.values();
            if (values != null && values.size() > 0) {
                for(MyCatBufferPage v : values) {
                    v.recycle();
                }
            }
            map.clear();
        } finally {
            writeLock.unlock();
        }

    }


    /**
     * 将index Page 数据，刷到磁盘中去
     * @param key
     * @return
     * @throws IOException
     */

    public MyCatBufferPage remove(final Long key) throws IOException {
        try {
            writeLock.lock();
            MyCatBufferPage value = map.remove(key);
            if (value != null) {
                value.recycle();
            }
            return value;
        } finally {
            writeLock.unlock();
        }
    }




    public Collection<MyCatBufferPage> getValues() {
        try {
            readLock.lock();
            return  map.values();
        } finally {
            readLock.unlock();
        }
    }


    /**
     * 异步操作task，负责将内存中的页数据，刷到磁盘中
     */
    private static class Task implements Runnable {
        Collection<MyCatBufferPage> values;
        public Task(Collection<MyCatBufferPage> values) {
            this.values = values;
        }
        public void run() {
            for(MyCatBufferPage v : values) {
                try {
                    if (v != null) {
                        v.recycle();
                    }
                } catch (IOException e) {
                }
            }
        }
    }
}
