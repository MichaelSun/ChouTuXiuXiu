/**
 * 
 */
package com.canruoxingchen.uglypic;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photoview.PhotoView;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.almeros.android.multitouch.gesturedetector.MoveGestureDetector;
import com.canruoxingchen.uglypic.cache.AsyncImageView;
import com.canruoxingchen.uglypic.cache.ImageInfo;
import com.canruoxingchen.uglypic.footage.FootAge;
import com.canruoxingchen.uglypic.footage.FootAgeType;
import com.canruoxingchen.uglypic.footage.FootageManager;
import com.canruoxingchen.uglypic.overlay.EditorContainerView;
import com.canruoxingchen.uglypic.overlay.IEditor;
import com.canruoxingchen.uglypic.overlay.ImageWidgetOverlay;
import com.canruoxingchen.uglypic.overlay.ObjectOverlay;
import com.canruoxingchen.uglypic.overlay.ObjectOverlay.ObjectOperationListener;
import com.canruoxingchen.uglypic.overlay.SceneOverlay;
import com.canruoxingchen.uglypic.overlay.SceneOverlay.SceneSizeAquiredListener;
import com.canruoxingchen.uglypic.overlay.TextOverlay;
import com.canruoxingchen.uglypic.util.FileUtils;
import com.canruoxingchen.uglypic.util.ImageUtils;
import com.canruoxingchen.uglypic.util.Logger;
import com.canruoxingchen.uglypic.view.HorizontalListView;

/**
 * 
 * 编辑器主界面
 * 
 * @author wsf
 * 
 */
