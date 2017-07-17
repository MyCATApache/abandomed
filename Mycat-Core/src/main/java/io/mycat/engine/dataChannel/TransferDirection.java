package io.mycat.engine.dataChannel;

/**
 * 传输方向
 * @author yanjunli
 *
 */
public enum TransferDirection {
	
	TOFRONT,       // 像前端传输
	TOBACKEND,     // 像后端传输
	NONE         // 当前状态没有透传
}
