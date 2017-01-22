package io.mycat.sqlcache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * 主要用来解析 Hint SQL
 * 兼容mycat 1.6版本 新的注解方式
 *
 * @author zagnix
 * @create 2017-01-16 17:23
 */

public class HintSQLParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(HintSQLParser.class);

    //注解格式:/*!mycat:type=value */ sql. sql为真实的执行的sql语句
    private final static String MYCAT_HINT_START = "/*!mycat:";
    private final static String HINT_PRO_SPLIT = " ";
    private final static String HINT_KV_SPLIT = "=";


    public static HintSQLInfo parserHintSQL(String sql){
        if (sql == null)
            return null;

        HintSQLInfo hintSQLInfo = new HintSQLInfo(sql);
        Map<String,String> hintKv = new HashMap<String,String>();
        int endIndex = sql.indexOf("*/");

        if (endIndex !=-1 ){
            String hintPart = sql.substring(0,endIndex);
            hintSQLInfo.setExecSQL(sql.substring(endIndex+2));
            String kvsPart=hintPart.replace(MYCAT_HINT_START,"").replace("*/","");
            StringTokenizer token = new StringTokenizer(kvsPart,HINT_PRO_SPLIT);

            /**hint功能*/
            if (token.hasMoreElements()) {
                StringTokenizer function = new StringTokenizer(token.nextToken(), HINT_KV_SPLIT);
                if (function.countTokens()==2){
                    String kName = function.nextToken();
                    String vValue = function.nextToken();
                    if (kName.equalsIgnoreCase("cacheable") && vValue.equals("true")){
                        hintSQLInfo.setFunctionName(kName);
                        hintSQLInfo.setCache(true);
                    }

                }else {
                    LOGGER.error(sql + "注解中的KV属性不对");
                    return null;
                }

                /**KV**/
                while (token.hasMoreElements()) {
                    StringTokenizer t = new StringTokenizer(token.nextToken(), HINT_KV_SPLIT);

                    if (t.countTokens() == 2) {
                        /**TODO 限制hintSQL中kv中k具体命名必须在{cacheable, cache-time,auto-refresh, access-count}*/
                        hintKv.put(t.nextToken(),t.nextToken());
                    }else { /**KV*/
                        LOGGER.error(sql + "注解中的KV属性不对");
                        return null;
                    }
                }
                hintSQLInfo.setParamsKv(hintKv);
            }else { /**没有function name*/
                LOGGER.error(sql + "注解中的功能没有名字");
                return null;
            }
        }else {
            return null;
        }
        return hintSQLInfo;
    }
}
