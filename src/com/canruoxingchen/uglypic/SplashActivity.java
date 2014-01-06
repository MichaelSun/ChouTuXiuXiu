/**
 * 
 */
package com.canruoxingchen.uglypic;

import android.content.Intent;
import android.os.Bundle;

import com.umeng.update.UmengUpdateAgent;

/**
 * 
 * 欢迎页面
 * 
 * @author wsf
 *
 */
public class SplashActivity extends BaseActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UmengUpdateAgent.update(this);
		
		initUI();
		initListers();
		
		UglyPicApp.getUiHander().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				finish();
				Intent intent = new Intent(SplashActivity.this, CameraActivity.class);
				startActivity(intent);
			}
		}, 1000);
	}

	@Override
	protected void initUI() {
		
	}

	@Override
	protected void initListers() {
		
	}
	
}
