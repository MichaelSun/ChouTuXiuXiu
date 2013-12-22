/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

import java.lang.ref.WeakReference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import com.canruoxingchen.uglypic.UglyPicApp;
import com.canruoxingchen.uglypic.cache.AsyncImageView;
import com.canruoxingchen.uglypic.cache.ImageInfo;
import com.canruoxingchen.uglypic.concurrent.ThreadPoolManager;
import com.canruoxingchen.uglypic.util.ImageUtils;
import com.canruoxingchen.uglypic.util.Logger;

/**
 * 
 * 场景蒙层
 * 
 * @author wsf
 * 
 */
public class SceneOverlay implements IOverlay {

	// 场景图片
	private Uri mSceneUri = null;

	// 是否选中
	private boolean mSelected = false;

	// 是否包含文案
	private boolean mHasText = false;

	private Context mContext;

	// 文字左上角在整个场景中的左上角
	private float mTextViewLeft = 0.0f;
	private float mTextViewTop = 0.0f;
	private float mTextViewRight = 0.0f;
	private float mTextViewBottom = 0.0f;
	private int mTextSize = 16;
	private String mTextHint = "";
	private int mTextColor = Color.WHITE;
	private int mTextGravity = Gravity.LEFT;

	// 放缩的倍数
	private float mSreenScale = 1.0f;

	private SceneSizeAquiredListener mSceneSizeAquiredListener = null;
	
	private SceneLayout mSceneLayout = null;

	private SceneOverlay(Context context, Uri uri) {
		this.mContext = context;
		this.mSceneUri = uri;
	}

	public void setViewSizeAdjustedListener(SceneSizeAquiredListener listener) {
		mSceneSizeAquiredListener = listener;
	}

	public static class Builder {

		SceneOverlay scene = null;

		public Builder(Context context, Uri uri) {
			scene = new SceneOverlay(context, uri);
		}

		public Builder setTextBounds(int left, int top, int right, int bottom) {
			scene.mHasText = true;
			scene.mTextViewLeft = left;
			scene.mTextViewTop = top;
			scene.mTextViewRight = right;
			scene.mTextViewBottom = bottom;
			return this;
		}

		public Builder setTextSize(int size) {
			scene.mTextSize = size;
			return this;
		}

		public Builder setTextHint(String hint) {
			scene.mTextHint = hint;
			return this;
		}

