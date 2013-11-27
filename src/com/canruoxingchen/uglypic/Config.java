/**
 * 
 */
package com.canruoxingchen.uglypic;

import com.canruoxingchen.uglypic.util.FileUtils;

import android.os.Environment;


/**
 * 应用配置
 * 
 * @author wsf
 *
 */
public class Config {
	
	public static final boolean DEBUG = BuildConfig.DEBUG;
	
	//应用数据根目录
	public static final String ROOT_PATH;
	
	//照片目录
	public static final String IMAGE_PATH;

	public static final String CACHED_IMAGE_PATH;
	
	public static final String IMAGE_TITLE = "丑图秀秀";
	
	public static final String DB_NAME = "ugly_pic_db";
	
	public static final int MAX_POST_CONTENT_COUNT = 140;
	
	public static final String PUBLISH_TEMP_PATH;
	
	public static final long MAX_DISK_CACHE_SIZE = 1024 * 1024 * 80;

	public static final int MAX_TEXTURE_WIDTH = 720;
	public static final int MAX_TEXTURE_HEIGHT = 720;
	
	public static final long DEFAULT_PACKAGE_ID = 45;

	public static final int MAX_PUBLISH_PIC_WIDTH=512;
	public static final int MAX_PUBLISH_PIC_HEIGHT=512;
	
	public static final int MAX_WORK = 9;
	
	public static final String ROOT_PATH_IN_SDCARD = ".uglypic/";
	
	static {
		boolean isSdCardAvailable = FileUtils.checkSdCardUserful();
		if(isSdCardAvailable && Environment.getExternalStorageDirectory() != null) {
			String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
			if(sdcardPath.endsWith("/")) {
				ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + ROOT_PATH_IN_SDCARD;
			} else {
				ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + ROOT_PATH_IN_SDCARD;
			}
		} else {
			ROOT_PATH = "/sdcard/" + ROOT_PATH_IN_SDCARD;
		}
		
		IMAGE_PATH = ROOT_PATH + "images/";
		CACHED_IMAGE_PATH = ROOT_PATH + "cachedImages/";
		PUBLISH_TEMP_PATH = ROOT_PATH+"publish_temp_dir/";
	}
	
	public static final long SHOW_PHOTO_FORUM = 2;
	public static final long FOR_SUGGESTION_FORUM = 1;
}
