/**
 * 
 */
package com.canruoxingchen.uglypic;

import com.actionbarsherlock.app.SherlockActivity;

/**
 * 
 * @author wsf
 *
 */
public abstract class BaseSherlockActivity extends SherlockActivity {
	
	/**
	 * 初始化UI
	 */
	protected abstract void initUI();
	
	/**
	 * 初始化事件
	 */
	protected abstract void initListers();
}
