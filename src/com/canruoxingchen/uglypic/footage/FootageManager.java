/**
 * 
 */
package com.canruoxingchen.uglypic.footage;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.net.Uri;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.GetDataCallback;
import com.canruoxingchen.uglypic.MessageCenter;
import com.canruoxingchen.uglypic.R;
import com.canruoxingchen.uglypic.concurrent.ThreadPoolManager;
import com.canruoxingchen.uglypic.dao.DaoManager;
import com.canruoxingchen.uglypic.dao.FootAgeTypeDao;
import com.canruoxingchen.uglypic.dao.Footage;
import com.canruoxingchen.uglypic.dao.FootageDao;
import com.canruoxingchen.uglypic.util.FileUtils;
import com.canruoxingchen.uglypic.util.Logger;

/**
 * @author wsf
 * 
 */
public class FootageManager {

	public static final int MSG_LOAD_FOOTAGE_TYPES_SUCCESS = R.id.msg_load_footage_types_success;
	public static final int MSG_LOAD_FOOTAGE_TYPES_FAILURE = R.id.msg_load_footage_types_failure;
	public static final int MSG_LOAD_LOCAL_FOOTAGE_TYPES_SUCCESS = R.id.msg_load_local_footage_types_success;
	public static final int MSG_LOAD_LOCAL_FOOTAGE_TYPES_FAILURE = R.id.msg_load_local_footage_types_failure;
	public static final int MSG_LOAD_FOOTAGE_SUCCESS = R.id.msg_load_footage_success;
	public static final int MSG_LOAD_FOOTAGE_FAILURE = R.id.msg_load_footage_failure;
	public static final int MSG_LOAD_LOCAL_FOOTAGE_SUCCESS = R.id.msg_load_local_footage_success;
	public static final int MSG_LOAD_LOCAL_FOOTAGE_FAILURE = R.id.msg_load_local_footage_failure;
	public static final int MSG_LOAD_FOOTAGE_ICON_SUCCESS = R.id.msg_load_footage_icon_success;
	public static final int MSG_LOAD_FOOTAGE_ICON_FAILURE = R.id.msg_load_footage_icon_failure;

	private static FootageManager sIntance = null;
	private static final byte[] sLock = new byte[0];
	private Context mContext;

	private FootageManager(Context context) {
		this.mContext = context;
	}

	public static FootageManager getInstance(Context context) {
		if (sIntance == null) {
			synchronized (sLock) {
				if (sIntance == null) {
					sIntance = new FootageManager(context.getApplicationContext());
				}
			}
		}
		return sIntance;
	}

