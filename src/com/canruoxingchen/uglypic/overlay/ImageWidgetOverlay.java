package com.canruoxingchen.uglypic.overlay;

import java.util.LinkedList;
import java.util.List;

import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import com.canruoxingchen.uglypic.cache.AsyncImageView;
import com.canruoxingchen.uglypic.cache.ImageInfo;
import com.canruoxingchen.uglypic.overlay.ImageOverlayContextView.ContrastChangedListener;
import com.canruoxingchen.uglypic.overlay.ImageOverlayContextView.EraseListener;
import com.canruoxingchen.uglypic.overlay.ImageOverlayContextView.IlluminationChangedListener;
import com.canruoxingchen.uglypic.overlay.ImageOverlayContextView.ResetListener;
import com.canruoxingchen.uglypic.overlay.ImageOverlayContextView.SatuationChangedListener;
import com.canruoxingchen.uglypic.util.Logger;

public class ImageWidgetOverlay extends ObjectOverlay implements IlluminationChangedListener, ContrastChangedListener,
		SatuationChangedListener, ResetListener, EraseListener {

	private static final int[] ERASER_WIDTH = new int[] { 5, 10, 15, 20, 25, 30, 35 };
	private static final int TOUCH_SPAN_THREASHOLD = 4;
	private static final String TAG = "ImageWidgetOverlay";

	private CopyOnWriteImageView mAivImage;

	private Context mContext;

	private boolean mSelected = false;

	private float mScaleX = 1.0f;
	private float mScaleY = 1.0f;

	private ImageOverlayContextView mContextView;

	private Uri mUri = null;

	public ImageWidgetOverlay(Context context, Uri uri) {
		this.mContext = context;
		initContentView();
		if (uri != null) {
			mUri = uri;
			mAivImage.setImageInfo(ImageInfo.obtain(uri.toString()));
			mAivImage.setScaleType(ScaleType.MATRIX);
		}
	}

	@Override
	public View getView() {
		return mAivImage;
	}

	@Override
	protected View initContentView() {
		mAivImage = new CopyOnWriteImageView(mContext);
		return mAivImage;
	}

	@Override
	public Rect getContentBounds() {
		return mAivImage.getDrawable() == null ? null : mAivImage.getDrawable().getBounds();
	}

	@Override
	public void translate(int dx, int dy) {
		super.translate(dx, dy);
		if (mAivImage.getDrawable() != null) {
			mAivImage.setImageMatrix(getMatrix());
		}
	}

	@Override
	public void scale(float sx, float sy) {
		super.scale(sx, sy);
		mScaleX *= sx;
		mScaleY *= sy;
		if (mAivImage.getDrawable() != null) {
			mAivImage.setImageMatrix(getMatrix());
		}
	}

	@Override
	public void rotate(float degrees) {
		super.rotate(degrees);
		if (mAivImage.getDrawable() != null) {
			mAivImage.setImageMatrix(getMatrix());
		}
	}

	@Override
	public void setSelected(boolean selected) {
		mSelected = selected;
	}

	@Override
	public boolean isSelected() {
		return mSelected;
	}

	/**
	 * 擦除照片的View
	 * 
	 * @author wsf
	 * 
	 */
	public class EraseImageEditorView extends RelativeLayout implements IEditor {

		public EraseImageEditorView(Context context) {
			super(context);
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT);
			params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		}

		@Override
		protected void onDetachedFromWindow() {
			super.onDetachedFromWindow();
		}

		@Override
		public void onCancel() {
			// 返回，则恢复原有View的大小
			mAivImage.setEraserMode(false);
			disableEraseMode();
			getEditorContainerView().setEditorView(null);
			getEditorContainerView().setVisibility(View.GONE);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.canruoxingchen.uglypic.overlay.IEditor#onRegret()
		 */
		@Override
		public void onRegret() {
			if (mAivImage.hasMoreRegret()) {
				mAivImage.regret();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.canruoxingchen.uglypic.overlay.IEditor#onRedo()
		 */
		@Override
		public void onRedo() {
			if (mAivImage.hasMoreRedo()) {
				mAivImage.redo();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.canruoxingchen.uglypic.overlay.IEditor#onFinish()
		 */
		@Override
		public void onFinish() {
			// 传回擦除的结果
			mAivImage.saveEraserResult();
			mAivImage.setEraserMode(false);
			disableEraseMode();
			getEditorContainerView().setEditorView(null);
			getEditorContainerView().setVisibility(View.GONE);
		}

		@Override
		public boolean hasMoreRegret() {
			return mAivImage.hasMoreRegret();
		}

		@Override
		public boolean hasMoreRedo() {
			return mAivImage.hasMoreRedo();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.canruoxingchen.uglypic.overlay.IEditor#canRegret()
		 */
		@Override
		public boolean canRegret() {
			return true;
		}
	}

	/**
	 * 橡皮轨迹记录
	 * 
	 * @author wsf
	 * 
	 */
	private static class EraseRecord {
		Path path;
		int strokeWidth;

		public EraseRecord(Path path, int strokeWidth) {
			this.path = path;
			this.strokeWidth = strokeWidth;
		}
	}

	private class CopyOnWriteImageView extends AsyncImageView {

		private static final int CONTROL_POINTS_RADIS = 20;
		private float mDensity = 1.0f;
		private Paint mPaint;

		/* 用于绘制橡皮轨迹 */
		private boolean mEraserMode = false;
		private Paint mEraserPaint;
		private Path mCurrentPath = null;
		private float mLastX = 0.0f;
		private float mLastY = 0.0f;
		private int mCurrentStrokeWidth = 5;
		// 橡皮记录
		private List<EraseRecord> mRecords = new LinkedList<EraseRecord>();
		private int mPathIndex = 0;

		// 附加操作，不包含亮度、对比度、饱和度
		private List<IImageOperation> mOperations = new LinkedList<IImageOperation>();

		private Bitmap mImage = null;

		private ColorMatrix mColorMatrix = new ColorMatrix();

		private float mIllumination;
		private float mSatuation;
		private float mContrast;

		public CopyOnWriteImageView(Context context) {
			super(context);
			WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics dm = new DisplayMetrics();
			wm.getDefaultDisplay().getMetrics(dm);
			mDensity = dm.density;
			int padding = (int) (CONTROL_POINTS_RADIS * mDensity);
			setPadding(padding, padding, padding, padding);

			mEraserPaint = new Paint();
			mEraserPaint.setAntiAlias(true);
			mEraserPaint.setColor(Color.RED);
			// mPaint.setAlpha(0);
			mEraserPaint.setDither(true);
			mEraserPaint.setStyle(Paint.Style.STROKE);
			mEraserPaint.setStrokeJoin(Paint.Join.ROUND);
			mEraserPaint.setStrokeCap(Paint.Cap.ROUND);
			mEraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			mCurrentStrokeWidth = ERASER_WIDTH[3];
		}

		private void setEraserMode(boolean eraserMode) {
			this.mEraserMode = eraserMode;
			// 首次进入擦除模式时复制照片
			if (mEraserMode) {
				copyImage();
			} else {
				mRecords.clear();
				mPathIndex = 0;
			}
		}

		private void reset() {
			mOperations.clear();
			mIllumination = IlluminationImageOperation.DEFAULT;
			mSatuation = SatuationImageOperation.DEFAULT;
			mContrast = ContrastImageOperation.DEFAULT;
			mEraserMode = false;
			if (mImage != null && !mImage.isRecycled()) {
				setImageDrawable(null);
				setImageInfo(ImageInfo.obtain(mUri.toString()));
				mImage.recycle();
				mImage = null;
			} else {
				setImageInfo(ImageInfo.obtain(mUri.toString()));
			}
		}

		private void setContrast(int contrast) {
			copyImage();
			// mContrast = (contrast + 64) / 128.0f;
			// mContrast = 128 * (1 - contrast);
			mContrast = contrast;
			invalidate();
		}

		private void setSatuation(int satuation) {
			copyImage();
			mSatuation = satuation * 1.0F / SatuationImageOperation.DEFAULT;
			invalidate();
		}

		private void setIllumination(int illumination) {
			copyImage();
			// mIllumination = (illumination -
			// IlluminationImageOperation.DEFAULT)
			// * 1.0F / IlluminationImageOperation.DEFAULT * 180;

			mIllumination = illumination;
			invalidate();
		}

		private void copyImage() {
			// 第一次添加操作时，从当前的缓存中复制一张图出来，在新的图上做操作
			if (mImage == null) {
				if (getDrawable() == null || !(getDrawable() instanceof CacheableBitmapDrawable)) {
					return;
				}

				CacheableBitmapDrawable drawable = (CacheableBitmapDrawable) getDrawable();
				Bitmap image = drawable.getBitmap();
				if (image == null || image.isRecycled()) {
					return;
				}

				mImage = image.copy(Config.ARGB_8888, true);
				// 重新设置当前imageView的drawable
				setImageBitmap(mImage);
			}
		}

		public void addOperation(IImageOperation operation) {
			if (operation == null) {
				return;
			}

			copyImage();

			mOperations.add(operation);
			mImage = operation.operate(mImage);
			invalidate();
		}

		@Override
		protected void onDetachedFromWindow() {
			super.onDetachedFromWindow();

			mOperations.clear();
			setImageDrawable(null);
			if (mImage != null && !mImage.isRecycled()) {
				mImage.recycle();
				mImage = null;
			}
		}

		@Override
		public void setImageBitmap(Bitmap bm) {
			super.setImageBitmap(bm);

			if (mImage != null && !mImage.isRecycled()) {
				if (bm != mImage) {
					mImage.recycle();
					mImage = null;
				}
			}
			mImage = bm;
		}

		private Bitmap getImage() {
			if (mImage == null) {
				copyImage();
			}
			return mImage;
		}

		/*-橡皮相关*/
		private boolean hasMoreRegret() {
			return (mPathIndex > 0 ? true : mRecords.size() > 0);
		}

		private boolean hasMoreRedo() {
			return (mRecords.size() - mPathIndex > 1);
		}

		private void setCurrentStrokeWidth(int strokeWidth) {
			mCurrentStrokeWidth = strokeWidth;
		}

		private void regret() {
			if (mPathIndex > 0) {
				mPathIndex = mPathIndex - 1;
			} else {
				mRecords.clear();
			}
			invalidate();
		}

		private void redo() {
			mPathIndex = mPathIndex < (mRecords.size() - 1) ? (mPathIndex + 1) : mPathIndex;
			invalidate();
		}

		private void touchStart(float x, float y) {
			mCurrentPath = new Path();
			mCurrentPath.moveTo(x, y);
			mLastX = x;
			mLastY = y;
		}

		private void touchMove(float x, float y) {
			float dx = Math.abs(x - mLastX);
			float dy = Math.abs(y - mLastY);
			if (dx >= TOUCH_SPAN_THREASHOLD || dy >= TOUCH_SPAN_THREASHOLD) {
				mCurrentPath.quadTo(mLastX, mLastY, (x + mLastX) / 2, (y + mLastY) / 2);
				mLastX = x;
				mLastY = y;
			}
		}

		private void touchUp(float x, float y) {
			if (mCurrentPath != null) {
				mCurrentPath.lineTo(x, y);
				if (mPathIndex < mRecords.size() - 1) {
					mRecords = mRecords.subList(0, mPathIndex);
				}
				mRecords.add(new EraseRecord(mCurrentPath, mCurrentStrokeWidth));
				mPathIndex = mRecords.size() - 1;
			}
		}

		private void saveEraserResult() {
			if (mImage == null) {
				return;
			}
			Canvas canvas = new Canvas(mImage);
			if (mPathIndex < mRecords.size() - 1 && mPathIndex >= 0) {
				mRecords = mRecords.subList(0, mPathIndex);
			}
			int padding = (int) (CONTROL_POINTS_RADIS * mDensity);
			int saveCount = canvas.getSaveCount();
			canvas.save();
			android.graphics.Matrix matrix = new Matrix();
			getImageMatrix().invert(matrix);
			canvas.concat(matrix);
			canvas.translate(-padding, -padding);
			for (EraseRecord record : mRecords) {
				mEraserPaint.setStrokeWidth(record.strokeWidth);
				canvas.drawPath(record.path, mEraserPaint);
			}
			canvas.restoreToCount(saveCount);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (!mEraserMode) {
				return super.onTouchEvent(event);
			}
			float x = event.getX();
			float y = event.getY();

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touchStart(x, y);
				invalidate();
				break;
			case MotionEvent.ACTION_UP:
				touchUp(x, y);
				invalidate();
				break;
			case MotionEvent.ACTION_MOVE:
				// 判断移动的距离是否已经超过了门限
				touchMove(x, y);
				invalidate();
				break;
			}
			return true;
		}

		@Override
		protected void onDraw(Canvas canvas) {
			// 先调整亮度、对比度、饱和度
			mColorMatrix.set(new float[] { mContrast, 0, 0, 0, mIllumination, 0, mContrast, 0, 0, mIllumination, 0, 0,
					mContrast, 0, mIllumination, 0, 0, 0, 1, 0 });
			mColorMatrix.setSaturation(mSatuation);

			int padding = (int) (CONTROL_POINTS_RADIS * mDensity);
			if (mPaint == null) {
				mPaint = new Paint();
			}

			if (mImage != null) {
				LOGD("<<<<<<<<< draw with color matrix >>>>>>>>>>>>");
				int saveCount = canvas.getSaveCount();
				canvas.save();
				canvas.translate(padding, padding);
				canvas.concat(getImageMatrix());
				ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(mColorMatrix);
				mPaint.setColorFilter(colorFilter);
				canvas.drawBitmap(mImage, 0, 0, mPaint);
				canvas.restoreToCount(saveCount);
			} else {
				super.onDraw(canvas);
			}

			// // 再开始绘制
			// super.onDraw(canvas);

			// 非擦除模式时，绘制控制点
			if (!mEraserMode) {
				drawBtns(canvas);
			} else {
				// 绘制已有的擦除痕迹
				for (int i = 0; i <= mPathIndex && i < mRecords.size(); ++i) {
					EraseRecord record = mRecords.get(i);
					int strokeWidth = mCurrentStrokeWidth;
					strokeWidth = strokeWidth > 1 ? strokeWidth : 1;
					mEraserPaint.setStrokeWidth(strokeWidth);
					canvas.drawPath(record.path, mEraserPaint);
				}
				// 再绘制当前的痕迹
				if (mCurrentPath != null) {
					int strokeWidth = mCurrentStrokeWidth;
					strokeWidth = strokeWidth > 1 ? strokeWidth : 1;
					mEraserPaint.setStrokeWidth(strokeWidth);
					canvas.drawPath(mCurrentPath, mEraserPaint);
				}
			}
		}

		private void drawBtns(Canvas canvas) {
			PointF leftTop = getDeletePoint();
			PointF rightBottom = getControlPoint();
			PointF leftBottom = getLeftBottom();
			PointF rightTop = getRightTop();
			int padding = (int) (CONTROL_POINTS_RADIS * mDensity);

			if (leftTop == null || rightBottom == null || leftBottom == null || rightTop == null) {
				return;
			}
			if (getDrawable() != null && mSelected) {
				mPaint.setColorFilter(null);
				// int saveCount = canvas.getSaveCount();
				// canvas.save();
				// 画线
				mPaint.setStyle(Style.STROKE);
				mPaint.setColor(Color.BLACK);
				mPaint.setStrokeWidth(2);
				float[] pts = new float[] { leftTop.x, leftTop.y, rightTop.x, rightTop.y, rightTop.x, rightTop.y,
						rightBottom.x, rightBottom.y, rightBottom.x, rightBottom.y, leftBottom.x, leftBottom.y,
						leftBottom.x, leftBottom.y, leftTop.x, leftTop.y };
				canvas.drawLines(pts, mPaint);

				mPaint.setStyle(Style.FILL);
				mPaint.setColor(Color.RED);

				// 画删除键
				canvas.drawCircle(leftTop.x, leftTop.y, padding, mPaint);
				// 画移动键
				canvas.drawCircle(rightBottom.x, rightBottom.y, padding, mPaint);
			}
		}
	}

	private static void LOGD(String logMe) {
		Logger.d(TAG, logMe);
	}

	@Override
	public View getContextView() {
		if (mContextView == null) {
			mContextView = new ImageOverlayContextView(mContext, null);

			mContextView.setIlluminationChangedListener(this);
			mContextView.setContrastChangedListener(this);
			mContextView.setSatuationChangedListener(this);
			mContextView.setResetListener(this);
			mContextView.setEraseListener(this);
		}
		return mContextView;
	}

	// 转换到擦除模式的参数（对当前View放大缩小的参数）
	private static class EraseModeParams {
		float scaleX = 1.0f;
		float scaleY = 1.0f;
		float tx = 0.0f;
		float ty = 0.0f;
	}

	// 获取切换到擦除模式的参数
	private EraseModeParams getEraseModeParams() {
		EraseModeParams params = new EraseModeParams();
		// 首先找到当前overlay所处的方形区域
		PointF leftTop = getDeletePoint();
		PointF rightTop = getRightTop();
		PointF leftBottom = getLeftBottom();
		PointF rightBottom = getControlPoint();
		if (leftTop == null || rightTop == null || leftBottom == null || rightBottom == null) {
			return null;
		}

		float left = Math.min(leftTop.x, rightTop.x);
		left = Math.min(left, leftBottom.x);
		left = Math.min(left, rightBottom.x);
		int l = (int) left;

		float right = Math.max(leftTop.x, rightTop.x);
		right = Math.max(right, leftBottom.x);
		right = Math.max(right, rightBottom.x);
		int r = (int) right;

		float top = Math.min(leftTop.y, rightTop.y);
		top = Math.min(top, leftBottom.y);
		top = Math.min(top, rightBottom.y);
		int t = (int) top;

		float bottom = Math.max(leftTop.y, rightTop.y);
		bottom = Math.max(bottom, leftBottom.y);
		bottom = Math.max(bottom, rightBottom.y);
		int b = (int) bottom;

		ViewParent vg = getView().getParent();

		if (vg == null || !(vg instanceof View)) {
			return null;
		}
		// 先隐藏当前view
		View v = (View) vg;
		int width = v.getWidth();
		int height = v.getHeight();
		if (width > 0 && height > 0 && r > l && b > t) {
			params.tx = l;
			params.ty = t;
			params.scaleX = width / (1.0f * (r - l));
			params.scaleY = height / (1.0f * (b - t));
		}
		return params;
	}

	@Override
	public void onErased(ImageOverlayContextView view) {
		if (getEditorContainerView().getVisibility() != View.VISIBLE) {
			EraseImageEditorView eraseImageEditorView = new EraseImageEditorView(mContext);
			getEditorContainerView().setEditorView(eraseImageEditorView);
			getEditorContainerView().setVisibility(View.VISIBLE);
			mAivImage.setEraserMode(true);
			enableEraseMode();
		}
	}
	
	private void enableEraseMode() {
		EraseModeParams params = getEraseModeParams();
		float scale = Math.min(params.scaleX, params.scaleY);
//		mAivImage.setScaleX(scale);
//		mAivImage.setScaleY(scale);
//		mAivImage.setTranslationX(params.tx);
//		mAivImage.setTranslationY(params.ty);
//		ViewParent parent = mAivImage.getParent();
		View editorPanel = getEditorPanel();
		if(editorPanel != null && scale > 1.0f) {
//			editorPanel.setTranslationX(-params.tx / scale);
//			editorPanel.setTranslationY(-params.ty / scale);
			editorPanel.scrollTo((int)params.tx, (int)params.ty);
			editorPanel.setPivotX(params.tx);
			editorPanel.setPivotY(params.ty);
			editorPanel.setScaleX(scale);
			editorPanel.setScaleY(scale);
			LOGD("enableEraseMode >>>> scale = " + scale + ", tx=" + params.tx + ", ty=" + params.ty);
		}
	}
	
	private void disableEraseMode() {
//		mAivImage.setTranslationX(-mAivImage.getTranslationX());
//		mAivImage.setTranslationY(-mAivImage.getTranslationY());
//		mAivImage.setScaleX(1/mAivImage.getScaleX());
//		mAivImage.setScaleY(1/mAivImage.getScaleY());
		View editorPanel = getEditorPanel();
		if(editorPanel != null) {
//			editorPanel.setTranslationX(0);
//			editorPanel.setTranslationY(0);
			editorPanel.scrollTo(0, 0);
			editorPanel.setScaleX(1);
			editorPanel.setScaleY(1);
		}
	}

	@Override
	public void onReset(ImageOverlayContextView view) {
		mAivImage.reset();
	}

	@Override
	public void onSatuationChanged(ImageOverlayContextView view, int satuation) {
		mAivImage.setSatuation(satuation);
		LOGD("setSatuation >>>>>> " + satuation);
	}

	@Override
	public void onContrastChanged(ImageOverlayContextView view, int contrast) {
		mAivImage.setContrast(contrast);
		LOGD("setContrast >>>>>> " + contrast);
	}

	@Override
	public void onIlluminationChanged(ImageOverlayContextView view, int illumination) {
		mAivImage.setIllumination(illumination);
		LOGD("setIllumination >>>>>> " + illumination);
	}
}
