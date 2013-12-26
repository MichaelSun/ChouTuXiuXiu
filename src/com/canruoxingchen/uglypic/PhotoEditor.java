/**
 * 
 */
package com.canruoxingchen.uglypic;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photoview.PhotoView;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
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

import com.alibaba.fastjson.JSON;
import com.almeros.android.multitouch.gesturedetector.MoveGestureDetector;
import com.canruoxingchen.uglypic.cache.AsyncImageView;
import com.canruoxingchen.uglypic.cache.ImageInfo;
import com.canruoxingchen.uglypic.concurrent.ThreadPoolManager;
import com.canruoxingchen.uglypic.footage.FootAge;
import com.canruoxingchen.uglypic.footage.FootAgeType;
import com.canruoxingchen.uglypic.footage.FootageManager;
import com.canruoxingchen.uglypic.footage.NetSence;
import com.canruoxingchen.uglypic.footage.RecentFootAge;
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
import com.canruoxingchen.uglypic.util.UiUtils;
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
	private static final String EXTRA_PHOTO_PATH = "photo_uri";

	private static final String KEY_PHOTO_PATH = EXTRA_PHOTO_PATH;

	private static final int REQUEST_CODE_EDIT_TEXT = 1001;

	private static final int MSG_REGRET_STATUS_CHANGED = R.id.msg_editor_regret_status_change;
	private static final int MSG_ORIGIN_IMAGE_SAVED = R.id.msg_editor_origin_image_saved;

	// 原始照片的uri
	private Uri mPhotoUri;
	private String mImagePath;
	private String mCroppedPath;

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
	private View mViewModifyFinish;
	private View mViewBottomPanel;

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
	private List<Object> mFootages = new ArrayList<Object>();
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
	public static void start(Context context, String photoPath) {
		Intent intent = new Intent(context, PhotoEditor.class);
		intent.putExtra(EXTRA_PHOTO_PATH, photoPath);
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
					activity.mFootageTypes.clear();
					activity.mFootageTypes.add(FootAgeType.RECENT_TYPE);
					activity.mFootageTypes.addAll(types);
					activity.mTypeAdapter.notifyDataSetChanged();
					// 如果尚无选中的类型，则选择第一个
					if (activity.mCurrentType == null) {
						activity.mCurrentType = FootAgeType.RECENT_TYPE;
						activity.mFootageManager.loadRecentFootages();
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
					activity.mFootageTypes.clear();
					activity.mFootageTypes.add(FootAgeType.RECENT_TYPE);
					activity.mFootageTypes.addAll(types);
					activity.mTypeAdapter.notifyDataSetChanged();
					// 如果尚无选中的类型，则选择第一个
					if (activity.mCurrentType == null) {
						activity.mCurrentType = FootAgeType.RECENT_TYPE;
						activity.mFootageManager.loadRecentFootages();
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
			case FootageManager.MSG_LOAD_SCENES_SUCCESS: {// 显示本地的场景
				List<NetSence> scenes = (List<NetSence>) msg.obj;
				LOGD("<<<<<<<<<<<<<<<<<<<load local scenes>>>>>>>>>>>>>>>>>>>" + scenes);
				if (scenes != null && scenes.size() > 0 && activity.mCurrentType != null) {
					// 判断当前的素材的id是否为当前选中的type
					NetSence scene = scenes.get(0);
					if (scene.getSenceParentId().equals(activity.mCurrentType.getObjectId())) {
						activity.mFootages.clear();
						activity.mFootages.addAll(scenes);
						activity.mFootageAdapter.notifyDataSetChanged();
					}
				}
				break;
			}
			case FootageManager.MSG_LOAD_SCENES_FAILURE:
				break;
			case FootageManager.MSG_LOAD_LOCAL_SCENES_SUCCESS: {
				List<NetSence> scenes = (List<NetSence>) msg.obj;
				LOGD("<<<<<<<<<<<<<<<<<<<load local footages>>>>>>>>>>>>>>>>>>>" + scenes);
				// 显示本地的素材
				if (scenes != null && scenes.size() > 0 && activity.mCurrentType != null) {
					// 判断当前的素材的id是否为当前选中的type
					NetSence scene = scenes.get(0);
					if (scene.getSenceParentId().equals(activity.mCurrentType.getObjectId())) {
						activity.mFootages.clear();
						activity.mFootages.addAll(scenes);
						activity.mFootageAdapter.notifyDataSetChanged();
					}
				} else {
					if (activity.mCurrentType != null) {
						activity.mFootageManager.loadScenesFromServer(activity.mCurrentType.getObjectId());
					}
				}
				break;
			}
			case FootageManager.MSG_LOAD_LOCAL_SCENES_FAILURE: {
				if (activity.mCurrentType != null) {
					activity.mFootageManager.loadScenesFromServer(activity.mCurrentType.getObjectId());
				}
				break;
			}
			case FootageManager.MSG_LOAD_FOOTAGE_ICON_SUCCESS: {
				if (activity.mFootages != null) {
					if (msg.obj instanceof FootAge) {
						FootAge footage = (FootAge) msg.obj;
						for (Object obj : activity.mFootages) {
							if (obj instanceof NetSence || obj instanceof RecentFootAge) {
								break;
							}
							FootAge f = (FootAge) obj;
							if (footage != null && footage.getObjectId().equals(f.getObjectId())) {
								f.setIconUrl(footage.getIconUrl());
								activity.mFootageAdapter.notifyDataSetChanged();
								break;
							}
						}
					} else {
						NetSence netScene = (NetSence) msg.obj;
						for (Object obj : activity.mFootages) {
							if (obj instanceof FootAge || obj instanceof RecentFootAge) {
								break;
							}
							NetSence ns = (NetSence) obj;
							if (netScene != null && netScene.getObjectId().equals(ns.getObjectId())) {
								ns.setSenceNetIcon(netScene.getSenceNetIcon());
								activity.mFootageAdapter.notifyDataSetChanged();
								break;
							}
						}
					}
				}
				break;
			}
			case FootageManager.MSG_LOAD_FOOTAGE_ICON_FAILURE: {
				break;
			}
			case MSG_REGRET_STATUS_CHANGED: {
				if (activity.mEditorContainerView != null) {
					activity.mEditorContainerView.onRegretStatusChanged();
				}
				break;
			}
			case FootageManager.MSG_LOAD_RECENT_FOOTAGE_SUCCESS: {// 显示最近素材成功
				if (msg.obj == null) {
					break;
				}
				List<RecentFootAge> footages = (List<RecentFootAge>) msg.obj;
				if (activity.mCurrentType == FootAgeType.RECENT_TYPE) {
					activity.mFootages.clear();
					activity.mFootages.addAll(footages);
					activity.mFootageAdapter.notifyDataSetChanged();
				}
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
			mImagePath = intent.getStringExtra(EXTRA_PHOTO_PATH);
			mPhotoUri = Uri.fromFile(new File(mImagePath));
		}

		if (savedInstanceState != null) {
			mImagePath = savedInstanceState.getString(KEY_PHOTO_PATH);
			mPhotoUri = Uri.fromFile(new File(mImagePath));
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

		// 设置一个空场景
		mSceneOverlay = new SceneOverlay.Builder(this, null).create();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (outState != null) {
			outState.putString(KEY_PHOTO_PATH, mImagePath);
		}
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
		mViewBottomPanel = findViewById(R.id.photo_editor_bottom_panel);
		mPvPhoto = (PhotoView) findViewById(R.id.photo_editor_photo);

		mTopContextMenu = findViewById(R.id.photo_editor_topbar_object_menu);
		mViewModify = findViewById(R.id.photo_editor_top_bar_object_modify);
		mViewDelete = findViewById(R.id.photo_editor_top_bar_object_delete);
		mViewEraser = findViewById(R.id.photo_editor_top_bar_object_eraser);
		mViewContextBtn = (Button) findViewById(R.id.photo_editor_context_button);
		mLvTypes = (HorizontalListView) findViewById(R.id.photo_editor_footage_types_list);
		mLvFootages = (HorizontalListView) findViewById(R.id.photo_editor_footage_list);
		mViewModifyFinish = findViewById(R.id.photo_editor_topbar_modify_finish);

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
		mViewModify.setOnClickListener(this);
		mViewDelete.setOnClickListener(this);
		mViewEraser.setOnClickListener(this);
		mViewContextBtn.setOnClickListener(this);
		mViewModifyFinish.setOnClickListener(this);

		mViewBackToCamera.setOnClickListener(this);

		mRlOverlayContainer.setOnTouchListener(this);
		mEditorContainerView.setVisibilityChangeListener(new EditorContainerView.VisibilityChangeListener() {

			@Override
			public void onVisible() {
				mViewContextBtn.setVisibility(View.GONE);
			}

			@Override
			public void onInvisible() {
				mViewContextBtn.setVisibility(View.VISIBLE);
			}
		});

		mLvTypes.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				FootAgeType type = mFootageTypes.get(position);
				mCurrentType = type;
				switch (type.getTypeTarget()) {
				case FootAgeType.TYPE_RECENT: // 最近使用
					// TODO: 加载最近使用的素材
					mFootageManager.loadRecentFootages();
					break;
				case FootAgeType.TYPE_IMAGE: // 图片
					// TODO：加载footage
					mFootageManager.loadLocalFootages(type.getObjectId());
					break;
				case FootAgeType.TYPE_SCENE: // 场景
					// TODO: 加载场景
					mFootageManager.loadLocalScenes(type.getObjectId());
					break;
				}
			}
		});

		mLvFootages.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final Object obj = mFootages.get(position);
				if (obj instanceof FootAge) {
					FootAge footage = (FootAge) obj;
					onFootAgeClick(footage, true);
				} else if (obj instanceof NetSence) {
					NetSence netScene = (NetSence) obj;
					onSceneClick(netScene, true);
				} else if (obj instanceof RecentFootAge) {
					RecentFootAge recent = (RecentFootAge) obj;
					if (recent.getType() == FootAgeType.TYPE_IMAGE) {
						FootAge footage = JSON.parseObject(recent.getJson(), FootAge.class);
						onFootAgeClick(footage, true);
					} else if (recent.getType() == FootAgeType.TYPE_SCENE) {
						NetSence netScene = JSON.parseObject(recent.getJson(), NetSence.class);
						onSceneClick(netScene, true);
					}
				}
			}

		});
	}

	private void onFootAgeClick(FootAge footage, boolean save) {
		if (!TextUtils.isEmpty(footage.getIconUrl())) {
			ImageWidgetOverlay overlay = new ImageWidgetOverlay(PhotoEditor.this, Uri.parse(footage.getIconUrl()));
			addOverlay(overlay);
			if (save) {
				mFootageManager.saveRecentFootage(footage);
			}
		}
	}

	private void onSceneClick(NetSence netScene, boolean save) {
		if (!TextUtils.isEmpty(netScene.getSenceNetIcon())) {
			SceneOverlay.Builder builder = new SceneOverlay.Builder(PhotoEditor.this, Uri.parse(netScene
					.getSenceNetIcon()));
			Rect inputRect = netScene.getInputRectBounds();
			if (inputRect != null) {
				builder.setTextBounds(inputRect.left, inputRect.top, inputRect.right, inputRect.bottom);
				builder.setTextHint(netScene.getInputContent());
				builder.setTextSize(netScene.getInputFontSize());
				builder.setTextAlignment(netScene.getInputFontAlignment());
				builder.setTextColor(netScene.getInputFontColor());
				builder.setTextFontName(netScene.getInputFontName());
			}

			Rect timeRect = netScene.getTimeRectBounds();
			if (timeRect != null) {
				builder.setTimeBounds(timeRect.left, timeRect.top, timeRect.right, timeRect.bottom);
				builder.setTimeSize(netScene.getTimeFontSize());
				builder.setTimeAlignment(netScene.getTimeFontAlignment());
				builder.setTimeColor(netScene.getTimeFontColor());
				builder.setTimeFontName(netScene.getTimeFontName());
			}
			setSceneOverlay(builder.create());
			if (save) {
				mFootageManager.saveRecentFootage(netScene);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerAllFootageMsg();
		// 注册编辑内容可回退状态改变的消息
		MessageCenter.getInstance(this).registerMessage(R.id.msg_editor_regret_status_change, mHandler);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterAllFootageMsg();
		MessageCenter.getInstance(this).unregisterMessage(R.id.msg_editor_regret_status_change, mHandler);
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
		messageCenter.registerMessage(FootageManager.MSG_LOAD_LOCAL_SCENES_SUCCESS, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_LOCAL_SCENES_FAILURE, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_SCENES_SUCCESS, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_SCENES_FAILURE, mHandler);
		messageCenter.registerMessage(FootageManager.MSG_LOAD_RECENT_FOOTAGE_SUCCESS, mHandler);
	}

	private void unregisterAllFootageMsg() {
		MessageCenter messageCenter = MessageCenter.getInstance(this);
		messageCenter.unregisterMessage(FootageManager.MSG_LOAD_FOOTAGE_FAILURE, mHandler);
		messageCenter.unregisterMessage(FootageManager.MSG_LOAD_FOOTAGE_SUCCESS, mHandler);
		messageCenter.unregisterMessage(FootageManager.MSG_LOAD_FOOTAGE_TYPES_FAILURE, mHandler);
		messageCenter.unregisterMessage(FootageManager.MSG_LOAD_FOOTAGE_TYPES_SUCCESS, mHandler);
		messageCenter.unregisterMessage(FootageManager.MSG_LOAD_LOCAL_FOOTAGE_FAILURE, mHandler);
		messageCenter.unregisterMessage(FootageManager.MSG_LOAD_LOCAL_FOOTAGE_SUCCESS, mHandler);
		messageCenter.unregisterMessage(FootageManager.MSG_LOAD_LOCAL_FOOTAGE_TYPES_FAILURE, mHandler);
		messageCenter.unregisterMessage(FootageManager.MSG_LOAD_LOCAL_FOOTAGE_TYPES_SUCCESS, mHandler);
		messageCenter.unregisterMessage(FootageManager.MSG_LOAD_FOOTAGE_ICON_SUCCESS, mHandler);
		messageCenter.unregisterMessage(FootageManager.MSG_LOAD_FOOTAGE_ICON_FAILURE, mHandler);
		messageCenter.unregisterMessage(FootageManager.MSG_LOAD_LOCAL_SCENES_SUCCESS, mHandler);
		messageCenter.unregisterMessage(FootageManager.MSG_LOAD_LOCAL_SCENES_FAILURE, mHandler);
		messageCenter.unregisterMessage(FootageManager.MSG_LOAD_SCENES_SUCCESS, mHandler);
		messageCenter.unregisterMessage(FootageManager.MSG_LOAD_SCENES_FAILURE, mHandler);
		messageCenter.unregisterMessage(FootageManager.MSG_LOAD_RECENT_FOOTAGE_SUCCESS, mHandler);
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
				if (mCurrentOverlay != null && mCurrentOverlay instanceof ImageWidgetOverlay) {
					((ImageWidgetOverlay) mCurrentOverlay).reset();
				}
			}
			break;
		}
		case R.id.photo_editor_top_bar_camera: { // 返回照相页面
			finish();
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
		case R.id.photo_editor_top_bar_object_modify: {// 调整
			if (mCurrentOverlay != null && mCurrentOverlay.getContextView() != null) {
				mTopContextMenu.setVisibility(View.GONE);
				mViewModifyFinish.setVisibility(View.VISIBLE);
				mViewBottomPanel.setVisibility(View.INVISIBLE);

				// 显示调整view
				mVgContextMenuContainer.removeAllViews();
				mVgContextMenuContainer.addView(mCurrentOverlay.getContextView());
				mVgContextMenuContainer.setVisibility(View.VISIBLE);
				mRlOverlayContainer.bringChildToFront(mCurrentOverlay.getContainerView(PhotoEditor.this));
				// 显示完成按钮

				// 显示重置按钮
				mViewContextBtn.setText(R.string.photo_editor_context_btn_reset);
			} else {
				mVgContextMenuContainer.setVisibility(View.GONE);
			}
			break;
		}
		case R.id.photo_editor_top_bar_object_delete: { // 删除当前的ObjecOverlay
			if (mCurrentOverlay != null) {
				removeOverlay(mCurrentOverlay);
			}
			break;
		}
		case R.id.photo_editor_top_bar_object_eraser: { // 擦除
			if (mCurrentOverlay != null && mCurrentOverlay instanceof ImageWidgetOverlay) {
				((ImageWidgetOverlay) mCurrentOverlay).startErase();
			}
			break;
		}
		case R.id.photo_editor_topbar_modify_finish: // 编辑完成
			if (mCurrentOverlay != null && mCurrentOverlay.getContextView() != null) {
				mVgContextMenuContainer.setVisibility(View.GONE);
				mTopContextMenu.setVisibility(View.VISIBLE);
				mViewModifyFinish.setVisibility(View.GONE);
				mViewBottomPanel.setVisibility(View.VISIBLE);
			}
		}
	}

