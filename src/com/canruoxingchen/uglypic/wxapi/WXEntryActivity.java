/**
 * 
 */
package com.canruoxingchen.uglypic.wxapi;

import android.app.Activity;
import android.os.Bundle;

import com.canruoxingchen.uglypic.sns.WeixinHelper;
import com.tencent.mm.sdk.openapi.BaseReq;
import com.tencent.mm.sdk.openapi.BaseResp;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;

/**
 * @author wsf
 *
 */
public class WXEntryActivity extends Activity implements IWXAPIEventHandler{
	
	private WeixinHelper mWeixinHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mWeixinHelper = new WeixinHelper(this);
		mWeixinHelper.onCreate(this, savedInstanceState);
		mWeixinHelper.handleIntent(getIntent(), this);
	}

	@Override
	public void onReq(BaseReq arg0) {
		
	}

	@Override
	public void onResp(BaseResp arg0) {
		
	}

}
