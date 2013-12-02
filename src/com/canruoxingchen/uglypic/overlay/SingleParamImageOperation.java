package com.canruoxingchen.uglypic.overlay;

import android.graphics.Bitmap;

public abstract class SingleParamImageOperation implements IImageOperation {
	
	public abstract int getMax();
	
	public abstract int getMin();
	
	public abstract int getValue();
	
	public abstract void setValue(int value);

	@Override
	public Bitmap operate(Bitmap bitmap) {
		return null;
	}

}
