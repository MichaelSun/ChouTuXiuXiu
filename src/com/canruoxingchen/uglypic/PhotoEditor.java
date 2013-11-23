/**
 * 
 */
package com.canruoxingchen.uglypic;

import com.canruoxingchen.uglypic.cache.ImageInfo;

import uk.co.senab.photoview.PhotoView;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * 
 * 编辑器主界面
 * 
 * @author wsf
 * 
 */
public class PhotoEditor extends BaseActivity implements OnClickListener {

	private static final String EXTRA_PHOTO_URI = "photo_uri";

	private static final String KEY_PHOTO_URI = EXTRA_PHOTO_URI;

	private Uri mPhotoUri;

	/*-
	 * 各种View
	 */
	private PhotoView mPvPhoto;
	private View mTabScene;
	private View mTabWidget;
	private View mTabText;
	private View mTabFinish;

	/**
	 * 启动照片编辑页面
	 * 
	 * @param context
	 * @param photoUri
	 */
	public static void start(Context context, Uri photoUri) {
		Intent intent = new Intent(context, PhotoEditor.class);
		intent.putExtra(EXTRA_PHOTO_URI, photoUri);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		if (intent != null) {
			mPhotoUri = intent.getParcelableExtra(EXTRA_PHOTO_URI);
		}

		if (savedInstanceState != null) {
			mPhotoUri = savedInstanceState.getParcelable(KEY_PHOTO_URI);
		}

		initUI();
		initListers();

		//显示当前的照片
		if (mPhotoUri != null) {
			mPvPhoto.setImageInfo(ImageInfo.obtain(mPhotoUri.toString()));
		}
	}

	@Override
	protected void initUI() {
		setContentView(R.layout.photo_editor);
		
		mPvPhoto = (PhotoView) findViewById(R.id.photo_editor_photo);
		mTabScene = findViewById(R.id.photo_editor_tab_scene);
		mTabWidget = findViewById(R.id.photo_editor_tab_widget);
		mTabText = findViewById(R.id.photo_editor_tab_text);
		mTabFinish = findViewById(R.id.photo_editor_tab_finish);
	}

	@Override
	protected void initListers() {
		mTabScene.setOnClickListener(this);
		mTabWidget.setOnClickListener(this);
		mTabText.setOnClickListener(this);
		mTabFinish.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()) {
		case R.id.photo_editor_tab_scene:
			break;
		case R.id.photo_editor_tab_widget:
			break;
		case R.id.photo_editor_tab_text:
			break;
		case R.id.photo_editor_tab_finish:
			break;
		}
	}

}
