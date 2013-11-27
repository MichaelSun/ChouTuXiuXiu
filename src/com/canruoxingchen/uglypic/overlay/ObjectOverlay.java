/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

/**
 * 物品浮层，可以删除，并且可以平移、放大缩小、以及旋转
 * 
 * @author wsf
 * 
 */
public abstract class ObjectOverlay implements IOverlay {

	private static final int SIZE_CONTORL_VIEW = 30;

	private static final int CONTROL_POINTS_RADIUS = 10;

	private float mDensity;

	//
	private Paint mPaint = null;

	private Matrix mMatrix = new Matrix();

	public interface ObjectOperationListener {

		public void onDelete(ObjectOverlay overlay);

		public void onMoveOverlay(ObjectOverlay overlay, int dx, int dy);
	}

	protected View mContentView;

	private ObjectOperationListener mObjectOperationListener;

	public void setOperationListener(ObjectOperationListener listener) {
		this.mObjectOperationListener = listener;
	}

	protected ObjectOperationListener getOperationListener() {
		return mObjectOperationListener;
	}

	@Override
	public Rect getContentBounds() {
		// TODO Auto-generated method stub
		return null;
	}

	// 初始化内容区域
	protected abstract View initContentView();

	@Override
	public View getView() {
		return mContentView;
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
		Rect rect = getContentBounds();
		if (rect != null) {
			Matrix matrix = new Matrix();
			mMatrix.invert(matrix);
			float[] pts = new float[] { x, y };
			matrix.mapPoints(pts);
			return rect.contains((int) pts[0], (int) pts[1]);
		}
		return false;
	}
	
	protected Matrix getMatrix() {
		return mMatrix;
	}

	/**
	 * 平移
	 * 
	 * @param dx
	 * @param dy
	 */
	public void translate(int dx, int dy) {
		if (getContentBounds() != null) {
			mMatrix.postTranslate(dx, dy);
		}
	}

	/**
	 * 放大
	 * 
	 * @param scale
	 */
	public void scale(float sx, float sy) {
		if (getContentBounds() != null) {
			mMatrix.postScale(sx, sy);
		}
	}

	/**
	 * 
	 * @param degree
	 */
	public void rotate(float degrees) {
		if (getContentBounds() != null) {
			Rect rect = getContentBounds();
			int centerX = (rect.left + rect.right) / 2;
			int centerY = (rect.top + rect.bottom) / 2;
			mMatrix.postRotate(degrees, centerX, centerY);
		}
	}

	public boolean onTouchEvent(View view, MotionEvent e) {
		return false;
	}
}
