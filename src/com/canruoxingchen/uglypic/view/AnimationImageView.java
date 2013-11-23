package com.canruoxingchen.uglypic.view;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.ImageView;

import com.canruoxingchen.uglypic.R;

public class AnimationImageView extends ImageView {
	AnimationDrawable animationDrawable;

	public AnimationImageView(Context context) {
		super(context);
		init();
	}

	public AnimationImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public AnimationImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		setBackgroundResource(R.drawable.big_loading_anim_icon);
		animationDrawable = (AnimationDrawable) getBackground();
		getViewTreeObserver().addOnPreDrawListener(opdl);
	}

	OnPreDrawListener opdl = new OnPreDrawListener() {

		@Override
		public boolean onPreDraw() {
			animationDrawable.start();
			getViewTreeObserver().removeOnPreDrawListener(this);
			return true;
		}
	};

	public void stopAnimation() {
		if (animationDrawable != null) {
			if (animationDrawable.isRunning()) {
				animationDrawable.stop();
			}
		}
	}

	public void startAnimation() {
		if (animationDrawable != null) {
			animationDrawable.start();
		}
	}

	@Override
	public void setVisibility(int visibility) {
		super.setVisibility(visibility);
	}

}
