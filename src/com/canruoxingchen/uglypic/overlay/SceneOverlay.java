/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

	private static final int ALIGNMENT_LEFT = 0;
	private static final int ALIGNMENT_CENTER = 1;
	private static final int ALIGNMENT_RIGHT = 2;
	
	private static final float IPHONE_SCREEN_SIZE = 320.0f;

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
	private String mTextFontName = "";

	// 是否包含时间
	private boolean mHasTime = false;
	private float mTimeLeft = 0.0f;
	private float mTimeTop = 0.0f;
	private float mTimeRight = 0.0f;
	private float mTimeBottom = 0.0f;
	private int mTimeSize = 16;
	private int mTimeColor = Color.WHITE;
	private int mTimeGravity = Gravity.LEFT;
	private String mTimeFontName = "";

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

		public Builder setTextFontName(String fontName) {
			scene.mTextFontName = fontName;
			return this;
		}

		public Builder setTextColor(int color) {
			scene.mTextColor = 0xFF000000 + color;
			return this;
		}

		public Builder setTextAlignment(int alignment) {
			switch (alignment) {
			case ALIGNMENT_LEFT:
				scene.mTextGravity = Gravity.LEFT;
				break;
			case ALIGNMENT_CENTER:
				scene.mTextGravity = Gravity.CENTER;
				break;
			case ALIGNMENT_RIGHT:
				scene.mTextGravity = Gravity.RIGHT;
				break;
			}
			return this;
		}

		public Builder setTimeBounds(int left, int top, int right, int bottom) {
			scene.mHasTime = true;
			scene.mTimeLeft = left;
			scene.mTimeTop = top;
			scene.mTimeRight = right;
			scene.mTimeBottom = bottom;
			return this;
		}

		public Builder setTimeSize(int size) {
			scene.mTimeSize = size;
			return this;
		}

		public Builder setTimeFontName(String timeFontName) {
			scene.mTimeFontName = timeFontName;
			return this;
		}

		public Builder setTimeColor(int color) {
			scene.mTimeColor = 0xFF000000 + color;
			return this;
		}

		public Builder setTimeAlignment(int alignment) {
			switch (alignment) {
			case ALIGNMENT_LEFT:
				scene.mTextGravity = Gravity.LEFT;
				break;
			case ALIGNMENT_CENTER:
				scene.mTextGravity = Gravity.CENTER;
				break;
			case ALIGNMENT_RIGHT:
				scene.mTextGravity = Gravity.RIGHT;
				break;
			}
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
		if (mSceneLayout == null) {
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
		private TextView mTvTime = null;
		private WeakReference<SceneOverlay> mOverlay = null;
		private Context mContext = null;
		private boolean mViewAdded = false;
		private boolean mLayoutFinished = false;
		private boolean mSceneSizeAquired = false;
		private float mSceneWidth = 0.0f;
		private float mSceneHeight = 0.0f;

		private float mDensity = -1.0f;

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
			// 可以获得宽高之后，再添加背景以及文字，通过post方式调用，防止造成layout的递归调用
			if (!mViewAdded) {
				UglyPicApp.getUiHander().post(new Runnable() {

					@Override
					public void run() {
						addViews();
					}
				});
			}
		}

		protected void retrieveDensity() {
			if (mDensity < 0) {
				WindowManager wm = (WindowManager) UglyPicApp.getAppExContext()
						.getSystemService(Context.WINDOW_SERVICE);
				DisplayMetrics dm = new DisplayMetrics();
				wm.getDefaultDisplay().getMetrics(dm);
				mDensity = dm.density;
			}
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
				
				retrieveDensity();

				// 添加或修改EditText的位置
				if (overlay != null && overlay.mHasText && mDensity > 0) {
					int viewWidth = getWidth();
					int viewHeight = getHeight();
					mEtText = new EditText(mContext);
					float etWidth = (overlay.mTextViewRight - overlay.mTextViewLeft);
					float etHeight = (overlay.mTextViewBottom - overlay.mTextViewTop) ;
					float scaleX = viewWidth / IPHONE_SCREEN_SIZE;
					float scaleY = viewHeight / IPHONE_SCREEN_SIZE;
					float scale = Math.min(scaleX, scaleY);

					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int)(etWidth * scale),
							 LayoutParams.WRAP_CONTENT);
					params.leftMargin = (int) (overlay.mTextViewLeft * scale);
					params.topMargin = (int) (overlay.mTextViewTop * scale);
					mEtText.setTextColor(overlay.mTextColor);
					mEtText.setHintTextColor(overlay.mTextColor);
					mEtText.setBackgroundDrawable(null);
					mEtText.setTextSize(overlay.mTextSize * scale / 2);
					mEtText.setLayoutParams(params);
					mEtText.setHint(overlay.mTextHint == null ? "" : overlay.mTextHint);
					mEtText.setLines(1);
					mEtText.setGravity(overlay.mTextGravity | Gravity.CENTER_VERTICAL);
					if (!TextUtils.isEmpty(overlay.mTextFontName)
							&& overlay.mTextFontName.toLowerCase().contains("bold")) {
						mEtText.setTypeface(null, Typeface.BOLD);
					}
					// 如果尚未添加EditText，则将其加入到Layout中
					if (!mViewAdded) {
						addView(mEtText);
					}
				}

				// 添加或修改EditText的位置
				if (overlay != null && overlay.mHasTime && mDensity > 0) {
					int viewWidth = getWidth();
					int viewHeight = getHeight();
					mTvTime = new TextView(mContext);
					float etWidth = overlay.mTimeRight - overlay.mTimeLeft;
					float etHeight = overlay.mTimeBottom - overlay.mTimeTop;
					float scaleX = viewWidth / IPHONE_SCREEN_SIZE;
					float scaleY = viewHeight / IPHONE_SCREEN_SIZE;
					float scale = Math.min(scaleX, scaleY);

					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
							(int) (etWidth * scale), LayoutParams.WRAP_CONTENT);
					params.leftMargin = (int) (overlay.mTimeLeft * scale);
					params.topMargin = (int) (overlay.mTimeTop * scale);
					mTvTime.setTextColor(overlay.mTimeColor);
					mTvTime.setBackgroundDrawable(null);
					mTvTime.setTextSize(overlay.mTimeSize * scale / 2);
					mTvTime.setLayoutParams(params);
					mTvTime.setLines(1);
					mTvTime.setGravity(overlay.mTimeGravity | Gravity.CENTER_VERTICAL);
					SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
					mTvTime.setText(dateFormat.format(new Date(System.currentTimeMillis())));
					if (!TextUtils.isEmpty(overlay.mTimeFontName)
							&& overlay.mTimeFontName.toLowerCase().contains("bold")) {
						mTvTime.setTypeface(null, Typeface.BOLD);
					}
					// 如果尚未添加EditText，则将其加入到Layout中
					if (!mViewAdded) {
						addView(mTvTime);
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
