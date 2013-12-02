/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

/**
 * 饱和度
 * 
 * @author wsf
 *
 */
public class SatuationImageOperation extends SingleParamImageOperation{
	
	public static final int MAX = 200;
	public static final int MIN = 0;
	public static final int DEFAULT = 100;
	
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
		this.mValue = value;
	}

}
