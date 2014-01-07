/**
 * ImageUtils.java
 */
package com.canruoxingchen.uglypic.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Shader.TileMode;
import android.net.Uri;
import android.provider.MediaStore.Images;

import com.canruoxingchen.uglypic.Config;
import com.canruoxingchen.uglypic.UglyPicApp;
import com.canruoxingchen.uglypic.cache.ImageDownloader;

/**
 * 提供照片相关工具方法
 * 
 */
public class ImageUtils {

	private static final Map<String, String> sImgMIMETypeDict = new HashMap<String, String>();

	private static final int MAX_SIZE = 400 * 1024;
	private static final long MAX_MEMORY_SIZE = 720 * 1028 * 4;
	private static final long MAX_WIDTH = 2048;
	private static final int MAX_HEIGHT = 2048;

	static {
		sImgMIMETypeDict.put("bmp", "image/bmp");
		sImgMIMETypeDict.put("cod", "image/cis-cod");
		sImgMIMETypeDict.put("gif", "image/gif");
		sImgMIMETypeDict.put("ief", "image/ief");
		sImgMIMETypeDict.put("jpe", "image/jpeg");
		sImgMIMETypeDict.put("jpeg", "image/jpeg");
		sImgMIMETypeDict.put("jpg", "image/jpeg");
		sImgMIMETypeDict.put("jfif", "image/pipeg");
		sImgMIMETypeDict.put("svg", "image/svg+xml");
		sImgMIMETypeDict.put("tif", "image/tiff");
		sImgMIMETypeDict.put("tiff", "image/tiff");
		sImgMIMETypeDict.put("ras", "image/x-cmu-raster");
		sImgMIMETypeDict.put("cmx", "image/x-cmx");
		sImgMIMETypeDict.put("ico", "image/x-icon");
		sImgMIMETypeDict.put("pnm", "image/x-portable-anymap");
		sImgMIMETypeDict.put("pbm", "image/x-portable-bitmap");
		sImgMIMETypeDict.put("pgm", "image/x-portable-graymap");
		sImgMIMETypeDict.put("ppm", "image/x-portable-pixmap");
		sImgMIMETypeDict.put("rgb", "image/x-rgb");
		sImgMIMETypeDict.put("xbm", "image/x-xbitmap");
		sImgMIMETypeDict.put("xpm", "image/x-xpixmap");
		sImgMIMETypeDict.put("xwd", "image/x-xwindowdump");
		sImgMIMETypeDict.put("png", "image/x-png");
	}

	/**
	 * 获取一个可用的Image路径
	 * 
	 * @param name
	 * @return
	 */
	public static File getTempImageFile(String name) {
		String dirPath = Config.IMAGE_PATH;
		File dir = new File(dirPath);
		if (dir.exists() && !dir.isDirectory()) {
			if (!dir.delete()) {
				// return null;
				dir = UglyPicApp.getAppExContext().getCacheDir();
				if (dir != null) {
					return new File(dir.getAbsoluteFile() + "/" + name);
				}
			}
		}
		if (dir != null && !dir.exists()) {
			if (!dir.mkdirs()) {
				// return null;
				dir = UglyPicApp.getAppExContext().getCacheDir();
				if (dir != null) {
					return new File(dir.getAbsoluteFile() + "/" + name);
				}
			}
		}

		return new File(dirPath + name);
	}

	/**
	 * 获取一个可用的缓存发型的路径
	 * 
	 * @param name
	 * @return
	 */
	public static File getCachedImageFile(String name) {
		String dirPath = Config.CACHED_IMAGE_PATH;
		File dir = new File(dirPath);
		if (dir.exists() && !dir.isDirectory()) {
			if (!dir.delete()) {
				// return null;

				dir = UglyPicApp.getAppExContext().getCacheDir();
				if (dir != null) {
					return new File(dir.getAbsoluteFile() + "/" + name);
				}
			}
		}
		if (dir != null && !dir.exists()) {
			if (!dir.mkdirs()) {
				// return null;

				dir = UglyPicApp.getAppExContext().getCacheDir();
				if (dir != null) {
					return new File(dir.getAbsoluteFile() + "/" + name);
				}
			}
		}

		return new File(dirPath + name);
	}

