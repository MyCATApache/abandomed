package io.mycat.backend;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import io.mycat.backend.mysql.MySQLBackendConnection;
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

    public BackendConnection tryTakeCon(String reactor,final String schema, boolean autoCommit) {
        final ConQueue queue = items.get(schema);
        BackendConnection con = tryTakeCon(reactor,queue, autoCommit);
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

    private BackendConnection tryTakeCon(String reacotr,ConQueue queue, boolean autoCommit) {

        BackendConnection con = null;
        if (queue != null && ((con = queue.takeIdleCon(autoCommit)) != null) && ((Connection)con).belongsActor(reacotr)) {
            return con;
        } else {
            return null;
        }

    }

    public Collection<ConQueue> getAllConQueue() {
        return items.values();
    }

    public int getActiveCountForSchema(String schema, DHSource dataSouce) {
        int total = 0;
        for (Connection conn : NetSystem.getInstance().getAllConnectios().values()) {
            if (conn instanceof MySQLBackendConnection) {
                BackendConnection theCon = (BackendConnection) conn;
                if (theCon.getSchema().equals(schema) && theCon.getPool() == dataSouce) {
                    if (theCon.isBorrowed()) {
                        total++;
                    }
                }
            }

        }
        return total;
    }

    public int getActiveCountForDs(DHSource dataSouce) {

        int total = 0;
        for (Connection conn : NetSystem.getInstance().getAllConnectios().values()) {
            if (conn instanceof BackendConnection) {
                BackendConnection theCon = (BackendConnection) conn;
                if (theCon.getPool() == dataSouce) {
                    if (theCon.isBorrowed()) {
                        total++;
                    }
                }
            }

        }
        return total;
    }

    public void clearConnections(String reason, DHSource dataSouce) {

        Iterator<Entry<Long, Connection>> itor = NetSystem.getInstance().getAllConnectios().entrySet().iterator();
        while (itor.hasNext()) {
            Entry<Long, Connection> entry = itor.next();
            Connection con = entry.getValue();
            if (con instanceof BackendConnection) {
                if (((BackendConnection) con).getPool() == dataSouce) {
                    con.close(reason);
                    itor.remove();
                }
            }

        }
        items.clear();
    }

}
