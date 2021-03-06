/**
 * 
 */
package com.canruoxingchen.uglypic.sns;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;

import com.canruoxingchen.uglypic.MessageCenter;
import com.canruoxingchen.uglypic.R;
import com.canruoxingchen.uglypic.SettingManager;
import com.canruoxingchen.uglypic.UglyPicApp;
import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WeiboMessage;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.api.share.IWeiboDownloadListener;
import com.sina.weibo.sdk.api.share.IWeiboHandler;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.SendMultiMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuth;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;

/**
 * 
 * 微博分享逻辑封装
 * 
 * @author Shaofeng Wang
 * 
 */
public class WeiboHelper {
	private static final String APP_ID = "431566928";
	private static final String REDIRECT_URL = "http://newchinar.com";
	private static final String SCOPE = "email,direct_messages_read,direct_messages_write,"
			+ "friendships_groups_read,friendships_groups_write,statuses_to_me_read,"
			+ "follow_app_official_microblog," + "invitation_write";

	private WeiboAuth mWeiboAuth;

	/**
	 * ailed to find the associated render view host for url:
	 * http://newchinar.com
	 * /?access_token=2.004IxRwB0uQoMT6f0599bd77r4T_gE&remind_in
	 * =7526137&expires_in=7526137&uid=1777439211
	 */
	private Oauth2AccessToken mAccessToken = null;

	private SettingManager mSettingManager = null;

	private MessageCenter mMessageCenter = null;

	private SsoHandler mSsoHandler = null;

	private IWeiboShareAPI mWeiboShareAPI = null;

	public WeiboHelper(Context context) {
		mWeiboAuth = new WeiboAuth(context, APP_ID, REDIRECT_URL, SCOPE);
		mMessageCenter = MessageCenter.getInstance(context);
		mSettingManager = SettingManager.getInstance();
		mAccessToken = mSettingManager.readAccessToken(context);
		mWeiboShareAPI = WeiboShareSDK.createWeiboAPI(context, APP_ID);
	}


	boolean hasAuthorized() {
		return mAccessToken != null && mAccessToken.isSessionValid();
	}

	void onCreate(Activity activity, Bundle savedInstanceState) {
		// 如果未安装微博客户端，设置下载微博对应的回调
		if (!mWeiboShareAPI.isWeiboAppInstalled()) {
			mWeiboShareAPI.registerWeiboDownloadListener(new IWeiboDownloadListener() {
				@Override
				public void onCancel() {

				}
			});
		}

		if (savedInstanceState != null) {
			if (activity instanceof IWeiboHandler.Response) {
				mWeiboShareAPI.handleWeiboResponse(activity.getIntent(), (IWeiboHandler.Response) activity);
			}
		}
	}

	void onNewIntent(Activity activity, Intent intent) {
		if (activity instanceof IWeiboHandler.Response) {
			mWeiboShareAPI.handleWeiboResponse(intent, (IWeiboHandler.Response) activity);
		}
	}

	/**
	 * 必须在发起分享的onActivityResult中调用
	 * 
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	void authorizeCallBack(int requestCode, int resultCode, Intent data) {
		if (mSsoHandler == null) {
			return;
		}
		mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
	}

	void authorize(Activity activity) {
		if (mSsoHandler == null) {
			mSsoHandler = new SsoHandler(activity, mWeiboAuth);
		}
		mSsoHandler.authorize(new AuthListener());
	}

	void share(String content, String filePath) {
		// 检查微博客户端环境是否正常，如果未安装微博，弹出对话框询问用户下载微博客户端
		if (mWeiboShareAPI.checkEnvironment(true)) {
			// 注册第三方应用 到微博客户端中，注册成功后该应用将显示在微博的应用列表中。
			mWeiboShareAPI.registerApp();
			// } else {
			if (mWeiboShareAPI.isWeiboAppSupportAPI()) {
				int supportApi = mWeiboShareAPI.getWeiboAppSupportAPI();
				if (supportApi >= 10351 /* ApiUtils.BUILD_INT_VER_2_2 */) {
					WeiboMultiMessage message = new WeiboMultiMessage();
					message.textObject = createTextObj(content);
					message.imageObject = createImageObj(filePath);
					SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
					// 用transaction唯一标识一个请求
					request.transaction = String.valueOf(System.currentTimeMillis());
					request.multiMessage = message;
					// 3. 发送请求消息到微博，唤起微博分享界面
					mWeiboShareAPI.sendRequest(request);
				} else {

					WeiboMessage weiboMessage = new WeiboMessage();
					weiboMessage.mediaObject = createImageObj(filePath);
					// 2. 初始化从第三方到微博的消息请求
					SendMessageToWeiboRequest request = new SendMessageToWeiboRequest();
					// 用transaction唯一标识一个请求
					request.transaction = String.valueOf(System.currentTimeMillis());
					request.message = weiboMessage;

					// 3. 发送请求消息到微博，唤起微博分享界面
					mWeiboShareAPI.sendRequest(request);
				}
			}
		}
	}

	private ImageObject createImageObj(String filePath) {
		ImageObject imageObject = new ImageObject();
		BitmapDrawable bitmapDrawable = new BitmapDrawable(filePath);
		imageObject.setImageObject(bitmapDrawable.getBitmap());
		return imageObject;
	}

	private TextObject createTextObj(String content) {
		content = content == null ? "" : content;
		TextObject textObject = new TextObject();
		textObject.text = content;
		return textObject;
	}

	class AuthListener implements WeiboAuthListener {
		@Override
		public void onComplete(Bundle values) {
			// 从 Bundle 中解析 Token
			mAccessToken = Oauth2AccessToken.parseAccessToken(values);
			if (mAccessToken.isSessionValid()) {
				// 存入sharedpreferences
				mSettingManager.writeAccessToken(UglyPicApp.getAppExContext(), mAccessToken);
				mMessageCenter.notifyHandlers(R.id.msg_weibo_auth_success);
			} else {
				// 当您注册的应用程序签名不正确时，就会收到 Code，请确保签名正确
				mMessageCenter.notifyHandlers(R.id.msg_weibo_auth_failure);
			}
		}

		@Override
		public void onCancel() {
			mMessageCenter.notifyHandlers(R.id.msg_weibo_auth_failure);
		}

		@Override
		public void onWeiboException(WeiboException arg0) {
			mMessageCenter.notifyHandlers(R.id.msg_weibo_auth_failure);
		}
	}
	
	public static void LOGD(String logMe) {
		Log.d("Weibo", logMe);
	}
}
