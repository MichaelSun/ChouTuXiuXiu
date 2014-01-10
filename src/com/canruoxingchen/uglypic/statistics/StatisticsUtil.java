package com.canruoxingchen.uglypic.statistics;

import java.io.File;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVObject;
import com.canruoxingchen.uglypic.footage.FootAge;
import com.canruoxingchen.uglypic.footage.NetSence;

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

	public static void saveFile(Context context, String path) {
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = tm == null ? "" : tm.getDeviceId();
		AVFile avFile;
		try {
			AVObject avObject = new AVObject("SocialObject");
			File file = new File(path);
			avFile = new AVFile(file.getName(), path);
			avFile.save();
			avObject.put("exNmae", imei);
			avObject.put("exIcon", avFile);
			avObject.saveInBackground();
		} catch (AVException e) {
			
		}
	}
}
