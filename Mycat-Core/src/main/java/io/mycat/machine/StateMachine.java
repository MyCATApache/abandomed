package io.mycat.machine;

import java.io.IOException;

/**
 * 状态操作接口
 *
 * @author ynfeng
 */
public interface StateMachine {
    /**
     * 设置下一个状态
     *
     * @param state
     */
    StateMachine setNextState(State state);

    /**
     * 获取下一个状态 用于同步
     *
     * @return
     */
    State getNextState();

    /**
     * 获取当前状态
     *
     * @return
     */
    State getCurrentState();

    /**
     * 驱动状态
     *
     * @param attachment 附加参数
     */
    void driveState(Object attachment) throws IOException;

    /**
     * 驱动状态
     */
    void driveState() throws IOException;

}
