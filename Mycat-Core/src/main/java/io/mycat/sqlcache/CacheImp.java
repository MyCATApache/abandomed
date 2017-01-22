package io.mycat.sqlcache;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * ICache实现类
 *
 * @author zagnix
 * @version 1.0
 * @create 2016-12-30 16:53
 */

public class CacheImp<K,V> implements ICache<K,V> {

    private final static Logger logger
            = LoggerFactory.getLogger(CacheImp.class);

    public static final long DEFAULT_TTL = 5 * 1000;
    public static final int ACCESS_COUNT = 500;
    private static final int REMOVE_ACTION = 1;
    private static final int RELOAD_ACTION = 2;

    private final Map<K,V> cacheMap;
    private final Map<K,Keyer<K,V>> cacheKeyers;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();


    /**
     * 异步线程，按照cache规则生效
     * 1.缓存时间到期，进行del
     * 2.如果在缓存时间内，访问次数超过N次，重新从DB后台load数据，更新Value
     */
    private static final ThreadFactory threadFactory =
            new ThreadFactoryBuilder().setDaemon(true).setNameFormat("async-op-cache ").build();
    private static final ScheduledExecutorService scheduleAtFixedRate =
            new ScheduledThreadPoolExecutor(1,threadFactory);
    private final Set<Keyer<K,V>> keysToDel = new HashSet<Keyer<K,V>>();
    private final Set<Keyer<K,V>> keysToReload= new HashSet<Keyer<K,V>>();


    public CacheImp(){
        cacheMap = new ConcurrentHashMap<K, V>();
        cacheKeyers = new ConcurrentHashMap<K, Keyer<K,V>>();

        scheduleAtFixedRate.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                keysToDel.clear();
                Set<K> keySets = cacheKeyers.keySet();

                long currentTime = System.currentTimeMillis();
                for (K key:keySets) {
                    Keyer<K,V> k = cacheKeyers.get(key);
                    long diff = currentTime-k.getLastAccessTime();
                    if (diff>k.getCacheTTL()){
                        keysToDel.add(k);
                    }else if (diff<=k.getCacheTTL() && k.getRefCount().get()>=k.getAccessCount()){
                        keysToReload.add(k);
                    }
                }

                /**
                 * key 失效,回调remove接口
                 */
                for (Keyer<K,V> key:keysToDel) {
                    cacheMap.remove(key.getKey());
                    if(key.getRemoveKeyListener() !=null){
                        key.getRemoveKeyListener().removeNotify(key.getKey(),key.getValue());
                    }
                    cacheKeyers.remove(key.getKey());
                }


                /**
                 * 回调用户reload接口
                 */
                for (Keyer<K,V> key:keysToReload) {
                    /**
                     * reload 设计为异步加载
                     */
                    if(key.getiDataLoader() !=null){
                        key.getiDataLoader().reload(key);
                    }
                }
            }
        },0,DEFAULT_TTL, TimeUnit.SECONDS);
    }

    /**
     * put (k，v) to map
     *
     * @param key
     * @param value
     * @param keyer
     */
    @Override
    public void put(K key, V value,Keyer<K,V> keyer) {
        cacheMap.put(key,value);
        cacheKeyers.put(key,keyer);
    }

    /**
     * get value
     *
     * @param key
     * @return
     */
    @Override
    public V get(K key) {

        try {
            readLock.lock();
            Keyer<K,V> keyer = cacheKeyers.get(key);

            if (keyer != null){
                keyer.setLastAccessTime(System.currentTimeMillis());
                keyer.getRefCount().decrementAndGet();
            }

            return cacheMap.get(key);

        }finally {
            readLock.unlock();
        }


    }

    /**
     * remove key
     *
     * @param key
     */
    @Override
    public void remove(K key) {
        Keyer<K,V> keyer = cacheKeyers.get(key);

        if (keyer.getRemoveKeyListener()!=null){
            keyer.getRemoveKeyListener().
                    removeNotify(keyer.getKey(),keyer.getValue());
        }
        cacheKeyers.remove(key);
        cacheMap.remove(key);
    }


    /**
     * remove all map's element
     */
    @Override
    public void removeALL() {
        for (K key:cacheKeyers.keySet()) {
            Keyer<K,V> keyer = cacheKeyers.get(key);
            if (keyer.getRemoveKeyListener()!=null){
                keyer.getRemoveKeyListener().removeNotify(keyer.getKey(),keyer.getValue());
            }
        }
        cacheKeyers.clear();
        cacheMap.clear();
    }
}
