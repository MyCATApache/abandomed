package io.mycat.buffer;

/**
 * Created by ynfeng on 2017/7/27.
 */
public interface PacketIterator {

    /**
     * Returns {@code true} if there is a packet, {@code false} there is no more packet.
     */
    boolean hasPacket();

    /**
     * Get next packet.
     * If {@link #hasPacket()} return {@code false} this method return 0,{@code true} return a packet descriptor.
     * Packet descriptor is a 64-bit integer,the structure is as follows,
     * <pre>
     * +-------------------------------------------+----------------------------+-----------------------------------+----------------+
     * |                30 bits                    |           24 bits          |           8 bits                  |      2 bits    |
     * | The packet start position in buffer       | The length of packet       |         command type              |   packet type  |
     * +-------------------------------------------+----------------------------+-----------------------------------+----------------+
     * </pre>
     * A packet up to 16MB,24-bit is enough.
     * 2-bit packet type may be {@link PacketDescriptor.PacketType#LONG_HALF} or
     * {@link PacketDescriptor.PacketType#SHORT_HALF} or {@link PacketDescriptor.PacketType#FULL}
     * When packet type is {@link PacketDescriptor.PacketType#SHORT_HALF} the packet length and command type are meaningless.
     */
    long nextPacket();

    /**
     * Reset the internal state.
     */
    void reset();

    /**
     * fallback to previous packet.
     * if success return {@code true}
     */
    boolean fallback();
}
