package com.canruoxingchen.uglypic.overlay;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.canruoxingchen.uglypic.MessageCenter;
import com.canruoxingchen.uglypic.R;
import com.canruoxingchen.uglypic.UglyPicApp;
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

	private static final int[] ERASER_WIDTH = new int[] {16, 20, 24, 28, 32, 36, 40 };
	private static final int TOUCH_SPAN_THREASHOLD = 4;
	private static final String TAG = "ImageWidgetOverlay";

	private CopyOnWriteImageView mAivImage;

	private Context mContext;

	private boolean mSelected = false;

	private ImageOverlayContextView mContextView;

	private Uri mUri = null;

	private float mScaleX = 1.0f;
	private float mScaleY = 1.0f;
	private float mTranslateX = 0.0f;
	private float mTranslateY = 0.0f;
	private float mRotate = 0.0f;

	public ImageWidgetOverlay(Context context, Uri uri) {
		this.mContext = context;
		initContentView();
		if (uri != null) {
			mUri = uri;
			mAivImage.setImageInfo(ImageInfo.obtain(uri.toString()));
			mAivImage.setScaleType(ScaleType.CENTER_INSIDE);
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
	public Rect getInitialContentBounds() {
		// super.getInitialContentBounds();
		// return mAivImage.getDrawable() == null ? null :
		// mAivImage.getDrawable().getBounds();

		retrieveDensity();
		// float density = mDensity > 0 ? mDensity : 1.0f;
		float density = 1.0f;
		int left = (int) (mAivImage.getLeft() / density);
		int top = (int) (mAivImage.getTop() / density);
		int right = (int) (mAivImage.getRight() / density);
		int bottom = (int) (mAivImage.getBottom() / density);
		return new Rect(left, top, right, bottom);
	}

	@Override
	public void translate(int dx, int dy) {
		super.translate(dx, dy);
		if (mAivImage.getDrawable() != null) {
			// mAivImage.setImageMatrix(getMatrix());
			mTranslateX += dx;
			mTranslateY += dy;
			mAivImage.setTranslationX(mTranslateX);
			mAivImage.setTranslationY(mTranslateY);
		}
	}

	@Override
	public void scale(float sx, float sy) {
		super.scale(sx, sy);
		if (mAivImage.getDrawable() != null) {
			// mAivImage.setImageMatrix(getMatrix());
			mScaleX *= sx;
			mScaleY *= sy;
			mAivImage.setScaleX(mScaleX);
			mAivImage.setScaleY(mScaleY);
		}
	}

	@Override
	public void rotate(float degrees) {
		super.rotate(degrees);
		if (mAivImage.getDrawable() != null) {
			// mAivImage.setImageMatrix(getMatrix());
			mRotate += degrees;
			mAivImage.setRotation(mRotate);
			LOGD("<<<<<<<<<rotate widget object >>>>>>>>> degree=" + mRotate);
		}
	}

	@Override
	public void setOverlaySelected(boolean selected) {
		mSelected = selected;
	}

	@Override
	public boolean isOverlaySelected() {
		return mSelected;
	}

	@Override
	public void flip() {
		if (mAivImage != null) {
			mAivImage.flip();
		}
	}

	@Override
	public boolean isFlipable() {
		return true;
	}

	
	private static class EraseWidthViewHolder {
		TextView tvWidth;
	}
	
	private static class EraseWidthAdapter extends BaseAdapter {
		
		private int[] mWidths;
		
		private int mCurrentWidth = 0;
		
		private float mDensity = -1.0f;
		
		public EraseWidthAdapter(int[] widths) {
			this.mWidths = widths;
			mCurrentWidth = widths[0];
		}

		@Override
		public int getCount() {
			return mWidths.length;
		}

		@Override
		public Object getItem(int position) {
			return mWidths[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			EraseWidthViewHolder viewHolder = new EraseWidthViewHolder();
			if(convertView == null) {
				convertView = View.inflate(UglyPicApp.getAppExContext(), 
						R.layout.image_erase_width_panel_item, null);
				viewHolder.tvWidth = (TextView) convertView.findViewById(R.id.image_erase_width);
				convertView.setTag(viewHolder);
			}
			viewHolder = (EraseWidthViewHolder) convertView.getTag();
			
			if(mDensity < 0) {
				WindowManager wm = (WindowManager) UglyPicApp.getAppExContext().getSystemService(Context.WINDOW_SERVICE);
				DisplayMetrics dm = new DisplayMetrics();
				wm.getDefaultDisplay().getMetrics(dm);
				mDensity = dm.density;
			}
			
			int width = mWidths[position];
			viewHolder.tvWidth.setText("" + width);
			float density = mDensity < 0 ? 1.0f : mDensity;
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) viewHolder.tvWidth.getLayoutParams();
			params.width = (int) (width * density);
			params.height = (int) (width * density);
			if(width == mCurrentWidth) {
				viewHolder.tvWidth.setSelected(true);
			} else {
				viewHolder.tvWidth.setSelected(false);
			}
		
			return convertView;
		}
		
	}
	
	/**
	 * 擦除照片的View
	 * 
	 * @author wsf
	 * 
	 */
	public class EraseImageEditorView extends RelativeLayout implements IEditor {
		
		private EraseWidthAdapter mAdapter;

		public EraseImageEditorView(Context context) {
			super(context);
			mAdapter = new EraseWidthAdapter(ERASER_WIDTH);
			initView();
		}

		private void initView() {
			GridView gv = new GridView(getContext());
			gv.setBackgroundColor(getResources().getColor(R.color.default_background));
			int height = (int) getResources().getDimension(R.dimen.photo_editor_bottom_bar_height);
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, height);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			gv.setAdapter(mAdapter);
			addView(gv, params);
			gv.setNumColumns(ERASER_WIDTH.length);
			gv.setDuplicateParentStateEnabled(true);
			gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					mAivImage.setCurrentStrokeWidth(ERASER_WIDTH[position]);
					mAdapter.mCurrentWidth = ERASER_WIDTH[position];
					mAdapter.notifyDataSetChanged();
				}
			});
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
			// 显示上下文菜单
			if (mContextView != null) {
				mContextView.setVisibility(View.VISIBLE);
			}
		}

		@Override
		public void onRegret() {
			if (mAivImage.hasMoreRegret()) {
				mAivImage.regret();
			}
		}

		@Override
		public void onRedo() {
			if (mAivImage.hasMoreRedo()) {
				mAivImage.redo();
			}
		}

		@Override
		public void onFinish() {
			// 传回擦除的结果
			mAivImage.saveEraserResult();
			mAivImage.setEraserMode(false);
			disableEraseMode();
			getEditorContainerView().setEditorView(null);
			getEditorContainerView().setVisibility(View.GONE);
			// 显示上下文菜单
			if (mContextView != null) {
				mContextView.setVisibility(View.VISIBLE);
			}
		}

		@Override
		public boolean hasMoreRegret() {
			return mAivImage.hasMoreRegret();
		}

		@Override
		public boolean hasMoreRedo() {
			return mAivImage.hasMoreRedo();
		}

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

		// private static final int CONTROL_POINTS_RADIS = 20;
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
		private int mPathIndex = -1;

		// 附加操作，不包含亮度、对比度、饱和度
		private List<IImageOperation> mOperations = new LinkedList<IImageOperation>();

		private Bitmap mImage = null;
		private Bitmap mEraseableImage = null;
		private Canvas mEraseableCanvas = null;

		private ColorMatrix mColorMatrix = new ColorMatrix();
		private ColorMatrix mSatuationMatrix = new ColorMatrix();

		private float mIllumination;
		private float mSatuation;
		private float mContrast;

		public CopyOnWriteImageView(Context context) {
			super(context);
			WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics dm = new DisplayMetrics();
			wm.getDefaultDisplay().getMetrics(dm);
			mDensity = dm.density;
			// int padding = (int) (CONTROL_POINTS_RADIS * mDensity);
			// setPadding(padding, padding, padding, padding);

			setIllumination(IlluminationImageOperation.DEFAULT);
			setSatuation(SatuationImageOperation.DEFAULT);
			setContrast(ContrastImageOperation.DEFAULT);

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
				mEraseableCanvas = null;
				mEraseableImage = mImage.copy(Config.ARGB_8888, true);
			} else {
				mRecords.clear();
				if (mEraseableImage != null && !mEraseableImage.isRecycled()) {
					mEraseableImage.recycle();
					mEraseableImage = null;
				}
				mEraseableCanvas = null;
			}
			invalidate();
		}

		private void reset() {
			mEraserMode = false;
			mSatuationMatrix.reset();
			if (mImage != null && !mImage.isRecycled()) {
				setImageDrawable(null);
				setImageInfo(ImageInfo.obtain(mUri.toString()));
				mImage.recycle();
				mImage = null;
			} else {
				setImageInfo(ImageInfo.obtain(mUri.toString()));
			}
			invalidate();
			setSatuation(IlluminationImageOperation.DEFAULT);
			setContrast(SatuationImageOperation.DEFAULT);
			setIllumination(ContrastImageOperation.DEFAULT);
		}

		private void setContrast(int contrast) {
			copyImage();
			// 参考了http://blog.csdn.net/pizi0475/article/details/6740428
			// 首先调整contrast到[-1,1]
			float c = (contrast * 1.0f - ContrastImageOperation.DEFAULT) / ContrastImageOperation.DEFAULT;
			mContrast = (float) Math.tan((45 + 44 * c) / 180 * Math.PI);
			LOGD(">>>>>>>> color params >>>>>>>> c=" + c + ", contrast=" + mContrast);
			invalidate();
		}

		private void setSatuation(int satuation) {
			copyImage();
			mSatuation = satuation * 1.0F / SatuationImageOperation.DEFAULT;
			invalidate();
		}

		private void setIllumination(int illumination) {
			copyImage();
			// 参考了http://blog.csdn.net/pizi0475/article/details/6740428
			// 调整亮度参数到 [-1, 1]区间
			mIllumination = (illumination - IlluminationImageOperation.DEFAULT)
					/ (1.0f * IlluminationImageOperation.DEFAULT);
			invalidate();
		}

		private void flip() {
			copyImage();
			int width = mImage.getWidth();
			int height = mImage.getHeight();
			int middle = width / 2;
			int[] pixels = new int[mImage.getWidth() * mImage.getHeight()];
			mImage.getPixels(pixels, 0, width, 0, 0, width, height);
			// TODO: 可以在JNI实现
			for (int i = 0; i < height; i++) {
				int start = i * width;
				for (int j = 0; j < middle; j++) {
					int temp = pixels[start + j];
					pixels[start + j] = pixels[start + width - j - 1];
					pixels[start + width - j - 1] = temp;
				}
			}
			mImage.setPixels(pixels, 0, width, 0, 0, width, height);
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

			if (mEraseableImage != null && !mEraseableImage.isRecycled()) {
				mEraseableImage.recycle();
				mEraseableImage = null;
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
			return (mRecords.size() > 0 && mPathIndex >= 0 && mPathIndex < mRecords.size());
		}

		private boolean hasMoreRedo() {
			return (mRecords.size() > 0 && mPathIndex + 1 >= 0 && mPathIndex + 1 < mRecords.size());
		}

		private void setCurrentStrokeWidth(int strokeWidth) {
			mCurrentStrokeWidth = strokeWidth;
		}

		private void regret() {
			if (mPathIndex >= 0) {
				mPathIndex = mPathIndex - 1;
			} else {
				mRecords.clear();
			}
			if (mEraseableImage != null && !mEraseableImage.isRecycled()) {
				mEraseableImage.recycle();
			}
			mEraseableImage = mImage.copy(Config.ARGB_8888, true);
			mEraseableCanvas = null;
			drawOnErasableImage();

			invalidate();
		}

		private void redo() {
			mPathIndex = mPathIndex < (mRecords.size() - 1) ? (mPathIndex + 1) : mPathIndex;
			drawOnErasableImage();
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
				if(mPathIndex >= 0 && mPathIndex < mRecords.size()) {
					mRecords = mRecords.subList(0, mPathIndex + 1);
				} else {
					mRecords.clear();
				}
				mRecords.add(new EraseRecord(mCurrentPath, mCurrentStrokeWidth));
				mPathIndex = mRecords.size() - 1;
				//通知容器，可回退状态已改变
				MessageCenter.getInstance(getContext())
						.notifyHandlers(R.id.msg_editor_regret_status_change, 0, 0, null);
				mCurrentPath = null;
			}
		}

		private void saveEraserResult() {
			if (mImage == null) {
				return;
			}
			Canvas canvas = new Canvas(mImage);
			if (mPathIndex < mRecords.size() - 1 && mPathIndex >= 0) {
				mRecords = mRecords.subList(0, mPathIndex + 1);
			}
			// int padding = (int) (CONTROL_POINTS_RADIS * mDensity);
			int saveCount = canvas.getSaveCount();
			canvas.save();
			android.graphics.Matrix matrix = new Matrix();
			getImageMatrix().invert(matrix);
			canvas.concat(matrix);
			// canvas.translate(-padding, -padding);
			for (EraseRecord record : mRecords) {
				mEraserPaint.setStrokeWidth(record.strokeWidth);
				canvas.drawPath(record.path, mEraserPaint);
			}
			canvas.restoreToCount(saveCount);
		}

		private void drawOnErasableImage() {
			if (mEraseableImage == null) {
				return;
			}

			if (mEraseableCanvas == null) {
				mEraseableCanvas = new Canvas(mEraseableImage);
			}
			
			List<EraseRecord> records = new ArrayList<ImageWidgetOverlay.EraseRecord>();
			if (mPathIndex < mRecords.size() && mPathIndex >= 0) {
				records = mRecords.subList(0, mPathIndex + 1);
			}
			// int padding = (int) (CONTROL_POINTS_RADIS * mDensity);
			int saveCount = mEraseableCanvas.getSaveCount();
			mEraseableCanvas.save();
			android.graphics.Matrix matrix = new Matrix();
			getImageMatrix().invert(matrix);
			mEraseableCanvas.concat(matrix);
			// mEraseableCanvas.translate(-padding, -padding);
			for (EraseRecord record : records) {
				mEraserPaint.setStrokeWidth(record.strokeWidth);
				mEraseableCanvas.drawPath(record.path, mEraserPaint);
			}
			if (mCurrentPath != null) {
				mEraserPaint.setStrokeWidth(mCurrentStrokeWidth);
				mEraseableCanvas.drawPath(mCurrentPath, mEraserPaint);
			}
			mEraseableCanvas.restoreToCount(saveCount);
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
				drawOnErasableImage();
				invalidate();
				break;
			case MotionEvent.ACTION_UP:
				touchUp(x, y);
				drawOnErasableImage();
				invalidate();
				break;
			case MotionEvent.ACTION_MOVE:
				// 判断移动的距离是否已经超过了门限
				touchMove(x, y);
				drawOnErasableImage();
				invalidate();
				break;
			}
			return true;
		}

		@Override
		protected void onDraw(Canvas canvas) {
			// 先调整亮度、对比度、饱和度
			// 参考了http://blog.csdn.net/pizi0475/article/details/6740428
			// 通过对比度和亮度计算一个参数
			// y = [x - 127.5 * (1 - B)] * k + 127.5 * (1 + B);

			float factor = mContrast;
			float addon = -127.5f * (1 - mIllumination) * mContrast + 127.5f * (1 + mIllumination);
			// 叠加饱和度，参照了setSatuation源码

			// m[0] = R + sat; m[1≤] = G; m[2] = B;
			// m[5] = R; m[6] = G + sat; m[7] = B;
			// m[10] = R; m[11] = G; m[12] = B + sat;
			mSatuationMatrix.setSaturation(mSatuation);
			mColorMatrix.set(new float[] { factor, 0, 0, 0, addon, // 1
					0, factor, 0, 0, addon, // 1
					0, 0, factor, 0, addon, // 2
					0, 0, 0, 1, 0 }); // 4
			mColorMatrix.postConcat(mSatuationMatrix);

			// int padding = (int) (CONTROL_POINTS_RADIS * mDensity);
			if (mPaint == null) {
				mPaint = new Paint();
			}

			Bitmap image = mEraserMode ? mEraseableImage : mImage;

			if (image != null) {
				LOGD("<<<<<<<<< draw with color matrix >>>>>>>>>>>>");
				int saveCount = canvas.getSaveCount();
				canvas.save();
				// canvas.translate(padding, padding);
				canvas.concat(getImageMatrix());
				ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(mColorMatrix);
				mPaint.setColorFilter(colorFilter);
				canvas.drawBitmap(image, 0, 0, mPaint);
				canvas.restoreToCount(saveCount);
			} else {
				super.onDraw(canvas);
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
		}
		return mContextView;
	}

	// 转换到擦除模式的参数（对当前View放大缩小的参数）
	private static class EraseModeParams {
		float scaleX = 1.0f;
		float scaleY = 1.0f;
		float tx = 0.0f;
		float ty = 0.0f;
		float pivotX = 0.0f;
		float pivotY = 0.0f;
	}

	// 获取切换到擦除模式的参数
	private EraseModeParams getEraseModeParams() {
		EraseModeParams params = new EraseModeParams();
		// 首先找到当前overlay所处的方形区域
		PointF leftTop = getDeletePoint();
		PointF rightTop = getFlipButton();
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
			// 计算重心
			params.pivotX = (width * l) / (width + l - r);
			params.pivotY = (height * t) / (height + t - b);
		}
		return params;
	}

	@Override
	public void onErased(ImageOverlayContextView view) {

	}

	public void startErase() {
		if (getEditorContainerView().getVisibility() != View.VISIBLE) {
			EraseImageEditorView eraseImageEditorView = new EraseImageEditorView(mContext);
			getEditorContainerView().setEditorView(eraseImageEditorView);
			getEditorContainerView().setVisibility(View.VISIBLE);
			mAivImage.setEraserMode(true);
			enableEraseMode();
			if (mContextView != null) {
				mContextView.setVisibility(View.GONE);
			}
		}
	}

	public void reset() {
		mAivImage.reset();
		if (mContextView != null) {
			mContextView.reset();
		}
	}

	private void enableEraseMode() {
		EraseModeParams params = getEraseModeParams();
		float scale = Math.min(params.scaleX, params.scaleY);
		View editorPanel = getEditorPanel();
		if (editorPanel != null && scale > 1.0f) {
			editorPanel.setPivotX(params.pivotX);
			editorPanel.setPivotY(params.pivotY);
			editorPanel.setScaleX(scale);
			editorPanel.setScaleY(scale);
		}
		setOverlaySelected(false);
		View container = getContainerView(mContext);
		if (container != null) {
			container.invalidate();
		}
		mAivImage.setAlpha(0.7f);
	}

	private void disableEraseMode() {
		View editorPanel = getEditorPanel();
		if (editorPanel != null) {
			editorPanel.scrollTo(0, 0);
			editorPanel.setScaleX(1);
			editorPanel.setScaleY(1);
		}
		setOverlaySelected(true);
		View container = getContainerView(mContext);
		if (container != null) {
			container.invalidate();
		}
		mAivImage.setAlpha(1.0f);
	}

	@Override
	public void onReset(ImageOverlayContextView view) {
		mAivImage.reset();
	}

	@Override
	public void onSatuationChanged(ImageOverlayContextView view, int satuation) {
		mAivImage.setSatuation(satuation);
	}

	@Override
	public void onContrastChanged(ImageOverlayContextView view, int contrast) {
		mAivImage.setContrast(contrast);
		LOGD("setContrast >>>>>> " + contrast);
	}

	@Override
	public void onIlluminationChanged(ImageOverlayContextView view, int illumination) {
		mAivImage.setIllumination(illumination);
	}
}
