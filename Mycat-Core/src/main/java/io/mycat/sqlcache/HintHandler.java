package io.mycat.sqlcache;

/**
 * Hint处理Handler
 *
 * @author zagnix
 * @create 2017-01-17 14:07
 */

public interface HintHandler {
    public boolean handle(String sql);
}
