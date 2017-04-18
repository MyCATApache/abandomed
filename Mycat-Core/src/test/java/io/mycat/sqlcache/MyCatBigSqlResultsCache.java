package io.mycat.sqlcache;

import com.google.common.util.concurrent.*;
import io.mycat.bigmem.sqlcache.*;
import io.mycat.util.Utils;
import io.mycat.bigmem.console.LocatePolicy;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static com.google.common.hash.Hashing.murmur3_32;

/**
 * Cache SQL 大结果集 对外接口
 *
 * @author zagnix
 * @version 1.0
 * @create 2016-12-30 10:51
 */

public class MyCatBigSqlResultsCache {

    private final CacheImp<String,BigSQLResult> sqlResultCacheImp;

    private final static MyCatBigSqlResultsCache INSTANCE = new MyCatBigSqlResultsCache();

    private MyCatBigSqlResultsCache(){
        sqlResultCacheImp = new CacheImp<String,BigSQLResult>();
    }

    /**
     * 将sql结果集缓存起来
     *
     * @param sql  sql语句
     * @param bigSQLResult  sql结果集缓存
     * @param cache 缓存时间
     * @param accesCount  在缓存时间内，被访问的次数
     * @param loader  结果集load or reload 接口
     * @param listener key被移除时，调用的接口
     */
    public void cacheSQLResult(String sql, BigSQLResult bigSQLResult, long cache, long accesCount,
                               IDataLoader<String,BigSQLResult> loader, IRemoveKeyListener<String,BigSQLResult> listener){
        /**
         * TODO
         */
        String key = "" + murmur3_32().hashUnencodedChars(sql);

        Keyer<String,BigSQLResult> keyer = new Keyer<String,BigSQLResult>();
        keyer.setSql(sql);
        keyer.setKey(key);
        keyer.setValue(bigSQLResult);
        keyer.setCacheTTL(cache);
        keyer.setAccessCount(accesCount);
        keyer.setRemoveKeyListener(listener);
        keyer.setiDataLoader(loader);
        sqlResultCacheImp.put(key,bigSQLResult,keyer);
    }


    /**
     * 获取sql语句，已经缓存的结果集
     *
     * @param sql  sql 语句
     * @return
     */
    public BigSQLResult getSQLResult(String sql){
        /**
         * TODO
         */
        String key = "" + murmur3_32().hashUnencodedChars(sql);
        return sqlResultCacheImp.get(key);
    }



    /**
     * 获取sql语句，已经缓存的结果集
     *
     * @param sql  sql 语句
     */
    public void remove(String sql){
        /**
         * TODO
         */
        String key = "" + murmur3_32().hashUnencodedChars(sql);
        sqlResultCacheImp.remove(key);
    }



    /**
     * 对外对象实例
     * @return
     */
    public static MyCatBigSqlResultsCache getInstance() {
        return INSTANCE;
    }


    public static void main(String [] args){

        String sql = "select * from table";

        BigSQLResult sqlResultCache =
                new BigSQLResult(LocatePolicy.Normal,sql,32*1024*1024);

        long ROWS = 10000;


        /** 模拟从后端DB拉取数据 */
        for (int i = 0; i < ROWS ; i++) {
            byte[] rows = Utils.randomString(1024).getBytes();

            /**
             * 使用内存映射Cache，存放SQL结果集
             */
            sqlResultCache.put(rows);
        }


        MyCatBigSqlResultsCache.getInstance().cacheSQLResult(sql,sqlResultCache,300,5000,
                new IDataLoader<String, BigSQLResult>() {

                    /**
                     * 根据sql,异步从后台DB reload数据，替换旧值
                     * @param keyer
                     * @return
                     */
                    @Override
                    public BigSQLResult reload(Keyer<String,BigSQLResult> keyer) {
                        ListeningExecutorService executor =
                                MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

                        final ListenableFuture<BigSQLResult> listenableFuture  = executor
                                .submit(new Callable<BigSQLResult>() {
                                    @Override
                                    public BigSQLResult call() throws Exception {
                                        String sql = keyer.getSql();
                                        String sqlkey = keyer.getLastAccessTime() +"_"+murmur3_32().hashUnencodedChars(sql);
                                        BigSQLResult sqlResultCache
                                                = new BigSQLResult(LocatePolicy.Normal,sqlkey,32*1024*1024);

                                        /**模拟从后端DB拉取数据*/
                                        for (int i = 0; i < ROWS ; i++) {
                                            byte[] rows = Utils.randomString(1024).getBytes();

                                            /**
                                             * 使用内存映射Cache，存放SQL结果集
                                             */
                                            sqlResultCache.put(rows);
                                        }

                                        return sqlResultCache;
                                    }
                                });

                        Futures.addCallback(listenableFuture, new FutureCallback<BigSQLResult>() {
                                    @Override
                                    public void onSuccess(BigSQLResult bigSQLResult) {
                                        if(bigSQLResult!=null && bigSQLResult.hasNext()){

                                            BigSQLResult oldSqlResult=
                                                    MyCatBigSqlResultsCache.getInstance().getSQLResult(keyer.getSql());

                                            if (oldSqlResult != null){
                                                oldSqlResult.removeAll();
                                            }

                                            /**替换旧的值*/
                                            MyCatBigSqlResultsCache.getInstance().
                                                    sqlResultCacheImp.put(keyer.getKey(),bigSQLResult,keyer);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Throwable t) {
                                        //TODO
                                    }
                                }
                        );

                        return null;
                    }


                }, new IRemoveKeyListener<String, BigSQLResult>() {

                    /**
                     * key 失效，做清理工作
                     *
                     * @param key
                     * @param value
                     */
                    @Override
                    public void removeNotify(String key, BigSQLResult value) {
                        if (value !=null){
                            value.removeAll();
                        }

                        System.out.println("key :" + key);
                    }
                });

        /**
         * 访问Cache
         */
        BigSQLResult bigSQLResult =
                MyCatBigSqlResultsCache.getInstance().getSQLResult(sql);


        bigSQLResult.reset();

        while (bigSQLResult.hasNext()){

            byte[] data = bigSQLResult.next();
            //TODO
            System.out.println("String :" + new String(data));

        }


        MyCatBigSqlResultsCache.getInstance().remove(sql);

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
