/**
 * UiUtils.java
 */
package com.canruoxingchen.uglypic.util;

import android.content.Context;
import android.widget.Toast;

/**
 * Toast等UI相关方法
 * 
 */
public class UiUtils {
	public static Toast toast;

	/**
	 * 弹出Toast消息
	 * 
	 * @param msg
	 */
	public static void toastMessage(Context cont, int msg) {
		toastMessage(cont, cont.getString(msg));
	}

	public static void toastMessage(Context cont, String msg) {
		synchronized (UiUtils.class) {
			if (toast == null) {
				toast = Toast.makeText(cont, "", Toast.LENGTH_SHORT);
			}
		}
		toast.setText(msg);
		toast.setDuration(Toast.LENGTH_SHORT);
		toast.show();
	}
}
