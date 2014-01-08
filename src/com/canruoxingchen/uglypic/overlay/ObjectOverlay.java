/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;

import com.canruoxingchen.uglypic.R;
import com.canruoxingchen.uglypic.UglyPicApp;
import com.canruoxingchen.uglypic.util.Logger;

/**
 * 物品浮层，可以删除，并且可以平移、放大缩小、以及旋转
 * 
 * @author wsf
 * 
 */
public abstract class ObjectOverlay implements IOverlay {

	public static final int CONTROL_POINTS_RADIUS = 20;

	protected float mDensity = -1.0f;

	private Matrix mMatrix = new Matrix();

	private float mRotateDegrees = 0.0f;

	// 控制点是否被选中
	private boolean mControlPointSelected = false;
	// 是否选中了删除键
	private boolean mFlipPointSelected = false;

	private View mEditorPanel;

	private boolean mHasBeenSelected = false;

	public interface ObjectOperationListener {

		public void onDelete(ObjectOverlay overlay);

		public void onMoveOverlay(ObjectOverlay overlay, int dx, int dy);
	}

	private EditorContainerView mEditorContainer;

	protected View mContentView;

	private ObjectOperationListener mObjectOperationListener;

	// contentView外加控制按钮
	private ViewGroup mContainerView = null;

	private Bitmap mFlipBtn;
	private Bitmap mRotateBtn;

	public void setOperationListener(ObjectOperationListener listener) {
		this.mObjectOperationListener = listener;
	}

	protected ObjectOperationListener getOperationListener() {
		return mObjectOperationListener;
	}

	public void setEditorContainerView(EditorContainerView editorContainer) {
		mEditorContainer = editorContainer;
	}

	public EditorContainerView getEditorContainerView() {
		return mEditorContainer;
	}

	public void setEditorPanel(View editorPanel) {
		this.mEditorPanel = editorPanel;
	}

	public boolean hasBeenSelected() {
		return mHasBeenSelected;
	}

	public void setHasBeenSelected(boolean hasSelected) {
		this.mHasBeenSelected = hasSelected;
	}

	protected View getEditorPanel() {
		return this.mEditorPanel;
	}

	public boolean isFlipable() {
		return false;
	}

	public void flip() {

	}

	@Override
	public Rect getInitialContentBounds() {
		retrieveDensity();
		return null;
	}

	// 初始化内容区域
	protected abstract View initContentView();

	/**
	 * 返回上下文菜单，比如贴图的编辑菜单
	 * 
	 * @return
	 */
	public abstract View getContextView();

	// @Override
	// public View getView() {
	// return mContentView;
	// }

	protected void retrieveDensity() {

		if (mDensity < 0) {
			WindowManager wm = (WindowManager) UglyPicApp.getAppExContext().getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics dm = new DisplayMetrics();
			wm.getDefaultDisplay().getMetrics(dm);
			mDensity = dm.density;
		}
	}

