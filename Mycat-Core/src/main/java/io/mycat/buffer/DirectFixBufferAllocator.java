package io.mycat.buffer;

import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Created by ynfeng on 2017/7/7.
 */
public class DirectFixBufferAllocator implements MycatByteBufferAllocator {
    private final LinkedList<MycatByteBuffer> freeBuffers = new LinkedList<>();
    private int chunkSize;
    private MycatByteBufferAllocator parent;

    public DirectFixBufferAllocator(int capacity) {
        this.chunkSize = capacity;
    }

    @Override
    public MycatByteBuffer allocate() {
        MycatByteBuffer mycatByteBuffer = freeBuffers.pollLast();
        if (mycatByteBuffer == null) {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(chunkSize);
            mycatByteBuffer = new DirectFixBuffer(byteBuffer, chunkSize);
        }
        mycatByteBuffer.clear();
        return mycatByteBuffer;
    }

    @Override
    public void recyle(MycatByteBuffer buffer) {
        if (buffer.capacity() == chunkSize) {
            freeBuffers.addFirst(buffer);
        } else {
            throw new IllegalArgumentException("Can't recyle MycatByteBuffer capacity " + buffer.capacity());
        }
    }

    @Override
    public int getChunkSize() {
        return chunkSize;
    }
}
