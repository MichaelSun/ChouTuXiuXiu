/**
 * 
 */
package com.canruoxingchen.uglypic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photoview.PhotoView;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;

import com.canruoxingchen.uglypic.cache.ImageInfo;
import com.canruoxingchen.uglypic.overlay.IOverlay;
import com.canruoxingchen.uglypic.overlay.ImageWidgetOverlay;
import com.canruoxingchen.uglypic.overlay.ObjectOverlay;
import com.canruoxingchen.uglypic.overlay.ObjectOverlay.ObjectOperationListener;
import com.canruoxingchen.uglypic.overlay.SceneOverlay;

/**
 * 
 * 编辑器主界面
 * 
 * @author wsf
 * 
 */
public class PhotoEditor extends BaseActivity implements OnClickListener, OnTouchListener, ObjectOperationListener {

	private static final String EXTRA_PHOTO_URI = "photo_uri";

	private static final String KEY_PHOTO_URI = EXTRA_PHOTO_URI;

	private static final int REQUEST_CODE_EDIT_TEXT = 1001;

	// 原始照片的uri
	private Uri mPhotoUri;

	/*-
	 * 各种View
	 */
	private PhotoView mPvPhoto;
	private View mTabScene;
	private View mTabWidget;
	private View mTabText;
	private View mTabFinish;

	private View mViewBackToCamera;
	private View mViewReset;

	private View mViewSceneList; // 场景列表
	private View mViewWidgetList; // 贴图列表
	private View mViewTextList; // 文本背景

	private View mEditroView;
	private RelativeLayout mRlOverlayContainer;

	private Dialog mDialog = null;

	/**
	 * 添加在图片上的浮层
	 */
	private List<ObjectOverlay> mOverlays = new ArrayList<ObjectOverlay>();
	/**
	 * 场景浮层，整张图只能有一个场景浮层
	 */
	private SceneOverlay mSceneOverlay = null;
	/**
	 * 当前被选中的浮层
	 */
	private IOverlay mCurrentOverlay;

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

		// 显示当前的照片
		if (mPhotoUri != null) {
			mPvPhoto.setImageInfo(ImageInfo.obtain(mPhotoUri.toString()));
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		dismissDialog();
	}

	@Override
	protected void initUI() {
		setContentView(R.layout.photo_editor);

		mPvPhoto = (PhotoView) findViewById(R.id.photo_editor_photo);
		mTabScene = findViewById(R.id.photo_editor_tab_scene);
		mTabWidget = findViewById(R.id.photo_editor_tab_widget);
		mTabText = findViewById(R.id.photo_editor_tab_text);
		mTabFinish = findViewById(R.id.photo_editor_tab_finish);

		mViewBackToCamera = findViewById(R.id.photo_editor_top_bar_camera);
		mViewReset = findViewById(R.id.photo_editor_top_bar_reset);

		mEditroView = findViewById(R.id.photo_editor_edit_panel);
		mRlOverlayContainer = (RelativeLayout) findViewById(R.id.photo_editor_overlay_container);
	}

	@Override
	protected void initListers() {
		mTabScene.setOnClickListener(this);
		mTabWidget.setOnClickListener(this);
		mTabText.setOnClickListener(this);
		mTabFinish.setOnClickListener(this);

		mViewBackToCamera.setOnClickListener(this);
		mViewReset.setOnClickListener(this);

		mRlOverlayContainer.setOnTouchListener(this);
	}

	// 应用效果
	private void performEffect() {

	}

	// 重置所有效果
	private void reset() {
		for (ObjectOverlay overlay : mOverlays) {
			if(overlay.getView() != null) {
				mRlOverlayContainer.removeView(overlay.getView());
			}
		}
		mOverlays.clear();
	}

	// 关闭对话框
	private void dismissDialog() {
		if (mDialog != null && mDialog.isShowing()) {
			mDialog.dismiss();
			mDialog = null;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_CODE_EDIT_TEXT: { // 编辑了文本后，显示选择背景列表
			if (resultCode == RESULT_OK) {
				dismissAllLists();
				showTextList();
			}
			break;
		}
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.photo_editor_tab_scene: {
			dismissAllLists();
			showSceneList();
			break;
		}
		case R.id.photo_editor_tab_widget: {
			dismissAllLists();
			showWidgetList();
			// TODO: 添加一个图片widget
			ImageWidgetOverlay overlay = new ImageWidgetOverlay(this, Uri.fromFile(new File("/sdcard/test.jpg")));
			// TODO
			addOverlay(overlay);
			break;
		}
		case R.id.photo_editor_tab_text: { // 编辑文字
			Intent intent = new Intent(this, EditTextActivity.class);
			startActivityForResult(intent, REQUEST_CODE_EDIT_TEXT);
			break;
		}
		case R.id.photo_editor_tab_finish: {
			performEffect();
			saveCurrentImage();
			break;
		}
		case R.id.photo_editor_top_bar_camera: { // 返回照相页面
			finish();
			break;
		}
		case R.id.photo_editor_top_bar_reset: { // 重置所有素材
			dismissDialog();
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.photo_editor_delete_all_widgets)
					.setPositiveButton(R.string.photo_editor_confirm, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							reset();
						}
					}).setNegativeButton(R.string.photo_editor_cancel, null);
			mDialog = builder.show();
			break;
		}
		}
	}

	// 隐藏所有的列表
	private void dismissAllLists() {
		if (mViewSceneList != null) {
			mViewSceneList.setVisibility(View.GONE);
		}
		if (mViewTextList != null) {
			mViewTextList.setVisibility(View.GONE);
		}
		if (mViewWidgetList != null) {
			mViewWidgetList.setVisibility(View.GONE);
		}
	}
	
	
	//保存当前图片
	private void saveCurrentImage() {
		//TODO: 测试，暂时写在了主线程中
		mEditroView.buildDrawingCache();
		Bitmap image = mEditroView.getDrawingCache();
		if(image != null) {
			try {
				image.compress(CompressFormat.JPEG, 80, new FileOutputStream(new File("/sdcard/result.jpg")));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			image.recycle();
			Toast.makeText(this, "success", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "fail", Toast.LENGTH_SHORT).show();
		}
		//TODO:
	}

	// 显示场景列表
	private void showSceneList() {

	}

	// 显示文本背景列表
	private void showTextList() {

	}

	// 显示贴图列表
	private void showWidgetList() {

	}

	private void addOverlay(ObjectOverlay overlay) {
		mOverlays.add(overlay);
		if (overlay.getView() != null) {
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT);
			overlay.getView().setLayoutParams(params);
			overlay.setOperationListener(this);
			mRlOverlayContainer.addView(overlay.getView());
		}
	}

	private void removeOverlay(ObjectOverlay overlay) {
		mOverlays.remove(overlay);
		if (overlay.getView() != null) {
			mRlOverlayContainer.removeView(overlay.getView());
		}
	}

	@Override
	public boolean onTouch(View view, MotionEvent e) {
		return false;
	}

	@Override
	public void onDelete(ObjectOverlay overlay) {
		removeOverlay(overlay);
	}

	@Override
	public void onMoveOverlay(ObjectOverlay overlay, int dx, int dy) {

	}
}
