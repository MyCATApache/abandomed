/*
 * Copyright (c) 2017, MyCAT and/or its affiliates. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */
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
