package com.canruoxingchen.uglypic;

import com.avos.avoscloud.LogUtil.log;
import com.canruoxingchen.uglypic.sns.WeiboHelper;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SettingManager {
    private static SettingManager mInstance;

    private Context mContext;

    private SharedPreferences mSharedPreferences;

    private Editor mEditor;

    public static synchronized SettingManager getInstance() {
        if (mInstance == null) {
            mInstance = new SettingManager();
        }

        return mInstance;
    }

    private static final String SHARE_PREFERENCE_NAME = "setting_manager_share_pref";

    // 在Application中一定要调用
    public void init(Context context) {
        mContext = context.getApplicationContext();
        mSharedPreferences = mContext.getSharedPreferences(SHARE_PREFERENCE_NAME, 0);
        mEditor = mSharedPreferences.edit();
    }

    private SettingManager() {
   
    }

    public void saveLastLoadTypesTime(long time) {
    	mEditor.putLong(mContext.getString(R.string.pref_last_load_footage_type_time), time).commit();
    }
    
    public long getLastLoadTypesTime() {
    	return mSharedPreferences.getLong(mContext.getString(R.string.pref_last_load_footage_type_time), 0L);
    }
    
    public void saveWeiboTokenInfo(String tokenString) {
    	mEditor.putString(mContext.getString(R.string.pref_weibo_access_token), tokenString);
    	mEditor.commit();
    }
    
    /**
     * 保存 Token 对象到 SharedPreferences。
     * 
     * @param context 应用程序上下文环境
     * @param token   Token 对象
     */
    public void writeAccessToken(Context context, Oauth2AccessToken token) {
        if (null == context || null == token) {
            return;
        }
        
        WeiboHelper.LOGD("-----save------ + exprires time: " + token.getExpiresTime());
        
        mEditor.putString(mContext.getString(R.string.pref_weibo_uid), token.getUid());
        mEditor.putString(mContext.getString(R.string.pref_weibo_access_token), token.getToken());
        mEditor.putLong(mContext.getString(R.string.pref_weibo_expires_in), token.getExpiresTime());
        mEditor.commit();
    }

    /**
     * 从 SharedPreferences 读取 Token 信息。
     * 
     * @param context 应用程序上下文环境
     * 
     * @return 返回 Token 对象
     */
    public Oauth2AccessToken readAccessToken(Context context) {
        if (null == context) {
            return null;
        }
        
        Oauth2AccessToken token = new Oauth2AccessToken();
        token.setUid(mSharedPreferences.getString(mContext.getString(R.string.pref_weibo_uid), ""));
        token.setToken(mSharedPreferences.getString(mContext.getString(R.string.pref_weibo_access_token), ""));
        token.setExpiresTime(mSharedPreferences.getLong(mContext.getString(R.string.pref_weibo_expires_in), 0));
        return token;
    }

    public String getAccessToken() {
    	return mSharedPreferences.getString(mContext.getString(R.string.pref_weibo_access_token), "");
    }    
    
    public void clearWeiboInfo() {
    	  mEditor.remove(mContext.getString(R.string.pref_weibo_uid));
          mEditor.remove(mContext.getString(R.string.pref_weibo_access_token));
          mEditor.remove(mContext.getString(R.string.pref_weibo_expires_in));
          mEditor.commit();
    }
}
