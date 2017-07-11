package io.mycat.buffer;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by ynfeng on 2017/7/7.
 */
public class MycatByteBufferTest {
    private static final int CHUNK_SIZE = 1024 * 1024 * 512;
    private MycatByteBufferAllocator allocator = new DirectFixBufferAllocator(CHUNK_SIZE);

    @Test
    public void testIntReadWrite() {
        MycatByteBuffer buffer = allocator.allocate();
        Assert.assertTrue(allocator.getChunkSize() == CHUNK_SIZE);
        //fix int test
        buffer.putFixInt(0, 1, 100);
        Assert.assertTrue(buffer.getFixInt(0, 1) == 100);
        buffer.putFixInt(1, 2, 200);
        Assert.assertTrue(buffer.getFixInt(1, 2) == 200);
        Assert.assertTrue(buffer.writeIndex() == 0);
        Assert.assertTrue(buffer.readIndex() == 0);
        buffer.clear();

        //对读写指针的影响测试
        buffer.writeFixInt(1, 100);
        Assert.assertTrue(buffer.writeIndex() == 1);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.readFixInt(1) == 100);
        Assert.assertTrue(buffer.readIndex() == 1);
        buffer.clear();

        //Lenenc int test
        buffer.putLenencInt(0, 1024);
        Assert.assertTrue(buffer.getLenencInt(0) == 1024);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.writeIndex() == 0);
        buffer.clear();

        buffer.putLenencInt(0, 1 << 17);
        Assert.assertTrue(buffer.getLenencInt(0) == 1 << 17);
        buffer.clear();

        buffer.putLenencInt(0, 1 << 25);
        Assert.assertTrue(buffer.getLenencInt(0) == 1 << 25);
        buffer.clear();

        buffer.writeLenencInt(1);
        Assert.assertTrue(buffer.readLenencInt() == 1);
        buffer.clear();

        buffer.writeLenencInt(1 << 17);
        Assert.assertTrue(buffer.readLenencInt() == 1 << 17);
        buffer.clear();

        buffer.writeLenencInt(1 << 25);
        Assert.assertTrue(buffer.readLenencInt() == 1 << 25);
        buffer.clear();

        buffer.writeLenencInt(2048);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.writeIndex() == 3);
        Assert.assertTrue(buffer.readLenencInt() == 2048);
        Assert.assertTrue(buffer.readIndex() == 2);
        allocator.recyle(buffer);
    }

    @Test
    public void testStringReadWrite() {
        MycatByteBuffer buffer = allocator.allocate();
        //Fix String test
        buffer.putFixString(0, "hello");
        Assert.assertTrue(buffer.writeIndex() == 0);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.getFixString(0, 5).equals("hello"));
        buffer.clear();

        buffer.writeFixString("hello");
        Assert.assertTrue(buffer.writeIndex() == 5);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.readFixString(5).equals("hello"));
        Assert.assertTrue(buffer.readIndex() == 5);
        buffer.clear();

        //Lenenc String test
        buffer.putLenencString(0, "hello");
        Assert.assertTrue(buffer.writeIndex() == 0);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.getLenencString(0).equals("hello"));
        buffer.clear();

        buffer.writeLenencString("hello");
        Assert.assertTrue(buffer.writeIndex() == 6);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.readLenencString().equals("hello"));
        Assert.assertTrue(buffer.readIndex() == 6);
        buffer.clear();

        //Var String test
        buffer.putVarString(0, "hello");
        Assert.assertTrue(buffer.writeIndex() == 0);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.getVarString(0, 5).equals("hello"));
        buffer.clear();

        buffer.writeVarString("hello");
        Assert.assertTrue(buffer.writeIndex() == 5);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.readVarString(5).equals("hello"));
        Assert.assertTrue(buffer.readIndex() == 5);
        buffer.clear();


        //NUL String test
        buffer.putNULString(0, "hello");
        Assert.assertTrue(buffer.writeIndex() == 0);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.getNULString(0).equals("hello"));
        buffer.clear();

        buffer.writeNULString("hello");
        Assert.assertTrue(buffer.remaining() == 6);
        Assert.assertTrue(buffer.freeBytes() == CHUNK_SIZE - 6);
        Assert.assertTrue((buffer.writeIndex() == 6));
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.readNULString().equals("hello"));
        Assert.assertTrue(buffer.readIndex() == 6);
        Assert.assertFalse(buffer.hasRemaining());
        buffer.clear();

        String str = randomString(1 << 17);
        buffer.putLenencString(0,str);
        Assert.assertTrue(buffer.getLenencString(0).equals(str));
        buffer.clear();

        str = randomString(1 << 25);
        buffer.putLenencString(0,str);
        Assert.assertTrue(buffer.getLenencString(0).equals(str));
        buffer.clear();

        str = randomString(1 << 15);
        buffer.putLenencString(0,str);
        Assert.assertTrue(buffer.getLenencString(0).equals(str));
        buffer.clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void recyleExceptionTest(){
        MycatByteBuffer buffer = new DirectFixBuffer(ByteBuffer.allocateDirect(10),10);
        allocator.recyle(buffer);
    }

    private String randomString(int length){
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) ThreadLocalRandom.current().nextInt(33,128);
        }
        return new String(bytes);
    }
}