public class PhotoEditor extends BaseActivity implements OnClickListener, OnTouchListener, ObjectOperationListener,
		SceneSizeAquiredListener {
	private static final String EXTRA_PHOTO_URI = "photo_uri";

	private static final String KEY_PHOTO_URI = EXTRA_PHOTO_URI;

	private static final int REQUEST_CODE_EDIT_TEXT = 1001;

	// 原始照片的uri
	private Uri mPhotoUri;

	/*-
	 * 各种View
	 */
	private PhotoView mPvPhoto;
	private ViewGroup mVgContextMenuContainer;

	/**
	 * 图片调整的按钮
	 */
	private View mViewModify;
	private View mViewDelete;
	private View mViewEraser;
	private View mTopContextMenu;

	/**
	 * "分享"和"重置"
	 */
	private Button mViewContextBtn;

	/**
	 * 类型列表
	 */
	private HorizontalListView mLvTypes;
	/**
	 * 素材列表
	 */
	private HorizontalListView mLvFootages;

	private View mViewBackToCamera;

	private View mEditorPanel;
	private RelativeLayout mRlOverlayContainer;
	private View mEditorPanelRefView; // 参考View，用来获取宽高

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

	private List<FootAgeType> mFootageTypes = new ArrayList<FootAgeType>();
	private List<FootAge> mFootages = new ArrayList<FootAge>();
	private TypeAdapter mTypeAdapter = new TypeAdapter();
	private FootageAdapter mFootageAdapter = new FootageAdapter();

	private FootageManager mFootageManager;

	private FootAgeType mCurrentType;

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

	private MyHandler mHandler = null;

	private static class MyHandler extends Handler {

		public WeakReference<PhotoEditor> mActivity;

		public MyHandler(PhotoEditor activity) {
			mActivity = new WeakReference<PhotoEditor>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			PhotoEditor activity = mActivity.get();
			if (activity == null) {
				return;
			}
			switch (msg.what) {
			case FootageManager.MSG_LOAD_LOCAL_FOOTAGE_TYPES_SUCCESS: {
				List<FootAgeType> types = (List<FootAgeType>) msg.obj;
				LOGD(">>>>>>>>>>> load types from local >>>>>>>>>> " + types);
				if (types != null && types.size() > 0) {
					activity.mFootageTypes.addAll(types);
					activity.mTypeAdapter.notifyDataSetChanged();
					// 如果尚无选中的类型，则选择第一个
					if (activity.mCurrentType == null) {
						activity.mCurrentType = activity.mFootageTypes.get(2);
						activity.mFootageManager.loadLocalFootages(activity.mCurrentType.getObjectId());
					}
				} else {
					activity.mFootageManager.loadFootageTypeFromServer();
				}
				break;
			}
			case FootageManager.MSG_LOAD_LOCAL_FOOTAGE_TYPES_FAILURE: { // 从本地没有取到，则从服务器直接取数据
				activity.mFootageManager.loadFootageTypeFromServer();
				break;
			}
			case FootageManager.MSG_LOAD_FOOTAGE_TYPES_SUCCESS: {
				List<FootAgeType> types = (List<FootAgeType>) msg.obj;
				LOGD(">>>>>>>>>>> load types from server >>>>>>>>>> " + types);
				// 如果从本地未取到素材类型列表，则显示服务器取到的数据
				if (types != null && types.size() > 0 && activity.mFootageTypes.size() == 0) {
					activity.mFootageTypes.addAll(types);
					activity.mTypeAdapter.notifyDataSetChanged();
					// 如果尚无选中的类型，则选择第一个
					if (activity.mCurrentType == null) {
						activity.mCurrentType = activity.mFootageTypes.get(2);
						activity.mFootageManager.loadLocalFootages(activity.mCurrentType.getObjectId());
					}
				}
				break;
			}
			case FootageManager.MSG_LOAD_FOOTAGE_TYPES_FAILURE:
				break;
			case FootageManager.MSG_LOAD_FOOTAGE_SUCCESS: {// 显示本地的素材
				List<FootAge> footages = (List<FootAge>) msg.obj;
				LOGD("<<<<<<<<<<<<<<<<<<<load local footages>>>>>>>>>>>>>>>>>>>" + footages);
				if (footages != null && footages.size() > 0 && activity.mCurrentType != null) {
					// 判断当前的素材的id是否为当前选中的type
					FootAge footage = footages.get(0);
					if (footage.getParentId().equals(activity.mCurrentType.getObjectId())) {
						activity.mFootages.clear();
						activity.mFootages.addAll(footages);
						activity.mFootageAdapter.notifyDataSetChanged();
					}
				} else {
					if (activity.mCurrentType != null) {
						activity.mFootageManager.loadFootagesFromServer(activity.mCurrentType.getObjectId());
					}
				}
				break;
			}
			case FootageManager.MSG_LOAD_FOOTAGE_FAILURE:
				break;
			case FootageManager.MSG_LOAD_LOCAL_FOOTAGE_SUCCESS: {
				List<FootAge> footages = (List<FootAge>) msg.obj;
				LOGD("<<<<<<<<<<<<<<<<<<<load local footages>>>>>>>>>>>>>>>>>>>" + footages);
				// 显示本地的素材
				if (footages != null && footages.size() > 0 && activity.mCurrentType != null) {
					// 判断当前的素材的id是否为当前选中的type
					FootAge footage = footages.get(0);
					if (footage.getParentId().equals(activity.mCurrentType.getObjectId())) {
						activity.mFootages.clear();
						activity.mFootages.addAll(footages);
						activity.mFootageAdapter.notifyDataSetChanged();
					}
				} else {
					if (activity.mCurrentType != null) {
						activity.mFootageManager.loadFootagesFromServer(activity.mCurrentType.getObjectId());
					}
				}
				break;
			}
			case FootageManager.MSG_LOAD_LOCAL_FOOTAGE_FAILURE: {
				if (activity.mCurrentType != null) {
					activity.mFootageManager.loadFootagesFromServer(activity.mCurrentType.getObjectId());
				}
				break;
			}
			case FootageManager.MSG_LOAD_FOOTAGE_ICON_SUCCESS: {
				if (activity.mFootages != null) {
					FootAge footage = (FootAge) msg.obj;
					for (FootAge f : activity.mFootages) {
						if (footage == null || footage.getObjectId().equals(f.getObjectId())) {
							f.setIconUrl(footage.getIconUrl());
							activity.mFootageAdapter.notifyDataSetChanged();
							break;
						}
					}
				}
				break;
			}
			case FootageManager.MSG_LOAD_FOOTAGE_ICON_FAILURE: {
				break;
			}
			}
		}

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
		mTypeAdapter = new TypeAdapter();
		mFootageAdapter = new FootageAdapter();
		mLvTypes.setAdapter(mTypeAdapter);
		mLvFootages.setAdapter(mFootageAdapter);

		mHandler = new MyHandler(this);

		// 显示当前的照片
		if (mPhotoUri != null) {
			mPvPhoto.setImageInfo(ImageInfo.obtain(mPhotoUri.toString()));
		}

		mMoveGestureDetector = new MoveGestureDetector(this, new MoveListener());

		mFootageManager = FootageManager.getInstance(this);
		// 先从本地加载数据
		mFootageManager.loadFootageTypeFromLocal();
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
		// mTabScene = findViewById(R.id.photo_editor_tab_scene);
		// mTabWidget = findViewById(R.id.photo_editor_tab_widget);
		// mTabText = findViewById(R.id.photo_editor_tab_text);
		// mTabFinish = findViewById(R.id.photo_editor_tab_finish);

		mTopContextMenu = findViewById(R.id.photo_editor_topbar_object_menu);
		mViewModify = findViewById(R.id.photo_editor_top_bar_object_modify);
		mViewDelete = findViewById(R.id.photo_editor_top_bar_object_delete);
		mViewEraser = findViewById(R.id.photo_editor_top_bar_object_eraser);
		mViewContextBtn = (Button) findViewById(R.id.photo_editor_context_button);
		mLvTypes = (HorizontalListView) findViewById(R.id.photo_editor_footage_types_list);
		mLvFootages = (HorizontalListView) findViewById(R.id.photo_editor_footage_list);

		mViewBackToCamera = findViewById(R.id.photo_editor_top_bar_camera);

		mEditorPanel = findViewById(R.id.photo_editor_edit_panel);
		mEditorPanelRefView = findViewById(R.id.photo_editor_edit_panel_ref_view);
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
		// mTabScene.setOnClickListener(this);
		// mTabWidget.setOnClickListener(this);
		// mTabText.setOnClickListener(this);
		// mTabFinish.setOnClickListener(this);

		mViewModify.setOnClickListener(this);
		mViewDelete.setOnClickListener(this);
		mViewEraser.setOnClickListener(this);
		mViewContextBtn.setOnClickListener(this);

		mViewBackToCamera.setOnClickListener(this);

		mRlOverlayContainer.setOnTouchListener(this);

		mLvFootages.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final FootAge footage = mFootages.get(position);
				if (!TextUtils.isEmpty(footage.getIconUrl())) {
					ImageWidgetOverlay overlay = new ImageWidgetOverlay(PhotoEditor.this, Uri.parse(footage
							.getIconUrl()));
					addOverlay(overlay);
				}
			}

		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerAllFootageMsg();
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterAllFootageMsg();
	}

	private void registerAllFootageMsg() {
		MessageCenter messageCenter = MessageCenter.getInstance(this);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_FOOTAGE_FAILURE, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_FOOTAGE_SUCCESS, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_FOOTAGE_TYPES_FAILURE, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_FOOTAGE_TYPES_SUCCESS, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_LOCAL_FOOTAGE_FAILURE, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_LOCAL_FOOTAGE_SUCCESS, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_LOCAL_FOOTAGE_TYPES_FAILURE, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_LOCAL_FOOTAGE_TYPES_SUCCESS, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_FOOTAGE_ICON_SUCCESS, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_FOOTAGE_ICON_FAILURE, mHandler);
	}

	private void unregisterAllFootageMsg() {
		MessageCenter messageCenter = MessageCenter.getInstance(this);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_FOOTAGE_FAILURE, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_FOOTAGE_SUCCESS, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_FOOTAGE_TYPES_FAILURE, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_FOOTAGE_TYPES_SUCCESS, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_LOCAL_FOOTAGE_FAILURE, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_LOCAL_FOOTAGE_SUCCESS, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_LOCAL_FOOTAGE_TYPES_FAILURE, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_LOCAL_FOOTAGE_TYPES_SUCCESS, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_FOOTAGE_ICON_SUCCESS, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_FOOTAGE_ICON_FAILURE, mHandler);
	}

	// 重置所有效果
	private void reset() {
		for (ObjectOverlay overlay : mOverlays) {
			if (overlay.getContainerView(PhotoEditor.this) != null) {
				mRlOverlayContainer.removeView(overlay.getContainerView(PhotoEditor.this));
			}
		}
		mOverlays.clear();
		setSceneOverlay(null);
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
		// case R.id.photo_editor_tab_scene: {
		// dismissAllLists();
		// showSceneList();
		// // TODO: 添加一个背景
		// SceneOverlay.Builder builder = new SceneOverlay.Builder(this,
		// Uri.fromFile(new File("/sdcard/test_bg.png")));
		// builder.setTextBounds(116, 371, 500, 412);
		// builder.setTextHint("测试一下对不对");
		// builder.setTextSize(14);
		// setSceneOverlay(builder.create());
		// // TODO
		// break;
		// }
		// case R.id.photo_editor_tab_widget: {
		// dismissAllLists();
		// showWidgetList();
		// // TODO: 添加一个图片widget
		// final ImageWidgetOverlay overlay = new ImageWidgetOverlay(this,
		// Uri.fromFile(new File("/sdcard/test.jpg")));
		// Handler handler = new Handler();
		// handler.postDelayed(new Runnable() {
		//
		// @Override
		// public void run() {
		// Random random = new Random();
		// // overlay.translate(random.nextInt(100),
		// // random.nextInt(100));
		// float scale = random.nextFloat();
		// // overlay.scale(scale, scale);
		// }
		// }, 1000);
		// addOverlay(overlay);
		// // TODO
		// break;
		// }
		// case R.id.photo_editor_tab_text: { // 编辑文字
		// Intent intent = new Intent(this, EditTextActivity.class);
		// startActivityForResult(intent, REQUEST_CODE_EDIT_TEXT);
		// break;
		// }
		case R.id.photo_editor_context_button: {
			// 分享
			if (mViewContextBtn.getText().equals(getString(R.string.photo_editor_context_btn_share))) {
				saveCurrentImage();
			} else { // 重置

			}
			break;
		}
		case R.id.photo_editor_top_bar_camera: { // 返回照相页面
			finish();
			Intent intent = new Intent(this, CameraActivity.class);
			startActivity(intent);
			break;
		}
		// case R.id.photo_editor_top_bar_reset: { // 重置所有素材
		// dismissDialog();
		// AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// builder.setMessage(R.string.photo_editor_delete_all_widgets)
		// .setPositiveButton(R.string.photo_editor_confirm, new
		// DialogInterface.OnClickListener() {
		//
		// @Override
		// public void onClick(DialogInterface dialog, int which) {
		// if (mCurrentOverlay != null && mCurrentOverlay.getContextView() !=
		// null) {
		// mCurrentOverlay.getContextView().setVisibility(View.GONE);
		// }
		// reset();
		// }
		// }).setNegativeButton(R.string.photo_editor_cancel, null);
		// mDialog = builder.show();
		// break;
		// }
		case R.id.photo_editor_top_bar_object_modify: // 调整
			break;
		case R.id.photo_editor_top_bar_object_delete: // 删除当前的ObjecOverlay
			break;
		case R.id.photo_editor_top_bar_object_eraser: // 擦除
			break;
		}
	}

	// 保存当前图片
	private void saveCurrentImage() {
		// TODO: 测试，暂时写在了主线程中
		mEditorPanel.buildDrawingCache();
		Bitmap image = mEditorPanel.getDrawingCache();
		if (image != null && !image.isRecycled()) {
			String path = ImageUtils.saveBitmapForLocalPath(this, image, 0, true);
			if (path != null) {
				PublishActivity.start(this, mPhotoUri, path);
			} else {
				// TODO: 如果保存失败
			}
			image.recycle();
			Toast.makeText(this, "success", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "fail", Toast.LENGTH_SHORT).show();
		}
		mEditorPanel.destroyDrawingCache();
		// TODO:
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

	private void setSceneOverlay(SceneOverlay scene) {
		if (mSceneOverlay != null) {
			if (mSceneOverlay.getView() != null) {
				mRlOverlayContainer.removeView(mSceneOverlay.getView());
			}
		}

		if (scene != null) {
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT);
			scene.getView().setLayoutParams(params);
			mRlOverlayContainer.addView(scene.getView(), 0);
			scene.setViewSizeAdjustedListener(this);
		} else {
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mEditorPanel.getLayoutParams();
			params.width = LayoutParams.MATCH_PARENT;
			params.height = LayoutParams.MATCH_PARENT;
		}
		mSceneOverlay = scene;
	}

	private void unSelectAllOverlays() {
		for (ObjectOverlay overlay : mOverlays) {
			overlay.setOverlaySelected(false);
		}
	}

	private void addOverlay(ObjectOverlay overlay) {
		mOverlays.add(overlay);
		if (overlay.getContainerView(PhotoEditor.this) != null) {
			unSelectAllOverlays();
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT);
			overlay.getContainerView(PhotoEditor.this).setLayoutParams(params);
			overlay.setOperationListener(this);
			overlay.setEditorPanel(mEditorPanel);
			mRlOverlayContainer.addView(overlay.getContainerView(PhotoEditor.this));
			if (mCurrentOverlay != null) {
				mCurrentOverlay.setOverlaySelected(false);
				mCurrentOverlay.getContainerView(this).invalidate();
			}
			mVgContextMenuContainer.setVisibility(View.GONE);
			mCurrentOverlay = overlay;
			mCurrentOverlay.setOverlaySelected(true);
			mCurrentOverlay.setEditorContainerView(mEditorContainerView);
			mCurrentOverlay.getContainerView(PhotoEditor.this).invalidate();
		}
	}

	// private void addOverlay(ObjectOverlay overlay,
	// RelativeLayout.LayoutParams params) {
	// mOverlays.add(overlay);
	// if (overlay.getContainerView(PhotoEditor.this) != null) {
	// overlay.getContainerView(PhotoEditor.this).setLayoutParams(params);
	// overlay.setOperationListener(this);
	// overlay.setEditorPanel(mEditorPanel);
	// mRlOverlayContainer.addView(overlay.getContainerView(PhotoEditor.this));
	// if (mCurrentOverlay != null) {
	// mCurrentOverlay.setOverlaySelected(false);
	// }
	// mVgContextMenuContainer.setVisibility(View.GONE);
	// mCurrentOverlay = overlay;
	// mCurrentOverlay.setOverlaySelected(true);
	// mCurrentOverlay.setEditorContainerView(mEditorContainerView);
	// mCurrentOverlay.getContainerView(PhotoEditor.this).invalidate();
	// }
	// }

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

	@Override
	public void onSceneSizeAquired(int width, int height) {
		// 根据当前场景的尺寸调整大小
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mEditorPanel.getLayoutParams();
		int editorPanelWidth = mEditorPanelRefView.getWidth();
		int editorPanelHeight = mEditorPanelRefView.getHeight();
		float scaleX = (1.0f * editorPanelWidth) / (1.0f * width);
		float scaleY = (1.0f * editorPanelHeight) / (1.0f * height);
		if (scaleX > scaleY) { // 容器比背景胖，则调整宽
			params.topMargin = 0;
			params.bottomMargin = 0;
			int margin = (int) ((editorPanelWidth - width * scaleY) / 2);
			params.leftMargin = margin;
			params.rightMargin = margin;
		} else {
			params.leftMargin = 0;
			params.rightMargin = 0;
			int margin = (int) ((editorPanelHeight - height * scaleX) / 2);
			params.topMargin = margin;
			params.bottomMargin = margin;
		}
	}

	private class TypeAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mFootageTypes.size();
		}

		@Override
		public Object getItem(int position) {
			return mFootageTypes.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = View.inflate(PhotoEditor.this, R.layout.footage_type_item, null);
				TextView tvName = (TextView) convertView.findViewById(R.id.footage_type_item_name);
				convertView.setTag(tvName);
			}
			TextView tvName = (TextView) convertView.getTag();
			FootAgeType footageType = mFootageTypes.get(position);
			tvName.setText(footageType.getTypeName());

			tvName.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (position == 0) { // 常用的素材

					} else {
						FootAgeType type = mFootageTypes.get(position);
						if (type != null && mCurrentType != type) {
							mCurrentType = type;
							// 加载素材列表
							mFootageManager.loadLocalFootages(type.getObjectId());
						}
					}
				}
			});
			return convertView;
		}
	}

	private static class ViewHolder {
		private AsyncImageView aivIcon;
		private TextView tvName;
	}

	private class FootageAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mFootages.size();
		}

		@Override
		public Object getItem(int position) {
			return mFootages.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder = new ViewHolder();
			if (convertView == null) {
				convertView = View.inflate(PhotoEditor.this, R.layout.footage_item, null);
				viewHolder.aivIcon = (AsyncImageView) convertView.findViewById(R.id.footage_item_icon);
				viewHolder.tvName = (TextView) convertView.findViewById(R.id.footage_item_name);
				convertView.setTag(viewHolder);
			}
			viewHolder = (ViewHolder) convertView.getTag();
			final FootAge footage = mFootages.get(position);
			if (!TextUtils.isEmpty(footage.getIconUrl())) {
				viewHolder.aivIcon.setImageInfo(ImageInfo.obtain(footage.getIconUrl()));
				viewHolder.aivIcon.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						ImageWidgetOverlay overlay = new ImageWidgetOverlay(PhotoEditor.this, Uri.parse(footage
								.getIconUrl()));
						addOverlay(overlay);
					}
				});
			} else {
				// 尚未下载图片，则先下载
				viewHolder.aivIcon.setOnClickListener(null);
			}
			viewHolder.tvName.setText(footage.getIconName());

			return convertView;
		}

	}

	private static void LOGD(String logMe) {
		Logger.d("PhotoEditor", logMe);
	}
}
