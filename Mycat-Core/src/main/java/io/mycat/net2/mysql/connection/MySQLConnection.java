package io.mycat.net2.mysql.connection;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.Connection;
import io.mycat.net2.mysql.connection.back.MySQLBackendConnection;
import io.mycat.net2.mysql.connection.front.MySQLFrontendConnection;
import io.mycat.net2.mysql.packet.ErrorPacket;
import io.mycat.net2.mysql.packet.MySQLMessage;
import io.mycat.net2.mysql.packet.MySQLPacket;

public abstract class MySQLConnection extends Connection {

    protected MySQLConnectionStatus connectedStatus;

    protected byte[] seed;
    public final static int msyql_packetHeaderSize = 4;

	public final static int mysql_packetTypeSize = 1;

	public static final boolean validateHeader(long offset, long position) {
		return offset + msyql_packetHeaderSize + mysql_packetTypeSize <= position;
	}

	/**
	 * 获取报文长度
	 * 
	 * @param buffer
	 *			报文buffer
	 * @param offset
	 *			buffer解析位置偏移量
	 * @param position
	 *			buffer已读位置偏移量
	 * @return 报文长度(Header长度+内容长度)
	 */
	public static final int getPacketLength(ConDataBuffer buffer, int offset) {
		int length = buffer.getByte(offset) & 0xff;
		length |= (buffer.getByte(++offset) & 0xff) << 8;
		length |= (buffer.getByte(++offset) & 0xff) << 16;
		return length + msyql_packetHeaderSize;
	}

    public MySQLConnection(SocketChannel channel) {
        super(channel);
        // TODO Auto-generated constructor stub
    }

    public int getConnectedStatus() {
        return connectedStatus.getStatus();
    }

    public void setConnectedStatus(int connectedStatus) {
        this.connectedStatus.setStatus(connectedStatus);
    }

    public void setNextConnectedStatus(byte packetType) {
        this.connectedStatus.setNextStatus(packetType);
    }
    /**
	 * 获取报文类型(0-255)
	 * 
	 * @param buffer
	 *			报文buffer
	 * @param offset
	 *			buffer解析位置偏移量
	 * @param position
	 *			buffer已读位置偏移量
	 * @return 报文类型标识
	 */
	public static final byte getPacketType(byte field,	MySQLConnection con) {
		byte type = 0;
    	int connectedStatus = con.getConnectedStatus();
		// 目前基本上报文类型可以由data[4]一个字节确定，切mycat-nio中定义的报文类型枚举值与该字节值保持一致
		// 考虑到后面可能会根据报文类型处理部分业务并修改Connection中的状态变量，留出下方各个类型代码入口
		switch (con.getState()) {
		case connecting:
			// check quit packet
			if (field == MySQLPacket.COM_QUIT) {
				type = MySQLPacket.QUIT_PACKET;
			} else if (connectedStatus == MySQLFrontendConnection.LOGIN_STATUS) {
				// 区分开front和end的登录
				type = MySQLPacket.AUTH_PACKET;
				// Fake for unit test
				con.setState(Connection.State.connected);
				// Fake for unit test
			} else if (connectedStatus == MySQLBackendConnection.LOGIN_STATUS) {
				// 区分开front和end的登录
				type = handleLogin(field, con);
			}
			break;
		case connected:
			type = conStateCtrl(field, con);
			break;
		default:
			LOGGER.warn("not handled connecton state  err " + con.getState() + " for con " + con);
			break;
		}
		return type;
	}
	
	private static final byte conStateCtrl(byte field, MySQLConnection con) {
		byte type = 0;
		switch (field) {
		case MySQLPacket.OK_FIELD_COUNT:
			type = MySQLPacket.OK_PACKET;
			break;
		case MySQLPacket.ERROR_FIELD_COUNT:
			type = MySQLPacket.ERROR_PACKET;
			break;
		case MySQLPacket.EOF_FIELD_COUNT:
			type = MySQLPacket.EOF_PACKET;
			break;
		case MySQLPacket.COM_INIT_DB:
			type = MySQLPacket.COM_INIT_DB;
			break;
		case MySQLPacket.COM_QUERY:
			type = MySQLPacket.COM_QUERY;
			break;
		case MySQLPacket.COM_PING:
			type = MySQLPacket.COM_PING;
			break;
		case MySQLPacket.COM_QUIT:
			type = MySQLPacket.COM_QUIT;
			break;
		case MySQLPacket.COM_PROCESS_KILL:
			type = MySQLPacket.COM_PROCESS_KILL;
			break;
		case MySQLPacket.COM_STMT_PREPARE:
			type = MySQLPacket.COM_STMT_PREPARE;
			break;
		case MySQLPacket.COM_STMT_EXECUTE:
			type = MySQLPacket.COM_STMT_EXECUTE;
			break;
		case MySQLPacket.COM_STMT_CLOSE:
			type = MySQLPacket.COM_STMT_CLOSE;
			break;
		case MySQLPacket.COM_HEARTBEAT:
			type = MySQLPacket.COM_HEARTBEAT;
			break;
		default:
			// TODO
			break;
		}
		// 状态转移
		con.setNextConnectedStatus(type);
		return type;
	}

	private static final byte handleLogin(byte field, MySQLConnection con) {
		byte type = 0;
		switch (field) {
		case MySQLPacket.OK_FIELD_COUNT:
			type = MySQLPacket.OK_PACKET;
			break;
		case MySQLPacket.ERROR_FIELD_COUNT:
			type = MySQLPacket.ERROR_PACKET;
			break;
		case MySQLPacket.EOF_FIELD_COUNT:
			type = MySQLPacket.EOF_PACKET;
			break;
		default:
			// TODO
			break;
		}
		return type;
	}

  
	public void writeMsqlPackage(MySQLPacket pkg)
	{
		int pkgSize=pkg.calcPacketSize();
		ByteBuffer buf=getWriteDataBuffer().beginWrite(pkgSize+MySQLPacket.packetHeaderSize);
		pkg.write(buf,pkgSize);
		getWriteDataBuffer().endWrite(buf);
		this.enableWrite(true);
	}
    
    public void writeErrMessage(int errno, String info) {
        ErrorPacket err = new ErrorPacket();
        err.packetId = 1;
        err.errno = errno;
        err.message = info.getBytes();
       this.writeMsqlPackage(err);
    }
}
