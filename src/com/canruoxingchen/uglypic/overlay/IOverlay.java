/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

import android.graphics.Rect;
import android.view.View;

/**
 *
 * 浮层的接口
 * 
 * @author wsf
 *
 */
public interface IOverlay {
	
	/**
	 * 获取内容区域的边框
	 * @return
	 */
	public Rect getContentViewBounds();
	
	/**
	 * 获取此OverLay对应的view
	 * @return
	 */
	public View getView();
	
	/**
	 * 添加蒙层
	 */
	public void doOverlay();
	
	/**
	 * 重置overlay
	 */
	public void resetOverlay();
	
	/**
	 * 判断坐标为(x,y)的点是否在本overlay之内
	 * @param x
	 * @param y
	 */
	public void contains(int x, int y);
	
	public void setSelected(boolean selected);
	
	public boolean isSelected();
}
