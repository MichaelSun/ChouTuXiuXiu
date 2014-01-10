/**
 * 
 */
package com.canruoxingchen.uglypic.footage;


/**
 * @author wsf
 * 
 */
public class FootAge implements Comparable<FootAge>{
	public static final String CLASS_NAME = "Footage";
	public static final String COLUMN_FOOTAGE_ICON = "footageIcon";
	public static final String COLUMN_FOOTAGE_PARENT_ID = "footageParentId";
	public static final String COLUMN_FOOTAGE_OBJECT_ID = "objectId";
	public static final String COLUMN_FOOTAGE_ICON_NAME = "footageIconName";
	public static final String COLUMN_FOOTAGE_ORDER_NUM = "footageOrderNum";
	public static final String COLUMN_FOOTAGE_USE_NUM_ANDROID = "footageUseNum_Android";

	private String mObjectId;
	private String mParentId;
	private String mIconUrl;
	private String mIconName;
	private int mOrderNum;

	public String getObjectId() {
		return mObjectId;
	}

	public void setObjectId(String objectId) {
		this.mObjectId = objectId;
	}

	public String getParentId() {
		return mParentId;
	}

	public void setParentId(String parentId) {
		this.mParentId = parentId;
	}

	public String getIconUrl() {
		return mIconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.mIconUrl = iconUrl;
	}

	public String getIconName() {
		return mIconName;
	}

	public void setIconName(String iconName) {
		this.mIconName = iconName;
	}

	public int getOrderNum() {
		return mOrderNum;
	}

	public void setOrderNum(int orderNum) {
		this.mOrderNum = orderNum;
	}

	public FootAge() {

	}

	public FootAge(String mObjectId, String mParentId, String mIconUrl, String mIconName, int mOrderNum) {
		super();
		this.mObjectId = mObjectId;
		this.mParentId = mParentId;
		this.mIconUrl = mIconUrl;
		this.mIconName = mIconName;
		this.mOrderNum = mOrderNum;
	}

	@Override
	public String toString() {
		return "FootAge [mObjectId=" + mObjectId + ", mParentId=" + mParentId + ", mIconUrl=" + mIconUrl
				+ ", mIconName=" + mIconName + ", mOrderNum=" + mOrderNum + "]";
	}

	@Override
	public int compareTo(FootAge another) {
		if(another == null) {
			return 0;
		}
		
		if(mOrderNum > another.mOrderNum) {
			return -1;
		} else if(mOrderNum < another.mOrderNum) {
			return 1;
		}
		return 0;
	}
}
