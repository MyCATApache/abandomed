package io.mycat.engine.dataChannel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.mycat.engine.dataChannel.impl.PassThrouthtoBackendDataHandler;
import io.mycat.engine.dataChannel.impl.PassThrouthtoFrontDataHandler;
import io.mycat.mysql.MySQLConnection;

public class DefaultDataTransferChannel implements DataChannel {
	
	private List<DataHandler> toFronthandlers = new ArrayList<>();
	
	private List<DataHandler> toBackendhandlers = new ArrayList<>();
	
	{
		toFronthandlers.add(PassThrouthtoFrontDataHandler.INSTANCE);
		toBackendhandlers.add(PassThrouthtoBackendDataHandler.INSTANCE);
	}

	@Override
	public void transferToFront(MySQLConnection in, boolean isTransferLastPacket,boolean transferFinish, boolean isAllFinish) throws IOException {
		for(DataHandler handler:toFronthandlers){
			handler.transfer(in,isTransferLastPacket,transferFinish, isAllFinish);
		}
	}

	@Override
	public void transferToBackend(MySQLConnection in,
			boolean isTransferLastPacket,boolean transferFinish, boolean isAllFinish) throws IOException {
		for(DataHandler handler:toBackendhandlers){
			handler.transfer(in, isTransferLastPacket,transferFinish, isAllFinish);
		}
	}


	@Override
	public void addToFrontHandler(DataHandler handler) {
		toFronthandlers.add(handler);
	}

	@Override
	public void addToBackendHandler(DataHandler handler) {
		toBackendhandlers.add(handler);
	}

	@Override
	public void removeToFrontHandler(DataHandler handler) {
		toFronthandlers.remove(handler);
	}

	@Override
	public void removeToBackendHandler(DataHandler handler) {
		toBackendhandlers.remove(handler);
	}

}
