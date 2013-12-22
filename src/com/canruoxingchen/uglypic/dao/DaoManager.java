/**
 * DaoFactory.java
 */
package com.canruoxingchen.uglypic.dao;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.canruoxingchen.uglypic.Config;
import com.canruoxingchen.uglypic.util.Logger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;


/**
 * 管理Dao对象
 * 
 * @author Shaofeng Wang
 * 
 *         2013-3-24
 */
public class DaoManager {
	
	private static final String TAG = "DaoFactory";
	
	private static volatile DaoManager sInstance = null;

	private SQLiteDatabase mDb = null;
	private ReleaseOpenHelper mHelper = null;
	private DaoMaster mDaoMaster = null;
	private DaoSession mDaoSession = null;
	
	private static final byte[] sLockObj = new byte[0];
	
	public static DaoManager getInstance(Context context) {
		if(sInstance == null) {
			synchronized (sLockObj) {
				if(sInstance == null) {
					sInstance = new DaoManager(context);
				}
			}
		}
		return sInstance;
	}
	
	private DaoManager(Context context) {
		Context appContext = context.getApplicationContext();
//		mHelper = new DaoMaster.DevOpenHelper(appContext, Config.DB_NAME, null);
		mHelper = new ReleaseOpenHelper(appContext, Config.DB_NAME, null);
		mDb = mHelper.getWritableDatabase();
		mDaoMaster = new DaoMaster(mDb);
		mDaoSession = mDaoMaster.newSession();
	}

	/**
	 * 通过类名获取相应的Dao对象
	 * @param <T>
	 * 
	 * @param className
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T getDao(Class<T> clazz) {
		if(clazz == null) {
			return null;
		}
		
		String className = clazz.getSimpleName();
		if(!TextUtils.isEmpty(className)) {
			String getDaoMethodName = "get" + className;
			try {
				Method getDao = DaoSession.class.getDeclaredMethod(getDaoMethodName);
				if(getDao != null) {
					return (T) getDao.invoke(mDaoSession);
				}
			} catch (NoSuchMethodException e) {
				LOGD(e.getMessage());
			} catch (IllegalArgumentException e) {
				LOGD(e.getMessage());
			} catch (IllegalAccessException e) {
				LOGD(e.getMessage());
			} catch (InvocationTargetException e) {
				LOGD(e.getMessage());
			} catch (ClassCastException e) {
				LOGD(e.getMessage());
			}
		}
		return null;
	}
	
	private void LOGD(final String logMe) {
		Logger.d(TAG, logMe);
	}
}
