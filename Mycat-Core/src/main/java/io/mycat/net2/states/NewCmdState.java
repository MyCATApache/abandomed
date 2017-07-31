package io.mycat.net2.states;

import java.io.IOException;

import io.mycat.machine.State;
import io.mycat.machine.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.WriteCompleteListener;
import io.mycat.net2.Connection;

/**
 * 当前状态会
 *
 * @author yanjunli
 */
public class NewCmdState implements State {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewCmdState.class);
    public static final NewCmdState INSTANCE = new NewCmdState();


    private NewCmdState() {
    }

    @Override
    public boolean handle(StateMachine context, Connection conn, Object attachment) throws IOException {
        LOGGER.debug("Current conn in NewCmdState. conn is " + conn.getClass());
        //处于透传模式时,不指定connState. 由 listener 通过设置nextConnState 指定.
        // 通过回调可以设置业务状态机的状态等操作
        WriteCompleteListener listener = conn.getWriteCompleteListener();
        if (listener != null) {
            listener.wirteComplete();
            conn.setWriteCompleteListener(null);
        }

        if (conn.getNetworkStateMachine().getNextState() == null) {
            /* 命令解析完成后,应该制定后续状态,这里打印出 error 日志      */
            LOGGER.error("Current conn in ReadWaitingState. conn is " + conn.getClass());
            throw new RuntimeException(" error connState,you must set nextConnState ");
        }
        return true;
    }

}
