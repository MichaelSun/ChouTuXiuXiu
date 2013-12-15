/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * @author Shaofeng Wang
 * 
 */
public class TextOverlay extends ObjectOverlay {

	private boolean mSelected = false;

	private TextView mTextView = null;

	private Context mContext = null;

	private float mScaleX = 1.0f;
	private float mScaleY = 1.0f;
	private float mTranslateX = 0.0f;
	private float mTranslateY = 0.0f;
	private float mRotate = 0.0f;

	private int DEFAULT_WIDTH = 150;
	private int DEFAULT_HEIGHT = 80;
	private int DEFAULTI_TEXT_SIZE = 30;

	static class TextOverlayDesc {
		Uri backgroundUri;
		int left;
		int top;
		int right;
		int bottom;
	}

	public TextOverlay(Context context, String text) {
		super();
		mTextView = new TextView(context);
		mTextView.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_CLASS_TEXT);
		mTextView.setGravity(Gravity.CENTER);
		mTextView.setTextSize(DEFAULTI_TEXT_SIZE);
		mTextView.setText(text);

		retrieveDensity();

		int padding = (int) (CONTROL_POINTS_RADIUS * mDensity);
		mTextView.setPadding(padding, padding, padding, padding);
		mContext = context;
	}

	@Override
	protected RelativeLayout.LayoutParams getDefaultParams() {
		retrieveDensity();
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams
				((int)(mDensity * DEFAULT_WIDTH),
				(int)(mDensity * DEFAULT_HEIGHT));
//		params.addRule(RelativeLayout.CENTER_IN_PARENT);
		return params;
	}

	@Override
	public View getView() {
		if(mDensity < -1) {
			retrieveDensity();
			int padding = (int) (CONTROL_POINTS_RADIUS * mDensity);
			mTextView.setPadding(padding, padding, padding, padding);
		}
		return mTextView;
	}

	private void setDesc(TextOverlayDesc desc) {
		if (desc != null) {
			if (desc.backgroundUri != null) {
				ContentResolver cr = mContext.getContentResolver();
				InputStream is;
				try {
					is = cr.openInputStream(desc.backgroundUri);
					if (is != null) {
						mTextView.setBackgroundDrawable(new BitmapDrawable(mContext.getResources(), is));
					} else {
						mTextView.setBackgroundDrawable(null);
					}
				} catch (FileNotFoundException e) {
					mTextView.setBackgroundDrawable(null);
				}
			} else {
				mTextView.setBackgroundDrawable(null);
			}
		}
	}

	@Override
	public void setOverlaySelected(boolean selected) {
		mSelected = selected;
		if (selected) {
			mTextView.setTextColor(Color.RED);
		} else {
			mTextView.setTextColor(Color.BLACK);
		}
	}

	@Override
	public boolean isOverlaySelected() {
		return mSelected;
	}

	@Override
	public Rect getCurrentContentBounds() {
		return new Rect(0, 0, mTextView.getWidth(), mTextView.getHeight());
	}

	@Override
	protected View initContentView() {
		return mTextView;
	}

	@Override
	public View getContextView() {
		return null;
	}

	@Override
	public void translate(int dx, int dy) {
		super.translate(dx, dy);
		mTranslateX += dx;
		mTranslateY += dy;
		mTextView.setTranslationX(mTranslateX);
		mTextView.setTranslationY(mTranslateY);
	}

	@Override
	public void scale(float sx, float sy) {
		super.scale(sx, sy);
		mScaleX *= sx;
		mScaleY *= sy;
		mTextView.setScaleX(mScaleX);
		mTextView.setScaleY(mScaleY);
	}

	@Override
	public void rotate(float degrees) {
		super.rotate(degrees);
		mRotate += degrees;
		mTextView.setRotation(mRotate);
	}

	@Override
	public Rect getInitialContentBounds() {
		retrieveDensity();
//		int left = (int) (mTextView.getLeft() / mDensity);
//		int top = (int) (mTextView.getTop() / mDensity);
//		int right = (int) (mTextView.getRight() / mDensity);
//		int bottom = (int) (mTextView.getBottom() / mDensity);
		Rect rect = new Rect();
		mTextView.getLineBounds(0, rect);
		int left = rect.left;
		int top = rect.top;
		int right = mTextView.getRight();
		int bottom = mTextView.getBottom();
		return new Rect(left, top, right, bottom);
	}

}
