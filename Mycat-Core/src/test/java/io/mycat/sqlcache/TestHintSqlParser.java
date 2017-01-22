package io.mycat.sqlcache;

import org.junit.Test;

/**
 * @author zagnix
 * @create 2017-01-16 18:03
 */

public class TestHintSqlParser {
    //TODO 测试case待完善
    @Test
    public void testHintSqlParser(){
        String hintSql = "/*!mycat:cacheable=true cache-time=5000 auto-refresh=true access-count=5000*/select * from table";
        System.out.println(HintSQLParser.parserHintSQL(hintSql).toString());
    }
}
