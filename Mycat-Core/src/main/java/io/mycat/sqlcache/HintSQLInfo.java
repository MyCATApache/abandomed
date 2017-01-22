package io.mycat.sqlcache;

import java.util.HashMap;
import java.util.Map;

/**
 * Hint SQL 携带的属性KV 和 真实执行的sql
 *
 * @author zagnix
 * @version 1.0
 * @create 2017-01-16 17:32
 */

public class HintSQLInfo {
    private String functionName;
    private boolean isCache = false;
    private  HintHandler hintHandler;
    private String hintSQL;
    private String execSQL;

    /**
     * paramsKv 中的Key必须是 cache-time,auto-refresh,access-count
     * cache-time=xxx auto-refresh=true access-count=5000
     */
    private Map<String,String > paramsKv = new HashMap<>();

    public HintSQLInfo(String hintSQL){
        this.hintSQL = hintSQL;
    }

    public String getExecSQL() {
        return execSQL;
    }

    public void setExecSQL(String execSQL) {
        this.execSQL = execSQL;
    }

    public Map<String, String> getParamsKv() {
        return paramsKv;
    }

    public void setParamsKv(Map<String, String> paramsKv) {
        this.paramsKv = paramsKv;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getHintSQL() {
        return hintSQL;
    }

    public void setHintSQL(String hintSQL) {
        this.hintSQL = hintSQL;
    }

    public HintHandler getHintHandler() {
        return hintHandler;
    }

    public void setHintHandler(HintHandler hintHandler) {
        this.hintHandler = hintHandler;
    }

    public boolean isCache() {
        return isCache;
    }

    public void setCache(boolean cache) {
        isCache = cache;
    }

    @Override
    public String toString() {
        return "HintSQLInfo{" +
                "functionName='" + functionName + '\'' +
                ", hintSQL='" + hintSQL + '\'' +
                ", execSQL='" + execSQL + '\'' +
                ", paramsKv=" + paramsKv +
                '}';
    }
}
