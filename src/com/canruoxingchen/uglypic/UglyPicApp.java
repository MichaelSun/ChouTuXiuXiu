/**
 * DbnApp.java
 */
package com.canruoxingchen.uglypic;

import java.io.File;
import java.util.Calendar;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.avos.avoscloud.AVOSCloud;
import com.canruoxingchen.uglypic.footage.FootageManager;
import com.canruoxingchen.uglypic.util.Logger;

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

		SettingManager.getInstance().init(this);

		isApiUT11 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

		mContext = this.getApplicationContext();

		File cacheLocation;

		// If we have external storage use it for the disk cache. Otherwise we
		// use the cache dir
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			cacheLocation = new File(Config.IMAGE_PATH);
		} else {
			cacheLocation = new File(getFilesDir() + "/.DBN-BitmapCache");
		}

		if (cacheLocation.exists() == false)
			cacheLocation.mkdirs();

		BitmapLruCache.Builder builder = new BitmapLruCache.Builder(this);
		builder.setMemoryCacheEnabled(true).setMemoryCacheMaxSizeUsingHeapSize();
		builder.setDiskCacheEnabled(true).setDiskCacheLocation(cacheLocation);
		builder.setDiskCacheMaxSize(Config.MAX_DISK_CACHE_SIZE);

		mCache = builder.build();

		// 初始化AVOSCloud
		AVOSCloud.useAVCloudCN();
		AVOSCloud.initialize(this, "aawgg77nu8r9ore93ak5gc7lxgx3krb8y2rdo1e9nljpp8dw",
				"217pymwx0ndt4f67gqzj4nuy5zf6q1i142shmnhianu6gobu");

		// 初始化素材类型列表
		long lastLoadTypesTimeStamp = SettingManager.getInstance().getLastLoadTypesTime();
		long now = System.currentTimeMillis();
		Calendar.getInstance().setTimeInMillis(lastLoadTypesTimeStamp);
		int lastDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
		Calendar.getInstance().setTimeInMillis(now);
		int thisDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
		Logger.d("last: " + lastLoadTypesTimeStamp + ", now: " + now + ", lastDay: " + lastDay + ", thisDay: "
				+ thisDay);

		// 如果日期不一样，则加载一次类型列表
		if (lastDay != thisDay || lastLoadTypesTimeStamp == 0L) {
			loadFootageTypes();
		}
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

	private void loadFootageTypes() {
		FootageManager.getInstance(this).loadFootageTypeFromServer();
		SettingManager.getInstance().saveLastLoadTypesTime(System.currentTimeMillis());
	}
}
