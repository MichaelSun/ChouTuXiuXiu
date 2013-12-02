/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * 擦除照片的View
 * 
 * @author wsf
 *
 */
public class EraseImageEditorView extends ImageView implements IEditor {
	public EraseImageEditorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.IEditor#onCancel()
	 */
	@Override
	public void onCancel() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.IEditor#onRegret()
	 */
	@Override
	public void onRegret() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.IEditor#onRedo()
	 */
	@Override
	public void onRedo() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.IEditor#onFinish()
	 */
	@Override
	public void onFinish() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.IEditor#canRegret()
	 */
	@Override
	public boolean canRegret() {
		// TODO Auto-generated method stub
		return false;
	}

}
