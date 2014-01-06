/**
 * 
 */
package com.canruoxingchen.uglypic;

import java.io.File;

import com.canruoxingchen.uglypic.cache.ImageInfo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import uk.co.senab.photoview.PhotoView;

/**
 * 
 * 查看结果
 * 
 * @author wsf
 * 
 */
public class ViewUglyPicActivity extends BaseActivity {

	private static final String EXTRA_IMAGE_PATH = "image_path";
	private static final String KEY_IMAGE_PATH = EXTRA_IMAGE_PATH;

	private PhotoView mPvPic;
	private View mViewFinish;
	private String mImagePath;

	public static void start(Context context, String imagePath) {
		Intent intent = new Intent(context, ViewUglyPicActivity.class);
		intent.putExtra(EXTRA_IMAGE_PATH, imagePath);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		if (intent != null) {
			mImagePath = intent.getStringExtra(KEY_IMAGE_PATH);
		}

		if (savedInstanceState != null) {
			mImagePath = savedInstanceState.getString(KEY_IMAGE_PATH);
		}

		initUI();
		initListers();
		if (!TextUtils.isEmpty(mImagePath)) {
			mPvPic.setImageInfo(ImageInfo.obtain(Uri.fromFile(new File(mImagePath)).toString()));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(outState != null) {
			outState.putString(KEY_IMAGE_PATH, mImagePath);
		}
	}

	@Override
	protected void initUI() {
		setContentView(R.layout.view_pic);
		mPvPic = (PhotoView) findViewById(R.id.ugly_pic);
		mViewFinish = findViewById(R.id.finish);
	}

	@Override
	protected void initListers() {
		mViewFinish.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
}