//	private void saveOriginImage() {
//		String savePath = FileUtils.createSdCardFile("photo_editor_origin.jpg");
//		if (TextUtils.isEmpty(savePath)) {
//			UiUtils.toastMessage(this, R.string.photo_editor_save_failure);
//			return;
//		}
//		File file = new File(savePath);
//		if (file.exists()) {
//			file.delete();
//		}
//		try {
//			file.createNewFile();
//		} catch (IOException e) {
//			UiUtils.toastMessage(this, R.string.photo_editor_save_failure);
//			return;
//		}
//
//		mPvPhoto.buildDrawingCache();
//		final Bitmap image = mPvPhoto.getDrawingCache();
//		mPvPhoto.destroyDrawingCache();
//		ThreadPoolManager.getInstance().execute(new Runnable() {
//
//			@Override
//			public void run() {
//				String path = ImageUtils.saveBitmapForLocalPath(UglyPicApp.getAppExContext(), image, 0, true);
//				if (TextUtils.isEmpty(path)) {
//					UglyPicApp.getUiHander().post(new Runnable() {
//
//						@Override
//						public void run() {
//							UiUtils.toastMessage(PhotoEditor.this, R.string.photo_editor_save_failure);
//							return;
//						}
//					});
//				} else {
//					if (mHandler != null) {
//						Message msg = Message.obtain(mHandler, MSG_ORIGIN_IMAGE_SAVED, path);
//						msg.sendToTarget();
//					}
//				}
//			}
//		});
//	}

	// 保存当前图片
	private void saveCurrentImage() {
		String savePath = FileUtils.createSdCardFile("photo_editor_origin.jpg");
		if (TextUtils.isEmpty(savePath)) {
			UiUtils.toastMessage(this, R.string.photo_editor_save_failure);
			return;
		}
		File file = new File(savePath);
		if (file.exists()) {
			file.delete();
		}
		try {
			file.createNewFile();
		} catch (IOException e) {
			UiUtils.toastMessage(this, R.string.photo_editor_save_failure);
			return;
		}
		mPvPhoto.buildDrawingCache();
		final Bitmap origin = mPvPhoto.getDrawingCache();
		if(origin == null) {
			UiUtils.toastMessage(this, R.string.photo_editor_save_failure);
			return;
		}
		int height = origin.getHeight();
		final Bitmap mergedImage = Bitmap.createBitmap(origin.getWidth(), 2 * origin.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(mergedImage);
		Paint paint = new Paint();
		paint.setFlags(Paint.ANTI_ALIAS_FLAG);
		canvas.drawBitmap(origin, 0, 0, paint);
		mPvPhoto.destroyDrawingCache();
		
		mEditorPanel.buildDrawingCache();
		final Bitmap processedImage = mEditorPanel.getDrawingCache();
//		mEditorPanel.destroyDrawingCache();
		canvas.drawBitmap(processedImage, 0, height, paint);
		if(processedImage == null) {
			UiUtils.toastMessage(this, R.string.photo_editor_save_failure);
			if(mergedImage != null && !mergedImage.isRecycled()) {
				mergedImage.recycle();
			}
			return;
		}
		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				String mergedPath =  ImageUtils.saveBitmapForLocalPath(UglyPicApp.getAppExContext(), mergedImage, 0, true);
				if(mergedImage != null && !mergedImage.isRecycled()) {
					mergedImage.recycle();
				}
				String processedPath = ImageUtils.saveBitmapForLocalPath(UglyPicApp.getAppExContext(), processedImage, 0, true);
				if(processedImage != null && !processedImage.isRecycled()) {
					processedImage.recycle();
				}
				if (TextUtils.isEmpty(mergedPath) || TextUtils.isEmpty(processedPath)) {
					UglyPicApp.getUiHander().post(new Runnable() {

						@Override
						public void run() {
							UiUtils.toastMessage(PhotoEditor.this, R.string.photo_editor_save_failure);
							return;
						}
					});
				} else {
					PublishActivity.start(PhotoEditor.this, mergedPath, processedPath);
				}
			}
		});
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
				mCurrentOverlay = null;
				mTopContextMenu.setVisibility(View.GONE);
			}
			mVgContextMenuContainer.setVisibility(View.GONE);
		}
	}

	private void flipOverlay(ObjectOverlay overlay) {
		if (overlay.isFlipable()) {
			overlay.flip();
		}
	}

	private void removeOverlay(ObjectOverlay overlay) {
		mOverlays.remove(overlay);
		if (overlay.getContainerView(PhotoEditor.this) != null) {
			mRlOverlayContainer.removeView(overlay.getContainerView(PhotoEditor.this));
		}
		mTopContextMenu.setVisibility(View.GONE);
	}

	private boolean isEditting() {
		return mViewModifyFinish.getVisibility() == View.VISIBLE;
	}

	@Override
	public boolean onTouch(View view, MotionEvent e) {
		mMoveGestureDetector.onTouchEvent(e);

		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			if (isEditting()) { // 正在编辑的过程中，直接将手势传递到子view
				return false;
			}
			int size = mOverlays.size();
			if (mCurrentOverlay != null) {
				mCurrentOverlay.checkKeyPointsSelectionStatus((int) e.getX(), (int) e.getY());
				if (mCurrentOverlay.contains((int) e.getX(), (int) e.getY())
						|| mCurrentOverlay.isControlPointSelected() || mCurrentOverlay.isDeletePointSelected()) {
					// 是否点中了删除按钮
					if (mCurrentOverlay.isDeletePointSelected()) {
						// removeOverlay(mCurrentOverlay);
						// TODO: flip
						flipOverlay(mCurrentOverlay);
						// mCurrentOverlay = null;
						return true;
					}

					if (mCurrentOverlay != null && mCurrentOverlay.getContextView() != null) {
						mTopContextMenu.setVisibility(View.VISIBLE);
						// mVgContextMenuContainer.removeAllViews();
						// mVgContextMenuContainer.addView(mCurrentOverlay.getContextView());
						// mVgContextMenuContainer.setVisibility(View.VISIBLE);
						// mRlOverlayContainer.bringChildToFront(mCurrentOverlay.getContainerView(PhotoEditor.this));
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
					// removeOverlay(mCurrentOverlay);
					// mCurrentOverlay = null;
					// mVgContextMenuContainer.removeAllViews();
					// mVgContextMenuContainer.setVisibility(View.GONE);
					flipOverlay(mCurrentOverlay);
				} else {
					mCurrentOverlay.setEditorContainerView(mEditorContainerView);
					mTopContextMenu.setVisibility(View.VISIBLE);

					// if (mCurrentOverlay.getContextView() != null) {
					// mVgContextMenuContainer.removeAllViews();
					// mVgContextMenuContainer.addView(mCurrentOverlay.getContextView());
					// mVgContextMenuContainer.setVisibility(View.VISIBLE);
					// } else {
					// mVgContextMenuContainer.setVisibility(View.GONE);
					// }
					// mRlOverlayContainer.bringChildToFront(mCurrentOverlay.getContainerView(PhotoEditor.this));
				}
				return true;
			}

			mTopContextMenu.setVisibility(View.GONE);
			mViewModifyFinish.setVisibility(View.GONE);
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
			final Object obj = mFootages.get(position);
			if (obj instanceof FootAge) {
				final FootAge footage = (FootAge) obj;
				if (!TextUtils.isEmpty(footage.getIconUrl())) {
					viewHolder.aivIcon.setImageInfo(ImageInfo.obtain(footage.getIconUrl()));
				}
				viewHolder.tvName.setText(footage.getIconName());
			} else if (obj instanceof NetSence) {
				final NetSence netScene = (NetSence) obj;
				if (!TextUtils.isEmpty(netScene.getSenceNetIcon())) {
					viewHolder.aivIcon.setImageInfo(ImageInfo.obtain(netScene.getSenceNetIcon()));
				}
				viewHolder.tvName.setText(netScene.getSenceName());
			} else if (obj instanceof RecentFootAge) {
				RecentFootAge recent = (RecentFootAge) obj;
				if (recent.getType() == FootAgeType.TYPE_IMAGE) {
					FootAge footage = JSON.parseObject(recent.getJson(), FootAge.class);
					if (!TextUtils.isEmpty(footage.getIconUrl())) {
						viewHolder.aivIcon.setImageInfo(ImageInfo.obtain(footage.getIconUrl()));
					}
					viewHolder.tvName.setText(footage.getIconName());
				} else if (recent.getType() == FootAgeType.TYPE_SCENE) {
					NetSence netSence = JSON.parseObject(recent.getJson(), NetSence.class);
					if (!TextUtils.isEmpty(netSence.getSenceNetIcon())) {
						viewHolder.aivIcon.setImageInfo(ImageInfo.obtain(netSence.getSenceNetIcon()));
					}
					viewHolder.tvName.setText(netSence.getSenceName());
				}
			}

			return convertView;
		}

	}

	private static void LOGD(String logMe) {
		Logger.d("PhotoEditor", logMe);
	}
}
