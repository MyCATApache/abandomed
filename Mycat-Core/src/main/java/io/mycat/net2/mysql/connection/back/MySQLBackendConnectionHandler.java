package io.mycat.net2.mysql.connection.back;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.Connection;
import io.mycat.net2.ConnectionException;
import io.mycat.net2.NIOHandler;
import io.mycat.net2.mysql.connection.MySQLConnection;
import io.mycat.net2.mysql.definination.Capabilities;
import io.mycat.net2.mysql.packet.EOFPacket;
import io.mycat.net2.mysql.packet.ErrorPacket;
import io.mycat.net2.mysql.packet.HandshakePacket;
import io.mycat.net2.mysql.packet.MySQLPacket;
import io.mycat.net2.mysql.packet.OkPacket;
import io.mycat.net2.mysql.packet.Reply323Packet;
import io.mycat.net2.mysql.packet.util.CharsetUtil;
import io.mycat.net2.mysql.packet.util.SecurityUtil;

public class MySQLBackendConnectionHandler implements NIOHandler<MySQLBackendConnection> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLBackendConnectionHandler.class);

    private static final int RESULT_STATUS_INIT = 0;
    private static final int RESULT_STATUS_HEADER = 1;
    private static final int RESULT_STATUS_FIELD_EOF = 2;

    @Override
    public void onConnected(MySQLBackendConnection con) throws IOException {

        // con.asynRead();
    }

    @Override
    public void handleReadEvent(MySQLBackendConnection con) throws IOException{
    	ConDataBuffer dataBuffer=con.getReadDataBuffer();
        int offset = dataBuffer.readPos(), length = 0, limit = dataBuffer.writingPos();
   	// 循环收到的报文处理
		while(true)
		{
			if(!MySQLConnection.validateHeader(offset, limit))
			{
				return; 
			}
			length = MySQLConnection.getPacketLength(dataBuffer, offset);
			if(length+offset>limit)
			{
				 LOGGER.info("Not whole package :length "+length+" cur total length "+limit);
				return;
			}
			// 解析报文类型
			byte field = dataBuffer.getByte(offset+MySQLConnection.msyql_packetHeaderSize);
			byte packetType =MySQLConnection.getPacketType(field,  con);
			int pkgStartPos=offset;
			offset += length;
			dataBuffer.seReadingPos(offset);
			
			LOGGER.info("received pkg ,length "+length+" type "+field+" cur total length "+limit);
			
			switch (con.getState()) {
	        case connecting: {
	            LOGGER.debug("backend handle login", con);
	            handleLogin(con, dataBuffer, field,pkgStartPos,length);
	           
	            break;
	        }
	        case connected: {
	            try {
	                LOGGER.debug("backend handle business", con);
	                handleData(con,dataBuffer, field,pkgStartPos,length);
	            } catch (Exception e) {
	                LOGGER.warn("caught err of con " + con, e);
	            }
	            break;
	        }

	        default:
	            LOGGER.warn("not handled connecton state  err " + con.getState() + " for con " + con);
	            break;

	        }
			
		}
		
    }

   

    @Override
    public void onConnectFailed(MySQLBackendConnection source, Throwable e) {

    }

    private void handleLogin(MySQLBackendConnection source, ConDataBuffer dataBuffer,byte packageType,int pkgStartPos,int pkgLen) throws IOException {
        try {
            switch (packageType) {
            case OkPacket.FIELD_COUNT:
                HandshakePacket packet = source.getHandshake();
                if (packet == null) {
                	ByteBuffer byteBuff=dataBuffer.getBytes(pkgStartPos, pkgLen);
                    processHandShakePacket(source,byteBuff);
                    // 发送认证数据包
                    source.authenticate();
                    break;
                }
                // 处理认证结果
                source.setAuthenticated(true);
                source.setConnectedStatus(MySQLBackendConnection.IDLE_STATUS);
                source.setState(Connection.State.connected);
                boolean clientCompress = Capabilities.CLIENT_COMPRESS == (Capabilities.CLIENT_COMPRESS
                        & packet.serverCapabilities);
                boolean usingCompress = false;

                if (clientCompress && usingCompress) {
                    // source.setSupportCompress(true);
                }

                // if (source.getRespHandler() != null) {
                // source.getRespHandler().connectionAcquired(source);
                // }

                break;
            case ErrorPacket.FIELD_COUNT:
            	ByteBuffer byteBuff=dataBuffer.getBytes(pkgStartPos, pkgLen);
                ErrorPacket err = new ErrorPacket();
                err.read(byteBuff);
                String errMsg = new String(err.message);
                LOGGER.warn("can't connect to mysql server ,errmsg:" + errMsg + " " + source);
                // source.close(errMsg);
                throw new ConnectionException(err.errno, errMsg);

            case EOFPacket.FIELD_COUNT:
            	byte pkgId=dataBuffer.getByte(pkgStartPos+3);
                auth323(source, pkgId);
                break;
            default:
                packet = source.getHandshake();
                if (packet == null) {
                	byteBuff=dataBuffer.getBytes(pkgStartPos, pkgLen);
                    processHandShakePacket(source, byteBuff);
                    // 发送认证数据包
                    source.authenticate();
                    break;
                } else {
                    throw new RuntimeException("Unknown Packet!");
                }

            }

        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public void onClosed(MySQLBackendConnection source, String reason) {

    }

    

    public void connectionError(Throwable e) {

    }

    protected void handleData(final MySQLBackendConnection source, ConDataBuffer dataBuffer,byte packageType,int pkgStartPos,int pkgLen) throws IOException {
        LOGGER.debug("handle data business.");
        
        // TODO MOCK
        source.getResponseHandler().handleResponse( source,dataBuffer,packageType,pkgStartPos,pkgLen);
        if (source.getConnectedStatus() == MySQLBackendConnection.IDLE_STATUS && (packageType == MySQLPacket.EOF_PACKET
                || packageType == MySQLPacket.ERROR_PACKET || packageType == MySQLPacket.OK_PACKET)) {
            source.getResponseHandler().finishResponse(source);
        }
    }

    private void processHandShakePacket(MySQLBackendConnection source, final ByteBuffer byteBuf) {
        // 设置握手数据包
        HandshakePacket packet = new HandshakePacket();
        packet.read(byteBuf);
        source.setHandshake(packet);
       // System.out.println(packet.threadId+" "+new String(packet.serverVersion)+" "+packet.packetLength+" "+packet.protocolVersion+" "+packet.serverCapabilities+" "+packet.serverCharsetIndex+" "+packet.serverStatus+" "+packet.serverStatus+ " "+Arrays.toString(packet.seed)+ " "+Arrays.toString(packet.restOfScrambleBuff));
               // source.setThreadId(packet.threadId);

        // 设置字符集编码
        int charsetIndex = (packet.serverCharsetIndex & 0xff);
        String charset = CharsetUtil.getCharset(charsetIndex);
        // if (charset != null) {
        // source.setCharset(charset);
        // } else {
        // LOGGER.warn("Unknown charsetIndex:" + charsetIndex);
        // throw new RuntimeException("Unknown charsetIndex:" + charsetIndex);
        // }
    }

    private void auth323(MySQLBackendConnection source, byte packetId) {
        // 发送323响应认证数据包
        Reply323Packet r323 = new Reply323Packet();
        r323.packetId = ++packetId;
        String pass = source.getPassword();
        if (pass != null && pass.length() > 0) {
            byte[] seed = source.getHandshake().seed;
            r323.seed = SecurityUtil.scramble323(pass, new String(seed)).getBytes();
        }
        source.writeMsqlPackage(r323);
    }

}
