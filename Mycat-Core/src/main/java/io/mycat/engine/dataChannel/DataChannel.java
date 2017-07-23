package io.mycat.engine.dataChannel;

import java.io.IOException;

import io.mycat.mysql.MySQLConnection;

/**
 * 数据传输通道
 * @author yanjunli
 *
 */
public interface DataChannel {
	
	public void addToFrontHandler(DataHandler handler);
	
	public void addToBackendHandler(DataHandler handler);
	
	public void removeToFrontHandler(DataHandler handler);
	
	public void removeToBackendHandler(DataHandler handler);
		
	/**
	 * 透传处理
	 * @param in   透传数据输入连接
	 * @param out  透传数据输出连接
	 * @param shareDataBuffer  共享buffer
	 * @param mode         传输模式
	 * @param isTransferLastPacket 最后一个包是否传输
	 * @param isAllFinish  是否全部透传完成
	 */
	public void transferToFront(MySQLConnection in,boolean isTransferLastPacket,boolean transferFinish,boolean isAllFinish)throws IOException;
	
	/**
	 * 透传处理
	 * @param in   透传数据输入连接
	 * @param out  透传数据输出连接
	 * @param shareDataBuffer  共享buffer
	 * @param mode         传输模式
	 * @param isTransferLastPacket 最后一个包是否传输
	 * @param isAllFinish  是否全部透传完成
	 */
	public void transferToBackend(MySQLConnection in,boolean isTransferLastPacket,boolean transferFinish,boolean isAllFinish)throws IOException;

}
