/**
 * GestureView.java
 */
package com.canruoxingchen.uglypic.view;

import com.almeros.android.multitouch.gesturedetector.MoveGestureDetector;
import com.almeros.android.multitouch.gesturedetector.RotateGestureDetector;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * @Description 透明的View，用来接收touch事件
 * 
 * @author Shaofeng Wang
 * 
 * @time 2013-5-31 下午12:27:39
 * 
 */
public class GestureView extends View {

	public GestureView(Context context, AttributeSet attrs) {
		super(context, attrs);

		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		mRotateDetector = new RotateGestureDetector(context,
				new RotateListener());
		mMoveDetector = new MoveGestureDetector(context, new MoveListener());
	}

	// 是否启用旋转手势
	private boolean mEnableRotateGesture = true;

	private Matrix mMatrix = new Matrix();
	private float mScaleFactor = .4f;
	private float mRotationDegrees = 0.f;
	private float mFocusX = 0.f;
	private float mFocusY = 0.f;

	private int mViewWidth = 0;
	private int mViewHeight = 0;

	private ScaleGestureDetector mScaleDetector;
	private RotateGestureDetector mRotateDetector;
	private MoveGestureDetector mMoveDetector;

	public void initParams(float scaleFactor, float rotationDegrees,
			float focusX, float focusY) {
		this.mScaleFactor = scaleFactor;
		this.mRotationDegrees = rotationDegrees;
		this.mFocusX = focusX;
		this.mFocusY = focusY;
		
		float scaledImageCenterX = (mViewWidth * mScaleFactor) / 2;
		float scaledImageCenterY = (mViewHeight * mScaleFactor) / 2;

		mMatrix.reset();
		mMatrix.postScale(mScaleFactor, mScaleFactor);
		mMatrix.postRotate(mRotationDegrees, scaledImageCenterX,
				scaledImageCenterY);
		mMatrix.postTranslate(mFocusX, mFocusY);
	}

	public void reset() {
		mMatrix.reset();
	}

	public static interface OnScaleListener {
		void onScaled(float scale);
	}

	public static interface OnRotateListener {
		void onRotated(float degree);
	}

	public static interface OnMoveListener {
		void onMove(float x, float y);
	}

	private OnScaleListener mOnScaleListener;
	private OnRotateListener mOnRotateListener;
	private OnMoveListener mOnMoveListener;

	public void setOnScaleListener(OnScaleListener listener) {
		this.mOnScaleListener = listener;
	}

	public void setOnRotateListener(OnRotateListener listener) {
		this.mOnRotateListener = listener;
	}

	public void setOnMoveListener(OnMoveListener listener) {
		this.mOnMoveListener = listener;
	}

	public void setRotateGestureEnabled(boolean enabled) {
		mEnableRotateGesture = enabled;
	}

	public Matrix getCurrentMatrix() {
		return mMatrix;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		mViewWidth = (right - left);
		mViewHeight = (bottom - top);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mScaleDetector.onTouchEvent(event);
		mRotateDetector.onTouchEvent(event);
		mMoveDetector.onTouchEvent(event);

		float scaledImageCenterX = (mViewWidth * mScaleFactor) / 2;
		float scaledImageCenterY = (mViewHeight * mScaleFactor) / 2;

		mMatrix.reset();
		mMatrix.postScale(mScaleFactor, mScaleFactor);
		mMatrix.postRotate(mRotationDegrees, scaledImageCenterX,
				scaledImageCenterY);
		// mMatrix.postTranslate(mFocusX - scaledImageCenterX, mFocusY
		// - scaledImageCenterY);
		mMatrix.postTranslate(mFocusX, mFocusY);

		return false;
	}

	private class ScaleListener extends
			ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleFactor *= detector.getScaleFactor();
			mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));

			if (mOnScaleListener != null) {
				mOnScaleListener.onScaled(detector.getScaleFactor());
			}

			notifyScale(detector.getScaleFactor());

			return true;
		}
	}

	public void postRotate(float delta) {
		mRotationDegrees += delta;
		float scaledImageCenterX = (mViewWidth * mScaleFactor) / 2;
		float scaledImageCenterY = (mViewHeight * mScaleFactor) / 2;
		mMatrix.reset();
		mMatrix.postScale(mScaleFactor, mScaleFactor);
		mMatrix.postRotate(mRotationDegrees, scaledImageCenterX,
				scaledImageCenterY);
		// mMatrix.postTranslate(mFocusX - scaledImageCenterX, mFocusY
		// - scaledImageCenterY);
		mMatrix.postTranslate(mFocusX, mFocusY);
	}

	protected void notifyScale(float scale) {
		if (mOnScaleListener != null) {
			mOnScaleListener.onScaled(scale);
		}
	}
	
	protected void notifyRotate(float degree) {
		if (mOnRotateListener != null) {
			mOnRotateListener.onRotated(degree);
		}
	}

	private class RotateListener extends
			RotateGestureDetector.SimpleOnRotateGestureListener {
		@Override
		public boolean onRotate(RotateGestureDetector detector) {
			if (mEnableRotateGesture) {
				mRotationDegrees -= detector.getRotationDegreesDelta();
				notifyRotate(-detector.getRotationDegreesDelta());
			}
			return true;
		}
	}

	private class MoveListener extends
			MoveGestureDetector.SimpleOnMoveGestureListener {
		@Override
		public boolean onMove(MoveGestureDetector detector) {
			PointF d = detector.getFocusDelta();
			mFocusX += d.x;
			mFocusY += d.y;
			notifyMove(d.x, d.y);
			return true;
		}
	}

	protected void notifyMove(float dx, float dy) {

		if (mOnMoveListener != null) {
			mOnMoveListener.onMove(dx, dy);
		}
	}
}
