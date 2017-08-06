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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ynfeng on 2017/7/7.
 */
public class DirectFixBuffer extends AbstractMycatByteBuffer {
    private ByteBuffer byteBuffer;
    private int capacity;
    private int mark;


    protected DirectFixBuffer(ByteBuffer byteBuffer, int capacity) {
        this.byteBuffer = byteBuffer;
        this.capacity = capacity;
    }

    @Override
    public int transferToChannel(SocketChannel socketChannel) throws IOException {
        byteBuffer.limit(writeLimit() == 0 ? writeIndex() : writeLimit());
        byteBuffer.position(readIndex());
        int write = socketChannel.write(byteBuffer);
        readIndex(readIndex() + write);
        return write;
    }

    @Override
    public int transferToChannel(SocketChannel socketChannel, int length) throws IOException {
        byteBuffer.limit(readIndex() + length);
        byteBuffer.position(readIndex());
        int write = socketChannel.write(byteBuffer);
        readIndex(readIndex() + write);
        return write;
    }

    @Override
    public int transferFromChannel(SocketChannel socketChannel) throws IOException {
        byteBuffer.limit(capacity());
        byteBuffer.position(writeIndex());
        int read = socketChannel.read(byteBuffer);
        writeIndex(writeIndex() + read);
        return read;
    }

    @Override
    public void mark(int position) {
        this.mark = position;
    }

    @Override
    public void reset() {
        this.readIndex(this.mark);
    }

    @Override
    public MycatByteBuffer compact() {
        byteBuffer.limit(writeIndex());
        byteBuffer.position(readIndex());
        byteBuffer.compact();
        super.compact();
        return this;
    }

    @Override
    public void clear() {
        super.clear();
        byteBuffer.clear();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    long getInt(int index, int length) {
        byteBuffer.limit(index + length);
        byteBuffer.position(index);
        long rv = 0;
        for (int i = 0; i < length; i++) {
            byte b = byteBuffer.get();
            rv |= (((long) b) & 0xFF) << (i * 8);
        }
        return rv;
    }

    @Override
    public byte[] getBytes(int index, int length) {
        byteBuffer.limit(length + index);
        byteBuffer.position(index);
        byte[] bytes = new byte[length];
        byteBuffer.get(bytes);
        return bytes;
    }

    @Override
    public byte getByte(int index) {
        byteBuffer.limit(index + 1);
        byteBuffer.position(index);
        byte b = byteBuffer.get();
        return b;
    }

    @Override
    public MycatByteBuffer putBytes(int index, byte[] bytes) {
        putBytes(index, bytes.length, bytes);
        return this;
    }

    @Override
    public MycatByteBuffer putBytes(int index, int length, byte[] bytes) {
        byteBuffer.limit(index + length);
        byteBuffer.position(index);
        byteBuffer.put(bytes);
        return this;
    }

    @Override
    public MycatByteBuffer putByte(int index, byte val) {
        byteBuffer.limit(index + 1);
        byteBuffer.position(index);
        byteBuffer.put(val);
        return this;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }
}
