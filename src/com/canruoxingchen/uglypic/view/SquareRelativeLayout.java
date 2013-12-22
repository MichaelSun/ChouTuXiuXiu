/**
 * 
 */
package com.canruoxingchen.uglypic.view;

import com.canruoxingchen.uglypic.UglyPicApp;
import com.canruoxingchen.uglypic.util.Logger;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * @author wsf
 * 
 */
public class SquareRelativeLayout extends RelativeLayout {

	public SquareRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onLayout(boolean changed, final int l, final int t, final int r, final int b) {
		super.onLayout(changed, l, t, r, b);

		LOGD("<<<<<<<<< onLayout >>>>>>>>> l: " + l + ", t: " + t + ", r: " + r + ", b: " + b);
		if (r - l != b - t) {
			UglyPicApp.getUiHander().post(new Runnable() {

				@Override
				public void run() {
					int min = Math.min(r - l, b - t);
//					getLayoutParams().width = min;
//					getLayoutParams().height = min;
					LOGD("<<<<<<<<< onLayout >>>>>>>>> min: " + min);
				}
			});
		}
	}

	private void LOGD(String logMe) {
		Logger.d("SquareRelativeLayout", logMe);
	}
}

