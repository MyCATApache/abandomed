package io.mycat.mysql.state;

import java.io.IOException;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.backend.MySQLBackendConnectionFactory;
import io.mycat.backend.MySQLDataSource;
import io.mycat.backend.MySQLReplicatSet;
import io.mycat.beans.DNBean;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.engine.dataChannel.TransferMode;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.MySQLConnection;

/**
 * 状态基类，封装前后端状态处理分发
 *
 * @author ynfeng
 */
public abstract class AbstractMysqlConnectionState implements MysqlConnectionState {
    @Override
    public boolean handle(MySQLConnection mySQLConnection, Object attachment) throws IOException{
        if (mySQLConnection instanceof MySQLBackendConnection) {
            return backendHandle((MySQLBackendConnection) mySQLConnection, attachment);
        } else if (mySQLConnection instanceof MySQLFrontConnection) {
        	return frontendHandle((MySQLFrontConnection) mySQLConnection, attachment);
        } else {
            throw new IllegalArgumentException("No such type of MySQLConnection:" + mySQLConnection.getClass());
        }
    }
    
    /**
     * 处理报文头
     * @param conn
     * @return true 报文头解析成功，false 报文头没有解析出来
     * @throws IOException
     */
    protected void processPacketHeader(MySQLConnection conn)throws IOException{
//    	conn.clearCurrentPacket();
    	final MycatByteBuffer buffer = conn.getDataBuffer();
        int offset = buffer.writeLimit();
    	int limit = buffer.writeIndex();
    	int currentStartPos = conn.getCurrentPacketStartPos();
    	
    	if(currentStartPos < 0){  //完成一次透传后,如果有半包,并且半包已经透传出去的情况.offset 会小于0
    		int currentPacketLength = conn.getCurrentPacketLength();
 			if(currentPacketLength < limit){
 				offset = conn.getCurrentPacketLength() ;  //当前半包剩余部分不做解析,直接开始解析下一个包
 				buffer.writeLimit(offset);
 			}else{       // packet 太大,剩余部分仍然超过了缓冲区大小
 				conn.setDirectTransferMode(TransferMode.LONG_HALF_PACKET); 
 				return;
 			}
 		}

    	if (!MySQLConnection.validateHeader(offset, limit)) {
    		conn.setDirectTransferMode(TransferMode.SHORT_HALF_PACKET);  //半包透传
     	   return;
        }

        int length = MySQLConnection.getPacketLength(buffer, offset);
        final byte packetType = buffer.getByte(offset + MySQLConnection.msyql_packetHeaderSize);
        final int pkgStartPos = offset;

        if(length + offset > limit){
        	conn.setDirectTransferMode(TransferMode.LONG_HALF_PACKET);  //半包透传
        }else{
        	conn.setDirectTransferMode(TransferMode.COMPLETE_PACKET);  //完整的包
        }
        
        offset += length;
        conn.setCurrentPacketLength(offset);
        conn.setCurrentPacketStartPos(pkgStartPos);
        conn.setCurrentPacketType(packetType);
    }
    
    protected MySQLBackendConnection getBackendFrontConnection(MySQLFrontConnection mySQLFrontConnection) throws IOException {
//      // 直接透传（默认）获取连接池
//      MySQLBackendConnection existCon = null;
//      UserSession session = mySQLFrontConnection.getSession();
//      ArrayList<MySQLBackendConnection> allBackCons = session.getBackendCons();
//      if (!allBackCons.isEmpty()) {
//          existCon = allBackCons.get(0);
//      }
//      if (existCon == null || existCon.isClosed()) {
//          if (existCon != null) {
//              session.removeBackCon(existCon);
//          }
          final DNBean dnBean = mySQLFrontConnection.getMycatSchema().getDefaultDN();
          final String replica = dnBean.getMysqlReplica();
          final SQLEngineCtx ctx = SQLEngineCtx.INSTANCE();
          final MySQLReplicatSet repSet = ctx.getMySQLReplicatSet(replica);
          final MySQLDataSource datas = repSet.getCurWriteDH();
//
//          /**
//           * 如果该sql对应后端db，没有连接池，则创建连接池部分
//           */
//      MySQLBackendConnection existCon = datas.getConnection(mySQLFrontConnection.getReactor(), dnBean.getDatabase(), true, mySQLFrontConnection, null);
//      }
      MySQLBackendConnection con = new MySQLBackendConnectionFactory().make(datas, mySQLFrontConnection.getReactor(), dnBean.getDatabase(), mySQLFrontConnection, null);
              mySQLFrontConnection.setBackendConnection(con);
      return con;
  }

    abstract protected boolean frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment)throws IOException;

    abstract protected boolean backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment)throws IOException;
}
