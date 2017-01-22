package io.mycat.sqlcache.impl.mmap;


import io.mycat.sqlcache.AddressIndex;
import io.mycat.sqlcache.ISQLResult;
import io.mycat.sqlcache.MyCatBufferPage;
import io.mycat.util.UnsafeMemory;
import io.mycat.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 基于内存映射Cache,用queue实现
 *
 * @author zagnix
 * @create 2016-12-02 15:46
 */
public class MappedSQLResult implements ISQLResult {
    private final static Logger LOGGER = LoggerFactory.getLogger(MappedSQLResult.class);
    /**
     * 存放索引文件的目录
     */
    private static  final  String INDEX_PAGE_FOLDER = "index";
    /**
     * 存放数据的目录
     */
    private   static  final  String DATA_PAGE_FOLDER = "data";
    /**
     * 保存元数据信息目录
     */
    private static final  String META_DATA_PAGE_FOLDER = "meta_data";
    /**
     * Page File Size
     */
    private  final int DATA_PAGE_SIZE;

    /**
     * 默认页文件大小
     */
    public final static int DEFAULT_DATA_PAGE_SIZE = 128 * 1024 * 1024;

    /**
     * 最小页文件大小
     */
    public final static int MINIMUM_DATA_PAGE_SIZE = 8 * 1024 * 1024;

    /**
     * 索引页在内存中停留时间
     */
    public final static int INDEX_PAGE_CACHE_TTL = 1000;
    /**
     * 数据也在内存中停留时间
     */
    public  final static int DATA_PAGE_CACHE_TTL = 1000;
    /**
     *索引文件地址编码,使用 64bit,
     * 一条索引地址格式为：
     * page number  | page offset | data lenghe | op
     * 63        50 | 49       21 | 20         1|  0
     */
    /**
     * 最大data page 文件数16 * 1024
     */
    public final static int DATA_PAGE_NUMBER_BITS = 14;
    /**
     * 每个data文件大小最大为512M
     */
    public final static int DATA_PAGE_INDEX_ITEM_OFFSET_BITS = 29;
    /**
     * 每次添加的数据最大长度1M
     */
    public final static int DATA_ITEM_LEN_BITS = 20;

    /**
     * 标志是否被消费了。用于多线程并发访问标志
     */
    public final static int DATA_PAGE_INDEX_OP_BITS = 1;

    /**
     * 最大DATA页大小
     */
    public final static int MAX_DATA_PAGE_SIZE = 1 << DATA_PAGE_INDEX_ITEM_OFFSET_BITS;
    /**
     *最大DATA页的数量
     */
    public final static int MAX_DATA_PAGE_INDEX_SIZE = 1 << DATA_PAGE_NUMBER_BITS;
    /**
     * 每条item data最大长度
     */
    public final static int MAX_DATA_ITEM_LEN = 1 << DATA_ITEM_LEN_BITS;
    /**
     * 位操作用的
     */
    private static final long MASK_LONG_14_BITS = 0xFFF3L;
    private static final long MASK_LONG_29_BITS = 0x1FFFFFFFL;
    private static final long MASK_LONG_20_BITS = 0xFFFFFL;
    private static final long MASK_LONG_1_BITS = 0x1L;

    /**
     * 存放页文件目录
     */
    private String cacheDirectory = null;

    /**
     * 索引页管理对象，提供acquire/release/cache
     */
    private MappedBufferPageFactory indexPageFactory = null;

    /**
     * 数据页管理对象，提供acquire/release/cache
     */
    private MappedBufferPageFactory dataPageFactory = null;

    /**
     * 元数据管理对象，提供acquire/release/cache
     */
    private MappedBufferPageFactory metaPageFactory = null;

    private static final int META_DATA_PAGE_INDEX = 0;
    private static final int META_DATA_ITEM_LENGTH_BITS = 4;
    private static final int META_DATA_PAGE_SIZE = 1 << META_DATA_ITEM_LENGTH_BITS;

    /**
     * index页存放data item数量是1024 * 1024条
     */
    private static final int INDEX_ITEMS_PER_PAGE_BITS = 20;
    private static final int INDEX_ITEMS_PER_PAGE = 1 << INDEX_ITEMS_PER_PAGE_BITS;
    /**
     * 每条data item的编码 index address的大小为8bit
     */
    private static final  int INDEX_ITEM_LENGTH_BITS = 3;
    private static final  int INDEX_ITEM_LENGTH = 1 << INDEX_ITEM_LENGTH_BITS;
    /**
     * index 页文件大小为8*1024*1024
     */
    private static final int INDEX_PAGE_SIZE = INDEX_ITEM_LENGTH * INDEX_ITEMS_PER_PAGE;