	public static Bitmap decodeAsset(Context context, String assetName) {
		InputStream is = null;
		try {
			is = context.getAssets().open(assetName);
			return BitmapFactory.decodeStream(is);
		} catch (IOException e) {

		} finally {
			FileUtils.closeQuietly(is);
		}

		return null;
	}

	public static Bitmap decode(Context context, Uri uri) {
		return decode(context.getContentResolver(), uri);
	}

	public static Bitmap decode(ContentResolver resolver, Uri uri) {
		return scaleDecode(resolver, uri, 0, 0, 1);
	}

	public static Bitmap decode(ContentResolver resolver, Uri uri, Options opts) {

		if (uri == null || resolver == null) {
			return null;
		}

		InputStream is = null;

		try {
			// is = resolver.openInputStream(uri);
			if (uri.toString().startsWith("file:///android_asset")) {
				int index = uri.toString().lastIndexOf("/");
				if (index != -1) {
					String asset = uri.toString().substring(index + 1);
					is = UglyPicApp.getAppExContext().getAssets().open(asset);
				} else {
					return null;
				}
			} else {
				is = resolver.openInputStream(uri);
			}

			// opts.inScreenDensity = density;
			return BitmapFactory.decodeStream(is, null, opts);

		} catch (FileNotFoundException e) {
			Logger.i("文件没有找到:" + uri.toString());
			return null;
		} catch (IOException e) {
			Logger.d("IOException When scaleDecode: " + e.getMessage());
			return null;
		} finally {
			Logger.d("Success decode and will close the input stream");
			FileUtils.closeQuietly(is);
		}
	}

	public static Bitmap scaleDecode(ContentResolver resolver, Uri uri, int destWidth, int destHeight, int density) {

		if (uri == null || resolver == null) {
			return null;
		}

		boolean needScaled = true;
		if (destWidth <= 0 || destHeight <= 0) {
			needScaled = false;
		}

		InputStream is = null;
		Options opts = new Options();
		opts.inSampleSize = 1;

		try {
			if (needScaled) {
				if (uri.toString().startsWith("file:///android_asset")) {
					int index = uri.toString().lastIndexOf("/");
					if (index != -1) {
						String asset = uri.toString().substring(index + 1);
						is = UglyPicApp.getAppExContext().getAssets().open(asset);
					} else {
						return null;
					}
				} else {
					is = resolver.openInputStream(uri);
				}

				// 先获得Bitmap的宽高
				opts.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(is, null, opts);

				// 根据长宽比设置opts.inSampleSize

				// 比较长宽比，
				if (opts.outHeight <= 0 || opts.outWidth <= 0) {
					return null;
				}

				// 这里不考虑什么越界不越界了，撑死用个10000 * 10000的图片吧？
				int inSampleSize = 1;
				if (opts.outWidth * destHeight < opts.outHeight * destWidth) { // 图片比目标瘦，以高为准
					inSampleSize = (int) Math.round(opts.outHeight < (float) destHeight ? 1 : opts.outHeight
							/ (float) destHeight);
				} else {
					inSampleSize = (int) Math.round(opts.outWidth < (float) destWidth ? 1 : opts.outWidth
							/ (float) destWidth);
				}
				opts = new Options();
				opts.inSampleSize = inSampleSize;

				FileUtils.closeQuietly(is);
			}
			// is = resolver.openInputStream(uri);
			if (uri.toString().startsWith("file:///android_asset")) {
				int index = uri.toString().lastIndexOf("/");
				if (index != -1) {
					String asset = uri.toString().substring(index + 1);
					is = UglyPicApp.getAppExContext().getAssets().open(asset);
				} else {
					return null;
				}
			} else {
				is = resolver.openInputStream(uri);
			}

			// opts.inScreenDensity = density;
			return BitmapFactory.decodeStream(is, null, opts);

		} catch (FileNotFoundException e) {
			Logger.i("文件没有找到:" + uri.toString());
			return null;
		} catch (IOException e) {
			Logger.d("IOException When scaleDecode: " + e.getMessage());
			return null;
		} finally {
			Logger.d("Success decode and will close the input stream");
			FileUtils.closeQuietly(is);
		}
	}

