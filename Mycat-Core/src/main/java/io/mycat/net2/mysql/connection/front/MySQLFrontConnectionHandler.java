package io.mycat.net2.mysql.connection.front;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.net2.BackendConnection;
import io.mycat.net2.ByteBufferArray;
import io.mycat.net2.ConDataBuffer;
import io.mycat.net2.Connection;
import io.mycat.net2.NIOHandler;
import io.mycat.net2.mysql.MockMySQLServer;
import io.mycat.net2.mysql.connection.MySQLConnection;
import io.mycat.net2.mysql.connection.back.MySQLBackendConnection;
import io.mycat.net2.mysql.definination.ErrorCode;
import io.mycat.net2.mysql.handler.ResponseHandler;
import io.mycat.net2.mysql.handler.SelectHandler;
import io.mycat.net2.mysql.packet.AuthPacket;
import io.mycat.net2.mysql.packet.MySQLMessage;
import io.mycat.net2.mysql.packet.MySQLPacket;
import io.mycat.net2.mysql.parser.ServerParse;

public class MySQLFrontConnectionHandler implements NIOHandler<MySQLFrontendConnection> {
    private static final byte[] AUTH_OK = new byte[] { 7, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0 };
    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLFrontConnectionHandler.class);

    @Override
    public void onConnected(MySQLFrontendConnection con) throws IOException {
        LOGGER.debug("onConnected", con);
        con.sendAuthPackge();
    }

    @Override
    public void onConnectFailed(MySQLFrontendConnection con, Throwable e) {
        LOGGER.debug("onConnectFailed", con);
    }

    @Override
    public void onClosed(MySQLFrontendConnection con, String reason) {
        LOGGER.debug("onClosed", con);
    }

    @Override
    public void handleReadEvent(final MySQLFrontendConnection fronCon) throws IOException{
    	ConDataBuffer dataBuffer=fronCon.getReadDataBuffer();
        LOGGER.debug("handle", fronCon);
        int offset = dataBuffer.readPos(), length = 0, limit = dataBuffer.writingPos();
        
        byte packetType=0;
   	// 读取到了包头和长度
		//是否讀完一個報文
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
			packetType =MySQLConnection.getPacketType(field,  fronCon);
			int pkgStartPos=offset;
			offset += length;
			dataBuffer.seReadingPos(offset);
			
			LOGGER.info("received pkg ,offset: "+pkgStartPos+" length: "+length+" type: "+packetType+" cur total length: "+limit);
			int connectedStatus = fronCon.getConnectedStatus();
	        switch (connectedStatus) {
	        case MySQLFrontendConnection.LOGIN_STATUS:
	            LOGGER.debug("Handle Login msg");
	            doLogin(fronCon, dataBuffer,packetType,pkgStartPos,length);
	            break;
	        case MySQLFrontendConnection.CMD_RECIEVED_STATUS:
	            LOGGER.debug("Handle Query msg");
	            doQuery(fronCon, dataBuffer,packetType,pkgStartPos,length);
	            break;
	        case MySQLFrontendConnection.QUIT_STATUS:
	            LOGGER.info("Client quit.");
	            fronCon.close("quit packet");
	            break;
	         default:
	        	 LOGGER.warn("not processed request pkg .");
	        }
		}
		
    }

    private void doQuery(MySQLFrontendConnection frontCon,ConDataBuffer dataBuffer,byte packageType,int pkgStartPos,int pkgLen) throws IOException {
        // 取得语句
    	ByteBuffer byteBuff=dataBuffer.getBytes(pkgStartPos, pkgLen);
        MySQLMessage mm = new MySQLMessage(byteBuff);
        mm.position(5);
        String sql = null;
        sql = mm.readString();
        LOGGER.debug(sql);
        // 执行查询
        int rs = ServerParse.parse(sql);
        int sqlType = rs & 0xff;

        // 检查当前使用的DB
        // String db = "";
        // if (db == null
        // && sqlType!=ServerParse.USE
        // && sqlType!=ServerParse.HELP
        // && sqlType!=ServerParse.SET
        // && sqlType!=ServerParse.SHOW
        // && sqlType!=ServerParse.KILL
        // && sqlType!=ServerParse.KILL_QUERY
        // && sqlType!=ServerParse.MYSQL_COMMENT
        // && sqlType!=ServerParse.MYSQL_CMD_COMMENT) {
        // writeErrMessage(ErrorCode.ERR_BAD_LOGICDB, "No MyCAT Database
        // selected");
        // return;
        // }

        switch (sqlType) {
        case ServerParse.EXPLAIN:
            LOGGER.debug("EXPLAIN");
            break;
        case ServerParse.SET:
            LOGGER.debug("SET");
            break;
        case ServerParse.SHOW:
            LOGGER.debug("SHOW");
            break;
        case ServerParse.SELECT:
            LOGGER.debug("SELECT");
            // SelectHandler.handle(sql, con, rs >>> 8);
            break;
        case ServerParse.START:
            LOGGER.debug("START");
            break;
        case ServerParse.BEGIN:
            LOGGER.debug("BEGIN");
            break;
        case ServerParse.SAVEPOINT:
            LOGGER.debug("SAVEPOINT");
            break;
        case ServerParse.KILL:
            LOGGER.debug("KILL");
            break;
        case ServerParse.KILL_QUERY:
            LOGGER.debug("KILL_QUERY");
            break;
        case ServerParse.USE:
            LOGGER.debug("USE");
            break;
        case ServerParse.COMMIT:
            LOGGER.debug("COMMIT");
            break;
        case ServerParse.ROLLBACK:
            LOGGER.debug("ROLLBACK");
            break;
        case ServerParse.HELP:
            LOGGER.debug("HELP");
            break;
        case ServerParse.MYSQL_CMD_COMMENT:
            LOGGER.debug("MYSQL_CMD_COMMENT");
            break;
        case ServerParse.MYSQL_COMMENT:
            LOGGER.debug("MYSQL_COMMENT");
            break;
        case ServerParse.LOAD_DATA_INFILE_SQL:
            LOGGER.debug("LOAD_DATA_INFILE_SQL");
            break;
        default:
            LOGGER.debug("DEFAULT");
        }
        if (sql.equals("select @@version_comment limit 1")) {
            SelectHandler.handle(sql, frontCon, rs >>> 8);
            return;
        }
        // TODO MOCK
        try {
            BackendConnection backCon = MockMySQLServer.mockDBNodes.get(MockMySQLServer.MOCK_HOSTNAME)
                    .getConnection(MockMySQLServer.MOCK_SCHEMA, true, null);
            backCon.setResponseHandler(new ResponseHandler() {
                @Override
                public void connectionError(Throwable e, BackendConnection conn) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void connectionClose(BackendConnection conn, String reason) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void connectionAcquired(BackendConnection conn) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void handleResponse(BackendConnection conn,ConDataBuffer dataBuffer,byte packageType,int pkgStartPos,int pkgLen) throws IOException {
                    // Mock 直接转发给前端
                    // IMPORTANT!! 处理粘包
                	frontCon.getWriteDataBuffer().putBytes(dataBuffer.getBytes(pkgStartPos, pkgLen));
                	frontCon.enableWrite(false);
                }

                @Override
                public void finishResponse(BackendConnection conn) {
                   
                    conn.release();
                }
            });
            // 后面需要加入handler
            MySQLBackendConnection mysqlBackCon = (MySQLBackendConnection) backCon;
            mysqlBackCon.getWriteDataBuffer().putBytes(dataBuffer.getBytes(pkgStartPos, pkgLen));
            mysqlBackCon.enableWrite(false);
        } catch (Exception e) {
            LOGGER.error("Error", e);
        }
    }

  
    private void doLogin(MySQLFrontendConnection con, ConDataBuffer dataBuffer,byte packageType,int pkgStartPos,int pkgLen) throws IOException {
        // check quit packet
        if (packageType == MySQLPacket.QUIT_PACKET) {
            con.close("quit packet");
            return;
        }
        ByteBuffer byteBuff=dataBuffer.getBytes(pkgStartPos, pkgLen);
        AuthPacket auth = new AuthPacket();
        auth.read(byteBuff);

        // Fake check user
        LOGGER.debug("Check user name. " + auth.user);
        if (!auth.user.equals("root")) {
            LOGGER.debug("User name error. " + auth.user);
            failure(con, ErrorCode.ER_ACCESS_DENIED_ERROR, "Access denied for user '" + auth.user + "'");
            return;
        }

        // Fake check password
        LOGGER.debug("Check user password. " + new String(auth.password));

        // check schema
        LOGGER.debug("Check database. " + auth.database);
        success(con, auth);
    }

    protected void failure(MySQLFrontendConnection source, int errno, String info) {
        LOGGER.error(source.toString() + info);
        source.writeErrMessage(errno, info);
    }

    protected void success(MySQLFrontendConnection con, AuthPacket auth) throws IOException {
        LOGGER.debug("Login success.");
        // con.setAuthenticated(true);
        // con.setUser(auth.user);
        // con.setSchema(auth.database);
        // con.setCharsetIndex(auth.charsetIndex);

        con.write(AUTH_OK);
        con.setState(Connection.State.connected);
        con.setConnectedStatus(MySQLFrontendConnection.IDLE_STATUS);
    }

}
