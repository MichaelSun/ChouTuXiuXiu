/**
 * 
 */
package com.canruoxingchen.uglypic;

import java.io.File;

import com.actionbarsherlock.view.MenuItem;
import com.canruoxingchen.uglypic.cache.AsyncImageView;
import com.canruoxingchen.uglypic.cache.ImageInfo;
import com.canruoxingchen.uglypic.view.SlipButton;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

/**
 * 
 * 发布页面
 * 
 * @author wsf
 *
 */
public class PublishActivity extends BaseSherlockActivity{
	
	private static final String EXTRA_ORIGIN = "origin";
	private static final String EXTRA_RESULT = "result";
	
	public static final String KEY_ORIGIN = EXTRA_ORIGIN;
	public static final String KEY_RESULT = EXTRA_RESULT;
	
	private String mOriginPath;
	private String mResultPath;
	
	private AsyncImageView mAivImage;
	private EditText mEtDesc;
	
	private SlipButton mSlipBtn = null;
	private View mViewToWeibo;
	private View mViewToWeixin;
	private View mViewToFriends;
	
	private boolean mIsOrig = false;
	
	public static void start(Context context, String originPath, String resultPath) {
		Intent intent = new Intent(context, PublishActivity.class);
		intent.putExtra(EXTRA_ORIGIN, originPath);
		intent.putExtra(EXTRA_RESULT, resultPath);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		if(intent != null) {
			mOriginPath = intent.getStringExtra(EXTRA_ORIGIN);
			mResultPath = intent.getStringExtra(EXTRA_RESULT);
		}
		
		if(savedInstanceState != null) {
			mOriginPath = savedInstanceState.getString(KEY_ORIGIN);
			mResultPath = savedInstanceState.getString(KEY_RESULT);
		}
		
		initUI();
		initListers();
		
		mAivImage.setImageInfo(ImageInfo.obtain(Uri.fromFile(new File(mResultPath)).toString()));
	}

	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_ORIGIN, mOriginPath);
		outState.putString(KEY_RESULT, mResultPath);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void initUI() {
		setContentView(R.layout.publish);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);
		
		mAivImage = (AsyncImageView) findViewById(R.id.share_pic);
		mEtDesc = (EditText) findViewById(R.id.share_desc);
		mSlipBtn = (SlipButton) findViewById(R.id.publish_slip_btn);
		mViewToWeibo = findViewById(R.id.publish_share_to_weibo);
		mViewToWeixin = findViewById(R.id.publish_share_to_weixin);
		mViewToFriends = findViewById(R.id.publish_share_to_friends);
	}

	@Override
	protected void initListers() {
		mSlipBtn.SetOnChangedListener(new SlipButton.OnChangedListener() {
			
			@Override
			public void OnChanged(View v, boolean checkState) {
				if(checkState) {
					mIsOrig = true;
					mAivImage.setImageInfo(ImageInfo.obtain(Uri.fromFile(new File(mOriginPath)).toString()));
				} else {
					mIsOrig = false;
					mAivImage.setImageInfo(ImageInfo.obtain(Uri.fromFile(new File(mResultPath)).toString()));
				}
			}
		});
	}

}