	/**
	 * 
	 * 从图片的中心裁取一个正方形缩略图
	 * 
	 * @param resolver
	 * @param uri
	 * @return
	 */
	public static Bitmap centerSquareCut(Context context, Uri uri) {

		if (context == null || uri == null) {
			return null;
		}

		ContentResolver resolver = context.getContentResolver();

		InputStream is = null;
		Options opts = new Options();
		opts.inSampleSize = 1;

		try {
			if (uri.toString().startsWith("file:///android_asset")) {
				int index = uri.toString().lastIndexOf("/");
				if (index != -1) {
					String asset = uri.toString().substring(index + 1);

					is = UglyPicApp.getAppExContext().getAssets().open(asset);

				} else {
					return null;
				}
			} else {
				is = resolver.openInputStream(uri);
			}
		} catch (FileNotFoundException e) {
			FileUtils.closeQuietly(is);
			e.printStackTrace();
		} catch (IOException e) {
			FileUtils.closeQuietly(is);
			e.printStackTrace();
		}

		Bitmap origBitmap = BitmapFactory.decodeStream(is, null, opts);
		Bitmap tmpBitmap = null;

		if (opts.outHeight <= 0 || opts.outWidth <= 0) {
			return null;
		}
		FileUtils.closeQuietly(is);

		int bitmapWidth = opts.outWidth;
		int bitmapHeight = opts.outHeight;

		if (bitmapHeight == bitmapWidth) {// 图片本身就是正方形，原图返回
			return origBitmap;
		}

		int minBitmapLength;
		if (bitmapHeight > bitmapWidth) {
			minBitmapLength = bitmapWidth;
			tmpBitmap = Bitmap.createBitmap(origBitmap, 0, (int) ((bitmapHeight - minBitmapLength) / 2),
					minBitmapLength, minBitmapLength);
		} else {
			minBitmapLength = bitmapHeight;
			tmpBitmap = Bitmap.createBitmap(origBitmap, (int) ((bitmapWidth - minBitmapLength) / 2), 0,
					minBitmapLength, minBitmapLength);
		}

		if (origBitmap != null && origBitmap != tmpBitmap) {
			origBitmap.recycle();
			origBitmap = null;
		}

		return tmpBitmap;

	}

	public static Bitmap scaleDecode(String path, int destWidth, int destHeight) {

		if (path == null) {
			return null;
		}

		boolean needScaled = true;
		if (destWidth <= 0 || destHeight <= 0) {
			needScaled = false;
		}

		InputStream is = null;
		try {
			is = new FileInputStream(path);
			Options opts = new Options();
			opts.inSampleSize = 1;
			if (needScaled) {
				// 先获得Bitmap的宽高
				opts.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(is, null, opts);

				// 根据长宽比设置opts.inSampleSize

				// 比较长宽比，
				if (opts.outHeight <= 0 || opts.outWidth <= 0) {
					return null;
				}

				// 这里不考虑什么越界不越界了，撑死用个10000 * 10000的图片吧？
				int inSampleSize = 1;
				if (opts.outWidth * destHeight < opts.outHeight * destWidth) { // 图片比目标瘦，以高为准
					inSampleSize = (int) Math.round(opts.outHeight < (float) destHeight ? 1 : opts.outHeight
							/ (float) destHeight);
				} else {
					inSampleSize = (int) Math.round(opts.outWidth < (float) destWidth ? 1 : opts.outWidth
							/ (float) destWidth);
				}
				opts = new Options();
				opts.inSampleSize = inSampleSize;

				FileUtils.closeQuietly(is);
			}
			is = new FileInputStream(path);

			return BitmapFactory.decodeStream(is, null, opts);

		} catch (FileNotFoundException e) {
			Logger.i("图片不存在:" + path);
			return null;
		} finally {
			FileUtils.closeQuietly(is);
		}
	}

	/**
	 * 从指定UriDecode图片
	 * 
	 * @param uri
	 * @param destWidth
	 * @param destHeight
	 * @return
	 */
	public static Bitmap scaleDecode(Context context, Uri uri, int destWidth, int destHeight) {
		if (context == null || uri == null) {
			return null;
		}

		ContentResolver resolver = context.getContentResolver();
		return scaleDecode(resolver, uri, destWidth, destHeight, 1);
	}

