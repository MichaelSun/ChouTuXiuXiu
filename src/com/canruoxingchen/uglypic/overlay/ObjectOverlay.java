/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

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
	public Rect getContentViewBounds() {
		// TODO Auto-generated method stub
		return null;
	}

	//初始化内容区域
	protected abstract View initContentView();

	@Override
	public View getView() {
		return mContentView;
	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.BaseOverlay#doOverlay()
	 */
	@Override
	public void doOverlay() {

	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.BaseOverlay#resetOverlay()
	 */
	@Override
	public void resetOverlay() {

	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.BaseOverlay#contains(int, int)
	 */
	@Override
	public void contains(int x, int y) {

	}
	
	/**
	 * 平移
	 * @param dx
	 * @param dy
	 */
	public abstract void translate(int dx, int dy);
	
	/**
	 * 放大
	 * @param scale
	 */
	public abstract void scale(float scale);
	
	/**
	 * 
	 * @param degree
	 */
	public abstract void rotate(float degree);

	
}
