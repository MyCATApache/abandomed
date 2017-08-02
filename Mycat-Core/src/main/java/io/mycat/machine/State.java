package io.mycat.machine;

import java.io.IOException;

import io.mycat.net2.Connection;

/**
 * 状态机状态接口
 *
 * @author ynfeng
 */
public interface State {
    boolean handle(StateMachine context, Connection connection, Object attachment) throws IOException;
}
