package io.mycat.sqlcache;

import io.mycat.bigmem.sqlcache.BigSQLResult;
import io.mycat.bigmem.sqlcache.IRemoveKeyListener;

/**
 * HintSQL 结果集被移除时回调类
 *
 * @author zagnix
 * @create 2017-01-20 15:14
 */

public class HintSQLRemoveKeyListener<K,V extends BigSQLResult> implements IRemoveKeyListener<K,V> {
    /**
     * key 失效，做清理工作
     *
     * @param key
     * @param value
     */
    @Override
    public void removeNotify(K key, V value) {
        if (value !=null){
            value.removeAll();
        }
    }
}