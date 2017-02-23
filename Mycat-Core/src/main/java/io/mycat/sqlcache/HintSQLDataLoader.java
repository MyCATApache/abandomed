package io.mycat.sqlcache;

import com.google.common.util.concurrent.*;
import io.mycat.bigmem.sqlcache.BigSQLResult;
import io.mycat.bigmem.sqlcache.IDataLoader;
import io.mycat.bigmem.sqlcache.Keyer;
import io.mycat.bigmem.console.LocatePolicy;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static com.google.common.hash.Hashing.murmur3_32;

/**
 * SQL 异步加载结果集类
 *
 * @author zagnix
 * @create 2017-01-20 15:13
 */

public class HintSQLDataLoader<K,V extends BigSQLResult> implements IDataLoader<K,V> {
    /**
     * 根据sql,异步从后台DB reload数据，替换旧值
     * @param keyer
     * @return
     */
    @Override
    public V reload(Keyer<K, V> keyer) {
        ListeningExecutorService executor =
                MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

        final ListenableFuture<BigSQLResult> listenableFuture  = executor
                .submit(new Callable<BigSQLResult>() {
                    @Override
                    public BigSQLResult call() throws Exception {
                        //TODO
                        String sql = keyer.getSql();
                        String sqlkey = keyer.getLastAccessTime() +"_"+murmur3_32().hashUnencodedChars(sql);
                        BigSQLResult sqlResultCache
                                = new BigSQLResult(LocatePolicy.Normal,sqlkey,32*1024*1024);


                        return sqlResultCache;
                    }
                });

            Futures.addCallback(listenableFuture, new FutureCallback<BigSQLResult>() {
                        @Override
                        public void onSuccess(BigSQLResult bigSQLResult) {
                            //TODO
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            //TODO
                        }
                    }
            );
        return null;
    }
}
