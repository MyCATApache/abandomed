package io.mycat.sqlcache;

import java.util.concurrent.atomic.AtomicLong;

/**
 * key cache生效规则
 *
 * @author zagnix
 * @version 1.0
 * @create 2016-12-30 16:48
 */

public class Keyer<K,V> {
    private long cacheTTL=0;
    private long lastAccessTime=0;

    /**
     * TTL 时间内访问次数
     */
    private long accessCount=0;
    private AtomicLong refCount = new AtomicLong(0);


    private boolean autoRefresh = false;

    private String sql;
    private  K key;
    private  V value;

    private IDataLoader<K,V> iDataLoader = null;
    private IRemoveKeyListener<K,V> removeKeyListener = null;

    public long getCacheTTL() {
        return cacheTTL;
    }

    public void setCacheTTL(long cacheTTL) {
        this.cacheTTL = cacheTTL;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public AtomicLong getRefCount() {
        return refCount;
    }

    public void setRefCount(AtomicLong refCount) {
        this.refCount = refCount;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public IDataLoader<K, V> getiDataLoader() {
        return iDataLoader;
    }

    public void setiDataLoader(IDataLoader<K, V> iDataLoader) {
        this.iDataLoader = iDataLoader;
    }

    public IRemoveKeyListener<K, V> getRemoveKeyListener() {
        return removeKeyListener;
    }

    public void setRemoveKeyListener(IRemoveKeyListener<K, V> removeKeyListener) {
        this.removeKeyListener = removeKeyListener;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(long accessCount) {
        this.accessCount = accessCount;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }


    public boolean isAutoRefresh() {
        return autoRefresh;
    }

    public void setAutoRefresh(boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
    }
}
