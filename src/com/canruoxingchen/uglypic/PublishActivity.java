/**
 * 
 */
package com.canruoxingchen.uglypic;

import java.io.File;

import com.actionbarsherlock.view.MenuItem;
import com.canruoxingchen.uglypic.cache.AsyncImageView;
import com.canruoxingchen.uglypic.cache.ImageInfo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
	
	private Uri mOriginUri;
	private String mResultPath;
	
	private AsyncImageView mAivImage;
	private EditText mEtDesc;
	
	public static void start(Context context, Uri originUri, String resultPath) {
		Intent intent = new Intent(context, PublishActivity.class);
		intent.putExtra(EXTRA_ORIGIN, originUri);
		intent.putExtra(EXTRA_RESULT, resultPath);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		if(intent != null) {
			mOriginUri = intent.getParcelableExtra(EXTRA_ORIGIN);
			mResultPath = intent.getStringExtra(EXTRA_RESULT);
		}
		
		if(savedInstanceState != null) {
			mOriginUri = savedInstanceState.getParcelable(KEY_ORIGIN);
			mResultPath = savedInstanceState.getString(KEY_RESULT);
		}
		
		initUI();
		initListers();
		
		mAivImage.setImageInfo(ImageInfo.obtain(Uri.fromFile(new File(mResultPath)).toString()));
	}

	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(KEY_ORIGIN, mOriginUri);
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
	}

	@Override
	protected void initListers() {
		
	}

}
