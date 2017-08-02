package io.mycat.net2;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import io.mycat.buffer.DirectFixBufferAllocator;
import io.mycat.buffer.MycatByteBufferAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网络事件反应器
 *
 * @author wuzh
 */
public final class NIOReactor {
    private static final Logger LOGGER = LoggerFactory.getLogger(NIOReactor.class);
    private final String name;
    private final RWThread reactorR;

    public NIOReactor(String name) throws IOException {
        this.name = name;
        this.reactorR = new RWThread(name + "-RW");
    }

    public String getName() {
        return name;
    }

    final void startup() {
        reactorR.start();
    }

    public void ensureInActorThread() {
        if (Thread.currentThread().getId() != reactorR.getId()) {
            //System.out.println("thread "+Thread.currentThread().getId()+ ","+reactorR.getId());
            throw new RuntimeException("must called in actor thread ,connection.invokeLater(...) ,cur thread: " + Thread.currentThread().getName() + " should in thread: " + reactorR.getName());
        }
    }

    public void invokeLater(Runnable e) {
        reactorR.events.add(e);
    }

    final void postRegister(Connection c) {
        c.setNIOReactor(this.name);
        reactorR.registerQueue.offer(c);
        reactorR.selector.wakeup();
    }

    final Queue<Connection> getRegisterQueue() {
        return reactorR.registerQueue;
    }

    final long getReactCount() {
        return reactorR.reactCount;
    }

    private final class RWThread extends Thread {
        private final Selector selector;
        private final ConcurrentLinkedQueue<Connection> registerQueue;
        private long reactCount;
        private java.util.concurrent.CopyOnWriteArrayList<Runnable> events = new CopyOnWriteArrayList<Runnable>();
        private final MycatByteBufferAllocator mycatByteBufferAllocator;

        private RWThread(String name) throws IOException {
            this.setName(name);
            this.selector = Selector.open();
            this.registerQueue = new ConcurrentLinkedQueue<Connection>();
            this.mycatByteBufferAllocator = new DirectFixBufferAllocator(1024);
        }

        @Override
        public void run() {
            final Selector selector = this.selector;
            Set<SelectionKey> keys = null;
            int readys = 0;
            for (; ; ) {
                ++reactCount;
                try {
                    readys = selector.select(400 / (readys + 1));
                    if (readys == 0) {
                        handlerEvents(selector);
                        continue;
                    }
                    keys = selector.selectedKeys();

                    for (final SelectionKey key : keys) {
                        Connection con = null;
                        try {
                            final Object att = key.attachment();
                            LOGGER.debug("select-key-readyOps = {}, attachment = {}", key.readyOps(), att);
                            if (att != null && key.isValid()) {
                                con = (Connection) att;
                                con.getNetworkStateMachine().driveState();
                            } else {
                                key.cancel();
                            }
                        } catch (final Throwable e) {
                            if (e instanceof CancelledKeyException) {
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug(con + " socket key canceled");
                                }
                            } else {
                                LOGGER.warn(con + "", e);
                            }
                        }
                    }
                } catch (Throwable e) {
                    LOGGER.warn(name, e);
                } finally {
                    if (keys != null) {
                        keys.clear();
                    }
                }
                handlerEvents(selector);
            }
        }

        private void processEvents() {
            if (events.isEmpty()) {
                return;
            }
            Object[] objs = events.toArray();
            if (objs.length > 0) {
                for (Object obj : objs) {
                    ((Runnable) obj).run();
                }
                events.removeAll(Arrays.asList(objs));
            }
        }

        private void handlerEvents(Selector selector) {
            try {
                processEvents();
                register(selector);
            } catch (Exception e) {
                LOGGER.warn("caught user event err:", e);
            }
        }

        private void register(Selector selector) {

            if (registerQueue.isEmpty()) {
                return;
            }
            Connection c = null;
            while ((c = registerQueue.poll()) != null) {
                try {
                    c.setSelector(selector);
                    c.setMycatByteBufferAllocator(mycatByteBufferAllocator);
                    c.getNetworkStateMachine().driveState();
                } catch (Throwable e) {
                    LOGGER.warn("register error ", e);
                    c.close("register err");
                }
            }
        }

    }

}
