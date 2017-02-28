package io.mycat.sqlcache;


import io.mycat.bigmem.console.LocatePolicy;
import io.mycat.bigmem.sqlcache.BigSQLResult;
import io.mycat.util.Utils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.hash.Hashing.murmur3_32;
import static org.junit.Assert.*;

//TODO 待完善测试用例
public class testSQLResultCache {
    private BigSQLResult sqlResultCache;

    @Test
    public void simpleTest() throws IOException {
        for(int i = 1; i <= 2; i++) {

            sqlResultCache = new BigSQLResult(LocatePolicy.Normal, "select * from t",16*1024*1024);
            assertNotNull(sqlResultCache);

            for(int j = 1; j <= 3; j++) {
                assertTrue(sqlResultCache.size() == 0L);
                assertTrue(sqlResultCache.isEmpty());

                assertNull(sqlResultCache.next());

                sqlResultCache.put("hello".getBytes());
                assertTrue(sqlResultCache.size() == 1L);
                assertTrue(sqlResultCache.hasNext());
                assertEquals("hello", new String(sqlResultCache.next()));
                assertNull(sqlResultCache.next());

                sqlResultCache.put("world".getBytes());
                sqlResultCache.flush();
                assertTrue(sqlResultCache.size() == 1L);
                assertTrue(sqlResultCache.hasNext());
                assertEquals("world", new String(sqlResultCache.next()));
                assertNull(sqlResultCache.next());
            }
            sqlResultCache.recycle();
        }
    }


    @Test
    public void testSQLResultCache(){
        long ROWS = 10000;
        //long ROWS = 10000*10000;
        //long ROWS = 100000*100000;
        Map<String,BigSQLResult> sqlResultCacheMap = new HashMap<String,BigSQLResult>();

        String sql = "select * from table1";
        String sqlkey = ""+murmur3_32().hashUnencodedChars(sql);

        /**
         * sql results back list
         */

        ArrayList<byte[]> backList = new ArrayList<byte[]>();


        /**
         * 使用内存映射Cache，存放SQL结果集
         */

        BigSQLResult sqlResultCache
                = new BigSQLResult(LocatePolicy.Normal,sqlkey,16*1024*1024);

        for (int i = 0; i < ROWS ; i++) {
            byte[] rows = Utils.randomString(1024).getBytes();
            backList.add(rows);

            /**
             * 使用内存映射Cache，存放SQL结果集
             */
            sqlResultCache.put(rows);
        }
        sqlResultCacheMap.put(sqlkey,sqlResultCache);



        /**
         * 验证内存映射Cache，存放SQL结果集
         */
        BigSQLResult sqlResCache = sqlResultCacheMap.get(sqlkey);

        Assert.assertEquals(backList.size(),sqlResCache.size());
        for (int i = 0; i <backList.size() ; i++) {
            if (sqlResultCache.hasNext()){
                Assert.assertArrayEquals(backList.get(i),sqlResCache.next());
            }
        }


        /**
         * 重复读
         */
        sqlResCache.reset();
        for (int i = 0; i <backList.size() ; i++) {
            if (sqlResultCache.hasNext()){
                Assert.assertArrayEquals(backList.get(i),sqlResCache.next());
            }
        }



        if (sqlResultCache !=null){
            sqlResultCache.removeAll();
        }

    }

    @After
    public void clean() throws IOException {
        if (sqlResultCache != null) {
            sqlResultCache.removeAll();
        }
    }


}
