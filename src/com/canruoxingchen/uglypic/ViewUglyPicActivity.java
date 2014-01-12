/**
 * 
 */
package com.canruoxingchen.uglypic;

import java.io.File;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;

import com.canruoxingchen.uglypic.cache.ImageInfo;

/**
 * 
 * 查看结果
 * 
 * @author wsf
 * 
 */
public class ViewUglyPicActivity extends BaseActivity {

	private static final String EXTRA_IMAGE_PATH = "image_path";
	private static final String EXTRA_IMAGE_ONLY_VIEW = "only_view";
	private static final String KEY_IMAGE_PATH = EXTRA_IMAGE_PATH;
	private static final String KEY_IMAGE_ONLY_VIEW = EXTRA_IMAGE_ONLY_VIEW;

	private PhotoView mPvPic;
	private View mViewFinish;
	private String mImagePath;
	
	private boolean mOnlyView = false;

	public static void start(Activity context, int requestCode, String imagePath, boolean onlyView) {
		Intent intent = new Intent(context, ViewUglyPicActivity.class);
		intent.putExtra(EXTRA_IMAGE_PATH, imagePath);
		intent.putExtra(EXTRA_IMAGE_ONLY_VIEW, onlyView);
		context.startActivityForResult(intent, requestCode);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		if (intent != null) {
			mImagePath = intent.getStringExtra(EXTRA_IMAGE_PATH);
			mOnlyView = intent.getBooleanExtra(EXTRA_IMAGE_ONLY_VIEW, false);
		}

		if (savedInstanceState != null) {
			mImagePath = savedInstanceState.getString(KEY_IMAGE_PATH);
			mOnlyView = savedInstanceState.getBoolean(KEY_IMAGE_ONLY_VIEW, false);
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
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.view_pic);
		mPvPic = (PhotoView) findViewById(R.id.ugly_pic);
		mViewFinish = findViewById(R.id.finish);
		if(mOnlyView) {
			mViewFinish.setVisibility(View.GONE);
		}
	}

	@Override
	protected void initListers() {
		mViewFinish.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_FIRST_USER);
				Intent intent = new Intent(ViewUglyPicActivity.this, CameraActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
			}
		});
		mPvPic.setOnPhotoTapListener(new OnPhotoTapListener() {
			
			@Override
			public void onPhotoTap(View view, float x, float y) {
				finish();
			}
		});
	}
}