	/**
	 * 对本身的View进行了包装，增加了边框和控制按钮，子类可根据需要重写此方法s
	 * 
	 * @return
	 */
	public View getContainerView(Context context) {
		if (mFlipBtn == null) {
			mFlipBtn = BitmapFactory.decodeResource(context.getResources(), R.drawable.photo_editor_flip);
		}
		if (mRotateBtn == null) {
			mRotateBtn = BitmapFactory.decodeResource(context.getResources(), R.drawable.photo_editor_rotate);
		}

		if (mContainerView == null) {
			mContainerView = new ContainerView(context);
			if (getView() != null) {

				RelativeLayout.LayoutParams params = getDefaultParams();
				if (params == null) {
					params = new RelativeLayout.LayoutParams(android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
							android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
				}
				mContainerView.addView(getView(), params);
			}
		}
		if (getView() == null) {
			return null;
		}
		return mContainerView;
	}

	// 添加到container view中的默认参数
	protected RelativeLayout.LayoutParams getDefaultParams() {
		retrieveDensity();
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT);
		return params;
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
		mRotateDegrees = 0.0f;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.canruoxingchen.uglypic.overlay.BaseOverlay#contains(int, int)
	 */
	@Override
	public boolean contains(int x, int y) {
		Rect rect = getInitialContentBounds();
		if (rect != null) {
			Matrix matrix = new Matrix();
			mMatrix.invert(matrix);
			float[] pts = new float[] { x, y };
			matrix.mapPoints(pts);
			return rect.contains((int) (pts[0]), (int) (pts[1]));
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
		if (getInitialContentBounds() != null) {
			mMatrix.postTranslate(dx, dy);
		}
	}

	/**
	 * 放大
	 * 
	 * @param scale
	 */
	public void scale(float sx, float sy) {
		PointF center = getCenter();
		if (center != null) {
			mMatrix.postScale(sx, sy, center.x, center.y);
		}
	}

	/**
	 * 
	 * @param degree
	 */
	public void rotate(float degrees) {
		PointF center = getCenter();
		if (center != null) {
			mMatrix.postRotate(degrees, center.x, center.y);
			mRotateDegrees += degrees;
		}
	}

	public PointF getCenter() {
		PointF leftTop = getFlipButton();
		PointF rightBottom = getControlButton();
		if (leftTop != null && rightBottom != null) {
			float centerX = (leftTop.x + rightBottom.x) / 2;
			float centerY = (leftTop.y + rightBottom.y) / 2;
			return new PointF(centerX, centerY);
		}
		return null;
	}

	public boolean onTouchEvent(View view, MotionEvent e) {
		return false;
	}

	public PointF getControlButton() {
		if (getInitialContentBounds() != null) {
			Rect rect = getInitialContentBounds();
			float[] pts = new float[] { rect.right, rect.bottom };
			Matrix matrix = getMatrix();
			matrix.mapPoints(pts);
			return new PointF(pts[0], pts[1]);
		}
		return null;
	}

	public PointF getFlipButton() {
		if (getInitialContentBounds() != null) {
			Rect rect = getInitialContentBounds();
			float[] pts = new float[] { rect.left, rect.top };
			Matrix matrix = getMatrix();
			matrix.mapPoints(pts);
			return new PointF(pts[0], pts[1]);
		}
		return null;
	}

	public PointF getLeftBottom() {
		if (getInitialContentBounds() != null) {
			Rect rect = getInitialContentBounds();
			float[] pts = new float[] { rect.left, rect.bottom };
			Matrix matrix = getMatrix();
			matrix.mapPoints(pts);
			return new PointF(pts[0], pts[1]);
		}
		return null;
	}

	public PointF getRightTop() {
		if (getInitialContentBounds() != null) {
			Rect rect = getInitialContentBounds();
			float[] pts = new float[] { rect.right, rect.top };
			Matrix matrix = getMatrix();
			matrix.mapPoints(pts);
			return new PointF(pts[0], pts[1]);
		}
		return null;
	}

	public boolean isControlPointSelected() {
		return mControlPointSelected;
	}

	public boolean isDeletePointSelected() {
		return false;
	}

	public boolean isFlipPointSelected() {
		return mFlipPointSelected;
	}

	private float distance(float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

	/**
	 * 检查是否点到了删除以及控制按钮
	 * 
	 * @param x
	 * @param y
	 */
	public void checkKeyPointsSelectionStatus(int x, int y) {
		if (mDensity < 0) {
			WindowManager wm = (WindowManager) UglyPicApp.getAppExContext().getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics dm = new DisplayMetrics();
			wm.getDefaultDisplay().getMetrics(dm);
			mDensity = dm.density;
		}
		float radius = CONTROL_POINTS_RADIUS * mDensity;

		PointF point = getFlipButton();
		if (point != null && distance(point.x, point.y, x, y) < radius && isFlipable()) {
			mFlipPointSelected = true;
		} else {
			mFlipPointSelected = false;
		}

		point = getControlButton();
		if (point != null && distance(point.x, point.y, x, y) < radius) {
			mControlPointSelected = true;
		} else {
			mControlPointSelected = false;
		}
	}

	private class ContainerView extends RelativeLayout {

		private Paint mPaint;
		private Matrix mInvertMatrix = new Matrix();

		public ContainerView(Context context) {
			super(context);
			mPaint = new Paint();
			mPaint.setAntiAlias(true);
		}

		@Override
		protected void dispatchDraw(Canvas canvas) {
			super.dispatchDraw(canvas);
			drawBtns(canvas);
		}

		private void drawBtns(Canvas canvas) {
			PointF leftTop = getFlipButton();
			PointF rightBottom = getControlButton();
			PointF leftBottom = getLeftBottom();
			PointF rightTop = getRightTop();
			int padding = (int) (CONTROL_POINTS_RADIUS * mDensity);

			if (leftTop == null || rightBottom == null || leftBottom == null || rightTop == null) {
				return;
			}
			if (isOverlaySelected()) {
				mPaint.setColorFilter(null);
				// 画线
				mPaint.setStyle(Style.STROKE);
				mPaint.setColor(Color.WHITE);
				mPaint.setStrokeWidth(5);
				mPaint.setStrokeJoin(Join.ROUND);

				float[] pts = new float[] { leftTop.x, leftTop.y, rightTop.x, rightTop.y, rightTop.x, rightTop.y,
						rightBottom.x, rightBottom.y, rightBottom.x, rightBottom.y, leftBottom.x, leftBottom.y,
						leftBottom.x, leftBottom.y, leftTop.x, leftTop.y };
				canvas.drawLines(pts, mPaint);

				mPaint.setStyle(Style.FILL);
				mPaint.setColor(Color.RED);

				int saveCount = canvas.getSaveCount();
				canvas.save();
				canvas.rotate(mRotateDegrees, leftTop.x, leftTop.y);
				// 画删除键
				if (isFlipable() && mFlipBtn != null && !mFlipBtn.isRecycled()) {
					// canvas.drawCircle(rightTop.x, rightTop.y, padding,
					// mPaint);
					canvas.drawBitmap(mFlipBtn, new Rect(0, 0, mFlipBtn.getWidth(), mFlipBtn.getHeight()), new RectF(
							leftTop.x - padding, leftTop.y - padding, leftTop.x + padding, leftTop.y + padding), mPaint);
				}
				canvas.restoreToCount(saveCount);
				canvas.rotate(mRotateDegrees, rightBottom.x, rightBottom.y);
				// 画移动键
				// canvas.drawCircle(rightBottom.x, rightBottom.y, padding,
				// mPaint);
				if (mRotateBtn != null && !mRotateBtn.isRecycled()) {
					canvas.drawBitmap(mRotateBtn, new Rect(0, 0, mRotateBtn.getWidth(), mRotateBtn.getHeight()),
							new RectF(rightBottom.x - padding, rightBottom.y - padding, rightBottom.x + padding,
									rightBottom.y + padding), mPaint);
				}
				canvas.restoreToCount(saveCount);
			}
		}
	}

	private static void LOGD(String logMe) {
		Logger.d("ObjectOverlay", logMe);
	}
}
