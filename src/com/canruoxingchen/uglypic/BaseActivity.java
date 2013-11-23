/**
 * 
 */
package com.canruoxingchen.uglypic;

import android.app.Activity;

/**
 *	Activity基类，在此类中增加统计等操作
 */
public abstract class BaseActivity extends Activity{
	
	/**
	 * 初始化UI
	 */
	protected abstract void initUI();
	
	/**
	 * 初始化事件
	 */
	protected abstract void initListers();
}