	/**
	 * 从本地加载素材类型列表
	 */
	public void loadFootageTypeFromLocal() {
		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				FootAgeTypeDao fatDao = DaoManager.getInstance(mContext).getDao(FootAgeTypeDao.class);
				List<com.canruoxingchen.uglypic.dao.FootAgeType> footAgeTypes = fatDao.loadAll();
				List<FootAgeType> types = new ArrayList<FootAgeType>();
				LOGD("<<<<<<<<<<localFootageTypes:>>>>>>>>>>>>" + footAgeTypes);
				if (footAgeTypes != null) {
					for (com.canruoxingchen.uglypic.dao.FootAgeType type : footAgeTypes) {
						types.add(new FootAgeType(type.getObjectId(), type.getTypeName(), type.getOldName(), type
								.getIsDefault(), type.getOrderNum()));
					}
				}
				if (types.size() > 0) {
					Collections.sort(types);
					MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_LOCAL_FOOTAGE_TYPES_SUCCESS, 0, 0,
							types);
				} else {
					MessageCenter.getInstance(mContext)
							.notifyHandlers(MSG_LOAD_LOCAL_FOOTAGE_TYPES_FAILURE, 0, 0, null);
				}
			}
		});
	}

	/**
	 * 从server加载素材类型列表
	 */
	public void loadFootageTypeFromServer() {
		AVQuery<AVObject> query = AVQuery.getQuery(FootAgeType.CLASS_NAME);

		query.findInBackground(new FindCallback<AVObject>() {

			@Override
			public void done(List<AVObject> object, AVException e) {
				if (e == null && object != null) {
					LOGD(">>>>>> loadFootageType >>>>>> object size: " + object.size());
					List<FootAgeType> types = new ArrayList<FootAgeType>();
					FootAgeTypeDao fatDao = DaoManager.getInstance(mContext).getDao(FootAgeTypeDao.class);
					List<com.canruoxingchen.uglypic.dao.FootAgeType> dbTypes = new ArrayList<com.canruoxingchen.uglypic.dao.FootAgeType>();
					for (AVObject avo : object) {
						LOGD(">>>>>> loadFootageType >>>>>> AVObject: objectId=" + avo.getObjectId());
						FootAgeType type = new FootAgeType(avo.getObjectId(), avo
								.getString(FootAgeType.COLUMN_TYPE_NAME), avo.getString(FootAgeType.COLUMN_OLD_NAME),
								Integer.parseInt(avo.getString(FootAgeType.COLUMN_IS_DEFAULT)), Integer.parseInt(avo
										.getString(FootAgeType.COLUMN_ORDER_NUM)));
						LOGD(">>>>>> loadFootageType >>>>>> " + type.toString());
						// 结果存入数据库
						fatDao.insertOrReplaceInTx(new com.canruoxingchen.uglypic.dao.FootAgeType(type.getObjectId(),
								type.getTypeName(), type.getOldName(), type.getIsDefault(), type.getOrderNum()));
						types.add(type);
						dbTypes.add(new com.canruoxingchen.uglypic.dao.FootAgeType(type.getObjectId(), type
								.getTypeName(), type.getOldName(), type.getIsDefault(), type.getOrderNum()));
					}
					fatDao.insertOrReplaceInTx(dbTypes);
					Collections.sort(types);
					MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_FOOTAGE_TYPES_SUCCESS, 0, 0, types);
				} else {
					LOGD(">>>>>> loadFootageType >>>>>> exception= " + e);
					MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_FOOTAGE_TYPES_FAILURE, 0, 0, null);
				}
			}
		});
	}

	/**
	 * 加载素材列表
	 * 
	 * @param objectId
	 */
	public void loadLocalFootages(final String objectId) {
		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				FootageDao footageDao = DaoManager.getInstance(mContext).getDao(FootageDao.class);
				List<com.canruoxingchen.uglypic.dao.Footage> dbFootages = footageDao.loadAll();
				List<FootAge> footages = new ArrayList<FootAge>();
				LOGD("<<<<<<<<<<< local footages: >>>>>>>>>>>" + dbFootages);
				if (dbFootages != null) {
					for (com.canruoxingchen.uglypic.dao.Footage f : dbFootages) {
						if (f != null && f.getObjectId().equals(objectId)) {
							footages.add(new FootAge(f.getObjectId(), f.getFootageParentId(), f.getFootageIcon(), f
									.getFootageIconName(), f.getFootageOrderNum()));
						}
					}
				}
				if (footages.size() > 0) {
					Collections.sort(footages);
					MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_LOCAL_FOOTAGE_SUCCESS, 0, 0, footages);
				} else {
					MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_LOCAL_FOOTAGE_FAILURE, 0, 0, null);
				}
			}
		});
	}

	public void loadFootagesFromServer(final String objectId) {
		AVQuery<AVObject> query = AVQuery.getQuery(FootAge.CLASS_NAME);
		query.whereEqualTo(FootAge.COLUMN_FOOTAGE_PARENT_ID, objectId);
		query.findInBackground(new FindCallback<AVObject>() {

			@Override
			public void done(List<AVObject> object, AVException e) {
				if (e == null && object != null) {
					List<FootAge> footages = new ArrayList<FootAge>();
					final FootageDao footageDao = DaoManager.getInstance(mContext).getDao(FootageDao.class);
					List<com.canruoxingchen.uglypic.dao.Footage> dbFootages = new ArrayList<Footage>();

					LOGD(">>>>>> loadFootageType >>>>>> AVObject Count =" + object.size());
					for (AVObject avo : object) {
						LOGD(">>>>>> loadFootageType >>>>>> AVObject: objectId=" + avo.getObjectId());
						final FootAge footage = new FootAge(avo.getObjectId(), avo
								.getString(FootAge.COLUMN_FOOTAGE_PARENT_ID), avo
								.getString(FootAge.COLUMN_FOOTAGE_ICON), avo
								.getString(FootAge.COLUMN_FOOTAGE_ICON_NAME), Integer.parseInt(avo
								.getString(FootAge.COLUMN_FOOTAGE_ORDER_NUM)));
						LOGD(">>>>>> loadFootageType >>>>>> " + footage.toString());
						// 结果存入数据库
						footages.add(footage);
						com.canruoxingchen.uglypic.dao.Footage f = footageDao.load(avo.getObjectId());
						dbFootages.add(new Footage(avo.getObjectId(), f.getFootageIcon(), avo
								.getString(FootAge.COLUMN_FOOTAGE_ICON_NAME), Integer.parseInt(avo
								.getString(FootAge.COLUMN_FOOTAGE_ORDER_NUM)), avo
								.getString(FootAge.COLUMN_FOOTAGE_PARENT_ID)));

					}
					footageDao.insertOrReplaceInTx(dbFootages);
					Collections.sort(footages);
					MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_FOOTAGE_SUCCESS, 0, 0, footages);

					for (final AVObject avo : object) {
						// 如果尚未下载
						
						loadIconFile(avo, FootAge.COLUMN_FOOTAGE_ICON, avo.getString(FootAge.COLUMN_FOOTAGE_ICON_NAME) + ".png",
								new ILoadIconFileListener() {

									@Override
									public void onLoadFailed() {
										MessageCenter.getInstance(mContext).notifyHandlers(
												MSG_LOAD_FOOTAGE_ICON_FAILURE, 0, 0, null);
									}

									@Override
									public void onFileLoaded(Uri uri) {
										com.canruoxingchen.uglypic.dao.Footage footage = new Footage(avo.getObjectId(),
												uri.toString(), avo.getString(FootAge.COLUMN_FOOTAGE_ICON_NAME),
												Integer.parseInt(avo.getString(FootAge.COLUMN_FOOTAGE_ORDER_NUM)), avo
														.getString(FootAge.COLUMN_FOOTAGE_PARENT_ID));
										footageDao.insertOrReplace(footage);
										footage.setFootageIcon(uri.toString());
										FootAge fAge = new FootAge(footage.getObjectId(), footage.getFootageParentId(),
												footage.getFootageIcon(), footage.getFootageIconName(), footage
														.getFootageOrderNum());
										// 通知更新UI
										MessageCenter.getInstance(mContext).notifyHandlers(
												MSG_LOAD_FOOTAGE_ICON_SUCCESS, 0, 0, fAge);
									}
								});
					}
				} else {
					LOGD(">>>>>> loadFootageType >>>>>> " + e.getMessage());
					MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_FOOTAGE_FAILURE, 0, 0, null);
				}
			}
		});
	}

	private void LOGD(String logMe) {
		Logger.d(FootageManager.class.getSimpleName(), logMe);
	}

	public interface ILoadIconFileListener {
		void onFileLoaded(Uri uri);

		void onLoadFailed();
	}

	public void loadIconFile(AVObject avObject, final String key, final String name, final ILoadIconFileListener listener) {
		AVFile avFile = avObject.getAVFile(key);
		avFile.getDataInBackground(new GetDataCallback() {

			@Override
			public void done(byte[] data, AVException e) {
				if (e == null && data != null) {
					LOGD("<<<<<<<<<<<<<< load icon file success >>>>>>>>>>>>>>");
					String filePath = FileUtils.createSdCardFile(name);
					LOGD("<<<<<<<<<<<<<< load icon file success >>>>>>>>>>>>>> save to " + filePath);
					if (filePath == null) {
						if (listener != null) {
							listener.onLoadFailed();
						}
					} else {
						DataOutputStream dos = null;
						try {
							dos = new DataOutputStream(new FileOutputStream(filePath));
							dos.write(data);
							if (listener != null) {
								listener.onFileLoaded(Uri.fromFile(new File(filePath)));
							}
						} catch (FileNotFoundException e1) {
							if (listener != null) {
								listener.onLoadFailed();
							}
						} catch (IOException e1) {
							if (listener != null) {
								listener.onLoadFailed();
							}
						} finally {
							FileUtils.closeQuietly(dos);
						}
					}
				} else {
					if (listener != null) {
						listener.onLoadFailed();
					}
				}
			}

		});
	}
}
