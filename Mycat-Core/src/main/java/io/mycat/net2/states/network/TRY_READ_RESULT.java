package io.mycat.net2.states.network;

public enum TRY_READ_RESULT {        
	/** 数据接收完毕  */
    READ_DATA_RECEIVED,    
    /** 没有接收到数据  */
    READ_NO_DATA_RECEIVED, 
    /** an error occurred (on the socket) (or client closed connection) */
    READ_ERROR,        
    /** failed to allocate more memory */
    READ_MEMORY_ERROR,
}
