/**
 * 
 */
package com.canruoxingchen.uglypic;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

/**
 * 
 * 编辑文本的页面
 * 
 * @author wsf
 *
 */
public class EditTextActivity extends BaseSherlockActivity {
	
	public static String EXTRA_TEXT = "text";
	
	public EditText mEtText;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		initUI();
		initListers();
	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.BaseSherlockActivity#initUI()
	 */
	@Override
	protected void initUI() {
		setContentView(R.layout.edit_text);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);
		
		mEtText = (EditText) findViewById(R.id.edit_text_et); 
	}

	/* (non-Javadoc)
	 * @see com.canruoxingchen.uglypic.BaseSherlockActivity#initListers()
	 */
	@Override
	protected void initListers() {

	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if(item.getItemId() == android.R.id.home) { //直接返回
			finish();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menuItem = menu.add(R.string.edit_text_finish).setIcon(
				R.drawable.ic_launcher);
		menuItem
				.setShowAsAction(com.actionbarsherlock.view.MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menuItem
				.setOnMenuItemClickListener(new com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(
							com.actionbarsherlock.view.MenuItem item) {
						if(mEtText.getText().toString().trim().length() > 0) {
							Intent result = new Intent();
							result.putExtra(EXTRA_TEXT, mEtText.getText().toString().trim());
							setResult(RESULT_OK, result);
						}
						finish();
						return true;
					}
				});
		return super.onCreateOptionsMenu(menu);
	}
	
}
