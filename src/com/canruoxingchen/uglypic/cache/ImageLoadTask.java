package com.canruoxingchen.uglypic.cache;

import java.io.InputStream;
import java.lang.ref.WeakReference;

import com.canruoxingchen.uglypic.R;
import com.canruoxingchen.uglypic.util.ImageUtils;
import com.canruoxingchen.uglypic.util.Logger;

import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.view.ViewGroup;

/**
 * 
 * @Description 向{@link AsyncImageView}加载图片
 * 
 * @author Shaofeng Wang
 * 
 * @time 2013-11-1 下午4:41:30
 * 
 */
public class ImageLoadTask extends
		AsyncTask<Void, Void, CacheableBitmapDrawable> {
	public static final String TAG = "ImageLoadTask";

	/**
	 * 图片URI
	 */
	private String mImageUri = "";

	/**
	 * 图片类型（用来从缓存中取图片）
	 */
	private String mCategory = "";

	/**
	 * 目标宽度
	 */
	private int mDestWidth = 0;

	/**
	 * 目标高度
	 */
	private int mDestHeight = 0;

	/**
	 * 是否需要对图片进行重新采样
	 */
	private boolean mNeedReSample = true;

	private final ImageCacheManager mImageCacheManager;

	private WeakReference<AsyncImageView> mImageView = null;

	private WeakReference<ContentResolver> mContentResolver = null;

	private ViewGroup.LayoutParams mPreviousParams;
	private int mPreviousWidth;
	private int mPreviousHeight;
	private static final int MIN_DEST_WIDTH = 512;
	private static final int MIN_DEST_HEIGHT = 512;

	public ImageLoadTask(Context context, ImageInfo imageInfo,
			AsyncImageView imgView, boolean needReSample) {
		this.mCategory = imageInfo.getCategory();
		this.mImageUri = imageInfo.getUrl();
		this.mContentResolver = new WeakReference<ContentResolver>(imgView
				.getContext().getContentResolver());
		this.mImageView = new WeakReference<AsyncImageView>(imgView);
		this.mImageCacheManager = ImageCacheManager.getInstance(context);

		this.mNeedReSample = needReSample;
		this.mPreviousParams = imgView.getLayoutParams();
		if (mPreviousParams != null) {
			this.mPreviousWidth = mPreviousParams.width;
			this.mPreviousHeight = mPreviousParams.height;
		}

	}

	@Override
	protected CacheableBitmapDrawable doInBackground(Void... params) {

		// Return early if the ImageView has disappeared.
		AsyncImageView imageView = mImageView.get();
		if (null == imageView || isCancelled()) {
			return null;
		}

		Options opts = new Options();
		opts.inSampleSize = 1;

		if (mNeedReSample) {
			this.mDestHeight = imageView.getDestHeightPixel();
			this.mDestWidth = imageView.getDestWidthPixel();

			if (mDestHeight < MIN_DEST_HEIGHT) {
				mDestHeight = MIN_DEST_HEIGHT;
			}

			if (mDestWidth < MIN_DEST_WIDTH) {
				mDestWidth = MIN_DEST_WIDTH;
			}

			if (mImageUri != null && !mImageUri.startsWith("http")) {
				ContentResolver resolver = mContentResolver.get();
				opts = ImageUtils.getDecodeOptions(resolver,
						Uri.parse(mImageUri), mDestWidth, mDestHeight);
			} else {// 如果本地没有，则设置一个默认的opts
				opts.inDensity = DisplayMetrics.DENSITY_XHIGH;
			}
		}

		if (isCancelled()) {
			LOGD("Downloading Cancelled 1 ...");
			return null;
		}

		CacheableBitmapDrawable result = mImageCacheManager
				.getBitmapByCategoryAndUrl(mCategory, mImageUri, opts,
						imageView.getSuffix());
		if (result != null) {
			result = getAndCreateAndPutSpecialBitmap(imageView, result);
			if (result != null)
				return result;
		}

		if (isCancelled()) {
			return null;
		}

		// 如果给的Url为本地uri，则先从本地加载
		if (mImageUri != null && !mImageUri.startsWith("http")) {
			ContentResolver resolver = mContentResolver.get();
			Bitmap bmp = ImageUtils.scaleDecode(resolver, Uri.parse(mImageUri),
					mDestWidth, mDestHeight, 1);
			if (bmp != null) {
				// 如果为本地图片，则只加载到内存缓存中，否则将该图片存入缓存中
				result = mImageCacheManager.putToMem(bmp, mCategory, mImageUri);
				if (result != null) {
					result = getAndCreateAndPutSpecialBitmap(imageView, result);
					return result;
				}
			}
		}

		// Sleep 200 毫秒后再开始下载，来增加Fling情况下被cancel的几率
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {

		}

		if (isCancelled()) {
			LOGD("Downloading Cancelled 2 ...");
			return null;
		}

		if (mImageUri != null && mImageUri.startsWith("http")) {
			InputStream is = ImageDownloader.open(mImageUri);
			if (is != null) {
				result = mImageCacheManager.putBitmapByCategoryAndUrl(is, opts,
						mCategory, mImageUri);
				if (result != null) {
					result = getAndCreateAndPutSpecialBitmap(imageView, result);
				}
			}
		}
		if (result == null) {
			Logger.i("这个url取不到图片：" + mImageUri);
		}
		return result;
	}

	private CacheableBitmapDrawable getAndCreateAndPutSpecialBitmap(
			AsyncImageView imageView, CacheableBitmapDrawable wrapper) {
		Bitmap specialBitmap = null;
		if (wrapper != null && wrapper.hasValidBitmap()) {
			specialBitmap = imageView.createSpecialBitmap(wrapper.getBitmap());
			if (imageView.getSuffix() == null
					|| imageView.getSuffix().equalsIgnoreCase(
							ImageCategories.NULL_SUFFIX)) {
				return wrapper;
			} else {
				if (specialBitmap != null) {
					return this.mImageCacheManager.putBitmapByCategoryAndUrl(
							specialBitmap, mCategory, mImageUri,
							imageView.getSuffix());
				} else {
					return null;
				}
			}
		} else {
			return null;
		}

	}

	@Override
	protected void onPostExecute(CacheableBitmapDrawable result) {
		super.onPostExecute(result);

		AsyncImageView iv = (mImageView == null ? null : mImageView.get());
		if (isCancelled()) {
			if(iv != null) {
				iv.notifyFailure();
			}
			return;
		}

		if (result != null) {
			if (iv != null && iv.mImgInfo != null) {
				ImageInfo info = ImageInfo.obtain(mCategory, mImageUri);
				// 判断是否为当前所需的图片，防止前一张没下载完就更新了所需的图片
				Drawable anim = iv.getDrawable();
				if (anim != null && anim instanceof AnimationDrawable) {
					((AnimationDrawable) anim).stop();
				}
				// iv.setImageDrawable(null);
				if (info.equals(iv.mImgInfo)) {
					if (mPreviousParams != null) {
						mPreviousParams.width = mPreviousWidth;
						mPreviousParams.height = mPreviousHeight;
						iv.setLayoutParams(mPreviousParams);
					}
					iv.setImageDrawable(result);
				}
				
				int imageWidth = result.getBitmap() == null ? 0 : result.getBitmap().getWidth();
				int imageHeight = result.getBitmap() == null ? 0 : result.getBitmap().getHeight();
				iv.setImageWidth(imageWidth);
				iv.setImageHeight(imageHeight);
				if(iv != null) {
					iv.notifyComplete();
				}
				return;
			}
		}
		iv.notifyFailure();
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
		restoreLayoutParams();
	}

	@SuppressLint("NewApi")
	@Override
	protected void onCancelled(CacheableBitmapDrawable result) {
		super.onCancelled(result);
		restoreLayoutParams();
	}

	private void restoreLayoutParams() {
		AsyncImageView iv = (mImageView == null ? null : mImageView.get());
		if (iv != null && mPreviousParams != null) {
			mPreviousParams.width = mPreviousWidth;
			mPreviousParams.height = mPreviousHeight;
			iv.setLayoutParams(mPreviousParams);
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		AsyncImageView iv = (mImageView == null ? null : mImageView.get());
		if (iv != null && iv.mNeedProgressDrawable) {
			if (mPreviousParams != null) {
				mPreviousParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
				mPreviousParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
				iv.setLayoutParams(mPreviousParams);
			}

			Drawable drawable = iv.getDrawable();
			iv.setImageResource(R.drawable.big_loading_anim_icon);
			drawable = iv.getDrawable();
			if (drawable instanceof AnimationDrawable) {
				((AnimationDrawable) drawable).start();
			}
		}
	}

	public void LOGD(String str) {
		Logger.d(TAG, str);
	}
}