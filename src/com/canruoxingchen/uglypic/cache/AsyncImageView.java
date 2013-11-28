/**
 * AsyncImageView.java
 */
package com.canruoxingchen.uglypic.cache;

import java.lang.ref.WeakReference;
import java.util.concurrent.RejectedExecutionException;

import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import uk.co.senab.bitmapcache.CacheableImageView;
import uk.co.senab.photoview.SDK11;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.view.WindowManager;

import com.canruoxingchen.uglypic.UglyPicApp;
import com.canruoxingchen.uglypic.util.Logger;

/**
 * 异步ImageView，支持从Url下载图片;<br>
 * 注意： setTag() 和 getTag() 方法已经被本类内部使用了，为了避免错误的发生，调用者可以 使用 setData() 和 getData()
 * 来完成Tag的机制。<br>
 * <br>
 * <br>
 * 这个类实现的是异步加载图片，加载回来的图片是没有经过加工的。如果想获取比较特殊的ImageView，比如圆角图片，可以
 * 继承这个类来自定义View，同时重写一些method：<br>
 * 重写 {@link #createSpecialBitmap(Bitmap)} 来对原来的图片进行加工，然后返回加工过的bitmap ;<br>
 * 重写 {@link #getSuffix()} 来添加特殊后缀，比如 "round_angle";<br>
 * 重写 {@link #getShouldSavedToDisk()} 来告诉外界 当前下载的图片是否想要保存到 Disk 的缓存中;<br>
 * 重写 {@link #getShouldSavedToMem()} 来告诉外界 当前下载的图片是否想要保存到 内存的缓存中 ； <br>
 * 
 * @author wsf
 * 
 */
public class AsyncImageView extends CacheableImageView {

	private static final String TAG = "AsyncImageView";
	private static final int MIN_WIDTH_HEIGHT = 400;

	protected ImageInfo mImgInfo;

	private int mWidthMeasureMode = -1;
	private int mHeightMeasureMode = -1;
	private int mWidthMeasureSize = -1;
	private int mHeightMeasureSize = -1;

	private float mMaxWidthPercent = 1.0f;
	private float mMaxHeightPercent = 1.0f;

	private Object mData = new Object();
	
	private int mImageWidth = 0;
	private int mImageHeight = 0;

	/**
	 * 是否需要展示默认的动画
	 */
	boolean mNeedProgressDrawable = true;

	public AsyncImageView(Context context) {
		super(context);
	}