	/**
	 * 根据扩展名获取由uri指定图片的MIME Type
	 * 
	 * @param uri
	 * @return
	 */
	public static String getMIMETypeByExtention(Uri uri) {
		String contentType = "image/jpeg";
		if (uri != null) {
			String str = uri.getPath();
			if (str == null) {
				return contentType;
			}

			int index = str.lastIndexOf(".");
			String extention = (index >= 0 && index < str.length() - 1) ? str.substring(index + 1) : null;
			if (extention == null) {
				return contentType;
			}
			String type = sImgMIMETypeDict.get(extention);
			return type == null ? contentType : type;
		}
		return contentType;
	}

	public static Options getImageInfo(Context context, Uri uri) {
		InputStream is = null;
		Options opts = new Options();
		opts.inSampleSize = 1;
		try {
			if (uri.toString().startsWith("file:///android_asset")) {
				int index = uri.toString().lastIndexOf("/");
				if (index != -1) {
					String asset = uri.toString().substring(index + 1);
					is = context.getAssets().open(asset);
				} else {
					return null;
				}
			} else {
				is = context.getContentResolver().openInputStream(uri);
			}
			// 先获得Bitmap的宽高
			opts.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(is, null, opts);
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		} finally {
			FileUtils.closeQuietly(is);
		}

		return opts;
	}

	public static Options getDecodeOptions(ContentResolver resolver, Uri uri, int destWidth, int destHeight) {

		if (uri == null || resolver == null) {
			return null;
		}

		boolean needScaled = true;
		if (destWidth <= 0 || destHeight <= 0) {
			needScaled = false;
		}

		InputStream is = null;
		Options opts = new Options();
		opts.inSampleSize = 1;
		try {
			if (needScaled) {
				is = resolver.openInputStream(uri);
				// 先获得Bitmap的宽高
				opts.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(is, null, opts);

				// 根据长宽比设置opts.inSampleSize

				// 比较长宽比，
				if (opts.outHeight <= 0 || opts.outWidth <= 0) {
					return null;
				}

				// 这里不考虑什么越界不越界了，撑死用个10000 * 10000的图片吧？
				int inSampleSize = 1;
				if (opts.outWidth * destHeight < opts.outHeight * destWidth) { // 图片比目标瘦，以高为准
					inSampleSize = (int) Math.round(opts.outHeight < (float) destHeight ? 1 : opts.outHeight
							/ (float) destHeight);
				} else {
					inSampleSize = (int) Math.round(opts.outWidth < (float) destWidth ? 1 : opts.outWidth
							/ (float) destWidth);
				}
				int imgWidth = opts.outWidth;
				int imgHeight = opts.outHeight;
				opts = new Options();
				opts.outWidth = imgWidth;
				opts.outHeight = imgHeight;
				opts.inSampleSize = inSampleSize;
			}
			return opts;
		} catch (FileNotFoundException e) {
			return null;
		} finally {
			FileUtils.closeQuietly(is);
		}
	}

	public static Options getDecodeOptions(String path, int destWidth, int destHeight) {

		if (path == null) {
			return null;
		}

		boolean needScaled = true;
		if (destWidth <= 0 || destHeight <= 0) {
			needScaled = false;
		}

		InputStream is = null;
		try {
			is = new FileInputStream(path);
			Options opts = new Options();
			opts.inSampleSize = 1;
			if (needScaled) {
				// 先获得Bitmap的宽高
				opts.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(is, null, opts);

				// 根据长宽比设置opts.inSampleSize

				// 比较长宽比，
				if (opts.outHeight <= 0 || opts.outWidth <= 0) {
					return null;
				}

				// 这里不考虑什么越界不越界了，撑死用个10000 * 10000的图片吧？
				int inSampleSize = 1;
				if (opts.outWidth * destHeight < opts.outHeight * destWidth) { // 图片比目标瘦，以高为准
					inSampleSize = (int) Math.round(opts.outHeight < (float) destHeight ? 1 : opts.outHeight
							/ (float) destHeight);
				} else {
					inSampleSize = (int) Math.round(opts.outWidth < (float) destWidth ? 1 : opts.outWidth
							/ (float) destWidth);
				}
				opts = new Options();
				opts.inSampleSize = inSampleSize;

				FileUtils.closeQuietly(is);
			}

			return opts;

		} catch (FileNotFoundException e) {
			return null;
		} finally {
			FileUtils.closeQuietly(is);
		}
	}

