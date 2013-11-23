package com.canruoxingchen.uglypic.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.canruoxingchen.uglypic.Config;

/**
 * 
 * @Description 网络相关工具类
 *
 */

public class NetworkUtils {

	/**
	 * 检测网络连接是否正常
	 * 
	 * @param context
	 * @return true when network is available, otherwise return false
	 */
	public static boolean checkNetworkAvailable(Context context) {
		if (context == null) {
			LOGD("[[checkNetworkAvailable]] check context null");
			return false;
		}

		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			LOGD("[[checkNetworkAvailable]] connectivity null");
			return false;
		}

		NetworkInfo[] info = connectivity.getAllNetworkInfo();
		if (info != null) {
			for (int i = 0; i < info.length; i++) {
				if (info[i].getState() == NetworkInfo.State.CONNECTED) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 
	 * 获取网络类型，由1~5分别代表移动，电信，联通，wifi，其他
	 * 
	 * @return
	 */
	public static String getNetworkType(Context context) {
		if (context != null) {
			if (isWifiActive(context)) {
				return String.valueOf(4);
			} else {
				return String.valueOf(getOperatorName(context));
			}
		}
		return " ";
	}

	/**
	 * 
	 * 判断当前网络是否是Wifi网络
	 * 
	 * @param icontext
	 * @return
	 */
	public static boolean isWifiActive(Context icontext) {
		Context context = icontext.getApplicationContext();
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] info;
		if (connectivity != null) {
			info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getTypeName().equals("WIFI") &&

					info[i].isConnected()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * 
	 * 获取运营商的类型，1为移动，2为电信，3为联通,5为其他
	 * 
	 * @return
	 */
	public static int getOperatorName(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager) context
				.getApplicationContext().getSystemService(
						Context.TELEPHONY_SERVICE);
		String operator = telephonyManager.getSimOperator();
		if (operator != null) {
			if (operator.equals("46000") || operator.equals("46002")) {
				// 移动
				return 1;
			} else if (operator.equals("46003")) {
				// 电信
				return 2;
			} else if (operator.equals("46001")) {
				// 联通
				return 3;
			} else {
				return 5;
			}
		}
		return 5;
	}
	
	public static String getIMEI(Context context) {
        TelephonyManager mTelephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = (mTelephonyMgr == null ? "" : mTelephonyMgr.getDeviceId());
        return (imei == null ? "" : imei);
    }
	
	

	private static void LOGD(String message) {
		if (Config.DEBUG) {
			Log.i("Network Utils", message);
		}
	}

}
