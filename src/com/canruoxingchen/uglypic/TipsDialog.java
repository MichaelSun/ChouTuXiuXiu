/**
 * TipsDialog.java
 */
package com.canruoxingchen.uglypic;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;

import com.canruoxingchen.uglypic.view.AnimationImageView;

/**
 * @Description 自定义提示框
 * 
 */
public class TipsDialog {

	class CustomDialog extends Dialog implements OnClickListener {
		
		AnimationImageView progress;

		CustomDialog(Context context, boolean tipsOnly) {
			super(context, R.style.TipsDialog);

			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setBackgroundDrawableResource(
					android.R.color.transparent);

			if (tipsOnly) {
				setContentView(R.layout.tips_dialog_tips_only);
			} else {
				setContentView(R.layout.tips_dialog);
				progress = (AnimationImageView) findViewById(R.id.progress_icon);
			}
		}

		CustomDialog(Context context, View v) {
			super(context, R.style.TipsDialog);

			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setBackgroundDrawableResource(
					android.R.color.transparent);

			setContentView(v);
			progress = (AnimationImageView) v.findViewById(R.id.progress_icon);

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.view.View.OnClickListener#onClick(android.view.View)
		 */
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub

		}

		public void stopAnimation() {
			if (progress != null) {
				progress.stopAnimation();
			}
		}

		void setImageResource(int id) {
			progress.clearAnimation();
			progress.setImageResource(id);
		}

		void setTips(String tips) {
			TextView tv = (TextView) findViewById(R.id.tips);
			if (!TextUtils.isEmpty(tips)) {
				tv.setText(tips);
			} else {
				tv.setVisibility(View.GONE);
			}
		}
	}

	private static TipsDialog gTipsDialog = new TipsDialog();
	
	private static byte[] sLockObj = new byte[1];

	public static TipsDialog getInstance() {
		if (gTipsDialog == null) {
			synchronized (sLockObj) {
				if(gTipsDialog == null) {
					gTipsDialog = new TipsDialog();
				}
			}
		}

		return gTipsDialog;
	}

	private CustomDialog mDialog;

	private TipsDialog() {
	}

	public void dismiss() {
		if ((mDialog != null) && (mDialog.isShowing())) {
			mDialog.stopAnimation();
			mDialog.dismiss();
			mDialog = null;
		}
	}

	public void setImge(int id) {
		if (mDialog != null) {
			mDialog.setImageResource(id);
		}
	}

	public void setTips(String tips) {
		if (mDialog != null) {
			mDialog.setTips(tips);
		}
	}

	public void showProcess(Activity a, String tips) {
		dismiss();

		mDialog = new CustomDialog(a, false);
		mDialog.setTips(tips);
		mDialog.setCanceledOnTouchOutside(true);
		mDialog.setCancelable(true);
		mDialog.show();
	}

	public void show(Activity a, View contentview) {
		dismiss();

		mDialog = new CustomDialog(a, contentview);
		mDialog.setCanceledOnTouchOutside(false);
		mDialog.setCancelable(false);
		mDialog.show();
	}

	public void showOnlyTips(Activity a, String tips) {
		dismiss();

		mDialog = new CustomDialog(a, true);
		mDialog.setTips(tips);
		mDialog.setCanceledOnTouchOutside(true);
		mDialog.setCancelable(true);
		mDialog.show();
	}

}
