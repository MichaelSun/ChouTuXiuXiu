package com.canruoxingchen.uglypic.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView.ScaleType;

import com.canruoxingchen.uglypic.cache.AsyncImageView;
import com.canruoxingchen.uglypic.cache.ImageInfo;

public class ImageWidgetOverlay extends ObjectOverlay {

	private Uri mUri;
	
	private AsyncImageView mAivImage;
	
	private Context mContext;
	
	private Matrix mMatrix = new Matrix();
	
	public ImageWidgetOverlay(Context context, Uri uri) {
		this.mContext = context;
		this.mUri = uri;
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
	public void translate(int dx, int dy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scale(float scale) {
		// TODO Auto-generated method stub

	}

	@Override
	public void rotate(float degree) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSelected(boolean selected) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSelected() {
		// TODO Auto-generated method stub
		return false;
	}
	
	private static class MyImageView extends AsyncImageView {

		private static final int CONTROL_POINTS_RADIS = 10;
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
			
			if(mPaint == null) {
				mPaint = new Paint();
				mPaint.setStyle(Style.FILL);
				mPaint.setColor(Color.RED);
			}

			int padding = (int) (CONTROL_POINTS_RADIS * mDensity);
			//画删除键
			canvas.drawCircle(getLeft() + padding, getTop() + padding, 
					padding, mPaint);
			//画移动键
			canvas.drawCircle(getRight() - padding, getBottom() - padding, 
					padding, mPaint);
		}
		
	}

}
