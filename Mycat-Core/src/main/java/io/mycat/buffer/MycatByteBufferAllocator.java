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
