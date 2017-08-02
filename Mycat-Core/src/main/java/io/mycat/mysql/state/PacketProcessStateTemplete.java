package io.mycat.mysql.state;

import io.mycat.buffer.MycatByteBuffer;
import io.mycat.buffer.PacketDescriptor;
import io.mycat.buffer.PacketIterator;
import io.mycat.machine.StateMachine;
import io.mycat.net2.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by ynfeng on 2017/7/31.
 * <p>
 * 当状态需要处理收到的包时可继承此类，方便处理.如果不继承此类需要自行处理短半包，长半包和全包的情况
 */
public abstract class PacketProcessStateTemplete extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketProcessStateTemplete.class);
    private boolean interrupted = false;


    @Override
    public boolean handle(StateMachine context, Connection connection, Object attachment) throws IOException {
        LOGGER.debug(this.getClass().getSimpleName() + " handle packet = " + connection.getDataBuffer());
        MycatByteBuffer buffer = connection.getDataBuffer();
        PacketIterator it = buffer.packetIterator();
        boolean result = false;
        while (it.hasPacket()) {
            long pakcetDescriptor = it.nextPacket();
            LOGGER.debug("Iterate packet {}", pakcetDescriptor);
            int packetStartPos = PacketDescriptor.getPacketStartPos(pakcetDescriptor);
            int packetLen = PacketDescriptor.getPacketLen(pakcetDescriptor);
            byte type = PacketDescriptor.getCommandType(pakcetDescriptor);
            PacketDescriptor.PacketType packetType = PacketDescriptor.getPacketType(pakcetDescriptor);
            switch (packetType) {
                case FULL:
                    result = handleFullPacket(connection, attachment, packetStartPos, packetLen, type);
                    break;
                case LONG_HALF:
                    result = handleLongHalfPacket(connection, attachment, packetStartPos, packetLen, type);
                    interruptIterate();
                    break;
                case SHORT_HALF:
                    result = handleShortHalfPacket(connection, attachment, packetStartPos);
                    interruptIterate();
                    break;
            }
            if (interrupted) {
                interrupted = false;
                return result;
            }
        }
        return result;
    }

    /**
     * 用于强行停止迭代过程
     * 注意：因为读写缓冲区共享的原因，如果在迭代过程中向缓冲区写入了包，
     * 则必须要调用此方法中止迭代过程，否则会一直迭代新写入的包。
     * 另外当前包为两种半包的情况下也会一直迭代
     * //TODO 能否有更清晰的处理方式？
     */
    public void interruptIterate() {
        this.interrupted = true;
    }

    public abstract boolean handleShortHalfPacket(Connection connection, Object attachment, int packetStartPos) throws IOException;

    public abstract boolean handleLongHalfPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException;

    public abstract boolean handleFullPacket(Connection connection, Object attachment, int packetStartPos, int packetLen, byte type) throws IOException;
}
