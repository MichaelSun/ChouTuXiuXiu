/**
 * 
 */
package com.canruoxingchen.uglypic;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photoview.PhotoView;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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
import com.almeros.android.multitouch.gesturedetector.RotateGestureDetector;
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
import com.canruoxingchen.uglypic.statistics.StatisticsUtil;
import com.canruoxingchen.uglypic.util.ImageUtils;
import com.canruoxingchen.uglypic.util.Logger;
import com.canruoxingchen.uglypic.util.UiUtils;
import com.canruoxingchen.uglypic.view.GestureView;
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

	// 原始照片的uri
	private Uri mPhotoUri;
	private String mImagePath;

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
	private RelativeLayout mFootageListContainer;

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
	private ScaleGestureDetector mScaleGestureDetector;
	private RotateGestureDetector mRotateGestureDetector;

	/**
	 * 添加在图片上的浮层
	 */
	private List<ObjectOverlay> mOverlays = new ArrayList<ObjectOverlay>();
	/**
	 * 场景浮层，整张图只能有一个场景浮层
	 */
	private SceneOverlay mSceneOverlay = null;
	/**
	 * 默认的空场景
	 */
	private SceneOverlay mNullScene = null;
	/**
	 * 当前被选中的浮层
	 */
	private ObjectOverlay mCurrentOverlay;
	/**
	 * 上一次被选中的浮层
	 */
	private ObjectOverlay mLastOverlay;

	/**
	 * 素材类型列表
	 */
	private List<FootAgeType> mFootageTypes = new ArrayList<FootAgeType>();

	/**
	 * 素材（图片和场景）
	 */
	private List<Object> mFootages = new ArrayList<Object>();

	private TypeAdapter mTypeAdapter = new TypeAdapter();
	private FootageAdapter mFootageAdapter = new FootageAdapter();

	private FootageManager mFootageManager;

	private FootAgeType mCurrentType;

	private boolean mHasLoadPhoto = false;

	private boolean mSceneGestureEnabled = false;

	/**
	 * 启动照片编辑页面
	 * 
	 * @param context
	 * @param photoUri
	 */
	public static void start(Context context, String photoPath) {
		Intent intent = new Intent(context, PhotoEditor.class);
		intent.putExtra(EXTRA_PHOTO_PATH, photoPath);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
				if (types == null || types.size() == 0) {
					types = new ArrayList<FootAgeType>();
					activity.mFootageManager.loadFootageTypeFromServer();
				}
				activity.mFootageTypes.clear();
				activity.mFootageTypes.add(FootAgeType.RECENT_TYPE);
				activity.mFootageTypes.addAll(types);
				activity.mTypeAdapter.notifyDataSetChanged();
				// 如果尚无选中的类型，则选择第一个
				if (activity.mCurrentType == null) {
					activity.mCurrentType = FootAgeType.RECENT_TYPE;
					activity.mFootageManager.loadRecentFootages();
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
						activity.mLvFootages.setAdapter(activity.mFootageAdapter);
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
						activity.mLvFootages.setAdapter(activity.mFootageAdapter);
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
						// 在队首增加一个空场景
						activity.mFootages.add(NetSence.DEFAULT);
						activity.mFootages.addAll(scenes);
						activity.mLvFootages.setAdapter(activity.mFootageAdapter);
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
						// 在队首增加一个空场景
						activity.mFootages.add(NetSence.DEFAULT);
						activity.mFootages.addAll(scenes);
						activity.mLvFootages.setAdapter(activity.mFootageAdapter);
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
			// case FootageManager.MSG_LOAD_FOOTAGE_ICON_SUCCESS: {
			// if (activity.mFootages != null) {
			// if (msg.obj instanceof FootAge) {
			// FootAge footage = (FootAge) msg.obj;
			// for (Object obj : activity.mFootages) {
			// if (obj instanceof NetSence || obj instanceof RecentFootAge) {
			// break;
			// }
			// FootAge f = (FootAge) obj;
			// if (footage != null &&
			// footage.getObjectId().equals(f.getObjectId())) {
			// f.setIconUrl(footage.getIconUrl());
			// activity.mFootageAdapter.notifyDataSetChanged();
			// break;
			// }
			// }
			// } else {
			// NetSence netScene = (NetSence) msg.obj;
			// for (Object obj : activity.mFootages) {
			// if (obj instanceof FootAge || obj instanceof RecentFootAge) {
			// break;
			// }
			// NetSence ns = (NetSence) obj;
			// if (netScene != null &&
			// netScene.getObjectId().equals(ns.getObjectId())) {
			// ns.setSenceNetIcon(netScene.getSenceNetIcon());
			// activity.mFootageAdapter.notifyDataSetChanged();
			// break;
			// }
			// }
			// }
			// }
			// break;
			// }
			// case FootageManager.MSG_LOAD_FOOTAGE_ICON_FAILURE: {
			// break;
			// }
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
		setSceneOverlay(null);
		mHandler = new MyHandler(this);
		registerAllFootageMsg();

		mTypeAdapter = new TypeAdapter();
		mFootageAdapter = new FootageAdapter();
		mLvTypes.setAdapter(mTypeAdapter);
		mLvFootages.setAdapter(mFootageAdapter);

		mMoveGestureDetector = new MoveGestureDetector(this, new MoveListener());
		mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
		mRotateGestureDetector = new RotateGestureDetector(this, new RotateListener());

		mFootageManager = FootageManager.getInstance(this);

		// 加载图片
		if (mPhotoUri != null) {
			mPvPhoto.setImageInfo(ImageInfo.obtain(mPhotoUri.toString()));
		}

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
		unregisterAllFootageMsg();
		dismissDialog();
		TipsDialog.getInstance().dismiss();
	}

	@Override
	protected void initUI() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.photo_editor);

		mRootView = (RelativeLayout) findViewById(R.id.photo_editor_root_view);
		mViewBottomPanel = findViewById(R.id.photo_editor_bottom_panel);
		mPvPhoto = (PhotoView) findViewById(R.id.photo_editor_photo);
		mPvPhoto.setZoomable(true);
		mFootageListContainer = (RelativeLayout) findViewById(R.id.photo_editor_footage_list_container);

		mTopContextMenu = findViewById(R.id.photo_editor_topbar_object_menu);
		mViewContextBtn = (Button) findViewById(R.id.photo_editor_context_button);
		// 初始状态下，分享按钮不可见
		mViewContextBtn.setVisibility(View.GONE);
		mViewModify = findViewById(R.id.photo_editor_top_bar_object_modify);
		mViewDelete = findViewById(R.id.photo_editor_top_bar_object_delete);
		mViewEraser = findViewById(R.id.photo_editor_top_bar_object_eraser);
		mLvTypes = (HorizontalListView) findViewById(R.id.photo_editor_footage_types_list);
		mLvFootages = (HorizontalListView) findViewById(R.id.photo_editor_footage_list);
		mViewModifyFinish = findViewById(R.id.photo_editor_topbar_modify_finish);

		mViewBackToCamera = findViewById(R.id.photo_editor_top_bar_camera);

		mEditorPanel = findViewById(R.id.photo_editor_edit_panel);
		mEditorPanelRefView = findViewById(R.id.photo_editor_edit_panel_ref_view);
		mRlOverlayContainer = (RelativeLayout) findViewById(R.id.photo_editor_overlay_container);

		RelativeLayout.LayoutParams editorPanelParams = (RelativeLayout.LayoutParams) mEditorPanel.getLayoutParams();
		int width = getWindowManager().getDefaultDisplay().getWidth();
		editorPanelParams.height = width;

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
				if (mCurrentOverlay != null && mCurrentOverlay.isOverlaySelected()) {
					mViewContextBtn.setVisibility(View.VISIBLE);
				} else {
					mViewContextBtn.setVisibility(View.GONE);
				}
				mViewBottomPanel.setVisibility(View.GONE);
				// 编辑过程中，原图不可放缩
			}

			@Override
			public void onInvisible() {
				mViewContextBtn.setVisibility(View.VISIBLE);
				mViewBottomPanel.setVisibility(View.VISIBLE);
			}
		});

		mLvTypes.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				FootAgeType type = mFootageTypes.get(position);
				mCurrentType = type;
				mTypeAdapter.notifyDataSetChanged();
				mFootageAdapter.mSelectedPosition = -1;

				switch (type.getTypeTarget()) {
				case FootAgeType.TYPE_RECENT: // 最近使用
					mFootages.clear();
					mFootageManager.loadRecentFootages();
					break;
				case FootAgeType.TYPE_IMAGE: // 图片
					mFootages.clear();
					mFootageManager.loadLocalFootages(type.getObjectId());
					break;
				case FootAgeType.TYPE_SCENE: // 场景
					mFootages.clear();
					mFootageManager.loadLocalScenes(type.getObjectId());
					break;
				}
				// mLvFootages.setAdapter(mFootageAdapter);
			}
		});

		mLvFootages.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final Object obj = mFootages.get(position);
				mFootageAdapter.mSelectedPosition = position;
				mFootageAdapter.notifyDataSetChanged();
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

		mPvPhoto.setImageLoadedListener(new AsyncImageView.ImageLoadedListener() {

			@Override
			public void onFailure() {

			}

			@Override
			public void onComplete() {
				if (!mHasLoadPhoto) {
					mHasLoadPhoto = true;
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							LOGD(">>>>>>> StartToLoad >>>>>>>>>>>>>>>");
							// 加载素材类型数据
							mFootageManager.loadFootageTypeFromLocal();
						}
					});
				}
			}
		});
	}

	private void onFootAgeClick(FootAge footage, boolean save) {
		if (!TextUtils.isEmpty(footage.getIconUrl())) {
			// 统计素材使用
			StatisticsUtil.increaseFootageCount(footage.getObjectId());

			ImageWidgetOverlay overlay = new ImageWidgetOverlay(PhotoEditor.this, Uri.parse(footage.getIconUrl()));
			addOverlay(overlay);
			if (save) {
				mFootageManager.saveRecentFootage(footage);
			}
		}
	}

	private void onSceneClick(NetSence netScene, boolean save) {
		if (netScene == NetSence.DEFAULT) {
			setSceneOverlay(mNullScene);
			return;
		}
		if (!TextUtils.isEmpty(netScene.getSenceNetIcon())) {
			// 统计场景次数
			StatisticsUtil.increaseNetSceneCount(netScene.getObjectId());

			SceneOverlay.Builder builder = new SceneOverlay.Builder(PhotoEditor.this, Uri.parse(netScene
					.getSenceNetIcon()));
			Rect inputRect = netScene.getInputRectBounds();
			if (inputRect != null) {
				if ("52b2c476e4b0a1f3e5c5b373".equals(netScene.getObjectId())) {
					builder.setTextBounds(inputRect.left, inputRect.top - 6, inputRect.right, inputRect.bottom + 6);
				} else {
					builder.setTextBounds(inputRect.left, inputRect.top, inputRect.right, inputRect.bottom);
				}
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
		// 注册编辑内容可回退状态改变的消息
		MessageCenter.getInstance(this).registerMessage(R.id.msg_editor_regret_status_change, mHandler);

		// 设置一个空场景
		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				setSceneOverlay(null);
			}
		}, 200);

		if (mSceneOverlay != null) {
			mSceneOverlay.setCursorVisable(false);
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
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

	private Dialog showCancelDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage(R.string.photo_editor_cancel_hint)
				.setPositiveButton(R.string.photo_editor_confirm, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				}).setNegativeButton(R.string.photo_editor_cancel, null);
		return builder.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_CODE_EDIT_TEXT: { // 编辑了文本后，显示选择背景列表
			if (resultCode == RESULT_OK) {
			}
			break;
		}
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
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
			mDialog = showCancelDialog();
			break;
		}
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
				((ImageWidgetOverlay) mCurrentOverlay).startErase(mViewBottomPanel.getHeight());
			}
			break;
		}
		case R.id.photo_editor_topbar_modify_finish: // 编辑完成
			if (mCurrentOverlay != null && mCurrentOverlay.getContextView() != null) {
				mVgContextMenuContainer.setVisibility(View.GONE);
				mTopContextMenu.setVisibility(View.VISIBLE);
				mViewModifyFinish.setVisibility(View.GONE);
				mViewBottomPanel.setVisibility(View.VISIBLE);
				mViewContextBtn.setText(R.string.photo_editor_context_btn_share);
			}
		}
	}

	// 保存当前图片
	private void saveCurrentImage() {
		if (mCurrentOverlay != null) {
			mCurrentOverlay.setOverlaySelected(false);
			mCurrentOverlay.getContainerView(this).invalidate();
			mTopContextMenu.setVisibility(View.GONE);
			mViewModifyFinish.setVisibility(View.GONE);
		}

		if (mSceneOverlay != null) {
			mSceneOverlay.setCursorVisable(false);
		}

		mEditorPanel.invalidate();
		mEditorPanel.destroyDrawingCache();
		mEditorPanel.buildDrawingCache();
		final Bitmap processedImage = mEditorPanel.getDrawingCache();
		// mEditorPanel.destroyDrawingCache();

		TipsDialog.getInstance().show(this, R.drawable.tips_loading, R.string.photo_editor_saving, true, false);
		ThreadPoolManager.getInstance().execute(new Runnable() {
			@Override
			public void run() {
				final String processedPath = ImageUtils.saveBitmapForLocalPath(UglyPicApp.getAppExContext(),
						processedImage, 0, false);
				if (processedImage != null && !processedImage.isRecycled()) {
					processedImage.recycle();
				}
				if (TextUtils.isEmpty(processedPath)) {
					UglyPicApp.getUiHander().post(new Runnable() {

						@Override
						public void run() {
							TipsDialog.getInstance().dismiss();
							UiUtils.toastMessage(PhotoEditor.this, R.string.photo_editor_save_failure);
							return;
						}
					});
				} else {
					UglyPicApp.getUiHander().post(new Runnable() {
						@Override
						public void run() {
							TipsDialog.getInstance().dismiss();
							PublishActivity.start(PhotoEditor.this, mImagePath, processedPath);
						}
					});
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

			mDialog = showCancelDialog();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void setSceneOverlay(SceneOverlay scene) {
		if (mSceneOverlay != null) {
			if (mSceneOverlay.getView() != null) {
				mRlOverlayContainer.removeView(mSceneOverlay.getView());
			}
		}

		if (scene == null) {
			if (mNullScene == null) {
				mNullScene = new SceneOverlay.Builder(PhotoEditor.this, null).create();
			}
			scene = mNullScene;
		} else {
			mViewContextBtn.setVisibility(View.VISIBLE);
		}
		mSceneOverlay = scene;
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		scene.getView().setLayoutParams(params);
		mRlOverlayContainer.addView(scene.getView(), 0);
		scene.setViewSizeAdjustedListener(this);

		if (mHasLoadPhoto && mPvPhoto != null && mSceneOverlay == mNullScene) {
			mSceneGestureEnabled = false;
		} else {
			mSceneGestureEnabled = true;
		}
		LOGD("----------setSceneOverlay,,,,SceneGestureEnabled------------" + mSceneGestureEnabled);
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
			if (mLastOverlay != null) {
				removeOverlay(mLastOverlay);
			}
			if (mCurrentOverlay != null) {
				mCurrentOverlay.setOverlaySelected(false);
				mCurrentOverlay.getContainerView(this).invalidate();
				mCurrentOverlay = null;
				mTopContextMenu.setVisibility(View.GONE);
				mViewContextBtn.setText(R.string.photo_editor_context_btn_share);
			}
			mLastOverlay = overlay;
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
		mScaleGestureDetector.onTouchEvent(e);
		mRotateGestureDetector.onTouchEvent(e);

		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			if (isEditting()) { // 正在编辑的过程中，直接将手势吞掉不处理
				return true;
			}
			int size = mOverlays.size();
			if (mCurrentOverlay != null) {
				mCurrentOverlay.checkKeyPointsSelectionStatus((int) e.getX(), (int) e.getY());
				if (mCurrentOverlay.contains((int) e.getX(), (int) e.getY())
						|| mCurrentOverlay.isControlPointSelected() || mCurrentOverlay.isFlipPointSelected()) {
					// 标记为已经选中过
					mCurrentOverlay.setHasBeenSelected(true);
					// 显示context按钮
					mViewContextBtn.setVisibility(View.VISIBLE);
					// 选中素材后，背景不可以缩放
					mSceneGestureEnabled = false;
					LOGD("----------onTouch,,,,SceneGestureEnabled------------" + mSceneGestureEnabled);

					mLastOverlay = null;
					// 是否点中了删除按钮
					if (mCurrentOverlay.isFlipPointSelected()) {
						flipOverlay(mCurrentOverlay);
						return true;
					}

					if (mCurrentOverlay != null && mCurrentOverlay.getContextView() != null) {
						mTopContextMenu.setVisibility(View.VISIBLE);
						mCurrentOverlay.setOverlaySelected(true);
						mCurrentOverlay.getContainerView(PhotoEditor.this).invalidate();
					} else {
						mVgContextMenuContainer.setVisibility(View.GONE);
					}
					return true;
				}
				mCurrentOverlay.setOverlaySelected(false);
				mCurrentOverlay.getContainerView(PhotoEditor.this).invalidate();
				mCurrentOverlay = null;
				mViewContextBtn.setText(R.string.photo_editor_context_btn_share);
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
				// 标记为已经选中过
				mSceneGestureEnabled = false;
				LOGD("----------onTouch,,,,,SceneGestureEnabled------------" + mSceneGestureEnabled);
				mViewContextBtn.setVisibility(View.VISIBLE);
				mCurrentOverlay.setHasBeenSelected(true);
				mLastOverlay = null;
				if (mCurrentOverlay.isFlipPointSelected()) {
					flipOverlay(mCurrentOverlay);
				} else {
					mCurrentOverlay.setEditorContainerView(mEditorContainerView);
					mTopContextMenu.setVisibility(View.VISIBLE);
				}
				return true;
			}

			mViewContextBtn.setText(R.string.photo_editor_context_btn_share);
			mTopContextMenu.setVisibility(View.GONE);
			mViewModifyFinish.setVisibility(View.GONE);
			break;
		}
		}

		// 如果当前已取消了场景手势，则直接吞掉touch事件
		if (!mSceneGestureEnabled) {
			LOGD("----------onTouch,,,,Check,,,,SceneGestureEnabled------------" + mSceneGestureEnabled);
			return true;
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

	private class RotateListener extends RotateGestureDetector.SimpleOnRotateGestureListener {

		@Override
		public boolean onRotate(RotateGestureDetector detector) {
			// 编辑过程中不处理事件
			if (isEditting()) {
				return true;
			}

			float rotate = -detector.getRotationDegreesDelta();
			if (mCurrentOverlay != null && mCurrentOverlay.isOverlaySelected()) {
				mCurrentOverlay.rotate(rotate);
				mCurrentOverlay.getContainerView(PhotoEditor.this).invalidate();
			}
			return true;
		}

	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			// 编辑过程中不处理事件
			if (isEditting()) {
				return true;
			}
			float scale = detector.getScaleFactor();
			if (mCurrentOverlay != null && mCurrentOverlay.isOverlaySelected()) {
				mCurrentOverlay.scale(scale, scale);
				mCurrentOverlay.getContainerView(PhotoEditor.this).invalidate();
			}
			return true;
		}

	}

	private class MoveListener extends MoveGestureDetector.SimpleOnMoveGestureListener {
		@Override
		public boolean onMove(MoveGestureDetector detector) {
			// 编辑过程中不处理事件
			if (isEditting()) {
				return true;
			}
			PointF d = detector.getFocusDelta();

			if (mCurrentOverlay != null && mCurrentOverlay.isOverlaySelected()) {

				// 如果选中的是控制点，则需要计算旋转和放缩
				if (mCurrentOverlay.isControlPointSelected()) {
					PointF ctrlPoint = mCurrentOverlay.getControlButton();
					PointF deletePoint = mCurrentOverlay.getFlipButton();
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
				}
			}

			return true;
		}
	}

	private void adjustSceneSize(int width, int height) {
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mEditorPanel.getLayoutParams();
		int editorPanelWidth = mEditorPanelRefView.getWidth();
		int editorPanelHeight = mEditorPanelRefView.getHeight();
		if (editorPanelHeight != editorPanelWidth) {
			float scaleX = (1.0f * editorPanelWidth) / (1.0f * width);
			float scaleY = (1.0f * editorPanelHeight) / (1.0f * height);
			if (scaleX > scaleY) { // 容器比背景胖，则调整宽
				params.topMargin = 0;
				params.bottomMargin = 0;
				int margin = (int) ((editorPanelWidth - editorPanelHeight) / 2);
				params.leftMargin = margin;
				params.rightMargin = margin;
				params.width = editorPanelHeight;
				params.height = editorPanelHeight;
			} else {
				// params.height = (int) (height * scaleX);
				params.height = editorPanelWidth;
				params.width = editorPanelWidth;
			}
		}
		mTypeAdapter.notifyDataSetChanged();
		mFootageAdapter.notifyDataSetChanged();
		mPvPhoto.update();
	}

	@Override
	public void onSceneSizeAquired(int width, int height) {
		// 根据当前场景的尺寸调整大小
		adjustSceneSize(width, height);
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

			final FootAgeType type = mFootageTypes.get(position);

			if (type != mCurrentType) {
				tvName.setSelected(false);
			} else {
				tvName.setSelected(true);
			}
			tvName.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (position == 0) { // 常用的素材

					} else {
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
		// private TextView tvName;
	}

	private class FootageAdapter extends BaseAdapter {

		private int mSelectedPosition = -1;

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
				convertView.setTag(viewHolder);

				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) viewHolder.aivIcon.getLayoutParams();
				int width = mLvFootages.getWidth() / 3 - 20;
				int height = mLvFootages.getHeight() - 10;
				LOGD("-----------getView----------- width=" + width + ", height=" + height);
				int min = Math.min(width, height);
				params.width = min;
				params.height = min;
			}
			viewHolder = (ViewHolder) convertView.getTag();

			final Object obj = mFootages.get(position);
			if (obj instanceof FootAge) {
				final FootAge footage = (FootAge) obj;
				if (!TextUtils.isEmpty(footage.getIconUrl())) {
					viewHolder.aivIcon.setImageInfo(ImageInfo.obtain(footage.getIconUrl()));
				}
			} else if (obj instanceof NetSence) {
				final NetSence netScene = (NetSence) obj;
				if (netScene == NetSence.DEFAULT) {
					viewHolder.aivIcon.setImageResource(R.drawable.null_scene);
				} else {
					if (!TextUtils.isEmpty(netScene.getSenceNetIcon())) {
						viewHolder.aivIcon.setImageInfo(ImageInfo.obtain(netScene.getSenceNetIcon()));
					}
				}
			} else if (obj instanceof RecentFootAge) {
				RecentFootAge recent = (RecentFootAge) obj;
				if (recent.getType() == FootAgeType.TYPE_IMAGE) {
					FootAge footage = JSON.parseObject(recent.getJson(), FootAge.class);
					if (!TextUtils.isEmpty(footage.getIconUrl())) {
						viewHolder.aivIcon.setImageInfo(ImageInfo.obtain(footage.getIconUrl()));
					}
				} else if (recent.getType() == FootAgeType.TYPE_SCENE) {
					NetSence netSence = JSON.parseObject(recent.getJson(), NetSence.class);
					if (!TextUtils.isEmpty(netSence.getSenceNetIcon())) {
						viewHolder.aivIcon.setImageInfo(ImageInfo.obtain(netSence.getSenceNetIcon()));
					} else {
						if (!TextUtils.isEmpty(netSence.getSenceNetIcon())) {
							viewHolder.aivIcon.setImageInfo(ImageInfo.obtain(netSence.getSenceNetIcon()));
						}
					}
				}
			}

			if (position == mSelectedPosition) {
				viewHolder.aivIcon.setSelected(true);
			} else {
				viewHolder.aivIcon.setSelected(false);
			}

			return convertView;
		}
	}

	private static void LOGD(String logMe) {
		Logger.d("PhotoEditor", logMe);
	}
}
