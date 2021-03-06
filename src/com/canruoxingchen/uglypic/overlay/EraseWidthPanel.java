/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import com.canruoxingchen.uglypic.R;
import com.canruoxingchen.uglypic.UglyPicApp;

/**
 * 
 * 选择擦除宽度的View
 * 
 * @author Shaofeng Wang
 * 
 */
public class EraseWidthPanel extends LinearLayout {

	private static final int[] ERASER_WIDTH = new int[] { 8, 10, 12, 15, 20, 26, 32 };
	private List<ImageView> mImgViews = new ArrayList<ImageView>();

	private boolean mInitialized = false;

	interface OnWidthSelectedListener {
		void onWidthSelected(int width);
	}

	private OnWidthSelectedListener mListener;

	public EraseWidthPanel(Context context) {
		super(context);
	}

	public void setOnWidthSelectedListener(OnWidthSelectedListener listener) {
		this.mListener = listener;
	}

	public int getDefaultWidth() {
		return ERASER_WIDTH[3];
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		UglyPicApp.getUiHander().post(new Runnable() {

			@Override
			public void run() {
				init();
			}
		});
	}

	private void init() {
		setOrientation(LinearLayout.HORIZONTAL);
		WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(dm);

		if (mInitialized) {
			return;
		}
		mInitialized = true;
		setBackgroundColor(getResources().getColor(R.color.default_background));
		int total = 0;

		for (final int width : ERASER_WIDTH) {
			total += width;
		}
		float itemWidth = getWidth() * 1.0f / (2 * total);
		float PADDING = (getWidth() / 28.0f);
		for (int i = 0; i < ERASER_WIDTH.length; ++i) {
			final int width = ERASER_WIDTH[i];
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
					LayoutParams.MATCH_PARENT);
			params.width = (int) (itemWidth * width) + (int) (PADDING * 2);
			params.height = params.width;
			params.gravity = Gravity.CENTER_VERTICAL;
			final ImageView iv = new ImageView(getContext());
			iv.setPadding((int) (PADDING), 0, (int) (PADDING), 0);
			iv.setImageResource(R.drawable.erase_width_bg);
			iv.setBackgroundDrawable(null);
			iv.setScaleType(ScaleType.FIT_CENTER);
			addView(iv, params);
			iv.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					for (ImageView imgView : mImgViews) {
						imgView.setSelected(false);
					}
					iv.setSelected(true);
					if (mListener != null) {
						mListener.onWidthSelected(width);
					}
				}
			});
			mImgViews.add(iv);
		}
		
		mImgViews.get(3).setSelected(true);
	}
}