		public SceneOverlay create() {
			return scene;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.canruoxingchen.uglypic.overlay.BaseOverlay#getView()
	 */
	@Override
	public View getView() {
		if(mSceneLayout == null) {
			mSceneLayout = new SceneLayout(mContext);
			mSceneLayout.setSceneOverlay(this);
		}
		return mSceneLayout;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.canruoxingchen.uglypic.overlay.BaseOverlay#doOverlay()
	 */
	@Override
	public void doOverlay() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.canruoxingchen.uglypic.overlay.BaseOverlay#resetOverlay()
	 */
	@Override
	public void resetOverlay() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.canruoxingchen.uglypic.overlay.BaseOverlay#contains(int, int)
	 */
	@Override
	public boolean contains(int x, int y) {
		return false;
	}

	@Override
	public Rect getInitialContentBounds() {
		return null;
	}

	@Override
	public void setOverlaySelected(boolean selected) {
		mSelected = selected;
	}

	@Override
	public boolean isOverlaySelected() {
		return mSelected;
	}

	// 根据内容调整View的大小
	public static interface SceneSizeAquiredListener {
		void onSceneSizeAquired(int width, int height);
	}

	private static class SceneLayout extends RelativeLayout {

		private AsyncImageView mAivScene = null;
		private EditText mEtText = null;
		private WeakReference<SceneOverlay> mOverlay = null;
		private Context mContext = null;
		private boolean mViewAdded = false;
		private boolean mLayoutFinished = false;
		private boolean mSceneSizeAquired = false;
		private float mSceneWidth = 0.0f;
		private float mSceneHeight = 0.0f;

		public SceneLayout(Context context) {
			super(context);
			this.mContext = context;
		}

		private void setSceneOverlay(SceneOverlay overlay) {
			this.mOverlay = new WeakReference<SceneOverlay>(overlay);
			ThreadPoolManager.getInstance().execute(new GetSceneSizeTask(this, overlay.mSceneUri));
		}

		@SuppressLint("DrawAllocation")
		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			super.onLayout(changed, l, t, r, b);
			mLayoutFinished = true;
			LOGD(">>>>>>>>> onLayout >>>>>>>>> width=" + (r - l) + ", height=" + (b - t)
					+ ", l=" + l + ", t=" + t + ", r=" + r + ", b=" + b);
			// 可以获得宽高之后，再添加背景以及文字，通过post方式调用，防止造成layout的递归调用
			UglyPicApp.getUiHander().post(new Runnable() {

				@Override
				public void run() {
					addViews();
				}
			});
		}

		private void addViews() {
			if (mLayoutFinished && mSceneSizeAquired) {
				SceneOverlay overlay = mOverlay.get();
				if (!mViewAdded) {
					mAivScene = new AsyncImageView(mContext);
					if (mOverlay != null && overlay != null && overlay.mSceneUri != null) {
						mAivScene.setImageInfo(ImageInfo.obtain(overlay.mSceneUri.toString()));
						mAivScene.setScaleType(ScaleType.FIT_CENTER);
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
								LayoutParams.MATCH_PARENT);
						params.addRule(RelativeLayout.CENTER_IN_PARENT);
						addView(mAivScene, params);
					}
				}

				// 添加或修改EditText的位置
				if (overlay != null && overlay.mHasText) {
					int viewWidth = getWidth();
					int viewHeight = getHeight();
					LOGD(">>>>>> addViews >>>>>> viewWidth=" + viewWidth + ", viewHeight=" + viewHeight);
					mEtText = new EditText(mContext);
					float etWidth = overlay.mTextViewRight - overlay.mTextViewLeft;
					float etHeight = overlay.mTextViewBottom - overlay.mTextViewTop;
					float scaleX = viewWidth / mSceneWidth;
					float scaleY = viewHeight / mSceneHeight;
					float scale = Math.min(scaleX, scaleY);

					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) (etWidth * scale),
							(int) (etHeight * scale));
					params.leftMargin = (int) (overlay.mTextViewLeft * scale);
					params.topMargin = (int) (overlay.mTextViewTop * scale);
					mEtText.setTextColor(overlay.mTextColor);
					mEtText.setHintTextColor(overlay.mTextColor);
					mEtText.setBackgroundDrawable(null);
					mEtText.setTextSize(overlay.mTextSize * scale);
					mEtText.setLayoutParams(params);
					mEtText.setHint(overlay.mTextHint == null ? "" : overlay.mTextHint);
					mEtText.setMaxLines(1);
					mEtText.setGravity(overlay.mTextGravity);
					//如果尚未添加EditText，则将其加入到Layout中
					if (!mViewAdded) {
						addView(mEtText);
					}
				}
				
				mViewAdded = true;
			}
		}

		private void setSceneSize(final int sceneWidth, final int sceneHeight) {
			mSceneWidth = sceneWidth;
			mSceneHeight = sceneHeight;
			mSceneSizeAquired = true;
			final SceneOverlay overlay = mOverlay.get();
			UglyPicApp.getUiHander().post(new Runnable() {
				@Override
				public void run() {
					if (overlay != null && overlay.mSceneSizeAquiredListener != null) {
						overlay.mSceneSizeAquiredListener.onSceneSizeAquired(sceneWidth, sceneHeight);
					}
					addViews();
				}
			});
		}
	}

	private static class GetSceneSizeTask implements Runnable {
		WeakReference<SceneLayout> wSceneLayout;
		Uri mUri;

		public GetSceneSizeTask(SceneLayout sceneLayout, Uri uri) {
			wSceneLayout = new WeakReference<SceneOverlay.SceneLayout>(sceneLayout);
			mUri = uri;
		}

		@Override
		public void run() {
			final BitmapFactory.Options opts = ImageUtils.getImageInfo(UglyPicApp.getAppExContext(), mUri);
			final SceneLayout sceneLayout = wSceneLayout == null ? null : wSceneLayout.get();
			if (opts != null && sceneLayout != null) {
				sceneLayout.setSceneSize(opts.outWidth, opts.outHeight);
			}
			LOGD(">>>>>>>>> getSceneSize >>>>>>>>> width=" + opts.outWidth + ", height=" + opts.outHeight);
		}
	}
	
	private static void LOGD(String logMe) {
		Logger.d(SceneOverlay.class.getSimpleName(), logMe);
	}
}
