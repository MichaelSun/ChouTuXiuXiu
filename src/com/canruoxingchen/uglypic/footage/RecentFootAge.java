/**
 * 
 */
package com.canruoxingchen.uglypic.footage;

/**
 * 
 * 常用的素材
 * 
 * @author wsf
 * 
 */
public class RecentFootAge implements Comparable<RecentFootAge>{
	private String mObjectId;
	private long mAccessTime;
	private int mType;
	private String mJson;

	public RecentFootAge(String objectId, long accessTime, int type, String json) {
		this.mObjectId = objectId;
		this.mAccessTime = accessTime;
		this.mType = type;
		this.mJson = json;
	}
	
	public String getJson() {
		return mJson;
	}
	
	public void setJson(String json) {
		this.mJson = json;
	}
	
	public String getObjectId() {
		return mObjectId;
	}

	public void setObjectId(String objectId) {
		this.mObjectId = objectId;
	}

	public long getAccessTime() {
		return mAccessTime;
	}

	public void setAccessTime(long accessTime) {
		this.mAccessTime = accessTime;
	}

	public int getType() {
		return mType;
	}

	public void setType(int type) {
		this.mType = type;
	}

	@Override
	public int compareTo(RecentFootAge another) {
		if(another == null) {
			return 0;
		}
		
		if(mAccessTime < another.mAccessTime) {
			return 1;
		} else if(mAccessTime > another.mAccessTime) {
			return -1;
		}
		
		return 0;
	}

}
