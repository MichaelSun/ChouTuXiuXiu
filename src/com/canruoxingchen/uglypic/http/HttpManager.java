/**
 * 
 */
package com.canruoxingchen.uglypic.http;

import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.GetCallback;
import com.canruoxingchen.uglypic.util.Logger;

/**
 * 
 * 服务器接口的封装
 * 
 * author wsf
 *
 */
public class HttpManager {
	
	private static final String TAG = "HttpManager";
	
	public static void query(CloudObj cloudObj, String objectId, GetCallback<AVObject> callback) {
		AVQuery<AVObject> query = new AVQuery<AVObject>(cloudObj.getName());
		query.getInBackground(objectId, callback);
 	}
	
	private static void LOGD(String logMe) {
		Logger.d(TAG, logMe);
	}
}
