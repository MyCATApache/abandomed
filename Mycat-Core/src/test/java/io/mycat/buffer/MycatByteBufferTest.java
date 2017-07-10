package io.mycat.buffer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by ynfeng on 2017/7/7.
 */
public class MycatByteBufferTest {
    private MycatByteBufferAllocator allocator = new DirectFixBufferAllocator(100);

    @Test
    public void testIntReadWrite() {
        MycatByteBuffer buffer = allocator.allocate();
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

        buffer.writeLenencInt(2048);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.writeIndex() == 3);
        Assert.assertTrue(buffer.readLenencInt(0) == 2048);
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
        buffer.putLenencString(0,"hello");
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
        buffer.putVarString(0,"hello");
        Assert.assertTrue(buffer.writeIndex() == 0);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.getVarString(0,5).equals("hello"));
        buffer.clear();

        buffer.writeVarString("hello");
        Assert.assertTrue(buffer.writeIndex() == 5);
        Assert.assertTrue(buffer.readIndex() == 0);
        Assert.assertTrue(buffer.readVarString(5).equals("hello"));
        Assert.assertTrue(buffer.readIndex() == 5);
        buffer.clear();


        //NUL String test
        buffer.putNULString(0,"hello");
        Assert.assertTrue(buffer.writeIndex() == 0);
        Assert.assertTrue(buffer.readIndex() == 0);
        String s = buffer.getNULString(0);
        Assert.assertTrue(buffer.getNULString(0).equals("hello"));
        buffer.clear();
    }

}
