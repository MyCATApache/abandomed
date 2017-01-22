package io.mycat.sqlcache;

import io.mycat.util.UnsafeMemory;
import sun.misc.Unsafe;

/**
 * cache padding from disruptor
 *
 * @author zagnix
 * @create 2016-11-18 10:17
 */

class LhsPadding
{
    protected long p1, p2, p3, p4, p5, p6, p7;
}

class Value extends LhsPadding
{
    protected volatile long value;
}

class RhsPadding extends Value
{
    protected long p9, p10, p11, p12, p13, p14, p15;
}

public class AddressIndex extends RhsPadding {
    static final long INITIAL_VALUE = -1L;
    private static final Unsafe UNSAFE = UnsafeMemory.getUnsafe();
    private static final long VALUE_OFFSET;

    public AddressIndex() {
        this(INITIAL_VALUE);
    }

    public AddressIndex(long initialValue) {
        UNSAFE.putOrderedLong(this, VALUE_OFFSET, initialValue);
    }

    public long get() {
        return this.value;
    }

    public void set(long value) {
        UNSAFE.putOrderedLong(this, VALUE_OFFSET, value);
    }

    public void setVolatile(long value) {
        UNSAFE.putLongVolatile(this, VALUE_OFFSET, value);
    }

    /**
     * 内存地址值(通旧值this所在对象加上字节偏量VALUE_OFFSET找到所在的内存段)
     * 期望值: expectedValue
     * 新值: newValue
     * 如果 内存地址值 == 期望值
     *      则将新的值写入到内存地址中。
     *      return true
     *  else
     *      return false
     * @param expectedValue
     * @param newValue
     * @return
     */
    public boolean compareAndSet(long expectedValue, long newValue) {
        return UNSAFE.compareAndSwapLong(this, VALUE_OFFSET, expectedValue, newValue);
    }


    public long incrementAndGet() {
        return this.addAndGet(1L);
    }

    public long addAndGet(long increment) {
        long currentValue;
        long newValue;
        do {
            currentValue = this.get();
            newValue = currentValue + increment;
        } while(!this.compareAndSet(currentValue,newValue));
        return newValue;
    }
    public String toString() {
        return Long.toString(this.get());
    }
    static {
        try {
            VALUE_OFFSET = UNSAFE.objectFieldOffset(Value.class.getDeclaredField("value"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}