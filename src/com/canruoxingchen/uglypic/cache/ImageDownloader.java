/**
 * ImageDownloader.java
 */
package com.canruoxingchen.uglypic.cache;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.canruoxingchen.uglypic.util.FileUtils;
import com.canruoxingchen.uglypic.util.ImageUtils;
import com.canruoxingchen.uglypic.util.Logger;
import com.canruoxingchen.uglypic.util.StringUtils;


/**
 * 
 * 提供下载图片的工具方法
 * 
 * @author wsf
 * 
 */
public class ImageDownloader {

	private static final String TAG = "ImageDownloader";

	private static final ImageDownloader sInstance = new ImageDownloader();

	private ImageDownloader() {

	}

	public static ImageDownloader getInstance() {
		return sInstance;
	}

	/**
	 * 
	 * @param imgUrl
	 * @return
	 */
	public static InputStream open(String imgUrl) {

		HttpURLConnection connection = null;
		URL url;
		try {
			url = new URL(imgUrl);
			connection = (HttpURLConnection) url.openConnection();
			if (connection != null) {
				return connection.getInputStream();
			}
		} catch (MalformedURLException e) {

		} catch (IOException e) {
		}
		return null;
	}
	
	public static String download(String imgUrl, File destFile) {
		HttpURLConnection connection = null;
		BufferedInputStream bis = null;
		DataOutputStream dos = null;
		try {
			URL url = new URL(imgUrl);
			connection = (HttpURLConnection) url.openConnection();
			if (connection != null
					&& connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				if (connection.getInputStream() != null) {
					bis = new BufferedInputStream(connection.getInputStream());
					File tmpFile = (destFile != null ? destFile : ImageUtils.getTempImageFile(StringUtils.MD5Encode(imgUrl) + System
							.currentTimeMillis() + "_img"));
					if (tmpFile != null) {
						dos = new DataOutputStream(
								new FileOutputStream(tmpFile));
						byte[] buffer = new byte[1024];
						int size = 0;
						while ((size = bis.read(buffer, 0, buffer.length)) > 0) {
							dos.write(buffer, 0, size);
						}
						LOGD("Image Downloaded ------> "
								+ tmpFile.getAbsolutePath());
						return tmpFile.getAbsolutePath();
					}
				}
			}
		} catch (MalformedURLException e) {
			LOGD(e.getMessage());
		} catch (IOException e) {
			LOGD(e.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
			FileUtils.closeQuietly(bis);
			FileUtils.closeQuietly(dos);
		}

		return null;
	}


	private static void LOGD(String logMe) {
		Logger.d(TAG, logMe);
	}
}
