/*
 * Copyright (c) 2016, OpenCloudDB/MyCAT and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software;Designed and Developed mainly by many Chinese
 * opensource volunteers. you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 2 only, as published by the
 * Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Any questions about this component can be directed to it's project Web address
 * https://code.google.com/p/opencloudb/.
 *
 */
package io.mycat.backend;

import java.io.IOException;

import io.mycat.buffer.MycatByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.SQLEngineCtx;
import io.mycat.engine.dataChannel.TransferDirection;
import io.mycat.engine.dataChannel.TransferMode;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.MySQLConnection;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.ConnectionException;
import io.mycat.net2.NIOHandler;
import io.mycat.net2.states.ParseCmdState;
import io.mycat.net2.states.ReadWaitingState;

/**
 * backend mysql NIO handler (only one for all backend mysql connections)
 *
 * @author wuzhihui
 */
public class MySQLBackendConnectionHandler implements NIOHandler<MySQLBackendConnection> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLBackendConnectionHandler.class);

    @Override
    public void onConnected(MySQLBackendConnection con) throws IOException {
    }

    @Override
    public void handleReadEvent(MySQLBackendConnection con) throws IOException {
        MycatByteBuffer dataBuffer = con.getDataBuffer();
        int offset = con.getCurrentPacketStartPos();
        int length;
        int limit = dataBuffer.writeIndex();

        /*
         * 判断透传逻辑.
         * 1.先判断当前是否有透传一半的报文,如果有,
         */
        
        // 循环收到的报文处理
        while (true) {
        	/*
        	 * 在前端连接认证完成,向后端发送查询命令时,如果后端连接还没有创建,后端连接完成认证后.
        	 * 需要把前端查询命令 select @@version_comment limit 1  透传给后端.
        	 * 在这种情况下,当前连接状态 不再是 命令解析状态.
        	 * 在这里需要判断一下,如果不再是命令解析状态,退出命令解析状态.
        	 * 
        	 * 透传发生时,后端状态发生改变,应该从状态机进入.这里退出.
        	 * 
        	 */
        	if((con.getNextNetworkState()!=null&&!ParseCmdState.INSTANCE.equals(con.getNextNetworkState()))
        			||!ParseCmdState.INSTANCE.equals(con.getNetworkState())){ 
        		return ;
        	}
        	
        	/*
        	 * 只有在进行透传时,透传标记才设置为 true, 循环处理缓冲区中的数据时,透传标记为false.
        	 * 整个结果集解析完成,最后一个数据包透传完成后,需要结束透传状态.
        	 */
         	if(!con.isPassthrough()){  //完成一次透传后,
         		
         		if(offset < 0){  //完成一次透传后,如果有半包,并且半包已经透传出去的情况.offset 会小于0
         			if(con.getCurrentPacketLength()< limit){
         				offset = con.getCurrentPacketLength() ;
         			}else{       // packet 太大,剩余部分仍然超过了缓冲区大小
         				con.setDirectTransferMode(TransferMode.LONG_HALF_PACKET); 
         				SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(con, true, false);
         				return;
         			}
         		}
         		
         		/* 在两种情况下可能解析不出包头
         	    * 1. 缓冲区最后几个字节长度不够,这种情况下当前包之前的数据透传.当前包不透传.  这种情况下无法进入后端命令解析状态机
         	    * 2. 缓冲区不够长 , 缓冲区不够长的情况,已经在连接状态机中控制,这里不再处理
         	    */
                if (!MySQLConnection.validateHeader(offset, limit)) {
             	   MySQLFrontConnection mySQLFrontConnection = con.getMySQLFrontConnection();
             	   if(mySQLFrontConnection==null){
             		   return;
             	   }
             	   
             	   /* 当前半包不透传 ,透传  0-readPos 之间的数据,  直接把 readbuffer 共享给前端  writebuffer 
             	    *  在 header 没有解析出来的情况下
             	    *  需要在透传完成后,设置 后端 连接 包状态    packtype = COM_SLEEP,start = 0,length = 0 
             	    * */
             	   con.setDirectTransferMode(TransferMode.SHORT_HALF_PACKET); 
             	   SQLEngineCtx.INSTANCE().getDataTransferChannel().transferToFront(con, true, false);
             	   return;
                }else{
                	
             	   length = MySQLConnection.getPacketLength(dataBuffer, offset);
             	   
             	   /* 判断是否半包  */
            		   if (length + offset > limit) {
                        
            		   /* 如果可以解析出header. 进入状态机 .
                 	    * 进入状态机后,会出现两种情况.
                 	    * 1. 包需要全解析.例如.OK/EOF/ERROR 包.  这种情况半包不透传.
                 	    * 2. 包不需要全解析. 数据全部透传.修改当前   解析包的开始和结束位置.
                 	    * 
                 	    * 以上两种情况 都会触发数据透传事件.
                 	    */ 			   
                        if (offset + MySQLConnection.msyql_packetHeaderSize <= limit) {
                            LOGGER.debug("backend receive not whole packet!length={},offset={},limit={}", length, offset, limit);
                            byte packetType = dataBuffer.getByte(offset + MySQLConnection.msyql_packetHeaderSize);
                            int pkgStartPos = offset;
                            offset += length;
                            con.setCurrentPacketLength(length);
                            con.setCurrentPacketStartPos(pkgStartPos);
                            con.setCurrentPacketType(packetType);
                            con.setDirectTransferMode(TransferMode.LONG_HALF_PACKET);  //半包透传
                            //传true表示，需要进行一次透传
                            con.driveState(true);
                            return;
                        }
                    } else {
                        //完整的包
                        byte packetType = dataBuffer.getByte(offset + MySQLConnection.msyql_packetHeaderSize);
                        int pkgStartPos = offset;
                        LOGGER.info("received pkg ,pkgStartPos "+pkgStartPos+",offset,"+offset+",length " + length + " type " + packetType + " cur total length " + limit);
                        offset += length;
                        con.setCurrentPacketLength(offset);
                        con.setCurrentPacketStartPos(pkgStartPos);
                        con.setCurrentPacketType(packetType);
                        con.setDirectTransferMode(TransferMode.COMPLETE_PACKET);  //整包透传
                        con.driveState(false);
                    }
                }
        	}else{
        	   
        	   /* 在两种情况下可能解析不出包头
        	    * 1. 缓冲区最后几个字节长度不够,这种情况下当前包之前的数据透传.当前包不透传.  这种情况下无法进入后端命令解析状态机
        	    * 2. 缓冲区不够长 , 缓冲区不够长的情况,已经在连接状态机中控制,这里不再处理
        	    */
               if (!MySQLConnection.validateHeader(offset, limit)) {
            	   con.setNextNetworkState(ReadWaitingState.INSTANCE);
            	   return;
               }
               
        	   length = MySQLConnection.getPacketLength(dataBuffer, offset);
        	   /* 判断是否半包  */
       		   if (length + offset > limit) {
       			   con.setNextNetworkState(ReadWaitingState.INSTANCE);
       			   return;
               } else {
                   //完整的包
                   byte packetType = dataBuffer.getByte(offset + MySQLConnection.msyql_packetHeaderSize);
                   int pkgStartPos = offset;
                   LOGGER.info("received pkg ,pkgStartPos "+pkgStartPos+",offset,"+offset+",length " + length + " type " + packetType + " cur total length " + limit);
                   offset += length;
                   con.setCurrentPacketLength(length);
                   con.setCurrentPacketStartPos(pkgStartPos);
                   con.setCurrentPacketType(packetType);
                   con.setDirectTransferMode(TransferMode.COMPLETE_PACKET);  //整包透传
                   con.driveState(false);
               }
        	}      
        }
    }


    @Override
    public void onClosed(MySQLBackendConnection source, String reason) {
        source.getUserCallback().connectionClose(source, reason);
    }


    @Override
    public void onConnectFailed(MySQLBackendConnection con, ConnectionException e) {
        con.getUserCallback().connectionError(e, con);

    }

    @Override
    public void onHandlerError(MySQLBackendConnection con, Exception e) {
        con.getUserCallback().handlerError(e, con);
    }

}
