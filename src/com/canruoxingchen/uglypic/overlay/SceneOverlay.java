/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

import android.graphics.Rect;
import android.view.View;

/**
 * 
 * 场景蒙层
 * 
 * @author wsf
 *
 */
public class SceneOverlay implements IOverlay {
	
	//是否选中
	private boolean mSelected = false;
	
	//是否包含文案
	private boolean mHasText = false;
	
	//文字左上角在整个场景中的左上角
	private float mTextViewLeft = 0.0f;
	private float mTextViewTop = 0.0f;
	private float mTextViewRight = 0.0f;
	private float mTextViewBottom = 0.0f;
	
	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.BaseOverlay#getView()
	 */
	@Override
	public View getView() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.BaseOverlay#doOverlay()
	 */
	@Override
	public void doOverlay() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.BaseOverlay#resetOverlay()
	 */
	@Override
	public void resetOverlay() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.overlay.BaseOverlay#contains(int, int)
	 */
	@Override
	public boolean contains(int x, int y) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Rect getInitialContentBounds() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setOverlaySelected(boolean selected) {
		mSelected = selected;
	}

	@Override
	public boolean isOverlaySelected() {
		return mSelected;
	}

}
