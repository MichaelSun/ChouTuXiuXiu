/**
 * FileUtils.java
 */
package com.canruoxingchen.uglypic.util;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.canruoxingchen.uglypic.Config;

/**
 * 提供文件拷贝等基础文件操作方法
 * 
 */
public class FileUtils {

	private static final String TAG = "FileUtils";

	public static boolean copyFile(InputStream is, OutputStream os) {

		BufferedInputStream bis = null;
		DataOutputStream dos = null;
		byte[] buffer = new byte[1024];
		int size = 0;
		try {
			bis = new BufferedInputStream(is);
			dos = new DataOutputStream(os);
			while ((size = bis.read(buffer, 0, buffer.length)) > 0) {
				dos.write(buffer, 0, size);
			}
			return true;
		} catch (IOException e) {
			LOGD(e.getMessage());
		} finally {
			closeQuietly(bis);
			closeQuietly(dos);
		}

		return false;
	}

	/**
	 * 拷贝文件
	 * 
	 * @param src
	 * @param dest
	 * @return 拷贝成功返回true，否则返回false
	 */
	public static boolean copyFile(String src, String dest) {
		if (!TextUtils.isEmpty(src) && !TextUtils.isEmpty(dest)) {
			File srcFile = new File(src);
			if (!srcFile.exists() || !srcFile.canRead()) {
				return false;
			}

			File destFile = new File(dest);
			if (destFile.exists()) {
				if (!destFile.delete()) {
					return false;
				}
			}

			try {
				return copyFile(new FileInputStream(srcFile),
						new FileOutputStream(destFile));
			} catch (FileNotFoundException e) {
				LOGD(e.getMessage());
				return false;
			}
		}

		return false;
	}

