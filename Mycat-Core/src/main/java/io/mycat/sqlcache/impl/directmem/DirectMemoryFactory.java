package io.mycat.sqlcache.impl.directmem;



import io.mycat.sqlcache.IBufferPageFactory;
import io.mycat.sqlcache.MyCatBufferPage;

import java.io.IOException;
import java.util.Set;

/**
 * Direct Memroy Page Factory
 *
 * @author zagnix
 * @create 2016-12-28 07:46
 */

public class DirectMemoryFactory implements IBufferPageFactory {
    public MyCatBufferPage acquirePage(long index) throws IOException {
        return null;
    }

    public void releasePage(long index) {

    }

    public void deletePage(long index) throws IOException {

    }

    public void flush() {

    }

    public void deleteAllPages() throws IOException {

    }

    public Set<Long> getExistingPageIndexSet() {
        return null;
    }

    public void releaseCachedPages() throws IOException {

    }
}
