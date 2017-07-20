package io.mycat.mysql;

import java.io.IOException;

import io.mycat.mysql.state.MysqlConnectionState;

/**
 * 状态操作接口
 *
 * @author ynfeng
 */
public interface StatefulConnection {
//    /**
//     * 改变状态
//     *
//     * @param state      状态
//     * @param attachment 附加参数
//     */
//    void changeState(MysqlConnectionState state, Object attachment)throws IOException;

    /**
     * 设置下一个状态
     *
     * @param state
     */
    MySQLConnection setNextState(MysqlConnectionState state);
    
    /**
     * 获取下一个状态 用于同步
     * @return
     */
    MysqlConnectionState getNextState();
    
    /**
     * 获取当前状态
     *
     * @return
     */
    MysqlConnectionState getCurrentState();

    /**
     * 驱动状态
     *
     * @param attachment 附加参数
     */
    void driveState(Object attachment)throws IOException;

    /**
     * 驱动状态
     */
    void driveState()throws IOException;

}
