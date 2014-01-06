package com.canruoxingchen.uglypic;

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
    
    
    public String getAccessToken() {
    	return mSharedPreferences.getString(mContext.getString(R.string.pref_weibo_access_token), "");
    }    
}
