package com.canruoxingchen.uglypic.overlay;

import java.util.LinkedList;
import java.util.List;

import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView.ScaleType;

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

	private class CopyOnWriteImageView extends AsyncImageView {

		private static final int CONTROL_POINTS_RADIS = 20;
		private float mDensity = 1.0f;
		private Paint mPaint;

		// 附加操作，不包含亮度、对比度、饱和度
		private List<IImageOperation> mOperations = new LinkedList<IImageOperation>();

		private Bitmap mImage = null;

		private ColorMatrix mColorMatrix = new ColorMatrix();
		
		private float mIllumination;
		private float mSatuation;
		private float mContrast;
		
		private void reset() {
			mOperations.clear();
			mIllumination = IlluminationImageOperation.DEFAULT;
			mSatuation = SatuationImageOperation.DEFAULT;
			mContrast = ContrastImageOperation.DEFAULT;
			if(mImage != null && !mImage.isRecycled()) {
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
//			mContrast = (contrast + 64) / 128.0f;
//			mContrast = 128 * (1 - contrast);
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
//			mIllumination = (illumination - IlluminationImageOperation.DEFAULT) 
//					* 1.0F / IlluminationImageOperation.DEFAULT * 180;  
			
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
			if (mImage != null && !mImage.isRecycled()) {
				mImage.recycle();
				mImage = null;
			}
		}

		public CopyOnWriteImageView(Context context) {
			super(context);
			WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics dm = new DisplayMetrics();
			wm.getDefaultDisplay().getMetrics(dm);
			mDensity = dm.density;
			int padding = (int) (CONTROL_POINTS_RADIS * mDensity);
			setPadding(padding, padding, padding, padding);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			// 先调整亮度、对比度、饱和度
			mColorMatrix.set(new float[] { mContrast, 0, 0, 0, mIllumination, 0,
					mContrast, 0, 0, mIllumination, 0, 0, mContrast, 0,
					mIllumination, 0, 0, 0, 1, 0 });
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

			PointF leftTop = getDeletePoint();
			PointF rightBottom = getControlPoint();
			PointF leftBottom = getLeftBottom();
			PointF rightTop = getRightTop();

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

	@Override
	public void onErased(ImageOverlayContextView view) {

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
