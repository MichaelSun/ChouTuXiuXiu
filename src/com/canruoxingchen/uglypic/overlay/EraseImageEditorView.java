/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.canruoxingchen.uglypic.util.Logger;

/**
 * 擦除照片的View
 * 
 * @author wsf
 * 
 */
public class EraseImageEditorView extends RelativeLayout implements IEditor {

	private EraseableImageView mEraseImageView = null;

	private ImageView mIvBg = null;

	private ImageEraseListener mEraseListener;

	private Bitmap mBackground = null;

	public EraseImageEditorView(Context context) {
		super(context);
		mIvBg = new ImageView(context);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		addView(mIvBg, params);
		this.mEraseImageView = new EraseableImageView(context);
		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		addView(mEraseImageView, params);
	}

	// 设置背景和前景
	public void setImage(Bitmap background, Bitmap image, Matrix imageMatrix) {
		mEraseImageView.setImage(background, image, imageMatrix);
		mBackground = background;
		mEraseImageView.setBackgroundDrawable(new BitmapDrawable(mBackground));
	}

	public void setImageEraseListener(ImageEraseListener listener) {
		this.mEraseListener = listener;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mEraseImageView.setBackgroundDrawable(null);
		mIvBg.setImageDrawable(null);
		if (mBackground != null && !mBackground.isRecycled()) {
			mBackground.recycle();
			mBackground = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.canruoxingchen.uglypic.overlay.IEditor#onCancel()
	 */
	@Override
	public void onCancel() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.canruoxingchen.uglypic.overlay.IEditor#onRegret()
	 */
	@Override
	public void onRegret() {
		if (mEraseImageView.hasMoreRegret()) {
			mEraseImageView.regret();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.canruoxingchen.uglypic.overlay.IEditor#onRedo()
	 */
	@Override
	public void onRedo() {
		if (mEraseImageView.hasMoreRedo()) {
			mEraseImageView.redo();
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
		if (mEraseListener != null) {
			mEraseListener.onImageErased(mEraseImageView.getResult());
		}
	}

	@Override
	public boolean hasMoreRegret() {
		return mEraseImageView.hasMoreRegret();
	}

	@Override
	public boolean hasMoreRedo() {
		return mEraseImageView.hasMoreRedo();
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

	private static class EraseRecord {
		Path path;
		int strokeWidth;

		public EraseRecord(Path path, int strokeWidth) {
			this.path = path;
			this.strokeWidth = strokeWidth;
		}
	}

	private static class EraseableImageView extends View implements OnTouchListener {

		// 为了避免记录太多的点，两次间隔4像素以上才记录进路径中
		private static float TOUCH_SPAN_THREASHOLD = 4;

		private Bitmap mImage = null;

		private List<EraseRecord> mRecords = new LinkedList<EraseRecord>();

		private int mPathIndex = 0;

		private Paint mPaint = null;
		private Bitmap mBitmap = null;
		private Bitmap mBackground = null;
		private Canvas mCanvas = null;
		private Paint mBitmapPaint = null;

		private Path mCurrentPath = null;
		private float mLastX = 0.0f;
		private float mLastY = 0.0f;

		private int mCurrentStrokeWidth = 5;
		private float mScale = 1.0f;

		private Matrix mImageMatrix = new Matrix();
		private Matrix mMatrix = new Matrix();

		public EraseableImageView(Context context) {
			super(context);

			mBitmapPaint = new Paint(Paint.DITHER_FLAG);

			mPaint = new Paint();
			mPaint.setAntiAlias(true);
			mPaint.setColor(Color.RED);
			// mPaint.setAlpha(0);
			mPaint.setDither(true);
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeJoin(Paint.Join.ROUND);
			mPaint.setStrokeCap(Paint.Cap.ROUND);
			mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			mPaint.setStrokeWidth(12);
			setOnTouchListener(this);
		}

		public void setImage(Bitmap background, Bitmap image, Matrix matrix) {
			if(this.mImage != image) {
				this.mImage = image;
			}
			
			if(this.mBackground != background) {
				mBackground = background;
			}
			
			if (mBitmap != null && !mBitmap.isRecycled()) {
				mBitmap.recycle();
				mBitmap = null;
			}
			mBitmap = mBackground.copy(Config.ARGB_8888, true);
			mCanvas = new Canvas(mBitmap);

			if (mImageMatrix != matrix) {
				mImageMatrix = matrix;
			}
			
			if(mImageMatrix == null) {
				mImageMatrix = new Matrix();
			}
			
//			mCanvas.drawBitmap(mImage, mImageMatrix, null);

			int vwidth = getWidth();
			int vheight = getHeight();
			int dwidth = mBitmap.getWidth();
			int dheight = mBitmap.getHeight();
			if (dwidth * vheight > vwidth * dheight) {
				mScale = (float) vheight / (float) dheight;
			} else {
				mScale = (float) vwidth / (float) dwidth;
			}
			mMatrix.reset();
			mMatrix.postScale(mScale, mScale, dwidth / 2, dheight / 2);
//			matrix.postScale(mScale, mScale);

			// 绘制已有的擦除痕迹
			for (int i = 0; i <= mPathIndex && i < mRecords.size(); ++i) {
				EraseRecord record = mRecords.get(i);
				int strokeWidth = (int) (mCurrentStrokeWidth * mScale);
				strokeWidth = strokeWidth > 1 ? strokeWidth : 1;
				mPaint.setStrokeWidth(20);
				mCanvas.drawPath(record.path, mPaint);
			}
		}

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
			// 重新初始化mBitmap
			setImage(mBackground, mImage, mImageMatrix);
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
				mCanvas.drawPath(mCurrentPath, mPaint);

				if (mPathIndex < mRecords.size() - 1) {
					mRecords = mRecords.subList(0, mPathIndex);
				}
				mRecords.add(new EraseRecord(mCurrentPath, mCurrentStrokeWidth));
				mPathIndex = mRecords.size() - 1;
			}
		}

		@Override
		protected void onDraw(Canvas canvas) {
			// super.onDraw(canvas);
			if (mBitmap == null) {
				return;
			}

			// 从开始画到当前一条记录
			canvas.drawColor(Color.TRANSPARENT);
			
			// 绘制当前的路径
			canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

			if (mCurrentPath != null) {
				canvas.drawPath(mCurrentPath, mPaint);
			}
		}

		/**
		 * 返回擦除之后的结果
		 * 
		 * @return
		 */
		public Bitmap getResult() {
			Bitmap result = mImage.copy(Config.ARGB_8888, true);
			Canvas canvas = new Canvas(result);
			Matrix matrix = new Matrix();
			matrix.postScale(1 / mScale, 1 / mScale);
			if (mPathIndex < mRecords.size() - 1) {
				mRecords = mRecords.subList(0, mPathIndex);
			}
			for (EraseRecord record : mRecords) {
				record.path.transform(matrix);
				mPaint.setStrokeWidth(record.strokeWidth);
				canvas.drawPath(record.path, mPaint);
			}
			return result;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
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
	}

	interface ImageEraseListener {
		public void onImageErased(Bitmap image);
	}

	private static void LOGD(String logMe) {
		Logger.d("Eraser", logMe);
	}
}
