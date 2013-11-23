package com.canruoxingchen.uglypic.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 
 */

/**
 * @author Shaofeng Wang
 *
 */
public class CancelableTask implements Runnable{
	
	private AtomicBoolean mCancelled = new AtomicBoolean(false);
	
	private Runnable mRunnable = null;
	
	public CancelableTask(Runnable runnable) {
		this.mRunnable = runnable;
	}
	
	public void cancel() {
		mCancelled.set(true);
	}

	@Override
	public void run() {
		if(!mCancelled.get() && mRunnable != null) {
			mRunnable.run();
		}
	}
}
