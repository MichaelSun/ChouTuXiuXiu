/**
 * ImageInfo.java
 */
package com.canruoxingchen.uglypic.cache;

import android.text.TextUtils;

/**
 * 缓存中保存的图片信息
 * 
 */
public final class ImageInfo {

	private static final String DEFAULT_CATEGORY = ImageCategories.CATEGORY_DEFAULT;

	private static final String DEFAULT_IMAGE_URL = "default_image_url";

	private String category = DEFAULT_CATEGORY;

	private String url = DEFAULT_IMAGE_URL;
	
	
	private ImageInfo() {

	}
	
	

	public String getCategory() {
		return category;
	}

	public String getUrl() {
		return url;
	}

	
	public static ImageInfo obtain(String category,
			String url) {
		ImageInfo info = new ImageInfo();
		info.category = TextUtils.isEmpty(category) ? DEFAULT_CATEGORY
				: category;
		info.url = TextUtils.isEmpty(url) ? DEFAULT_IMAGE_URL : url;
		return info;
	}

	public static ImageInfo obtain(String url) {
		return obtain(null, url);
	}

	@Override
	public boolean equals(Object o) {
		
		if (o != null && o instanceof ImageInfo) {
			ImageInfo info = (ImageInfo) o;
			return url.equals(info.url) && category.equals(info.category);
		}
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		return url.hashCode() + category.hashCode();
	}

	@Override
	public String toString() {
		return "ImageInfo [category=" + category + ", url=" + url + "]";
	}
}
