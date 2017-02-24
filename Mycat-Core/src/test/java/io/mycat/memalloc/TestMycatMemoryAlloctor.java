package io.mycat.memalloc;

import io.netty.buffer.ByteBuf;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * @author zagnix
 * @create 2017-01-18 11:19
 */

public class TestMycatMemoryAlloctor {
    @Test
    public void testMemAlloc(){
        final MyCatMemoryAllocator memoryAllocator =
                new MyCatMemoryAllocator(Runtime.getRuntime().availableProcessors()*2);

        for (int i = 0; i <100 ; i++) {
            ByteBuf byteBuf = memoryAllocator.directBuffer(128);
            byteBuf.writeBytes("helll world".getBytes());
            ByteBuffer byteBuffer = byteBuf.nioBuffer(0,128);
           	String srt = getString(byteBuffer);
            byteBuf.release();
            System.out.println("refCnt:" + byteBuf.refCnt());
            // byteBuf.writeBytes(byteBuffer);
            // System.out.println("refCnt:" + byteBuf.refCnt());
        }
    }

    public static String getString(ByteBuffer buffer) {
        Charset charset = null;
        CharsetDecoder decoder = null;
        CharBuffer charBuffer = null;
        try {
            charset = Charset.forName("UTF-8");
            decoder = charset.newDecoder();
            charBuffer = decoder.decode(buffer.asReadOnlyBuffer());
            return charBuffer.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "error";
        }
    }

    public static ByteBuffer getByteBuffer(String str)
    {
        return ByteBuffer.wrap(str.getBytes());
    }
}
