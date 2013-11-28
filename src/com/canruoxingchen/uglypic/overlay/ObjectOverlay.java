/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

import com.canruoxingchen.uglypic.UglyPicApp;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * 物品浮层，可以删除，并且可以平移、放大缩小、以及旋转
 * 
 * @author wsf
 * 
 */
public abstract class ObjectOverlay implements IOverlay {

	private static final int SIZE_CONTORL_VIEW = 30;

	private static final int CONTROL_POINTS_RADIUS = 15;

	private float mDensity = -1.0f;

	private Matrix mMatrix = new Matrix();
	
	//控制点是否被选中
	private boolean mControlPointSelected = false;
	//是否选中了删除键
	private boolean mDeletePointSelected = false;

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
	
	/**
	 * 获得当前控制按钮的中心
	 * @return
	 */
	public PointF getControlPoint() {
		if(getContentBounds() != null) {
			Rect rect = getContentBounds();
			float[] pts = new float[]{rect.right, rect.bottom};
			mMatrix.mapPoints(pts);
			return new PointF(pts[0], pts[1]);
		}
		return null;
	}

	/**
	 * 获得当前控制按钮的中心
	 * @return
	 */
	public PointF getDeletePoint() {
		if(getContentBounds() != null) {
			Rect rect = getContentBounds();
			float[] pts = new float[]{rect.left, rect.top};
			mMatrix.mapPoints(pts);
			return new PointF(pts[0], pts[1]);
		}
		return null;
	}
	
	public boolean isControlPointSelected() {
		return mControlPointSelected;
	}
	
	public boolean isDeletePointSelected() {
		return mDeletePointSelected;
	}
	
	private float distance(float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}
	
	/**
	 * 检查是否点到了删除以及控制按钮
	 * @param x
	 * @param y
	 */
	public void checkKeyPointsSelectionStatus(int x, int y) {
		if(mDensity < 0) {
			WindowManager wm = (WindowManager) UglyPicApp.getAppExContext().getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics dm = new DisplayMetrics();
			wm.getDefaultDisplay().getMetrics(dm);
			mDensity = dm.density;
		}
		float radius = CONTROL_POINTS_RADIUS * mDensity;
		
		PointF point = getDeletePoint();
		if(point != null && distance(point.x, point.y, x, y) < radius) {
			mDeletePointSelected = true;
		} else {
			mDeletePointSelected = false;
		}
		
		point = getControlPoint();
		if(point != null && distance(point.x, point.y, x, y) < radius) {
			mControlPointSelected = true;
		} else {
			mControlPointSelected = false;
		}
	}
}
