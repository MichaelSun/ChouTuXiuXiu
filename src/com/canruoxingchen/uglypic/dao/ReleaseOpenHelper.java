/**
 * ReleaseOpenHelper.java
 */
package com.canruoxingchen.uglypic.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

import com.canruoxingchen.uglypic.dao.DaoMaster.OpenHelper;

/**
 * @Description 文件描述
 *
 * @author Shaofeng Wang
 *
 * @time 2013-8-14 下午12:22:59
 *
 */
public class ReleaseOpenHelper extends OpenHelper {

	public ReleaseOpenHelper(Context context, String name, CursorFactory factory) {
		super(context, name, factory);
	}

	/* (non-Javadoc)
	 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		//创建不存在的表
		
		DaoMaster.createAllTables(db, true);
		
		//更新修改过的表
		updateTables(db);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		DaoMaster.createAllTables(db, false);
	}
	
	private void dropTables(SQLiteDatabase db, boolean ifExists) {

	}
	
	private void updateTables(SQLiteDatabase db) {
		
	}

}
