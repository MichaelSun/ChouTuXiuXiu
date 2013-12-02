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
	
	public static final int MAX = 10;
	public static final int MIN = 0;
	public static final int DEFAULT = 1;
	
	private int mValue = DEFAULT;

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.SingleParamImageOperation#getMax()
	 */
	@Override
	public int getMax() {
		// TODO Auto-generated method stub
		return MAX;
	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.SingleParamImageOperation#getMin()
	 */
	@Override
	public int getMin() {
		// TODO Auto-generated method stub
		return MIN;
	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.SingleParamImageOperation#getValue()
	 */
	@Override
	public int getValue() {
		// TODO Auto-generated method stub
		return mValue;
	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.SingleParamImageOperation#setValue(float)
	 */
	@Override
	public void setValue(int value) {
		// TODO Auto-generated method stub

	}

}
