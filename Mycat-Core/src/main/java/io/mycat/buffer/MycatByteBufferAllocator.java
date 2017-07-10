package io.mycat.buffer;

/**
 * 缓冲分配器，该分配器只分配固定大小的缓冲区
 *
 *  Created by ynfeng on 2017/7/7.
 */
public interface MycatByteBufferAllocator {
    /**
     * 分配一个缓冲
     *
     * @return 缓冲区
     */
    MycatByteBuffer allocate();

    /**
     * 回收缓冲
     *
     * @param buffer 待回收的缓冲
     */
    void recyle(MycatByteBuffer buffer);

    /**
     * 获取分配器每次分配的缓冲区大小
     *
     * @return 缓冲区大小
     */
    int getChunkSize();
}