    /**
     * 队列头下标
     */
    private final AddressIndex queueFrontIndex = new AddressIndex();
    /**
     * 队列尾下标
     */
    private final AddressIndex queueTailIndex = new AddressIndex();
    /**
     * queueTailIndex下标 索引的data page
     */
    private final AddressIndex queueTailDataPageIndex = new AddressIndex();

    /**
     * queueTailIndex下标索引的data page的当前写位置
     */
    private final AddressIndex queueTailDataItemOffset = new AddressIndex();

    /**
     * lock
     */
    private final Lock appendLock = new ReentrantLock();
    private final ReadWriteLock arrayReadWritelock = new ReentrantReadWriteLock();
    private final Lock queueReadLock = arrayReadWritelock.readLock();
    private final Lock queueWriteLock = arrayReadWritelock.writeLock();



    /**
     * MappedBigCache构造函数
     * @param cacheDir
     * @param cacheName
     * @throws IOException
     */
    public MappedSQLResult(String cacheDir, String cacheName) throws IOException{
        this(cacheDir,cacheName,DEFAULT_DATA_PAGE_SIZE);
    }

    /**
     * MappedSQLResult 构造函数
     * @param cacheDir
     * @param cacheName
     * @param cacheSize
     * @throws IOException
     */
    public MappedSQLResult(String cacheDir, String cacheName, int cacheSize) throws IOException {

        this.cacheDirectory = cacheDir;
        /**
         * 目录没有加文件分割符
         */
        if (!cacheDirectory.endsWith(File.separator)) {
            cacheDirectory += File.separator;
        }

        /**
         * 构建一个消息队列名字
         */
        cacheDirectory = cacheDirectory + cacheName + File.separator;

        /**
         * 确定路径合法
         */
        if (!Utils.isFilenameValid(cacheDirectory)) {
            throw new IllegalArgumentException("invalid array directory : " + cacheDirectory);
        }

        long pageSize = UnsafeMemory.roundToOsPageSzie((long)cacheSize);

        if (pageSize < MINIMUM_DATA_PAGE_SIZE) {
            throw new IllegalArgumentException("invalid page size, allowed minimum is : " + MINIMUM_DATA_PAGE_SIZE + " bytes.");
        }

        if (pageSize > MAX_DATA_PAGE_SIZE){
            throw new IllegalArgumentException("invalid page size, allowed max is : " + MAX_DATA_PAGE_SIZE + " bytes.");
        }

        DATA_PAGE_SIZE = (int)pageSize;
        this.init();
    }


    private void init() throws IOException {
        /**
         * 索引页管理对象构建
         */
        this.indexPageFactory = new MappedBufferPageFactory(INDEX_PAGE_SIZE,
                this.cacheDirectory + INDEX_PAGE_FOLDER,
                INDEX_PAGE_CACHE_TTL);

        /**
         * 数据页管理对象构建
         */
        this.dataPageFactory = new MappedBufferPageFactory(DATA_PAGE_SIZE,
                this.cacheDirectory + DATA_PAGE_FOLDER,
                DATA_PAGE_CACHE_TTL);

        /**
         * 元信息管理对象构建
         */
        this.metaPageFactory = new MappedBufferPageFactory(META_DATA_PAGE_SIZE,
                this.cacheDirectory + META_DATA_PAGE_FOLDER,
                10 * 1000);

        /**
         * 初始化数组head 和 tail下标
         */
        initArrayIndex();

        /**
         * 初始化当前页号和偏移量
         */
        initDataPageIndex();

    }


