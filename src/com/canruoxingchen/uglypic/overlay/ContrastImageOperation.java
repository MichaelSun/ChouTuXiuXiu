/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

/**
 * 调整对比度操作
 * 
 * @author wsf
 *
 */
public class ContrastImageOperation extends SingleParamImageOperation {
	
	public static final int MAX = 106;
	public static final int MIN = -36;
	public static final int DEFAULT = 64;
	public static final int SCALE = 100;
	
	private int mValue = DEFAULT;

	@Override
	public int getMax() {
		return MAX;
	}

	@Override
	public int getMin() {
		return MIN;
	}

	@Override
	public int getValue() {
		return mValue;
	}

	@Override
	public void setValue(int value) {

	}

}
