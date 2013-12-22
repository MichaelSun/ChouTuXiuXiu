package com.canruoxingchen.uglypic.footage;

import com.canruoxingchen.uglypic.http.CloudObj;

public class FootAgeType implements CloudObj, Comparable<FootAgeType> {
	public static final String CLASS_NAME = "FootAgeType";

	public static final String COLUMN_OBJECT_ID = "objectId";
	public static final String COLUMN_TYPE_NAME = "typeName";
	public static final String COLUMN_OLD_NAME = "oldName";
	public static final String COLUMN_IS_DEFAULT = "isDefault";
	public static final String COLUMN_ORDER_NUM = "orderNum";
	
	public static FootAgeType RECENT_TYPE = new FootAgeType("recent", "常用", "", 1, -1);

	private String mObjectId;
	private String mTypeName;
	private String mOldName;
	private int mIsDefault;
	private int mOrderNum;
	
	public FootAgeType() {
		
	}

	public FootAgeType(String mObjectId, String mTypeName, String mOldName, int mIsDefault, int mOrderNum) {
		super();
		this.mObjectId = mObjectId;
		this.mTypeName = mTypeName;
		this.mOldName = mOldName;
		this.mIsDefault = mIsDefault;
		this.mOrderNum = mOrderNum;
	}

	public String getObjectId() {
		return mObjectId;
	}

	public void setObjectId(String objectId) {
		this.mObjectId = objectId;
	}

	public String getTypeName() {
		return mTypeName;
	}

	public void setTypeName(String typeName) {
		this.mTypeName = typeName;
	}

	public String getOldName() {
		return mOldName;
	}

	public void setOldName(String oldName) {
		this.mOldName = oldName;
	}

	public int getIsDefault() {
		return mIsDefault;
	}

	public void setIsDefault(int isDefault) {
		this.mIsDefault = isDefault;
	}

	public int getOrderNum() {
		return mOrderNum;
	}

	public void setOrderNum(int orderNum) {
		this.mOrderNum = orderNum;
	}

	@Override
	public String toString() {
		return "FootAgeType [mObjectId=" + mObjectId + ", mTypeName=" + mTypeName + ", mOldName=" + mOldName
				+ ", mIsDefault=" + mIsDefault + ", mOrderNum=" + mOrderNum + "]";
	}

	@Override
	public String getName() {
		return CLASS_NAME;
	}

	@Override
	public int compareTo(FootAgeType another) {
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