	public static Options getDecodeOptions(Context context, Uri uri, int destWidth, int destHeight) {
		if (context == null || uri == null) {
			return null;
		}

		ContentResolver resolver = context.getContentResolver();
		return getDecodeOptions(resolver, uri, destWidth, destHeight);
	}

	public static String saveBitmapForLocalPath(Context context, Bitmap bmp, int orientation, boolean saveToGallery) {
		long dateTaken = System.currentTimeMillis();
		String name = "ugly" + System.currentTimeMillis() + ".jpg";
		File file = getTempImageFile(name);
		if (file != null) {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(file);
				boolean result = bmp.compress(CompressFormat.JPEG, 100, fos);
				// 保存至图库
				if (result && saveToGallery) {
					ContentValues values = new ContentValues(7);

					values.put(Images.Media.TITLE, Config.IMAGE_TITLE);
					values.put(Images.Media.DISPLAY_NAME, name);
					values.put(Images.Media.DATE_TAKEN, dateTaken);
					values.put(Images.Media.MIME_TYPE, "image/jpg");
					values.put(Images.Media.ORIENTATION, orientation);
					values.put(Images.Media.DATA, file.getAbsolutePath());
					values.put(Images.Media.SIZE, file.length());

					context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
				}
			} catch (FileNotFoundException e) {
				return null;
			} finally {
				FileUtils.closeQuietly(fos);
			}
		} else {
			return null;
		}

