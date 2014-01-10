/**
 * 
 */
package com.canruoxingchen.uglypic.footage;

import android.graphics.Rect;

/**
 * 
 * 场景素材
 * 
 * @author wsf
 * 
 */
public class NetSence implements Comparable<NetSence> {
	public static final String CLASS_NAME = "NetSence";
	
	public static final String COLUMN_OBJECT_ID = "objectId";
	public static final String COLUMN_SENCE_NET_ICON = "senceNetIcon";
	public static final String COLUMN_SENCE_PARENT_ID = "senceParentId";
	public static final String COLUMN_SENCE_ORDER_NUM = "senceOrderNum";
	public static final String COLUMN_SENCE_NAME = "senceName";
	public static final String COLUMN_SENCE_DESCRIBE = "senceDescribe";
	public static final String COLUMN_INPUT_CONTENT = "inputContent";
	public static final String COLUMN_INPUT_RECT = "inputRect";
	public static final String COLUMN_INPUT_FONT_NAME = "inputFontName";
	public static final String COLUMN_INPUT_FONT_SIZE = "inputFontSize";
	public static final String COLUMN_INPUT_FONT_COLOR = "inputFontColor";
	public static final String COLUMN_INPUT_FONT_ALIGNMENT = "inputFontAlignment";
	public static final String COLUMN_TIME_RECT = "timeRect";
	public static final String COLUMN_TIME_FONT_NAME = "timeFontName";
	public static final String COLUMN_TIME_FONT_SIZE = "timeFontSize";
	public static final String COLUMN_TIME_FONT_COLOR = "timeFontColor";
	public static final String COLUMN_TIME_FONT_ALIGNMENT = "timeFontAlignment";
	public static final String COLUMN_NET_SCENE_USER_NUM = "senceUseNum_Android";

	private String mObjectId;
	private String mSenceNetIcon;
	private String mSenceParentId;
	private Integer mSenceOrderNum;
	private String mSenceName;
	private String mSenceDescribe;
	private String mInputContent;
	private String mInputRect;
	private String mInputFontName;
	private Integer mInputFontSize;
	private Integer mInputFontColor;
	private Integer mInputFontAlignment;
	private String mTimeRect;
	private String mTimeFontName;
	private Integer mTimeFontSize;
	private Integer mTimeFontColor;
	private Integer mTimeFontAlignment;
	
	public static final NetSence DEFAULT = new NetSence();
	
	public NetSence() {
		
	}

	public NetSence(String objectId, String senceNetIcon, String senceParentId, Integer senceOrderNum,
			String senceName, String senceDescribe, String inputContent, String inputRect, String inputFontName,
			Integer inputFontSize, Integer inputFontColor, Integer inputFontAlignment, String timeRect,
			String timeFontName, Integer timeFontSize, Integer timeFontColor, Integer timeFontAlignment) {
		this.mObjectId = objectId;
		this.mSenceNetIcon = senceNetIcon;
		this.mSenceParentId = senceParentId;
		this.mSenceOrderNum = senceOrderNum;
		this.mSenceName = senceName;
		this.mSenceDescribe = senceDescribe;
		this.mInputContent = inputContent;
		this.mInputRect = inputRect;
		this.mInputFontName = inputFontName;
		this.mInputFontSize = inputFontSize;
		this.mInputFontColor = inputFontColor;
		this.mInputFontAlignment = inputFontAlignment;
		this.mTimeRect = timeRect;
		this.mTimeFontName = timeFontName;
		this.mTimeFontSize = timeFontSize;
		this.mTimeFontColor = timeFontColor;
		this.mTimeFontAlignment = timeFontAlignment;
	}

	public String getObjectId() {
		return mObjectId;
	}

	public void setObjectId(String objectId) {
		this.mObjectId = objectId;
	}

	public String getSenceNetIcon() {
		return mSenceNetIcon;
	}

	public void setSenceNetIcon(String senceNetIcon) {
		this.mSenceNetIcon = senceNetIcon;
	}

