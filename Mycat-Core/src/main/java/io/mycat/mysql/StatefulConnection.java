package io.mycat.mysql;

import io.mycat.mysql.state.MysqlConnectionState;

/**
 * 状态操作接口
 *
 * @author ynfeng
 */
public interface StatefulConnection {
    /**
     * 改变状态
     *
     * @param state      状态
     * @param attachment 附加参数
     */
    void changeState(MysqlConnectionState state, Object attachment);

    /**
     * 设置下一个状态
     *
     * @param state
     */
    void setNextState(MysqlConnectionState state);

    /**
     * 驱动状态
     *
     * @param attachment 附加参数
     */
    void driveState(Object attachment);

    /**
     * 驱动状态
     */
    void driveState();

    /**
     * 获取当前状态
     *
     * @return
     */
    MysqlConnectionState getCurrentState();
}
