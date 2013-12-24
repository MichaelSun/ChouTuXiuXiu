package com.canruoxingchen.uglypic.overlay;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.canruoxingchen.uglypic.R;
import com.canruoxingchen.uglypic.view.VerticalSeekBar;

/**
 * 贴图的上下文菜单
 * 
 * @author wsf
 */
public class ImageOverlayContextView extends FrameLayout implements View.OnClickListener, OnSeekBarChangeListener {
	
	private Button mBtnIllumination;
	private Button mBtnContrast;
	private Button mBtnSatuation;
	
	private SeekBar mSbIllumination;
	private SeekBar mSbContrast;
	private SeekBar mSbSatuation;
	
	private IlluminationChangedListener mIlluminationListener;
	private ContrastChangedListener mContrastListener;
	private SatuationChangedListener mSatuationListner;
	
	private SingleParamImageOperation mIllumination;
	private SingleParamImageOperation mContrast;
	private SingleParamImageOperation mSatuation;
	
	public interface IlluminationChangedListener {
		void onIlluminationChanged(ImageOverlayContextView view, int illumination);
	}
	
	public interface ContrastChangedListener {
		void onContrastChanged(ImageOverlayContextView view, int contrast);
	}
	
	public interface SatuationChangedListener {
		void onSatuationChanged(ImageOverlayContextView view, int satuation);
	}
	
	public interface EraseListener {
		void onErased(ImageOverlayContextView view);
	}
	
	public interface ResetListener {
		void onReset(ImageOverlayContextView view);
	}
 	
	public ImageOverlayContextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mIllumination = new IlluminationImageOperation();
		mContrast = new ContrastImageOperation();
		mSatuation = new SatuationImageOperation();
		initView();
	}
	
	private void initView() {
		View view = inflate(getContext(), R.layout.image_overlay_context_menu, null);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		addView(view, params);
		
		mBtnIllumination = (Button) view.findViewById(R.id.image_illumination);
		mBtnContrast = (Button) view.findViewById(R.id.image_contrast);
		mBtnSatuation = (Button) view.findViewById(R.id.image_satuation);
		
		mSbIllumination = (SeekBar) view.findViewById(R.id.image_seekbar_illumination);
		mSbContrast = (SeekBar)  view.findViewById(R.id.image_seekbar_contrast);
		mSbSatuation = (SeekBar) view.findViewById(R.id.image_seekbar_satuation);
		
		mSbIllumination.setMax(mIllumination.getMax());
		mSbIllumination.setProgress(mIllumination.getValue());
		mSbContrast.setMax(mContrast.getMax());
		mSbContrast.setProgress(mContrast.getValue());
		mSbSatuation.setMax(mSatuation.getMax());
		mSbSatuation.setProgress(mSatuation.getValue());
		mSbIllumination.setVisibility(View.VISIBLE);
		
		mBtnIllumination.setOnClickListener(this);
		mBtnContrast.setOnClickListener(this);
		mBtnSatuation.setOnClickListener(this);
		
		mSbIllumination.setOnSeekBarChangeListener(this);
		mSbContrast.setOnSeekBarChangeListener(this);
		mSbSatuation.setOnSeekBarChangeListener(this);
		
	}
	
	public void setIlluminationChangedListener(IlluminationChangedListener listener) {
		this.mIlluminationListener = listener;
	}
	
	public void setContrastChangedListener(ContrastChangedListener listener) {
		this.mContrastListener = listener;
	}
	
	public void setSatuationChangedListener(SatuationChangedListener listener) {
		this.mSatuationListner = listener;
	}

	@Override
	public void onClick(View v) {
		resetAllBtnAndSeekbars();
		switch(v.getId()) {
		case R.id.image_contrast:
			resetAllBtnAndSeekbars();	
			mSbContrast.setVisibility(View.VISIBLE);
			break;
		case R.id.image_illumination:
			resetAllBtnAndSeekbars();
			mSbIllumination.setVisibility(View.VISIBLE);
			break;
		case R.id.image_satuation:
			resetAllBtnAndSeekbars();
			mSbSatuation.setVisibility(View.VISIBLE);
			break;
		}
	}
	
	void reset() {
		mSbContrast.setProgress(ContrastImageOperation.DEFAULT);
		mSbIllumination.setProgress(IlluminationImageOperation.DEFAULT);
		mSbSatuation.setProgress(SatuationImageOperation.DEFAULT);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if(seekBar == mSbIllumination) {
			if(mIlluminationListener != null) {
				mIllumination.setValue(progress);
				mIlluminationListener.onIlluminationChanged(this, seekBar.getProgress());
			}
		} else if(seekBar == mSbContrast) {
			if(mContrastListener != null) {
				mContrast.setValue(progress);
				mContrastListener.onContrastChanged(this, seekBar.getProgress());
			}
		} else if(seekBar == mSbSatuation) {
			if(mSatuationListner != null) {
				mSatuation.setValue(progress);
				mSatuationListner.onSatuationChanged(this, seekBar.getProgress());
			}
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
//		if(seekBar == mSbIllumination) {
//			mSbIllumination.setVisibility(View.GONE);
//			mBtnIllumination.setVisibility(View.VISIBLE);
//		} else if(seekBar == mSbContrast) {
//			mSbContrast.setVisibility(View.GONE);
//			mBtnContrast.setVisibility(View.VISIBLE);
//		} else if(seekBar == mSbSatuation) {
//			mSbSatuation.setVisibility(View.GONE);
//			mBtnSatuation.setVisibility(View.VISIBLE);
//		}
	}
	
	private void resetAllBtnAndSeekbars() {
		mSbIllumination.setVisibility(View.GONE);
		mSbContrast.setVisibility(View.GONE);
		mSbSatuation.setVisibility(View.GONE);
	}

}
