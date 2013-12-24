package com.canruoxingchen.uglypic.footage;

import com.canruoxingchen.uglypic.http.CloudObj;

public class FootAgeType implements CloudObj, Comparable<FootAgeType> {
	public static final String CLASS_NAME = "FootAgeType";
	
	public static final int TYPE_RECENT = 0;
	public static final int TYPE_IMAGE = 1;
	public static final int TYPE_SCENE = 3;

	public static final String COLUMN_OBJECT_ID = "objectId";
	public static final String COLUMN_TYPE_NAME = "typeName";
	public static final String COLUMN_OLD_NAME = "oldName";
	public static final String COLUMN_IS_DEFAULT = "isDefault";
	public static final String COLUMN_ORDER_NUM = "orderNum";
	public static final String COLUMN_TYPE_TARGET = "typeTarget";
	
	public static FootAgeType RECENT_TYPE = new FootAgeType("recent", "常用", "", 1, -1, 0);

	private String mObjectId;
	private String mTypeName;
	private String mOldName;
	private int mIsDefault;
	private int mOrderNum;
	private int mTypeTarget;
	
	public FootAgeType() {
		
	}

	public FootAgeType(String mObjectId, String mTypeName, String mOldName, int mIsDefault, int mOrderNum,
			int typeTarget) {
		super();
		this.mObjectId = mObjectId;
		this.mTypeName = mTypeName;
		this.mOldName = mOldName;
		this.mIsDefault = mIsDefault;
		this.mOrderNum = mOrderNum;
		this.mTypeTarget = typeTarget;
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


	public int getTypeTarget() {
		return mTypeTarget;
	}

	public void setTypeTarget(int typeTarget) {
		this.mTypeTarget = typeTarget;
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
