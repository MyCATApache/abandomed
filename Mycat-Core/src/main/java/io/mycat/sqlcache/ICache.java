package io.mycat.sqlcache;

/**
 * 定制化 Cache接口
 * 主要实现SQL结果集缓存规则
 * 1.缓存时间
 * 2.在缓存时间内，访问次数超过N次，重新从DB后台load数据，更新Value
 * @author zagnix
 * @version 1.0
 * @create 2016-12-30 11:26
 */
public interface ICache<K, V> {

    /**
     *
     * @param key
     * @param value
     * @param keyer
     */
    public void put(final K key, final V value, final Keyer<K, V> keyer);

    /**
     *
     * @param key
     * @return
     */
    public V get(final K key);


    /**
     *
     * @param key
     */
    public void remove(final K key);

    /**
     *
     */
    public void removeALL();


}
