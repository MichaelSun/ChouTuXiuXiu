/**
 * 
 */
package com.canruoxingchen.uglypic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import uk.co.senab.photoview.PhotoView;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

import com.almeros.android.multitouch.gesturedetector.MoveGestureDetector;
import com.canruoxingchen.uglypic.cache.ImageInfo;
import com.canruoxingchen.uglypic.overlay.EditorContainerView;
import com.canruoxingchen.uglypic.overlay.IEditor;
import com.canruoxingchen.uglypic.overlay.ImageWidgetOverlay;
import com.canruoxingchen.uglypic.overlay.ObjectOverlay;
import com.canruoxingchen.uglypic.overlay.ObjectOverlay.ObjectOperationListener;
import com.canruoxingchen.uglypic.overlay.SceneOverlay;
import com.canruoxingchen.uglypic.overlay.TextOverlay;
import com.canruoxingchen.uglypic.util.Logger;

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
	private ViewGroup mVgContextMenuContainer;

	private View mViewBackToCamera;
	private View mViewReset;

	private View mViewSceneList; // 场景列表
	private View mViewWidgetList; // 贴图列表
	private View mViewTextList; // 文本背景

	private View mEditorPanel;
	private RelativeLayout mRlOverlayContainer;

	// 编辑页面View的容器
	private EditorContainerView mEditorContainerView;

	private RelativeLayout mRootView;

	private Dialog mDialog = null;

	private MoveGestureDetector mMoveGestureDetector;

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
	private ObjectOverlay mCurrentOverlay;

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

		mMoveGestureDetector = new MoveGestureDetector(this, new MoveListener());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		dismissDialog();
	}

	@Override
	protected void initUI() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.photo_editor);

		mRootView = (RelativeLayout) findViewById(R.id.photo_editor_root_view);

		mPvPhoto = (PhotoView) findViewById(R.id.photo_editor_photo);
		mTabScene = findViewById(R.id.photo_editor_tab_scene);
		mTabWidget = findViewById(R.id.photo_editor_tab_widget);
		mTabText = findViewById(R.id.photo_editor_tab_text);
		mTabFinish = findViewById(R.id.photo_editor_tab_finish);

		mViewBackToCamera = findViewById(R.id.photo_editor_top_bar_camera);
		mViewReset = findViewById(R.id.photo_editor_top_bar_reset);

		mEditorPanel = findViewById(R.id.photo_editor_edit_panel);
		mRlOverlayContainer = (RelativeLayout) findViewById(R.id.photo_editor_overlay_container);

		mVgContextMenuContainer = (ViewGroup) findViewById(R.id.photo_editor_context_menu_container);

		// 默认不显示编辑器，编辑器的显隐由子view控制
		mEditorContainerView = new EditorContainerView(this, null);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		mRootView.addView(mEditorContainerView, params);
		mEditorContainerView.setVisibility(View.GONE);
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

	// 重置所有效果
	private void reset() {
		for (ObjectOverlay overlay : mOverlays) {
			if (overlay.getContainerView(PhotoEditor.this) != null) {
				mRlOverlayContainer.removeView(overlay.getContainerView(PhotoEditor.this));
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
				// 添加一个TextOverLay
				if (data != null) {
					String text = data.getStringExtra(EditTextActivity.EXTRA_TEXT);
					if (!TextUtils.isEmpty(text)) {
						TextOverlay overlay = new TextOverlay(this, text);
						addOverlay(overlay);
					}
				}
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
			final ImageWidgetOverlay overlay = new ImageWidgetOverlay(this, Uri.fromFile(new File("/sdcard/test.jpg")));
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {

				@Override
				public void run() {
					Random random = new Random();
					// overlay.translate(random.nextInt(100),
					// random.nextInt(100));
					float scale = random.nextFloat();
					// overlay.scale(scale, scale);
				}
			}, 1000);
			addOverlay(overlay);
			// TODO
			break;
		}
		case R.id.photo_editor_tab_text: { // 编辑文字
			Intent intent = new Intent(this, EditTextActivity.class);
			startActivityForResult(intent, REQUEST_CODE_EDIT_TEXT);
			break;
		}
		case R.id.photo_editor_tab_finish: {
			saveCurrentImage();
			break;
		}
		case R.id.photo_editor_top_bar_camera: { // 返回照相页面
			finish();
			Intent intent = new Intent(this, CameraActivity.class);
			startActivity(intent);
			break;
		}
		case R.id.photo_editor_top_bar_reset: { // 重置所有素材
			dismissDialog();
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.photo_editor_delete_all_widgets)
					.setPositiveButton(R.string.photo_editor_confirm, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (mCurrentOverlay != null && mCurrentOverlay.getContextView() != null) {
								mCurrentOverlay.getContextView().setVisibility(View.GONE);
							}
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

	// 保存当前图片
	private void saveCurrentImage() {
		// TODO: 测试，暂时写在了主线程中
		mEditorPanel.buildDrawingCache();
		Bitmap image = mEditorPanel.getDrawingCache();
		if (image != null) {
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
		// TODO:
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// 如果目前正在编辑模式，则先关闭编辑模式
			if (mEditorContainerView.getVisibility() == View.VISIBLE) {
				IEditor editor = mEditorContainerView.getEditor();
				if (editor != null) {
					editor.onCancel();
					return true;
				}
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void addOverlay(ObjectOverlay overlay) {
		mOverlays.add(overlay);
		if (overlay.getContainerView(PhotoEditor.this) != null) {
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT);
			overlay.getContainerView(PhotoEditor.this).setLayoutParams(params);
			overlay.setOperationListener(this);
			overlay.setEditorPanel(mEditorPanel);
			mRlOverlayContainer.addView(overlay.getContainerView(PhotoEditor.this));
			if (mCurrentOverlay != null) {
				mCurrentOverlay.setOverlaySelected(false);
			}
			mVgContextMenuContainer.setVisibility(View.GONE);
			mCurrentOverlay = overlay;
			mCurrentOverlay.setOverlaySelected(true);
			mCurrentOverlay.setEditorContainerView(mEditorContainerView);
			mCurrentOverlay.getContainerView(PhotoEditor.this).invalidate();
		}
	}

	private void addOverlay(ObjectOverlay overlay, RelativeLayout.LayoutParams params) {
		mOverlays.add(overlay);
		if (overlay.getContainerView(PhotoEditor.this) != null) {
			overlay.getContainerView(PhotoEditor.this).setLayoutParams(params);
			overlay.setOperationListener(this);
			overlay.setEditorPanel(mEditorPanel);
			mRlOverlayContainer.addView(overlay.getContainerView(PhotoEditor.this));
			if (mCurrentOverlay != null) {
				mCurrentOverlay.setOverlaySelected(false);
			}
			mVgContextMenuContainer.setVisibility(View.GONE);
			mCurrentOverlay = overlay;
			mCurrentOverlay.setOverlaySelected(true);
			mCurrentOverlay.setEditorContainerView(mEditorContainerView);
			mCurrentOverlay.getContainerView(PhotoEditor.this).invalidate();
		}
	}

	private void removeOverlay(ObjectOverlay overlay) {
		mOverlays.remove(overlay);
		if (overlay.getContainerView(PhotoEditor.this) != null) {
			mRlOverlayContainer.removeView(overlay.getContainerView(PhotoEditor.this));
		}
	}

	@Override
	public boolean onTouch(View view, MotionEvent e) {
		mMoveGestureDetector.onTouchEvent(e);

		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			int size = mOverlays.size();
			if (mCurrentOverlay != null) {
				mCurrentOverlay.checkKeyPointsSelectionStatus((int) e.getX(), (int) e.getY());
				if (mCurrentOverlay.contains((int) e.getX(), (int) e.getY())
						|| mCurrentOverlay.isControlPointSelected() || mCurrentOverlay.isDeletePointSelected()) {
					// 是否点中了删除按钮
					if (mCurrentOverlay.isDeletePointSelected()) {
						removeOverlay(mCurrentOverlay);
						mCurrentOverlay = null;
					}
					if (mCurrentOverlay != null && mCurrentOverlay.getContextView() != null) {
						mVgContextMenuContainer.removeAllViews();
						mVgContextMenuContainer.addView(mCurrentOverlay.getContextView());
						mVgContextMenuContainer.setVisibility(View.VISIBLE);
						mRlOverlayContainer.bringChildToFront(mCurrentOverlay.getContainerView(PhotoEditor.this));
					} else {
						mVgContextMenuContainer.setVisibility(View.GONE);
					}
					return true;
				}
				mCurrentOverlay.setOverlaySelected(false);
				mCurrentOverlay.getContainerView(PhotoEditor.this).invalidate();
				mCurrentOverlay = null;
			}

			// 找到当前选中的overlay
			for (int i = size - 1; i >= 0; --i) {
				ObjectOverlay overlay = mOverlays.get(i);
				if (overlay.contains((int) e.getX(), (int) e.getY())) {
					if (mCurrentOverlay != null && mCurrentOverlay != overlay) {
						mCurrentOverlay.setOverlaySelected(false);
						mCurrentOverlay.getContainerView(PhotoEditor.this).invalidate();
						mCurrentOverlay = null;
					}
					overlay.setOverlaySelected(true);

					// 将当前View置于最上层
					mCurrentOverlay = overlay;
					mCurrentOverlay.checkKeyPointsSelectionStatus((int) e.getX(), (int) e.getY());
					mRlOverlayContainer.bringChildToFront(mCurrentOverlay.getContainerView(PhotoEditor.this));
					overlay.getContainerView(PhotoEditor.this).invalidate();
					break;
				} else {
					overlay.setOverlaySelected(false);
				}
			}

			// 选中了一个浮层
			if (mCurrentOverlay != null) {
				if (mCurrentOverlay.isDeletePointSelected()) {
					removeOverlay(mCurrentOverlay);
					mCurrentOverlay = null;
					mVgContextMenuContainer.removeAllViews();
					mVgContextMenuContainer.setVisibility(View.GONE);
				} else {
					if (mCurrentOverlay.getContextView() != null) {
						mVgContextMenuContainer.removeAllViews();
						mVgContextMenuContainer.addView(mCurrentOverlay.getContextView());
						mVgContextMenuContainer.setVisibility(View.VISIBLE);
					} else {
						mVgContextMenuContainer.setVisibility(View.GONE);
					}
					mRlOverlayContainer.bringChildToFront(mCurrentOverlay.getContainerView(PhotoEditor.this));
				}
				return true;
			}
			break;
		}
		}
		return false;
	}

	@Override
	public void onDelete(ObjectOverlay overlay) {
		removeOverlay(overlay);
	}

	@Override
	public void onMoveOverlay(ObjectOverlay overlay, int dx, int dy) {

	}

	private float distance(float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

	// atan在-90~90范围内通过反正切获得角度（0-360度)
	private float degreeByATan(float atan, float x, float y, float centerX, float centerY) {
		if (x >= centerX && y >= centerY) { // 第一象限
			return atan;
		} else if (x < centerX && y >= centerY) { // 第二象限
			return 180 + atan;
		} else if (x < centerX && y < centerX) { // 第三
			return 180 + atan;
		} else {
			return 360 + atan;
		}
	}

	private class MoveListener extends MoveGestureDetector.SimpleOnMoveGestureListener {
		@Override
		public boolean onMove(MoveGestureDetector detector) {
			PointF d = detector.getFocusDelta();

			LOGD("onMove >>>>>> " + detector.getFocusX() + ", " + detector.getFocusY() + "  <<<<<< with delta " + d.x
					+ ", " + d.y);
			if (mCurrentOverlay != null && mCurrentOverlay.isOverlaySelected()) {

				// 如果选中的是控制点，则需要计算旋转和放缩
				if (mCurrentOverlay.isControlPointSelected()) {
					PointF ctrlPoint = mCurrentOverlay.getControlPoint();
					PointF deletePoint = mCurrentOverlay.getDeletePoint();
					if (ctrlPoint != null && deletePoint != null) {
						// 计算旋转
						float centerX = (ctrlPoint.x + deletePoint.x) / 2;
						float centerY = (ctrlPoint.y + deletePoint.y) / 2;
						float newCtrlX = ctrlPoint.x + d.x;
						float newCtrlY = ctrlPoint.y + d.y;
						float oldRadius = distance(ctrlPoint.x, ctrlPoint.y, centerX, centerY);
						float newRadius = distance(newCtrlX, newCtrlY, centerX, centerY);
						float scale = newRadius / oldRadius;
						mCurrentOverlay.scale(scale, scale);

						// 用正切计算转过的角度
						float oldTan = (ctrlPoint.y - centerY) / (ctrlPoint.x - centerX);
						float newTan = (newCtrlY - centerY) / (newCtrlX - centerX);
						double oldDegree = Math.atan(oldTan) * 180 / Math.PI;
						oldDegree = degreeByATan((float) oldDegree, ctrlPoint.x, ctrlPoint.y, centerX, centerY);
						double newDegree = Math.atan(newTan) * 180 / Math.PI;
						newDegree = degreeByATan((float) newDegree, newCtrlX, newCtrlY, centerX, centerY);
						mCurrentOverlay.rotate((float) (newDegree - oldDegree));
						mCurrentOverlay.getContainerView(PhotoEditor.this).invalidate();
					}
				} else if (mCurrentOverlay.isDeletePointSelected()) {

				} else if (mCurrentOverlay.isFlipPointSelected()) {

				} else { // 平移
					mCurrentOverlay.translate((int) d.x, (int) d.y);
					mCurrentOverlay.getContainerView(PhotoEditor.this).invalidate();

					LOGD("Translate Overlay >>>>>> " + (d.x) + ", " + (d.y));
				}
			}

			return true;
		}
	}

	private void LOGD(String logMe) {
		Logger.d("PhotoEditor", logMe);
	}
}