	public AsyncImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AsyncImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
	}
	
	//获得图片的宽度
	public int getImageWidth() {
		return mImageWidth;
	}
	
	//获得图片高度
	public int getImageHeight() {
		return mImageHeight;
	}
	
	//设置图片高度
	public void setImageWidth(int imageWidth) {
		this.mImageWidth = imageWidth;
	}
	
	//设置图片高度
	public void setImageHeight(int imageHeight) {
		this.mImageHeight = imageHeight;
	}

	// 设置图片信息,默认压缩图片
	public void setImageInfo(ImageInfo info) {
		setImageInfo(info, false, true);
	}

	// 设置图片信息
	public void setImageInfo(ImageInfo info, boolean needReSample) {
		setImageInfo(info, false, needReSample);
	}

	// 设置图片信息，并设置是否需要加载动画，默认不需要
	public final void setImageInfo(ImageInfo info, boolean needProgressAnim,
			boolean needReSample) {
		// 先取消之前的下载操作
		@SuppressWarnings("unchecked")
		WeakReference<ImageLoadTask> lastTask = (WeakReference<ImageLoadTask>) getTag();
		if (lastTask != null) {
			ImageLoadTask lastImageLoadTask = lastTask.get();
			if (lastImageLoadTask != null) {
				lastImageLoadTask.cancel(true);
				changeImageInfo(null);
			}
		}

		mNeedProgressDrawable = needProgressAnim;
		// if (mImgInfo == null || !mImgInfo.equals(info)) {
		if (info != null) {
			CacheableBitmapDrawable wrapper = ImageCacheManager.getInstance(
					getContext()).getMemBitmapByCategoryAndUrl(
					info.getCategory(), info.getUrl(),
					getSuffix());

			if (wrapper != null) {
				setImageDrawable(wrapper);
				return;
			}

//			if (!info.equals(mImgInfo)) {
				changeImageInfo(info);
				setImageDrawable(null);
				ImageLoadTask task = new ImageLoadTask(
						UglyPicApp.getAppExContext(), info, this, needReSample);
				try {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						// android系统在 API11 以后，对于AsyncTask就不再用线程池执行了，而是单独一个线程去做，
						// 如果想实现多任务并发执行，就调task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR
						// , params...)
						SDK11.executeOnThreadPool(task);
					} else {
						// 在API 1.6-3.0(10)之间，AsyncTask都是多线程并发执行的
						task.execute();

						// 串行执行:
						// task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
						// params..);
					}
				} catch (RejectedExecutionException e) {
					// This shouldn't happen, but might.
				}
				setTag(new WeakReference<ImageLoadTask>(task));
//			}

		} else {
			changeImageInfo(null);
			setImageDrawable(null);

		}
		// }
	}

	@SuppressWarnings("unused")
	private static void LOGD(String logMe) {
		Logger.d(TAG, logMe);
	}

	private void changeImageInfo(ImageInfo info) {
		this.mImgInfo = info;
	}

	/**
	 * 
	 * 图片的目标宽度，用于计算 inSimpleSize 的值
	 * 
	 * @return
	 */
	public int getDestWidthPixel() {
		if (getWidth() > 0) {
			return Math.max(getWidth(), MIN_WIDTH_HEIGHT);
		}
		WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		int displayWidth = wm.getDefaultDisplay().getWidth();
		switch (mWidthMeasureMode) {
		case MeasureSpec.AT_MOST:
			return Math.max(MIN_WIDTH_HEIGHT,
					Math.min(displayWidth, mWidthMeasureSize));
		case MeasureSpec.EXACTLY:
			return Math.max(MIN_WIDTH_HEIGHT, mWidthMeasureSize);
		case MeasureSpec.UNSPECIFIED:
			return Math.max(MIN_WIDTH_HEIGHT,
					(int) (displayWidth * mMaxWidthPercent));
		default:
			return Math.max(displayWidth, MIN_WIDTH_HEIGHT);
		}
	}

	/**
	 * 
	 * 图片的目标高度，用于计算 inSimpleSize 的值
	 * 
	 * @return
	 */
	public int getDestHeightPixel() {
		if (getHeight() > 0) {
			return Math.min(MIN_WIDTH_HEIGHT, getHeight());
		}
		WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		int displayHeight = wm.getDefaultDisplay().getHeight();
		switch (mHeightMeasureMode) {
		case MeasureSpec.AT_MOST:
			return Math.max(200, Math.min(displayHeight, mHeightMeasureSize));
		case MeasureSpec.EXACTLY:
			return Math.max(MIN_WIDTH_HEIGHT, mHeightMeasureSize);
		case MeasureSpec.UNSPECIFIED:
			return Math.max(MIN_WIDTH_HEIGHT,
					(int) (displayHeight * mMaxHeightPercent));
		default:
			return displayHeight;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		mWidthMeasureMode = MeasureSpec.getMode(widthMeasureSpec);
		mHeightMeasureMode = MeasureSpec.getMode(heightMeasureSpec);
		mWidthMeasureSize = MeasureSpec.getSize(widthMeasureSpec);
		mHeightMeasureSize = MeasureSpec.getSize(heightMeasureSpec);
	}

	/**
	 * 用法类似于setTag()与getTag();<br>
	 * 强烈建议使用getData() 与 setData() 来替换 getTag()与setTag()
	 * */
	public void setData(Object data) {
		this.mData = data;
	}

	/**
	 * 用法类似于setTag()与getTag();<br>
	 * 强烈建议使用getData() 与 setData() 来替换 getTag()与setTag()
	 * */
	public Object getData() {
		return mData;
	}

	/***
	 * 
	 * 生成特殊处理过的Bitmap。<br>
	 * 如果需要特殊处理，比如圆角、圆形..，子类需要自己去实现
	 * 
	 * @param oriBitmap
	 *            原始的bitmap
	 * @return 处理过的bitmap
	 */
	protected Bitmap createSpecialBitmap(Bitmap oriBitmap) {
		return oriBitmap;
	}

	/**
	 * 自定义的后缀，比如圆角的话，可以是 "round_angle" 等 <br>
	 * 子类应该继承他，重写。
	 * 
	 * @return
	 */
	protected String getSuffix() {
		return ImageCategories.NULL_SUFFIX;
	}

	/**
	 * 
	 * 是否应该保存到 Disk cache中。
	 * 
	 * @return
	 */
	protected boolean getShouldSavedToDisk() {
		return true;
	}

	/**
	 * 
	 * 是否 应该保存到内存Cache中
	 * 
	 * @return
	 */
	protected boolean getShouldSavedToMem() {
		return true;
	}

	public static class DebugTag {

	}
}
