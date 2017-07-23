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
	 * 传输处理
	 * @param in
	 * @param isTransferLastPacket  是否传输最后一个报文
	 * @param transferFinish 当前透传方向是否完成
	 * @param isAllFinish    整个命令交互,是否全部透传完成, 命令可能经过多次交互后，才完成，例如：load data local 命令，分两个阶段完成
	 * @throws IOException
	 */
	public void transfer(MySQLConnection in,boolean isTransferLastPacket,boolean transferFinish,boolean isAllFinish)throws IOException;

}
