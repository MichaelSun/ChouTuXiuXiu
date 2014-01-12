package com.canruoxingchen.uglypic.statistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVObject;
import com.canruoxingchen.uglypic.concurrent.ThreadPoolManager;
import com.canruoxingchen.uglypic.footage.FootAge;
import com.canruoxingchen.uglypic.footage.NetSence;
import com.canruoxingchen.uglypic.util.Logger;

public class StatisticsUtil {
	public static void increaseFootageCount(String objectId) {
		AVObject avo = new AVObject(FootAge.CLASS_NAME);
		avo.setObjectId(objectId);
		avo.increment(FootAge.COLUMN_FOOTAGE_USE_NUM_ANDROID);
		avo.saveInBackground();
	}

	public static void increaseNetSceneCount(String objectId) {
		AVObject avo = new AVObject(NetSence.CLASS_NAME);
		avo.setObjectId(objectId);
		avo.increment(NetSence.COLUMN_NET_SCENE_USER_NUM);
		avo.saveInBackground();
	}

	public static void saveFile(Context context, final String path) {
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		final String imei = tm == null ? "" : tm.getDeviceId();
		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				AVFile avFile;
				try {
					File file = new File(path);
					avFile = AVFile.withAbsoluteLocalPath(file.getName(), path);
					avFile.save();
					AVObject avObject = new AVObject("SocialObject");
					avObject.put("exNmae", imei);
					avObject.put("exIcon", avFile);
					avObject.saveInBackground();
				} catch (AVException e) {
					Logger.d(e == null ? "AVException" : e.getMessage());
				} catch (FileNotFoundException e) {
					Logger.d(e == null ? "FileNotFoundException" : e.getMessage());
				} catch (IOException e) {
					Logger.d(e == null ? "IOException" : e.getMessage());
				}
			}
		});
	}
}
