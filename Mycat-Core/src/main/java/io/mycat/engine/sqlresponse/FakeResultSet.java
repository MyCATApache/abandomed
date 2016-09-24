package io.mycat.engine.sqlresponse;

import java.io.IOException;
import java.nio.ByteBuffer;

import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.EOFPacket;
import io.mycat.mysql.packet.FieldPacket;
import io.mycat.mysql.packet.ResultSetHeaderPacket;
import io.mycat.mysql.packet.RowDataPacket;

public final class FakeResultSet {
    public static void response(MySQLFrontConnection c) throws IOException {
    	ByteBuffer byteBuf= c.getWriteDataBuffer().beginWrite(1024*10);
        byte packedId = 1;
        ResultSetHeaderPacket rshp = new ResultSetHeaderPacket();
        rshp.fieldCount = 3;
        rshp.packetId = packedId++;
        rshp.write(byteBuf,rshp.calcPacketSize());
        

        for (int i = 0; i < rshp.fieldCount; i++) {
            FieldPacket fieldPk = new FieldPacket();
            fieldPk.db = "fake_db".getBytes();
            fieldPk.table = "fake_table".getBytes();
            fieldPk.name = "fake_field1".getBytes();
            fieldPk.length = 255;
            fieldPk.type = 0x0F;
            if (i == 0) {
                // PK
                fieldPk.flags = 0x0002;
            }
            fieldPk.packetId = packedId++;
            fieldPk.write(byteBuf,fieldPk.calcPacketSize());
        }
        EOFPacket eof = new EOFPacket();
        eof.packetId = packedId++;
        eof.write(byteBuf,eof.calcPacketSize());

        for (int i = 0; i < 10; i++) {
            RowDataPacket rdp = new RowDataPacket(rshp.fieldCount);
            for (int j = 0; j < rshp.fieldCount; j++) {
                rdp.add(("fake_data_row" + i + "_col" + j).getBytes());
            }
            rdp.packetId = packedId++;
            rdp.write(byteBuf,rdp.calcPacketSize());
        }

        eof.packetId = packedId++;
        eof.write(byteBuf,eof.calcPacketSize());
        c.getWriteDataBuffer().endWrite(byteBuf);
        c.enableWrite(true);
    }
}