	public String getSenceParentId() {
		return mSenceParentId;
	}

	public void setSenceParentId(String senceParentId) {
		this.mSenceParentId = senceParentId;
	}

	public Integer getSenceOrderNum() {
		return mSenceOrderNum;
	}

	public void setSenceOrderNum(Integer senceOrderNum) {
		this.mSenceOrderNum = senceOrderNum;
	}

	public String getSenceName() {
		return mSenceName;
	}

	public void setSenceName(String senceName) {
		this.mSenceName = senceName;
	}

	public String getSenceDescribe() {
		return mSenceDescribe;
	}

	public void setSenceDescribe(String senceDescribe) {
		this.mSenceDescribe = senceDescribe;
	}

	public String getInputContent() {
		return mInputContent;
	}

	public void setInputContent(String inputContent) {
		this.mInputContent = inputContent;
	}

	public String getInputRect() {
		return mInputRect;
	}

	public void setInputRect(String inputRect) {
		this.mInputRect = inputRect;
	}

	public String getInputFontName() {
		return mInputFontName;
	}

	public void setInputFontName(String inputFontName) {
		this.mInputFontName = inputFontName;
	}

	public Integer getInputFontSize() {
		return mInputFontSize;
	}

	public void setInputFontSize(Integer inputFontSize) {
		this.mInputFontSize = inputFontSize;
	}

	public Integer getInputFontColor() {
		return mInputFontColor;
	}

	public void setInputFontColor(Integer inputFontColor) {
		this.mInputFontColor = inputFontColor;
	}

	public Integer getInputFontAlignment() {
		return mInputFontAlignment;
	}

	public void setInputFontAlignment(Integer inputFontAlignment) {
		this.mInputFontAlignment = inputFontAlignment;
	}

	public String getTimeRect() {
		return mTimeRect;
	}

	public void setTimeRect(String timeRect) {
		this.mTimeRect = timeRect;
	}

	public String getTimeFontName() {
		return mTimeFontName;
	}

	public void setTimeFontName(String timeFontName) {
		this.mTimeFontName = timeFontName;
	}

	public Integer getTimeFontSize() {
		return mTimeFontSize;
	}

	public void setTimeFontSize(Integer timeFontSize) {
		this.mTimeFontSize = timeFontSize;
	}

	public Integer getTimeFontColor() {
		return mTimeFontColor;
	}

	public void setTimeFontColor(Integer timeFontColor) {
		this.mTimeFontColor = timeFontColor;
	}

	public Integer getTimeFontAlignment() {
		return mTimeFontAlignment;
	}

	public void setTimeFontAlignment(Integer timeFontAlignment) {
		this.mTimeFontAlignment = timeFontAlignment;
	}

	public Rect getInputRectBounds() {
		if (mInputRect == null) {
			return null;
		}
		String[] pts = mInputRect.split(",");
		if (pts == null || pts.length != 4) {
			return null;
		}

		int left = Integer.parseInt(pts[0]);
		int top = Integer.parseInt(pts[1]);
		Rect rect = new Rect(left, top, left + Integer.parseInt(pts[2]), top +
				Integer.parseInt(pts[3]));
		return rect;
	}
	
	public Rect getTimeRectBounds() {
		if (mTimeRect == null) {
			return null;
		}
		String[] pts = mTimeRect.split(",");
		if (pts == null || pts.length != 4) {
			return null;
		}

		Rect rect = new Rect(Integer.parseInt(pts[0]), Integer.parseInt(pts[1]), Integer.parseInt(pts[2]),
				Integer.parseInt(pts[3]));
		return rect;
	}

	@Override
	public int compareTo(NetSence another) {
		if (another == null) {
			return 0;
		}
		if (mSenceOrderNum > another.mSenceOrderNum) {
			return -1;
		} else if (mSenceOrderNum < another.mSenceOrderNum) {
			return 1;
		}
		return 0;
	}

}
