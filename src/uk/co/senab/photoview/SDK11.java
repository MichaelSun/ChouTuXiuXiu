/**
 * SDK11.java
 */
package uk.co.senab.photoview;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;

/**
 * @Description 文件描述
 * 
 * @author Jie.yun
 * 
 * @time 2013-4-28 上午11:14:15
 * 
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SDK11 {

	public static <P> void executeOnThreadPool(AsyncTask<P, ?, ?> task,
			P... params) {
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
	}

}
