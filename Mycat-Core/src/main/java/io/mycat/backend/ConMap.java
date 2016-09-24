package io.mycat.backend;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import io.mycat.net2.Connection;
import io.mycat.net2.NetSystem;

public class ConMap {
    // key -schema
    private final ConcurrentHashMap<String, ConQueue> items = new ConcurrentHashMap<String, ConQueue>();

    public ConQueue getSchemaConQueue(String schema) {
        ConQueue queue = items.get(schema);
        if (queue == null) {
            ConQueue newQueue = new ConQueue();
            queue = items.putIfAbsent(schema, newQueue);
            return (queue == null) ? newQueue : queue;
        }
        return queue;
    }

    public MySQLBackendConnection tryTakeCon(String reactor,final String schema, boolean autoCommit) {
        final ConQueue queue = items.get(schema);
        MySQLBackendConnection con = tryTakeCon(reactor,queue, autoCommit);
        if (con != null) {
            return con;
        } else {
            for (ConQueue queue2 : items.values()) {
                if (queue != queue2) {
                    con = tryTakeCon(reactor,queue2, autoCommit);
                    if (con != null) {
                        return con;
                    }
                }
            }
        }
        return null;

    }

    private MySQLBackendConnection tryTakeCon(String reacotr,ConQueue queue, boolean autoCommit) {

    	MySQLBackendConnection con = null;
        if (queue != null && ((con = queue.takeIdleCon(autoCommit)) != null) && ((Connection)con).belongsActor(reacotr)) {
            return con;
        } else {
            return null;
        }

    }

    public Collection<ConQueue> getAllConQueue() {
        return items.values();
    }

    public int getActiveCountForSchema(String schema, MySQLDataSource dataSouce) {
        int total = 0;
        for (Connection conn : NetSystem.getInstance().getAllConnectios().values()) {
            if (conn instanceof MySQLBackendConnection) {
            	MySQLBackendConnection theCon = (MySQLBackendConnection) conn;
                if (theCon.getSchema().equals(schema) && theCon.getPool() == dataSouce) {
                    if (theCon.isBorrowed()) {
                        total++;
                    }
                }
            }

        }
        return total;
    }

    public int getActiveCountForDs(MySQLDataSource dataSouce) {

        int total = 0;
        for (Connection conn : NetSystem.getInstance().getAllConnectios().values()) {
            if (conn instanceof MySQLBackendConnection) {
            	MySQLBackendConnection theCon = (MySQLBackendConnection) conn;
                if (theCon.getPool() == dataSouce) {
                    if (theCon.isBorrowed()) {
                        total++;
                    }
                }
            }

        }
        return total;
    }

    public void clearConnections(String reason, MySQLDataSource dataSouce) {

        Iterator<Entry<Long, Connection>> itor = NetSystem.getInstance().getAllConnectios().entrySet().iterator();
        while (itor.hasNext()) {
            Entry<Long, Connection> entry = itor.next();
            Connection con = entry.getValue();
            if (con instanceof MySQLBackendConnection) {
                if (((MySQLBackendConnection) con).getPool() == dataSouce) {
                    con.close(reason);
                    itor.remove();
                }
            }

        }
        items.clear();
    }

}
