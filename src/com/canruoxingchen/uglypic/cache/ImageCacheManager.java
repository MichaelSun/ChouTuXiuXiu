/**
 * ImageCacheManager.java
 */
package com.canruoxingchen.uglypic.cache;

import java.io.InputStream;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.canruoxingchen.uglypic.UglyPicApp;
import com.canruoxingchen.uglypic.util.Logger;

/**
 * 
 * 图片缓存
 * 
 * @author wsf
 * 
 *         2013-3-26
 */
public class ImageCacheManager {

	private static final String TAG = "ImageCacheManager";

	private static volatile ImageCacheManager sInstance;

	private BitmapLruCache mCache;

	private static byte[] sLockObj = new byte[0];

	private ImageCacheManager(Context context) {
		mCache = UglyPicApp.getApplication(context).getBitmapCache();
	}

	public static ImageCacheManager getInstance(Context context) {
		if (sInstance == null) {
			synchronized (sLockObj) {
				if (sInstance == null) {
					sInstance = new ImageCacheManager(
							context.getApplicationContext());
				}
			}
		}
		return sInstance;
	}

	/**
	 * 通过类型以及Url来获取CacheableBitmapDrawable
	 * 
	 * @param category
	 * @param subCategory
	 * @param url
	 * @return
	 */
	public CacheableBitmapDrawable getBitmapByCategoryAndUrl(String category,
			String url, String suffix) {
		if (mCache != null && url != null) {
			return mCache.get(encodeUrlWithCategory(url, category, suffix));
		}
		return null;
	}

	public CacheableBitmapDrawable getBitmapByCategoryAndUrl(String category,
			String url) {
		return getBitmapByCategoryAndUrl(category, url,
				ImageCategories.NULL_SUFFIX);
	}

	/**
	 * 
	 * 通过类型以及Url来获取CacheableBitmapDrawable,可指定需要的图片大小
	 * 
	 * @param category
	 * @param subCategory
	 * @param url
	 * @param decodeOpts
	 * @return
	 */
	public CacheableBitmapDrawable getBitmapByCategoryAndUrl(String category,
			String url, BitmapFactory.Options decodeOpts, String suffix) {
		if (mCache != null && url != null) {
			return mCache.get(encodeUrlWithCategory(url, category, suffix),
					decodeOpts);

		}
		return null;
	}

	public CacheableBitmapDrawable getBitmapByCategoryAndUrl(String category,
			String url, BitmapFactory.Options decodeOpts) {
		return getBitmapByCategoryAndUrl(category, url, decodeOpts,
				ImageCategories.NULL_SUFFIX);
	}

	/**
	 * 通过类型以及Url来从内存获取CacheableBitmapDrawable
	 * 
	 * @param category
	 * @param subCategory
	 * @param url
	 * @return
	 */
	public CacheableBitmapDrawable getMemBitmapByCategoryAndUrl(
			String category, String url, String suffix) {
		if (mCache != null && url != null) {
			return mCache.getFromMemoryCache(encodeUrlWithCategory(url,
					category, suffix));
		}
		return null;
	}

	public CacheableBitmapDrawable getMemBitmapByCategoryAndUrl(
			String category, String url) {
		return getMemBitmapByCategoryAndUrl(category, url,
				ImageCategories.NULL_SUFFIX);
	}

	/**
	 * 将一幅图片加入缓存
	 * 
	 * @param bitmap
	 * @param category
	 * @param subCategory
	 * @param url
	 */
	public CacheableBitmapDrawable putBitmapByCategoryAndUrl(Bitmap bitmap,
			String category, String url, String suffix) {
		if (!TextUtils.isEmpty(suffix)) {
			return mCache.putOnlyToMem(
					encodeUrlWithCategory(url, category, suffix), bitmap);
		} else {
			return mCache.put(encodeUrlWithCategory(url, category, suffix),
					bitmap);
		}
	}

	public CacheableBitmapDrawable putBitmapByCategoryAndUrl(Bitmap bitmap,
			String category, String url) {
		return mCache.put(encodeUrlWithCategory(url, category), bitmap);
	}

	public CacheableBitmapDrawable putToMem(Bitmap bitmap, String category,
			String url) {
		return mCache
				.putOnlyToMem(encodeUrlWithCategory(url, category), bitmap);
	}

	/**
	 * 
	 * 将一幅图片加入缓存
	 * 
	 * @param inputStream
	 * @param category
	 * @param subCategory
	 * @param url
	 */
	public CacheableBitmapDrawable putBitmapByCategoryAndUrl(
			final InputStream inputStream, String category, String url,
			String suffix) {
		return mCache.put(encodeUrlWithCategory(url, category, suffix),
				inputStream);
	}

	public CacheableBitmapDrawable putBitmapByCategoryAndUrl(
			final InputStream inputStream, String category, String url) {
		return mCache.put(encodeUrlWithCategory(url, category), inputStream);
	}

	/**
	 * 
	 * 将一幅图片加入缓存
	 * 
	 * @param inputStream
	 * @param decodeOpts
	 * @param category
	 * @param subCategory
	 * @param url
	 */
	public CacheableBitmapDrawable putBitmapByCategoryAndUrl(
			final InputStream inputStream,
			final BitmapFactory.Options decodeOpts, String category,
			String url, String suffix) {
		return mCache.put(encodeUrlWithCategory(url, category, suffix),
				inputStream, decodeOpts);
	}

	public CacheableBitmapDrawable putBitmapByCategoryAndUrl(
			final InputStream inputStream,
			final BitmapFactory.Options decodeOpts, String category, String url) {
		return mCache.put(encodeUrlWithCategory(url, category), inputStream,
				decodeOpts);
	}

	private String encodeUrlWithCategory(String url, String category) {
		if (url == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder(url);

		if (category != null) {
			sb.append("-" + category);
		}

		return sb.toString();
	}

	private String encodeUrlWithCategory(String url, String category,
			String suffix) {
		String result = encodeUrlWithCategory(url, category);
		if (suffix == null
				|| suffix.equalsIgnoreCase(ImageCategories.NULL_SUFFIX)) {
			return result;
		} else {
			result = result + "_" + suffix;
			return (result);
		}
	}

	@SuppressWarnings("unused")
	private void LOGD(String logMe) {
		Logger.d(TAG, logMe);
	}
}
