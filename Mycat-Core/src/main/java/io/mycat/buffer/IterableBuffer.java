package io.mycat.buffer;

/**
 * Created by ynfeng on 2017/7/27.
 * <p>
 * The class that implements this interface can create multiple iterators,
 * Each of which keeps its internal state until the {@link PacketIterator#reset()} method calls.
 * Note:For performance reasons, try to create only one iterator.
 */
public interface IterableBuffer {

    /**
     * Gets an iterator named 'default'
     */
    PacketIterator packetIterator();

    /**
     * Gets an iterator named {@code name}
     *
     * @param name The name of iterator
     */
    PacketIterator packetIterator(String name);
}