		return file.getAbsolutePath();
	}
	
	public static String saveBitmapForLocalPath(Context context, Bitmap bmp, String fileName, int orientation, boolean saveToGallery) {
		String name = "ugly" + fileName + ".jpg";
		File file = getTempImageFile(name);
		if (file != null) {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(file);
				boolean result = bmp.compress(CompressFormat.JPEG, 100, fos);
				// 保存至图库
				if (result && saveToGallery) {
					ContentValues values = new ContentValues(7);

					values.put(Images.Media.TITLE, Config.IMAGE_TITLE);
					values.put(Images.Media.DISPLAY_NAME, name);
					values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
					values.put(Images.Media.MIME_TYPE, "image/jpg");
					values.put(Images.Media.ORIENTATION, orientation);
					values.put(Images.Media.DATA, file.getAbsolutePath());
					values.put(Images.Media.SIZE, file.length());

					context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
				}
			} catch (FileNotFoundException e) {
				return null;
			} finally {
				FileUtils.closeQuietly(fos);
			}
		} else {
			return null;
		}

		return file.getAbsolutePath();
	}

	public static Uri saveBitmapToGallery(Context context, Uri uri, int orientation) {
		if (uri == null) {
			return null;
		}

		long dateTaken = System.currentTimeMillis();
		String name = "ugly" + System.currentTimeMillis() + ".jpg";
		File file = getTempImageFile(name);
		String uriStr = uri.toString();
		DataInputStream dis = null;
		DataOutputStream fos = null;
		if (file == null) {
			return null;
		}
		try {
			fos = new DataOutputStream(new FileOutputStream(file));
			if (uriStr.startsWith("http://")) { // http url
				InputStream is = ImageDownloader.open(uriStr);
				dis = new DataInputStream(is);
			} else {
				InputStream is;
				is = context.getContentResolver().openInputStream(uri);
				dis = new DataInputStream(is);
			}

			byte[] buffer = new byte[1024];
			int size = 0;
			while ((size = dis.read(buffer, 0, buffer.length)) > 0) {
				fos.write(buffer, 0, size);
			}
			fos.flush();

			ContentValues values = new ContentValues(7);
			values.put(Images.Media.TITLE, Config.IMAGE_TITLE);
			values.put(Images.Media.DISPLAY_NAME, name);
			values.put(Images.Media.DATE_TAKEN, dateTaken);
			values.put(Images.Media.MIME_TYPE, getMIMETypeByExtention(uri));
			values.put(Images.Media.ORIENTATION, orientation);
			values.put(Images.Media.DATA, file.getAbsolutePath());
			values.put(Images.Media.SIZE, file.length());

			return context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
		} catch (FileNotFoundException e) {

		} catch (IOException e) {

		} finally {
			FileUtils.closeQuietly(dis);
			FileUtils.closeQuietly(fos);
		}

		return null;
	}

	/**
	 * 
	 * 保存图像到图库
	 * 
	 * @param bmp
	 * @return
	 */
	public static Uri saveBitmapToGallery(Context context, Bitmap bmp, int orientation, boolean saveToGallery) {

		if (bmp == null) {
			return null;
		}

		long dateTaken = System.currentTimeMillis();
		String name = "dbn" + System.currentTimeMillis() + ".jpg";
		File file = getTempImageFile(name);
		if (file != null) {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(file);
				boolean result = bmp.compress(CompressFormat.JPEG, 100, fos);
				// 保存至图库
				if (result && saveToGallery) {
					ContentValues values = new ContentValues(7);

					values.put(Images.Media.TITLE, Config.IMAGE_TITLE);
					values.put(Images.Media.DISPLAY_NAME, name);
					values.put(Images.Media.DATE_TAKEN, dateTaken);
					values.put(Images.Media.MIME_TYPE, "image/jpg");
					values.put(Images.Media.ORIENTATION, orientation);
					values.put(Images.Media.DATA, file.getAbsolutePath());
					values.put(Images.Media.SIZE, file.length());

					return context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
				} else if (result) {
					return Uri.fromFile(file);
				}
			} catch (FileNotFoundException e) {
				return null;
			} finally {
				FileUtils.closeQuietly(fos);
			}
		}

		return null;
	}

	public static void topDownMirror(int[] pixels, int width, int height) {
		int length = height / 2;

		int temp = 0;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < length; j++) {
				temp = pixels[j * width + i];
				pixels[j * width + i] = pixels[(height - j) * width + i];
				pixels[(height - j) * width + i] = temp;
			}
		}
	}

	/**
	 * 
	 * 返回指定图片文件的宽和高
	 * 
	 * @param contextResolver
	 * @param uri
	 *            图片文件的uri
	 * @return int[] 。int[0] 为图片的宽,int[1]为图片的高。
	 * @throws FileNotFoundException
	 */
	public static int[] getPicWidthAndHeight(ContentResolver contentResolver, Uri uri) throws FileNotFoundException {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		InputStream is = contentResolver.openInputStream(uri);
		BitmapFactory.decodeStream(is, null, options);
		int imageHeight = options.outHeight;
		int imageWidth = options.outWidth;
		FileUtils.closeQuietly(is);
		return new int[] { imageWidth, imageHeight };
	}

	/**
	 * 
	 * 返回一个 oriBitmap 的圆角头像。<br>
	 * 假设图片是正方形的，那么返回的头像刚好是标准的圆形。<br>
	 * 如果图片是长方形的，那么返回的头像是 占据中间位置的圆形 <br>
	 * 
	 * 推荐在 非UI线程调用
	 * 
	 * @param oriBitmap
	 * @param maxRadix
	 *            最大半径。如果这个值小于等于0，这个值将会被忽略，此时半径会取宽、高中小的那一个值。
	 * @return
	 */
	public static Bitmap getRoundBitmap(Bitmap oriBitmap, int maxRadix) {
		if (oriBitmap == null || oriBitmap.isRecycled()) {
			return null;
		}
		int oriBitmapWidth = oriBitmap.getWidth();
		int oriBitmapHeight = oriBitmap.getHeight();
		int minLength = Math.min(oriBitmapWidth, oriBitmapHeight);
		if (maxRadix > 0) {
			minLength = Math.min(minLength, maxRadix);
		}

		Bitmap bitmap = Bitmap.createBitmap(minLength, minLength, android.graphics.Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		Matrix matrix = new Matrix();
		matrix.postTranslate((minLength - oriBitmapWidth) / 2, (minLength - oriBitmapHeight) / 2);

		BitmapShader shader = new BitmapShader(oriBitmap, TileMode.CLAMP, TileMode.CLAMP);
		shader.setLocalMatrix(matrix);

		Paint paint = new Paint();
		paint.setShader(shader);
		paint.setAntiAlias(true);

		Path path = new Path();
		path.addCircle(minLength / 2, minLength / 2, minLength / 2, Direction.CCW);
		path.close();

		canvas.drawPath(path, paint);
		return bitmap;

	}

	public static Bitmap getRoundBitmap(Bitmap oriBitmap) {
		return getRoundBitmap(oriBitmap, -1);
	}

	private static int makeSample(File srcBitmap, int srcBtWidth, int srcBtHeight) {
		long fileMemorySize = srcBtWidth * srcBtHeight * 4;
		int sample = 1;
		if (fileMemorySize <= MAX_MEMORY_SIZE) {
			sample = 1;
		} else if (fileMemorySize <= MAX_MEMORY_SIZE * 4) {
			sample = 2;
		} else {
			long times = fileMemorySize / MAX_MEMORY_SIZE;
			sample = (int) (Math.log(times) / Math.log(2.0)) + 1;
			int inSampleScale = (int) (Math.log(sample) / Math.log(2.0));
			sample = (int) Math.scalb(1, inSampleScale);

			long curFileMemorySize = (srcBtWidth / sample) * (srcBtHeight / sample) * 4;
			if (curFileMemorySize > MAX_MEMORY_SIZE) {
				sample = sample * 2;
			}
		}

		if (sample == 1 && (srcBtWidth > MAX_WIDTH || srcBtHeight > MAX_WIDTH)) {
			sample = 2;
		}

		return sample;
	}

	@SuppressLint("NewApi")
	public static Bitmap loadBitmapWithSizeCheckAndBitmapReuse(File bitmapFile, Bitmap reuseBt, int orientataion) {

		Bitmap bmp = null;
		FileInputStream fis = null;
		int oomCount = 0;
		try {
			bitmapFile.setLastModified(System.currentTimeMillis());

			BitmapFactory.Options opt = new BitmapFactory.Options();
			opt.inPurgeable = true;
			opt.inJustDecodeBounds = true;

			BitmapFactory.decodeFile(bitmapFile.getAbsolutePath(), opt);
			int width = opt.outWidth;
			int height = opt.outHeight;

			BitmapFactory.Options newOpt = new BitmapFactory.Options();
			newOpt.inSampleSize = makeSample(bitmapFile, width, height);

			// check width and height with sample
			if (width > 2048 || height > 2048) {
				int max = width > height ? width : height;
				int maxOrigin = max;
				int curSample = newOpt.inSampleSize;
				max = maxOrigin / curSample;
				while (max > 2048) {
					curSample = curSample * 2;
					max = maxOrigin / curSample;
				}
				newOpt.inSampleSize = curSample;
			}

			// newOpt.inScaled = true;
			newOpt.inPurgeable = true;
			newOpt.inInputShareable = true;

			boolean reusedBt = false;
			if (reuseBt != null && !reuseBt.isRecycled() && width == reuseBt.getWidth()
					&& height == reuseBt.getHeight()) {
				// need to check target sdk version
				reusedBt = true;
				newOpt.inBitmap = reuseBt;
			}

			newOpt.outHeight = height;
			newOpt.outWidth = width;
			fis = new FileInputStream(bitmapFile);

			bmp = BitmapFactory.decodeStream(fis, null, newOpt);

			if (orientataion != 0 && bmp != null) {
				Matrix matrix = new Matrix();
				matrix.postRotate((float) orientataion);
				Bitmap tmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
				if (tmp != null) {
					bmp.recycle();
					bmp = null;
					bmp = tmp;
				}
			}

			return bmp;
		} catch (Exception e) {
			Logger.d(">>>>> Exception: " + e);
		} catch (OutOfMemoryError e) {
			Logger.d(">>>>> OutOfMemoryError: " + e);
			oomCount++;
			if (oomCount > 5) {
				/**
				 * 做一个释放资源的尝试
				 */
				oomCount = 0;
				UglyPicApp.getApplication(UglyPicApp.getAppExContext()).getBitmapCache().trimMemory();
			}
		} finally {
			try {
				if (fis != null) {
					fis.close();
					fis = null;
				}
			} catch (Exception ex) {
				Logger.d(">>>>> OutOfMemoryError: " + ex);
			}
		}
		return null;
	}
	

	public static Bitmap getRoundAngleBitmap(Bitmap oriBitmap, int roundWidth,
			int roundHeight) {
		if (oriBitmap == null || oriBitmap.isRecycled()) {
			return null;
		}
		InvisibleUtils.RoundAngleCreater creator = new InvisibleUtils.RoundAngleCreater(
				oriBitmap, roundWidth, roundHeight);
		return creator.getBitmap();
	}
}
