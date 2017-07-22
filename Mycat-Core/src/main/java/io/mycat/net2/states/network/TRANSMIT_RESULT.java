package io.mycat.net2.states.network;

/**
 * 向socket write 状态
 * @author yanjunli
 *
 */
public enum TRANSMIT_RESULT {
	 TRANSMIT_COMPLETE,   /** All done writing. */
	 TRANSMIT_INCOMPLETE, /** More data readableBytes to write. */
	 TRANSMIT_SOFT_ERROR, /** Can't write any more right now. */
	 TRANSMIT_HARD_ERROR  /** Can't write (c->state is set to conn_closing) */
}
