/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

/**
 * 亮度
 * 
 * @author wsf
 *
 */
public class IlluminationImageOperation extends SingleParamImageOperation{
	
	public static int MAX = 60;
	public static int MIN = 30;
	public static int DEFAULT = 30;
	public static int SCALE = 100;
	
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
		mValue = value;
	}

}
