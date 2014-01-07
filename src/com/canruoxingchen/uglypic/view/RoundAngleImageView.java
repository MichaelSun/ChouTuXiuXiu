package com.canruoxingchen.uglypic.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import com.canruoxingchen.uglypic.R;
import com.canruoxingchen.uglypic.cache.AsyncImageView;
import com.canruoxingchen.uglypic.util.ImageUtils;

public class RoundAngleImageView extends AsyncImageView {
	
	private int roundWidth=15;
	private int roundHeight=15;

	public RoundAngleImageView(Context context) {
		super(context);
		init(context,null);
	}

	public RoundAngleImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public RoundAngleImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}
	
	
	private void init(Context context,AttributeSet attrs){
		if(attrs != null) {   
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RoundAngleImageView); 
			roundWidth = a.getDimensionPixelSize(R.styleable.RoundAngleImageView_roundWidth, roundWidth);
			roundHeight = a.getDimensionPixelSize(R.styleable.RoundAngleImageView_roundHeight, roundHeight);
			a.recycle();
		} else {
			float density = context.getResources().getDisplayMetrics().density;
			density = density > 0.5f ? density : 1.0f;
			roundWidth = (int) (roundWidth*density);
			roundHeight = (int) (roundHeight*density);
		} 
	}
	
	@Override
	protected String getSuffix() {
		
		return "round_angle";
	}
	
	@Override
	protected Bitmap createSpecialBitmap(Bitmap oriBitmap) {
		Bitmap destiBitmap = ImageUtils.getRoundAngleBitmap(oriBitmap,roundWidth,roundHeight);
		return destiBitmap;
	}

}
