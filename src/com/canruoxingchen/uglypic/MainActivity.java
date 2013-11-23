package com.canruoxingchen.uglypic;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * 选照片入口页
 * 
 * @author wsf
 *
 */
public class MainActivity extends BaseActivity implements OnClickListener {
	
	private static final int REQUEST_CODE_PICK_PHOTO = 101;
	
	//选择照片入口
	private View mViewCamera;
	private View mViewGallery;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initUI();
		initListers();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	protected void initUI() {
		setContentView(R.layout.activity_main);
		
		mViewCamera = findViewById(R.id.main_photo_src_camera);
		mViewGallery = findViewById(R.id.main_photo_src_gallery);
	}

	@Override
	protected void initListers() {
		mViewCamera.setOnClickListener(this);
		mViewGallery.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()) {
		case R.id.main_photo_src_camera: //从相机选图
			PickPhotoActivity.startForPhotoFromCamera(this, REQUEST_CODE_PICK_PHOTO);
			break;
		case R.id.main_photo_src_gallery: //从相册选图
			PickPhotoActivity.startForPhotoFromGallery(this, REQUEST_CODE_PICK_PHOTO);
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
		case REQUEST_CODE_PICK_PHOTO: {
			if(resultCode == RESULT_OK) {
				if(data != null && data.getData() != null) {
					Uri photoUri = data.getData();
					PhotoEditor.start(this, photoUri);
				} else {
					//TODO: 异常处理
				}
			}
			break;
		}
		}
	}
	
}
