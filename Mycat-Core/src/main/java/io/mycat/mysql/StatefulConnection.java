package io.mycat.mysql;

import io.mycat.mysql.state.MysqlConnectionState;

/**
 * 状态操作接口
 *
 * @author ynfeng
 */
public interface StatefulConnection {
    /**
     * 更改状态
     *
     * @param state
     */
    void changeState(MysqlConnectionState state);

    /**
     * 获取当前状态
     *
     * @return
     */
    MysqlConnectionState getCurrentState();
}
