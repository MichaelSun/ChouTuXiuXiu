/**
 * 
 */
package com.canruoxingchen.uglypic.overlay;

/**
 * 编辑器的接口
 * 
 * @author wsf
 *
 */
public interface IEditor {
	
	/**
	 * 取消编辑
	 */
	public void onCancel();
	
	/**
	 * 回退一步
	 */
	public void onRegret();
	
	/**
	 * 前进一步
	 */
	public void onRedo();
	
	/**
	 * 编辑完成
	 */
	public void onFinish();
	
	/**
	 * 是否可以回退
	 * @return
	 */
	public boolean canRegret();
	
	/**
	 * 是否还可以继续回退
	 * @return
	 */
	public boolean hasMoreRegret();
	
	/**
	 * 是否还可以继续redo
	 * @return
	 */
	public boolean hasMoreRedo();
}
