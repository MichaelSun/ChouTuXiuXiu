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
	
	public static int MAX = 255;
	public static int MIN = 0;
	public static int DEFAULT = 127;
	
	private int mValue = DEFAULT;

	@Override
	public int getMax() {
		// TODO Auto-generated method stub
		return MAX;
	}

	@Override
	public int getMin() {
		// TODO Auto-generated method stub
		return MIN;
	}

	@Override
	public int getValue() {
		// TODO Auto-generated method stub
		return mValue;
	}

	@Override
	public void setValue(int value) {
		// TODO Auto-generated method stub
		mValue = value;
	}

}