    /**
     * 初始化Queue的 front index 和 tail index
     * @throws IOException
     */
    void initArrayIndex() throws IOException {
        /**
         * 从元数据信息中获取数组的head / tail index
         */
        MyCatBufferPage metaDataPage = this.metaPageFactory.acquirePage(META_DATA_PAGE_INDEX);
        ByteBuffer metaBuf = metaDataPage.getLocalByteBuffer(0);

        long front= metaBuf.getLong();
        long tail = metaBuf.getLong();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(" head " + front);
            LOGGER.debug(" tail " + tail);
        }
        /**
         * head .tail 是保存在文件中，
         * 程序启动时候，会读取数据元文件的head，tail信息初始化一个数组的head和tail
         */
        queueFrontIndex.set(front);
        queueTailIndex.set(tail);
    }

    /**
     * 初始化当前页号和偏移量
     * @throws IOException
     */
    void initDataPageIndex() throws IOException {
        /**
         * 数组为空的情况
         */
        if (this.isEmpty()) {
            queueTailDataPageIndex.set(0);
            queueTailDataItemOffset.set(0);
        } else {

            MyCatBufferPage previousIndexPage = null;
            long previousIndexPageIndex = -1;

            try {
                /**
                 * 根据数组的信息初始化Data Page的写位置 = tailIndex -1
                 */
                long previousIndex = this.queueTailIndex.get() - 1;

                if (previousIndex < 0) {
                    previousIndex = Long.MAX_VALUE;
                }

                /**
                 * previousIndex通过取模找到previousIndex所在的 index page
                 */
                previousIndexPageIndex = previousIndex / (INDEX_ITEMS_PER_PAGE);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.info("====>previousIndexPageIndex " + previousIndexPageIndex);
                }

                /**
                 * MemoryMappedFile Index Page
                 */
                previousIndexPage = this.indexPageFactory.acquirePage(previousIndexPageIndex);

                /**
                 * 当前 index page内写位置
                 */
                int previousIndexPageOffset = (int)(previousIndex * INDEX_ITEM_LENGTH);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("====>previousIndexPageOffset " + previousIndex);
                }

                /**
                 * 根据页内偏移的位置得到对应的ByteBuffer
                 */
                ByteBuffer previousIndexItemBuffer = previousIndexPage.getLocalByteBuffer(previousIndexPageOffset);

                /**
                 * data index address
                 * data page number|data page offset|data len|op
                 */
                long previousDataPageIndexAddress = previousIndexItemBuffer.getLong();

                /**
                 * 整个Data Page中当前的页号。
                 * 以及Page内当前的偏移量。
                 */
                queueTailDataPageIndex.set(decodeDataPageNumber(previousDataPageIndexAddress));
                queueTailDataItemOffset.set(decodeDataPageCurrentOffset(previousDataPageIndexAddress) +
                        decodeDataLen(previousDataPageIndexAddress));
            } finally {
                /**
                 * 释放当前页
                 */
                if (previousIndexPage != null) {
                    this.indexPageFactory.releasePage(previousIndexPageIndex);
                }
            }
        }
    }
    /**
     * 把data存到数据中
     * @param data
     * @return
     * @throws IOException
     */
    public long put(byte[] data) throws IOException {
        try {
            queueWriteLock.lock();

            /**
             * 数据长度超过1MB的情况
             */
            if (data.length > MAX_DATA_ITEM_LEN){
                throw new IOException("data length larger :" + MAX_DATA_ITEM_LEN);
            }

            /**
             * 数据页
             */
            MyCatBufferPage toAppendDataPage = null;

            /**
             * 索引页
             */
            MyCatBufferPage toAppendIndexPage = null;

            /**
             * 追加的索引下标....
             */
            long toAppendIndexPageIndex = -1L;

            /**
             * 追加数据索引下标....
             */
            long toAppendDataPageIndex = -1;
            long toAppendArrayIndex = -1L;

            try {
                /**
                 * 锁住当前，只允许一个线程访问
                 */
                appendLock.lock(); // only one thread can append

                /**
                 * 数据是否达到最大空间极限
                 */
                if (this.isFull()) { // end of the world check:)
                    throw new IOException("ring space of java long type used up, the end of the world!!!");
                }
                /**
                 * 大于Data Page的大小,跳到下一页
                 */
                if (this.queueTailDataItemOffset.get() + data.length > DATA_PAGE_SIZE) {
                    /**
                     * 整个索引下标超过了MAX_DATA_PAGE_INDEX_SIZE，回到下标为0
                     */
                    if (this.queueTailDataPageIndex.get() == (MAX_DATA_PAGE_INDEX_SIZE)) {
                        this.queueTailDataPageIndex.set(0L);
                    } else {
                        this.queueTailDataPageIndex.incrementAndGet();
                    }
                    /**
                     * 新的页内偏移设置0。
                     */
                    this.queueTailDataItemOffset.set(0L);
                }

                /**
                 * 还是在当前页中直接赋值。。。。。
                 */
                toAppendDataPageIndex = this.queueTailDataPageIndex.get();
                int toAppendDataItemOffset  = (int)this.queueTailDataItemOffset.get();

                /**
                 * 队列尾下标
                 */
                toAppendArrayIndex = this.queueTailIndex.get();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("toAppendArrayIndex " + toAppendArrayIndex);
                }

                /**
                 * 查找toAppendDataPageIndex对应的MemoryMappedFile对象
                 */
                toAppendDataPage = this.dataPageFactory.acquirePage(toAppendDataPageIndex);

                /**
                 * 根据toAppendDataItemOffset此时的读写偏移位置返回一个ByteBuffer对象，并设置position
                 * 的位置为 toAppendDataItemOffset
                 */
                ByteBuffer toAppendDataPageBuffer = toAppendDataPage.getLocalByteBuffer(toAppendDataItemOffset);

                /**
                 * 将item的数据写入ByteBuffer中
                 */
                toAppendDataPageBuffer.put(data);

                /**
                 * 当前页有数据了，标志为脏页
                 */
                toAppendDataPage.setDirty(true);

                /**
                 * 更新当前page页面的数据item偏移位置。
                 */
                this.queueTailDataItemOffset.addAndGet(data.length);

                /**
                 * 将数据页号和当前数据数据页号编码成 index address存放到索引页中
                 */

                toAppendIndexPageIndex = toAppendArrayIndex / (INDEX_ITEMS_PER_PAGE);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("index page number: " + toAppendIndexPageIndex);
                }

                toAppendIndexPage = this.indexPageFactory.acquirePage(toAppendIndexPageIndex);
                int toAppendIndexItemOffset = (int) ((toAppendArrayIndex&(INDEX_ITEMS_PER_PAGE-1))*INDEX_ITEM_LENGTH);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("index page offset: " + toAppendIndexItemOffset);
                }

                ByteBuffer toAppendIndexPageBuffer = toAppendIndexPage.getLocalByteBuffer(toAppendIndexItemOffset);

                /**
                 * 就是记录data item 所在的数据页号，数据页偏移位置，item数据长度
                 */
                toAppendIndexPageBuffer.putLong(encodeIndexAddress((int)toAppendDataPageIndex,toAppendDataItemOffset,data.length,(byte) 0x1));
                toAppendIndexPage.setDirty(true);
                /**
                 * 将数据的头加1
                 */
                this.queueTailIndex.incrementAndGet();

                /**
                 * 更新元数据,只有一页的0
                 * 保存当前的数组的头和尾信息
                 */
                MyCatBufferPage metaDataPage = this.metaPageFactory.acquirePage(META_DATA_PAGE_INDEX);
                ByteBuffer metaDataBuf = metaDataPage.getLocalByteBuffer(0);
                metaDataBuf.putLong(this.queueFrontIndex.get());
                metaDataBuf.putLong(this.queueTailIndex.get());
                metaDataPage.setDirty(true);

            } finally {

                appendLock.unlock();

                if (toAppendDataPage != null) {
                    this.dataPageFactory.releasePage(toAppendDataPageIndex);
                }
                if (toAppendIndexPage != null) {
                    this.indexPageFactory.releasePage(toAppendIndexPageIndex);
                }
            }

            return toAppendArrayIndex;

        } finally {
            queueWriteLock.unlock();
        }
    }

    /**
     * 取数据
     * @return
     * @throws IOException
     */
    public byte[] next() throws IOException {
        try {
            queueWriteLock.lock();

            if (this.isEmpty()) {
                return null;
            }

            long frontIndex = this.queueFrontIndex.get();


            MyCatBufferPage dataPage = null;
            long dataItemIndexAddress = -1L;
            int dataPageNumber = -1;
            try {
                /**
                 * 根据index，获取index address。
                 */
                ByteBuffer indexItemBuffer = this.getIndexItemBuffer(frontIndex);

                /**
                 * 得到data item 的索引地址，通过索引地址解码出页号，页偏移，data Item 的长度
                 */
                dataItemIndexAddress = indexItemBuffer.getLong();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("dataItemIndexAddress " + dataItemIndexAddress);
                }

                dataPageNumber = decodeDataPageNumber(dataItemIndexAddress);
                int dataItemOffset = decodeDataPageCurrentOffset(dataItemIndexAddress);
                int dataItemLength = decodeDataLen(dataItemIndexAddress);

                dataPage = this.dataPageFactory.acquirePage(dataPageNumber);

                byte[] data = dataPage.getBytes(dataItemOffset,dataItemLength);

                long nextQueueFrontIndex = frontIndex;
                if (nextQueueFrontIndex == Long.MAX_VALUE) {
                    nextQueueFrontIndex = 0L;
                } else {
                    nextQueueFrontIndex++;
                }
                this.queueFrontIndex.set(nextQueueFrontIndex);
                MyCatBufferPage queueFrontIndexPage =
                        this.metaPageFactory.acquirePage(META_DATA_PAGE_INDEX);
                ByteBuffer queueFrontIndexBuffer =
                        queueFrontIndexPage.getLocalByteBuffer(0);
                queueFrontIndexBuffer.putLong(queueFrontIndex.get());
                queueFrontIndexBuffer.putLong(queueTailIndex.get());
                queueFrontIndexPage.setDirty(true);
                return data;
            } finally {
                if (dataPage != null) {
                    this.dataPageFactory.releasePage(dataPageNumber);
                }
            }
        } finally {
            queueWriteLock.unlock();
        }
    }


    /**
     *get下标为index的数据
     * @param index
     * @return
     * @throws IOException
     */
    public byte[] get(long index) throws IOException {
        try {
            queueReadLock.lock();
            validateIndex(index);
            MyCatBufferPage dataPage = null;
            long dataItemIndexAddress = -1L;
            int dataPageNumber = -1;
            try {

                /**
                 * 根据index，获取index address。
                 */
                ByteBuffer indexItemBuffer = this.getIndexItemBuffer(index);

                /**
                 * 得到data item 的索引地址，通过索引地址解码出页号，页偏移，data Item 的长度
                 */
                dataItemIndexAddress = indexItemBuffer.getLong();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("dataItemIndexAddress " + dataItemIndexAddress);
                }

                dataPageNumber = decodeDataPageNumber(dataItemIndexAddress);
                int dataItemOffset = decodeDataPageCurrentOffset(dataItemIndexAddress);
                int dataItemLength = decodeDataLen(dataItemIndexAddress);

                dataPage = this.dataPageFactory.acquirePage(dataPageNumber);

                byte[] data = dataPage.getBytes(dataItemOffset,dataItemLength);

                return data;
            } finally {
                if (dataPage != null) {
                    this.dataPageFactory.releasePage(dataPageNumber);
                }
            }
        } finally {
            queueReadLock.unlock();
        }
    }

    /**
     * 队列大小
     * @return
     */
    public long size() {
        long qFront = this.queueFrontIndex.get();
        long qRear = this.queueTailIndex.get();
        if (qFront <= qRear) {
            return (qRear - qFront);
        } else {
            return Long.MAX_VALUE - qFront + 1 + qRear;
        }
    }

    /**
     * 确定index是否合法
     * @param index
     */

    void validateIndex(long index) {
        if (this.queueFrontIndex.get() <= this.queueTailIndex.get()) {
            if (index < this.queueFrontIndex.get() || index >= this.queueTailIndex.get()) {
                throw new IndexOutOfBoundsException();
            }
        } else {
            if (index < this.queueFrontIndex.get() && index >= this.queueTailIndex.get()) {
                throw new IndexOutOfBoundsException();
            }
        }
    }

    /**
     * 从索引文件中找到数组下标index对应的索引ByteBuffer
     * @param index
     * @return
     * @throws IOException
     */
    ByteBuffer getIndexItemBuffer(long index) throws IOException {

        MyCatBufferPage indexPage = null;
        long indexPageIndex = -1L;
        try {

            indexPageIndex = index / (INDEX_ITEMS_PER_PAGE);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("get index " + indexPageIndex);
            }

            indexPage = this.indexPageFactory.acquirePage(indexPageIndex);
            int indexItemOffset = (int) ((index&(INDEX_ITEMS_PER_PAGE-1)) * INDEX_ITEM_LENGTH);
            ByteBuffer indexItemBuffer = indexPage.getLocalByteBuffer(indexItemOffset);
            return indexItemBuffer;

        } finally {
            if (indexPage != null) {
                this.indexPageFactory.releasePage(indexPageIndex);
            }
        }
    }

    /**
     * 将Data页号，以及当前可写偏移位置，写入data的长度，以及op位编码成一个64为的地址
     * @param dataPageNumber
     * @param currentOffset
     * @param dataLen
     * @param op
     * @return
     */
    public static long encodeIndexAddress(int dataPageNumber,
                                          int currentOffset,int dataLen,byte op) {

        return  ((long)dataPageNumber)<<(DATA_PAGE_INDEX_ITEM_OFFSET_BITS+DATA_ITEM_LEN_BITS+DATA_PAGE_INDEX_OP_BITS)|
                ((long)currentOffset)<<(DATA_ITEM_LEN_BITS+DATA_PAGE_INDEX_OP_BITS)|
                ((long)dataLen)<<DATA_PAGE_INDEX_OP_BITS |
                ((long)op)<<0;
    }

    /**
     * 从索引地址中分离出页号
     * @param indexAddress
     * @return
     */
    public static int decodeDataPageNumber(long indexAddress) {
        return (int)((indexAddress >>>
                (DATA_PAGE_INDEX_ITEM_OFFSET_BITS +
                        DATA_ITEM_LEN_BITS+
                        DATA_PAGE_INDEX_OP_BITS))&MASK_LONG_14_BITS);
    }

    /**
     * 从索引地址中分离出当前data页写的位置
     * @param indexAddress
     * @return
     */
    private static int decodeDataPageCurrentOffset(long indexAddress) {
        return (int)((indexAddress >>> (DATA_ITEM_LEN_BITS+DATA_PAGE_INDEX_OP_BITS))&MASK_LONG_29_BITS);
    }

    /**
     * 从索引地址分离出当前data页的存放数据长度
     * @param indexAddress
     * @return
     */
    private static int decodeDataLen(long indexAddress) {
        return (int)((indexAddress >>> DATA_PAGE_INDEX_OP_BITS)&MASK_LONG_20_BITS);
    }

    /**
     * 从索引地址分离出最低位的值
     * @param indexAddress
     * @return
     */
    private static byte decodeOpBit(long indexAddress) {
        return (byte) (indexAddress&MASK_LONG_1_BITS);
    }


    /**
     * 判断队列是否为空？
     * @return
     */
    public boolean isEmpty() {
        try {
            queueReadLock.lock();
            return this.queueFrontIndex.get() == this.queueTailIndex.get();
        } finally {
            queueReadLock.unlock();
        }
    }

    /**
     * 判断队列是否满了
     * @return
     */
    public boolean isFull() {
        try {
            queueReadLock.lock();
            long currentIndex = this.queueTailIndex.get();
            long nextIndex = currentIndex == (Long.MAX_VALUE) ? 0 : currentIndex + 1;
            return nextIndex == this.queueFrontIndex.get();
        } finally {
            queueReadLock.unlock();
        }
    }


    /**
     * 队头下标
     * @return
     */
    public long getQueueFrontIndex() {
        try {
            queueReadLock.lock();
            return queueFrontIndex.get();
        } finally {
            queueReadLock.unlock();
        }
    }

    /**
     * 队尾下标
     * @return
     */
    public long getQueueTailIndex() {
        try {
            queueReadLock.lock();
            return queueTailIndex.get();
        } finally {
            queueReadLock.unlock();
        }
    }



    /**
     * 刷页面到磁盘总
     */
    public void flush() {
        try {
            queueReadLock.lock();
            this.metaPageFactory.flush();
            this.indexPageFactory.flush();
            this.dataPageFactory.flush();
        } finally {
            queueReadLock.unlock();
        }
    }

    /**
     * 从cache中移走，并unmap操作
     * @throws IOException
     */
    public void recycle() throws IOException {
        try {
            queueWriteLock.lock();
            if (this.metaPageFactory != null) {
                this.metaPageFactory.releaseCachedPages();
            }
            if (this.indexPageFactory != null) {
                this.indexPageFactory.releaseCachedPages();
            }
            if (this.dataPageFactory != null) {
                this.dataPageFactory.releaseCachedPages();
            }
        } finally {
            queueWriteLock.unlock();
        }
    }

    /**
     * reset front index = 0
     */
    public void reset(){
        this.queueFrontIndex.set(0);
        MyCatBufferPage metaDataPage = null;
        try {
            metaDataPage = this.metaPageFactory.acquirePage(META_DATA_PAGE_INDEX);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteBuffer metaDataBuf = metaDataPage.getLocalByteBuffer(0);
        metaDataBuf.putLong(this.queueFrontIndex.get());
        metaDataBuf.putLong(this.queueTailIndex.get());
        metaDataPage.setDirty(true);
    }

    /**
     * 删除所有文件并初始化环境
     * @throws IOException
     */
    public void removeAll() throws IOException {
        try {
            queueWriteLock.lock();
            this.indexPageFactory.deleteAllPages();
            this.dataPageFactory.deleteAllPages();
            this.metaPageFactory.deleteAllPages();
            this.init();
        } finally {
            queueWriteLock.unlock();
        }
    }
}