	public static void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				LOGD(e.getMessage());
			}
			closeable = null;
		}
	}

	public static void closeQuietly(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
				LOGD(e.getMessage());
			}
			is = null;
		}
	}

	public static void closeQuietly(OutputStream os) {
		if (os != null) {
			try {
				os.close();
			} catch (IOException e) {
				LOGD(e.getMessage());
			}
			os = null;
		}
	}

	/**
	 * 在sd卡上的.dabanniu文件夹下，建立一个空文件，并返回它的文件路径(这个文件已经存在了)
	 * ***/
	public static String createSdCardFile() {
		if (checkSdCardUserful()) {
			String dirPath = Config.PUBLISH_TEMP_PATH;
			File dir = new File(dirPath);
			if (dir.exists() && dir.isFile()) {
				dir.delete();
			}
			if (!dir.exists()) {
				dir.mkdirs();
			}
			File file = new File(dir, System.currentTimeMillis() + ".jpg");
			try {
				file.createNewFile();
			} catch (IOException e) {
				if (Logger.DEBUG)
					e.printStackTrace();
			}
			Logger.i("在sd卡目录下建了个新文件:" + file.getAbsolutePath());
			return file.getAbsolutePath();
		}
		return null;
	}
	
	public static String createSdCardFile(String name) {
		if(TextUtils.isEmpty(name)) {
			return null;
		}
		if (checkSdCardUserful()) {
			String dirPath = Config.PUBLISH_TEMP_PATH;
			File dir = new File(dirPath);
			if (dir.exists() && dir.isFile()) {
				dir.delete();
			}
			if (!dir.exists()) {
				dir.mkdirs();
			}
			File file = new File(dir, name);
			try {
				file.createNewFile();
			} catch (IOException e) {
				if (Logger.DEBUG)
					e.printStackTrace();
			}
			Logger.i("在sd卡目录下建了个新文件:" + file.getAbsolutePath());
			return file.getAbsolutePath();
		}
		return null;
	}

	public static String createInnerFile(Context context, String fileName) {
		String filePath = context.getFilesDir() + "/" + fileName;
		return filePath;
	}

	/**
	 * 检查sd卡是否可用
	 * */
	public static boolean checkSdCardUserful() {
		return Environment.MEDIA_MOUNTED.equalsIgnoreCase(Environment.getExternalStorageState());
	}

	/**
	 * Uri转文件路径
	 * 
	 * @param context
	 * @param uri
	 * @return 转换的文件路径
	 */
	public static String convertUriToFilePath(Context context, Uri originalUri) {
		if(originalUri == null) {
			return null;
		}
		
		boolean isPicasa = isPicasa(context, originalUri);
		if (isPicasa) {
			return getPicasaFilePath(context, "gallery3d_tempImage.jpg",
					originalUri);
		} else {
			String schema = originalUri.getScheme();
			if (schema != null) {
				if (schema.equals("file")) {
					return originalUri.getPath();
				} else {
					return getRealPathFromURI(context, originalUri);
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * @param context
	 * @param originalUri
	 * @return
	 */
	private static boolean isPicasa(Context context, Uri originalUri) {
		// some devices (OS versions return an URI of com.android instead of
		// com.google.android
		if (originalUri.toString().startsWith(
				"content://com.android.gallery3d.provider")) {
			// use the com.google provider, not the com.android provider.
			originalUri = Uri.parse(originalUri.toString().replace(
					"com.android.gallery3d", "com.google.android.gallery3d"));
		}

		// if it is a picasa image on newer devices with OS 3.0 and up
		if (originalUri.toString().startsWith(
				"content://com.google.android.gallery3d")) {
			return true;
		} else { // it is a regular local image file
			String schema = originalUri.getScheme();
			if (schema != null) {
				if (schema.equals("file")) {
					return false;
				} else {
					String path = getRealPathFromURI(context, originalUri);
					if (path.startsWith("http://")
							|| path.startsWith("https://")) {
						return true;
					} else {
						return false;
					}
				}
			}
		}
		return false;
	}

	/**
	 * get picasa image filepath
	 * 
	 * @param context
	 * @param tag
	 * @param url
	 * @return picasa文件路径
	 */
	private static String getPicasaFilePath(Context context, String tag, Uri url) {
		File cacheDir;
		if (checkSdCardUserful()) {
			cacheDir = new File(
					android.os.Environment.getExternalStorageDirectory(),
					".OCFL311");
		} else {
			cacheDir = context.getCacheDir();
		}
		if (!cacheDir.exists())
			cacheDir.mkdirs();

		File f = new File(cacheDir, tag);

		InputStream inputStream = null;
		OutputStream outputStream = null;

		try {
			String path = getRealPathFromURI(context, url);

			if (path != null) {
				if (path.startsWith("http://") || path.startsWith("https://")) {
					inputStream = new URL(path).openStream();
				} else {
					inputStream = context.getContentResolver().openInputStream(
							url);
				}
			} else {
				inputStream = context.getContentResolver().openInputStream(url);
			}

			outputStream = new FileOutputStream(f);

			byte[] buffer = new byte[1024];
			int len;
			while ((len = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, len);
			}

			/* 得到fileFullPath */
			return f.getAbsolutePath();
		} catch (FileNotFoundException ex) {
			if (Config.DEBUG)
				ex.printStackTrace();
		} catch (IOException e) {
			if (Config.DEBUG)
				e.printStackTrace();
		} finally {
			FileUtils.closeQuietly(inputStream);
			FileUtils.closeQuietly(outputStream);
		}
		return null;
	}

	/**
	 * 根据URI得到文件路径
	 * 
	 * @param context
	 * @param contentUri
	 * @return 文件路径
	 */
	private static String getRealPathFromURI(Context context, Uri contentUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = context.getContentResolver().query(contentUri, proj,
				null, null, null);
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		String path = cursor.getString(column_index);
		cursor.close();
		return path;
	}

	public static byte[] getBytes(String filePath) {
		if (filePath == null) {
			return null;
		}

		FileInputStream fin;
		try {
			fin = new FileInputStream(filePath);
			File file = new File(filePath);
			long length = file.length();
			byte[] buffer = new byte[Integer.parseInt(length + "")];
			fin.read(buffer);
			fin.close();
			return buffer;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}

	private static void LOGD(String logMe) {
		Logger.d(TAG, logMe);
	}
}
