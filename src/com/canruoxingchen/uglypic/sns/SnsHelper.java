/**
 * 
 */
package com.canruoxingchen.uglypic.sns;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * @author Shaofeng Wang
 *
 */
public class SnsHelper {
	private WeiboHelper mWeiboHelper;
	private WeixinHelper mWeixinHelper;
	
	public SnsHelper(Context context) {
		this.mWeiboHelper = new WeiboHelper(context);
		this.mWeixinHelper = new WeixinHelper(context);
	}
	
	public void onCreate(Activity activity, Bundle savedInstanceState) {
		mWeiboHelper.onCreate(activity, savedInstanceState);
		mWeixinHelper.onCreate(activity, savedInstanceState);
	}
	
	public void onNewIntent(Activity activity, Intent intent) {
		mWeiboHelper.onNewIntent(activity, intent);
	}
	
	public void authorizeCallBack(int requestCode, int resultCode, Intent data) {
		mWeiboHelper.authorizeCallBack(requestCode, resultCode, data);
	}
	
	public void authorizeToWeibo(Activity acitivty) {
		mWeiboHelper.authorize(acitivty);
	}
	
	public boolean hasWeiboAuthorized() {
		return mWeiboHelper.hasAuthorized();
	}

	/**
	 * 分享到微博
	 * @param content
	 * @param imagePath
	 */
	public void shareToWeibo(String content, String imagePath) {
		mWeiboHelper.share(content, imagePath);
	}
	
	/**
	 * 分享到微信
	 * @param content
	 * @param imagePath
	 */
	public void shareToWeixin(String content, String imagePath) {
		mWeixinHelper.shareToWeixin(content, imagePath);
	}
	
	/**
	 * 分享到朋友圈
	 * @param content
	 * @param imagePath
	 */
	public void shareToFriends(String content, String imagePath) {
		mWeixinHelper.shareToFriends(content, imagePath);
	}
}
