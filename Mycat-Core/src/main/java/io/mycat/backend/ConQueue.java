package io.mycat.backend;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConQueue {
    private final ConcurrentLinkedQueue<MySQLBackendConnection> autoCommitCons = new ConcurrentLinkedQueue<MySQLBackendConnection>();
    private final ConcurrentLinkedQueue<MySQLBackendConnection> manCommitCons = new ConcurrentLinkedQueue<MySQLBackendConnection>();
    private long executeCount;

    public MySQLBackendConnection takeIdleCon(boolean autoCommit) {
        ConcurrentLinkedQueue<MySQLBackendConnection> f1 = autoCommitCons;
        ConcurrentLinkedQueue<MySQLBackendConnection> f2 = manCommitCons;

        if (!autoCommit) {
            f1 = manCommitCons;
            f2 = autoCommitCons;

        }
        MySQLBackendConnection con = f1.poll();
        if (con == null || con.isClosed()) {
            con = f2.poll();
        }
        if (con == null || con.isClosed()) {
            return null;
        } else {
            return con;
        }

    }

    public long getExecuteCount() {
        return executeCount;
    }

    public void incExecuteCount() {
        this.executeCount++;
    }

    public void removeCon(MySQLBackendConnection con) {
        if (!autoCommitCons.remove(con)) {
            manCommitCons.remove(con);
        }
    }

    public boolean isSameCon(MySQLBackendConnection con) {
        if (autoCommitCons.contains(con)) {
            return true;
        } else if (manCommitCons.contains(con)) {
            return true;
        }
        return false;
    }

    public ConcurrentLinkedQueue<MySQLBackendConnection> getAutoCommitCons() {
        return autoCommitCons;
    }

    public ConcurrentLinkedQueue<MySQLBackendConnection> getManCommitCons() {
        return manCommitCons;
    }

    public ArrayList<MySQLBackendConnection> getIdleConsToClose(int count) {
        ArrayList<MySQLBackendConnection> readyCloseCons = new ArrayList<MySQLBackendConnection>(count);
        while (!manCommitCons.isEmpty() && readyCloseCons.size() < count) {
            MySQLBackendConnection theCon = manCommitCons.poll();
            if (theCon != null) {
                readyCloseCons.add(theCon);
            }
        }
        while (!autoCommitCons.isEmpty() && readyCloseCons.size() < count) {
            MySQLBackendConnection theCon = autoCommitCons.poll();
            if (theCon != null) {
                readyCloseCons.add(theCon);
            }

        }
        return readyCloseCons;
    }

}
