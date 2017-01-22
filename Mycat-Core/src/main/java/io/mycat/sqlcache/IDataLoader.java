package io.mycat.sqlcache;

/**
 * Value 数据加载接口
 *
 * @author zagnix
 * @version 1.0
 * @create 2016-12-30 16:44
 */
public interface IDataLoader<K,V>{

    /**
     * Key失效，时候重新异步reload数据
     * @param keyer
     * @return
     */
    public V reload(Keyer<K, V> keyer);
}
