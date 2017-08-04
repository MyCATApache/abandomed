package io.mycat.common;

import java.util.concurrent.ExecutorService;

/**
 * @author wuzh
 */
public interface NameableExecutorService extends ExecutorService {

	public String getName();
}
