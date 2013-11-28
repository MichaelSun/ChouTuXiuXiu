package com.canruoxingchen.uglypic.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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
import com.canruoxingchen.uglypic.util.Logger;

public class ImageWidgetOverlay extends ObjectOverlay {

	private static final String TAG = "ImageWidgetOverlay";

	private AsyncImageView mAivImage;

	private Context mContext;

	private boolean mSelected = false;

	private float mScaleX = 1.0f;
	private float mScaleY = 1.0f;

	public ImageWidgetOverlay(Context context, Uri uri) {
		this.mContext = context;
		initContentView();
		if (uri != null) {
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
		mAivImage = new MyImageView(mContext);
		return mAivImage;
	}

	@Override
	public Rect getContentBounds() {
		// TODO Auto-generated method stub
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

	private class MyImageView extends AsyncImageView {

		private static final int CONTROL_POINTS_RADIS = 20;
		private float mDensity = 1.0f;
		private Paint mPaint;

		public MyImageView(Context context) {
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
			super.onDraw(canvas);

			PointF leftTop = getDeletePoint();
			PointF rightBottom = getControlPoint();
			PointF leftBottom = getLeftBottom();
			PointF rightTop = getRightTop();

			if (leftTop == null || rightBottom == null || leftBottom == null
					|| rightTop == null) {
				return;
			}

			if (mPaint == null) {
				mPaint = new Paint();
			}

			if (getDrawable() != null && mSelected) {
				// int saveCount = canvas.getSaveCount();
				// canvas.save();

				int padding = (int) (CONTROL_POINTS_RADIS * mDensity);

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

				// canvas.restoreToCount(saveCount);
			}
		}
	}

	private static void LOGD(String logMe) {
		Logger.d(TAG, logMe);
	}

}
