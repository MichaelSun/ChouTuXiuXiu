package com.canruoxingchen.uglypic.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 
 */

/**
 * 
 * 线程池管理类
 * 
 * @author Shaofeng Wang
 * 
 */
public class ThreadPoolManager {

	private static final int CORE_POOL_SIZE = 5;
	private static final int MAXIMUM_POOL_SIZE = 128;
	private static final int KEEP_ALIVE = 10;

	private static final ThreadPoolManager sInstance = new ThreadPoolManager();

	private final ThreadFactory sThreadFactory = new ThreadFactory() {
		private final AtomicInteger mCount = new AtomicInteger(1);

		public Thread newThread(Runnable r) {
			return new Thread(r, "Task #" + mCount.getAndIncrement());
		}
	};

	private final BlockingQueue<Runnable> sWorkQueue = new LinkedBlockingQueue<Runnable>(
			10);

	private ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(
			CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.MILLISECONDS,
			sWorkQueue, sThreadFactory);

	private ThreadPoolManager() {
	}

	public static ThreadPoolManager getInstance() {
		return sInstance;
	}
	
	public Executor getExecutor() {
		return mExecutor;
	}

	public void execute(CancelableTask task) {
		if (task != null) {
			mExecutor.execute(task);
		}
	}
	
	public void execute(Runnable task) {
		if(task != null) {
			mExecutor.execute(task);
		}
	}
}
