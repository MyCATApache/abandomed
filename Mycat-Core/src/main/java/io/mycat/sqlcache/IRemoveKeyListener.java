package io.mycat.sqlcache;

/**
 * key 被移除时，回调该接口
 *
 * @author zagnix
 * @version 1.0
 * @create 2016-12-30 16:47
 */
public interface IRemoveKeyListener<K,V> {

    /**
     * 当Key被移除时，回调该接口
     *
     * @param key
     * @param value
     */
    public void  removeNotify(K key, V value);
}
