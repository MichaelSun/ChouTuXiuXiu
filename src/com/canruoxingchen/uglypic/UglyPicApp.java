/**
 * DbnApp.java
 */
package com.canruoxingchen.uglypic;

import java.io.File;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;


/**
 * 
 */
public class UglyPicApp extends Application {

	private static Handler mUiHandler = new Handler(Looper.getMainLooper());
	private static Context mContext;
	private BitmapLruCache mCache;

	public static boolean isApiUT11 = false;
	
	@Override
	public void onCreate() {

		super.onCreate();


		isApiUT11 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

		mContext = this.getApplicationContext();

		File cacheLocation;

		// If we have external storage use it for the disk cache. Otherwise we
		// use the cache dir
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {
			cacheLocation = new File(Config.IMAGE_PATH);
		} else {
			cacheLocation = new File(getFilesDir() + "/.DBN-BitmapCache");
		}

		if (cacheLocation.exists() == false)
			cacheLocation.mkdirs();

		BitmapLruCache.Builder builder = new BitmapLruCache.Builder(this);
		builder.setMemoryCacheEnabled(true)
				.setMemoryCacheMaxSizeUsingHeapSize();
		builder.setDiskCacheEnabled(true).setDiskCacheLocation(cacheLocation);
		builder.setDiskCacheMaxSize(Config.MAX_DISK_CACHE_SIZE);

		mCache = builder.build();
	}

	public static Handler getUiHander() {
		return mUiHandler;
	}

	public static Context getAppExContext() {
		return mContext;
	}

	public static UglyPicApp getApplication(Context context) {
		return (UglyPicApp) context.getApplicationContext();
	}

	public BitmapLruCache getBitmapCache() {
		return mCache;
	}

}
