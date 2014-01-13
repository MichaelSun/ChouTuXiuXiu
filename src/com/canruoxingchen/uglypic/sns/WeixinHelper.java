package com.canruoxingchen.uglypic.sns;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.canruoxingchen.uglypic.util.ImageUtils;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXImageObject;
import com.tencent.mm.sdk.openapi.WXMediaMessage;

/**
 * 微信分享相关封装
 * 
 * @author wsf
 * 
 */
public class WeixinHelper {
	private static final String APPID = "wx375024ff3d6e6707";
	private static final int THUMB_SIZE = 120;

	private IWXAPI mWXApi;

	public WeixinHelper(Context context) {

	}

	public void handleIntent(Intent intent, IWXAPIEventHandler handler) {
		mWXApi.handleIntent(intent, handler);
	}

	public void onCreate(Activity activity, Bundle savedInstanceState) {
		mWXApi = WXAPIFactory.createWXAPI(activity, APPID, true);
		mWXApi.registerApp(APPID);
	}

	private void share(String content, String imagePath, int scene) {
		Log.d("Weixin", content + ", imagePath=" + imagePath);
		WXImageObject imgObj = new WXImageObject();
		imgObj.setImagePath(imagePath);

		// WXTextObject textObj = new WXTextObject();
		// textObj.text = "测试";
		WXMediaMessage msg = new WXMediaMessage();
		msg.mediaObject = imgObj;
		Bitmap thumb = WeixinUtil.extractThumbNail(imagePath, THUMB_SIZE, THUMB_SIZE, true);
		msg.thumbData = WeixinUtil.bmpToByteArray(thumb, true);
		// msg.description = "测试";

		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.transaction = buildTransaction("img");
		req.message = msg;
		req.scene = scene;
		mWXApi.sendReq(req);
	}

	void shareToFriends(String content, String imagePath) {
		share(content, imagePath, SendMessageToWX.Req.WXSceneTimeline);
	}

	void shareToWeixin(String content, String imagePath) {
		share(content, imagePath, SendMessageToWX.Req.WXSceneSession);
	}

	private String buildTransaction(final String type) {
		return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
	}
}
