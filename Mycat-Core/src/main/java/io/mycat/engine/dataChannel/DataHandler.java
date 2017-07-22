package io.mycat.engine.dataChannel;

import java.io.IOException;

import io.mycat.mysql.MySQLConnection;

/**
 * 数据处理接口
 * @author yanjunli
 *
 */
public interface DataHandler {
	
	/**
	 * 透传处理
	 * @param in   透传数据输入连接
	 * @param out  透传数据输出连接
	 * @param shareDataBuffer  共享buffer
	 * @param mode         传输模式
	 * @param isTransferLastPacket 最后一个包是否传输
	 * @param isAllFinish  是否全部透传完成
	 */
	public void transfer(MySQLConnection in,boolean isTransferLastPacket,boolean isAllFinish)throws IOException;

}
