/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * @author Shaofeng Wang
 * 
 */
public class TextOverlay extends ObjectOverlay {
	
	private boolean mSelected = false;

	private TextView mTextView = null;
	
	private Context mContext = null;
	
	static class TextOverlayDesc {
		Uri backgroundUri;
		int left;
		int top;
		int right;
		int bottom;
	}

	public TextOverlay(Context context, String text) {
		mTextView = new TextView(context);
		mTextView.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		mTextView.setText(text);
		mTextView.setGravity(Gravity.CENTER);
		mTextView.setTextSize(36);
		mContext = context;
	}
	
	
	@Override
	public View getView() {
		return mTextView;
	}


	private void setDesc(TextOverlayDesc desc) {
		if(desc != null) {
			if(desc.backgroundUri != null) {
				ContentResolver cr = mContext.getContentResolver();
				InputStream is;
				try {
					is = cr.openInputStream(desc.backgroundUri);
					if(is != null) {
						mTextView.setBackgroundDrawable(new BitmapDrawable(mContext.getResources(), is));
					} else {
						mTextView.setBackgroundDrawable(null);
					}
				} catch (FileNotFoundException e) {
					mTextView.setBackgroundDrawable(null)
;				}
			} else {
				mTextView.setBackgroundDrawable(null);
			}
		}
	}

	@Override
	public void setSelected(boolean selected) {
		mSelected = selected;
		if(selected) {
			mTextView.setTextColor(Color.RED);
		} else {
			mTextView.setTextColor(Color.BLACK);
		}
	}

	@Override
	public boolean isSelected() {
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
		mTextView.setTranslationX(dx);
		mTextView.setTranslationY(dy);
	}

	@Override
	public void scale(float sx, float sy) {
		super.scale(sx, sy);
		mTextView.setScaleX(sx);
		mTextView.setScaleY(sy);
	}

	@Override
	public void rotate(float degrees) {
		super.rotate(degrees);
		mTextView.setRotation(degrees);
	}


	@Override
	public Rect getInitialContentBounds() {
		return new Rect(0, 0, mTextView.getWidth(), mTextView.getHeight());
	}

}
