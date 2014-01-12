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
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.GetDataCallback;
import com.canruoxingchen.uglypic.MessageCenter;
import com.canruoxingchen.uglypic.R;
import com.canruoxingchen.uglypic.UglyPicApp;
import com.canruoxingchen.uglypic.concurrent.ThreadPoolManager;
import com.canruoxingchen.uglypic.dao.DaoManager;
import com.canruoxingchen.uglypic.dao.FootAgeTypeDao;
import com.canruoxingchen.uglypic.dao.Footage;
import com.canruoxingchen.uglypic.dao.FootageDao;
import com.canruoxingchen.uglypic.dao.NetSenceDao;
import com.canruoxingchen.uglypic.dao.RecentFootage;
import com.canruoxingchen.uglypic.dao.RecentFootageDao;
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
	public static final int MSG_LOAD_SCENES_SUCCESS = R.id.msg_load_scenes_success;
	public static final int MSG_LOAD_SCENES_FAILURE = R.id.msg_load_scenes_failure;
	public static final int MSG_LOAD_LOCAL_SCENES_SUCCESS = R.id.msg_load_local_scenes_success;
	public static final int MSG_LOAD_LOCAL_SCENES_FAILURE = R.id.msg_load_local_scenes_failure;
	public static final int MSG_LOAD_RECENT_FOOTAGE_SUCCESS = R.id.msg_load_recent_footage_success;
	public static final int MSG_LOAD_RECENT_FOOTAGE_FAILURE = R.id.msg_load_recent_footage_failure;

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

	public void saveRecentFootage(final FootAge footage) {
		if (footage != null) {
			String json = JSON.toJSONString(footage);
			final RecentFootage recent = new RecentFootage(footage.getObjectId(), System.currentTimeMillis(),
					FootAgeType.TYPE_IMAGE, json);
			ThreadPoolManager.getInstance().execute(new Runnable() {

				@Override
				public void run() {
					com.canruoxingchen.uglypic.dao.RecentFootageDao recentDao = DaoManager.getInstance(mContext)
							.getDao(RecentFootageDao.class);
					recentDao.insertOrReplaceInTx(recent);
				}
			});
		}
	}

	public void saveRecentFootage(final NetSence netScene) {
		if (netScene != null) {
			String json = JSON.toJSONString(netScene);
			final RecentFootage recent = new RecentFootage(netScene.getObjectId(), System.currentTimeMillis(),
					FootAgeType.TYPE_SCENE, json);
			ThreadPoolManager.getInstance().execute(new Runnable() {

				@Override
				public void run() {
					com.canruoxingchen.uglypic.dao.RecentFootageDao recentDao = DaoManager.getInstance(mContext)
							.getDao(RecentFootageDao.class);
					recentDao.insertOrReplaceInTx(recent);
				}
			});
		}
	}

	public void loadRecentFootages() {
		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				com.canruoxingchen.uglypic.dao.RecentFootageDao recentDao = DaoManager.getInstance(mContext).getDao(
						RecentFootageDao.class);
				List<RecentFootage> footages = recentDao.loadAll();
				List<RecentFootAge> recentFootages = new ArrayList<RecentFootAge>();
				if (footages != null) {
					for (RecentFootage rf : footages) {
						recentFootages.add(new RecentFootAge(rf.getObjectId(), rf.getAccessTime(), rf.getFootageType(),
								rf.getJson()));
					}
				}

				Collections.sort(recentFootages);

				if (recentFootages.size() > 20) {
					recentFootages = recentFootages.subList(0, 20);
				}
				MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_RECENT_FOOTAGE_SUCCESS, 0, 0,
						recentFootages);
			}
		});
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
								.getIsDefault(), type.getOrderNum(), type.getTypeTarget()));
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
										.getString(FootAgeType.COLUMN_ORDER_NUM)), Integer.parseInt(avo
										.getString(FootAgeType.COLUMN_TYPE_TARGET)));
						LOGD(">>>>>> loadFootageType >>>>>> " + type.toString());
						// 结果存入数据库
						fatDao.insertOrReplaceInTx(new com.canruoxingchen.uglypic.dao.FootAgeType(type.getObjectId(),
								type.getTypeName(), type.getOldName(), type.getIsDefault(), type.getOrderNum(), type
										.getTypeTarget()));
						types.add(type);
						dbTypes.add(new com.canruoxingchen.uglypic.dao.FootAgeType(type.getObjectId(), type
								.getTypeName(), type.getOldName(), type.getIsDefault(), type.getOrderNum(), type
								.getTypeTarget()));
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
						if (f != null && f.getFootageParentId().equals(objectId)) {
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

	private static class FootageAVOComparator implements Comparator<AVObject> {

		@Override
		public int compare(AVObject lhs, AVObject rhs) {
			if (lhs == null || rhs == null) {
				return 0;
			}

			int lOrderNum = 0;
			int rOrderNum = 0;
			if (lhs.containsKey(FootAge.COLUMN_FOOTAGE_ORDER_NUM)) {
				lOrderNum = Integer.parseInt(lhs.getString(FootAge.COLUMN_FOOTAGE_ORDER_NUM));
			}
			if (rhs.containsKey(FootAge.COLUMN_FOOTAGE_ORDER_NUM)) {
				rOrderNum = Integer.parseInt(lhs.getString(FootAge.COLUMN_FOOTAGE_ORDER_NUM));
			}

			if (lOrderNum > rOrderNum) {
				return 1;
			} else if (lOrderNum < rOrderNum) {
				return -1;
			}

			return 0;
		}

	}

	private static class NetSceneAVOComparator implements Comparator<AVObject> {

		@Override
		public int compare(AVObject lhs, AVObject rhs) {
			if (lhs == null || rhs == null) {
				return 0;
			}

			int lOrderNum = 0;
			int rOrderNum = 0;
			if (lhs.containsKey(NetSence.COLUMN_SENCE_ORDER_NUM)) {
				lOrderNum = Integer.parseInt(lhs.getString(NetSence.COLUMN_SENCE_ORDER_NUM));
			}
			if (rhs.containsKey(NetSence.COLUMN_SENCE_ORDER_NUM)) {
				rOrderNum = Integer.parseInt(lhs.getString(NetSence.COLUMN_SENCE_ORDER_NUM));
			}

			if (lOrderNum > rOrderNum) {
				return 1;
			} else if (lOrderNum < rOrderNum) {
				return -1;
			}

			return 0;
		}

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
					List<AVObject> noIconObjs = new ArrayList<AVObject>();
					for (AVObject avo : object) {
						LOGD(">>>>>> loadFootageType >>>>>> AVObject: objectId=" + avo.getObjectId());
						final FootAge footage = new FootAge(avo.getObjectId(), avo
								.getString(FootAge.COLUMN_FOOTAGE_PARENT_ID), avo
								.getString(FootAge.COLUMN_FOOTAGE_ICON), avo
								.getString(FootAge.COLUMN_FOOTAGE_ICON_NAME), Integer.parseInt(avo
								.getString(FootAge.COLUMN_FOOTAGE_ORDER_NUM)));
						LOGD(">>>>>> loadFootageType >>>>>> " + footage.toString());
						// 结果存入数据库
						footage.setAVObject(avo);
						footages.add(footage);
						com.canruoxingchen.uglypic.dao.Footage f = footageDao.load(avo.getObjectId());
						String iconPath = f == null ? "" : f.getFootageIcon();
						
						AVFile avFile = avo.getAVFile( FootAge.COLUMN_FOOTAGE_ICON);
						avFile.getUrl();
						dbFootages.add(new Footage(avo.getObjectId(), avFile.getUrl(), footage.getIconName(), footage
								.getOrderNum(), footage.getParentId()));
						if (TextUtils.isEmpty(iconPath)) {
							noIconObjs.add(avo);
						}
					}
					footageDao.insertOrReplaceInTx(dbFootages);
					Collections.sort(footages);
					Collections.sort(noIconObjs, new FootageAVOComparator());
					MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_FOOTAGE_SUCCESS, 0, 0, footages);

//					for (final AVObject avo : noIconObjs) {
//						loadFootageIcon(avo);
//					}
				} else {
					LOGD(">>>>>> loadFootages >>>>>> errorcode=" + e.getCode() + ", message=" + e.getMessage());
					MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_FOOTAGE_FAILURE, 0, 0, null);
				}
			}
		});
	}

	public void loadFootageIcon(final AVObject avo) {
		// 如果尚未下载
		loadIconFile(avo, FootAge.COLUMN_FOOTAGE_ICON, avo.getString(FootAge.COLUMN_FOOTAGE_ICON_NAME) + ".png",
				new ILoadIconFileListener() {

					@Override
					public void onLoadFailed() {
						MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_FOOTAGE_ICON_FAILURE, 0, 0, null);
					}

					@Override
					public void onFileLoaded(Uri uri) {
						FootageDao footageDao = DaoManager.getInstance(UglyPicApp.getAppExContext()).getDao(
								FootageDao.class);
//						com.canruoxingchen.uglypic.dao.Footage footage = new Footage(avo.getObjectId(), uri.toString(),
//								avo.getString(FootAge.COLUMN_FOOTAGE_ICON_NAME), Integer.parseInt(avo
//										.getString(FootAge.COLUMN_FOOTAGE_ORDER_NUM)), avo
//										.getString(FootAge.COLUMN_FOOTAGE_PARENT_ID));
						com.canruoxingchen.uglypic.dao.Footage footage = footageDao.load(avo.getObjectId());
						if(footage == null) {
							return;
						}
						footage.setFootageIcon(uri == null ? "" : uri.toString());
						footageDao.insertOrReplace(footage);
						footage.setFootageIcon(uri.toString());
						FootAge fAge = new FootAge(footage.getObjectId(), footage.getFootageParentId(), footage
								.getFootageIcon(), footage.getFootageIconName(), footage.getFootageOrderNum());
						// 通知更新UI
						MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_FOOTAGE_ICON_SUCCESS, 0, 0, fAge);
					}
				});
	}

	/**
	 * 加载素材列表
	 * 
	 * @param objectId
	 */
	public void loadLocalScenes(final String objectId) {
		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				NetSenceDao sceneDao = DaoManager.getInstance(mContext).getDao(NetSenceDao.class);
				List<com.canruoxingchen.uglypic.dao.NetSence> dbScenes = sceneDao.loadAll();
				List<NetSence> scenes = new ArrayList<NetSence>();
				LOGD("<<<<<<<<<<< local footages: >>>>>>>>>>>" + dbScenes);
				if (dbScenes != null) {
					for (com.canruoxingchen.uglypic.dao.NetSence scene : dbScenes) {
						if (scene != null && scene.getSenceParentId().equals(objectId)) {
							scenes.add(new NetSence(scene.getObjectId(), scene.getSenceNetIcon(), scene
									.getSenceParentId(), scene.getSenceOrderNum(), scene.getSenceName(), scene
									.getSenceDescribe(), scene.getInputContent(), scene.getInputRect(), scene
									.getInputFontName(), scene.getInputFontSize(), scene.getInputFontColor(), scene
									.getInputFontAlignment(), scene.getTimeRect(), scene.getTimeFontName(), scene
									.getTimeFontSize(), scene.getTimeFontColor(), scene.getTimeFontAlignment()));
						}
					}
				}
				if (scenes.size() > 0) {
					Collections.sort(scenes);
					MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_LOCAL_SCENES_SUCCESS, 0, 0, scenes);
				} else {
					MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_LOCAL_SCENES_FAILURE, 0, 0, null);
				}
			}
		});
	}

	private String numFormat(String str) {
		if (!TextUtils.isEmpty(str)) {
			return str.replace("#", "0x");
		}
		return "0";
	}

	private int parseColor(String str) {
		if (TextUtils.isEmpty(str)) {
			return 0;
		}
		return Integer.parseInt(str.substring(1), 16);
	}

	public void loadScenesFromServer(final String objectId) {
		AVQuery<AVObject> query = AVQuery.getQuery(NetSence.CLASS_NAME);
		query.whereEqualTo(NetSence.COLUMN_SENCE_PARENT_ID, objectId);
		query.findInBackground(new FindCallback<AVObject>() {

			@Override
			public void done(List<AVObject> object, AVException e) {
				if (e == null && object != null) {
					List<NetSence> scenes = new ArrayList<NetSence>();
					final NetSenceDao netSenceDao = DaoManager.getInstance(mContext).getDao(NetSenceDao.class);
					List<com.canruoxingchen.uglypic.dao.NetSence> dbScenes = new ArrayList<com.canruoxingchen.uglypic.dao.NetSence>();

					LOGD(">>>>>> loadNetScenes >>>>>> AVObject Count =" + object.size());
					List<AVObject> noIconObjs = new ArrayList<AVObject>();
					for (AVObject avo : object) {
						LOGD(">>>>>> loadNetScenes >>>>>> AVObject:" + avo);

						final NetSence netScene = new NetSence(avo.getObjectId(), avo
								.getString(NetSence.COLUMN_SENCE_NET_ICON), avo
								.getString(NetSence.COLUMN_SENCE_PARENT_ID), Integer.parseInt(avo
								.getString(NetSence.COLUMN_SENCE_ORDER_NUM)),
								avo.getString(NetSence.COLUMN_SENCE_NAME), avo
										.getString(NetSence.COLUMN_SENCE_DESCRIBE), avo
										.getString(NetSence.COLUMN_INPUT_CONTENT), avo
										.getString(NetSence.COLUMN_INPUT_RECT), avo
										.getString(NetSence.COLUMN_INPUT_FONT_NAME), Integer.parseInt(numFormat(avo
										.getString(NetSence.COLUMN_INPUT_FONT_SIZE))), parseColor(avo
										.getString(NetSence.COLUMN_INPUT_FONT_COLOR)), Integer.parseInt(numFormat(avo
										.getString(NetSence.COLUMN_INPUT_FONT_ALIGNMENT))), avo
										.getString(NetSence.COLUMN_TIME_RECT), avo
										.getString(NetSence.COLUMN_TIME_FONT_NAME), Integer.parseInt(numFormat(avo
										.getString(NetSence.COLUMN_TIME_FONT_SIZE))), parseColor(avo
										.getString(NetSence.COLUMN_TIME_FONT_COLOR)), Integer.parseInt(numFormat(avo
										.getString(NetSence.COLUMN_TIME_FONT_ALIGNMENT))));
						// 结果存入数据库
						scenes.add(netScene);
						netScene.setAVObject(avo);
						AVFile avFile = avo.getAVFile(NetSence.COLUMN_SENCE_NET_ICON);
						com.canruoxingchen.uglypic.dao.NetSence ns = netSenceDao.load(avo.getObjectId());
						String iconPath = ns == null ? "" : ns.getSenceNetIcon();
						dbScenes.add(new com.canruoxingchen.uglypic.dao.NetSence(netScene.getObjectId(), avFile.getUrl(),
								netScene.getSenceParentId(), netScene.getSenceOrderNum(), netScene.getSenceName(),
								netScene.getSenceDescribe(), netScene.getInputContent(), netScene.getInputRect(),
								netScene.getInputFontName(), netScene.getInputFontSize(), netScene.getInputFontColor(),
								netScene.getInputFontAlignment(), netScene.getTimeRect(), netScene.getTimeFontName(),
								netScene.getTimeFontSize(), netScene.getTimeFontColor(), netScene
										.getTimeFontAlignment()));
						if (TextUtils.isEmpty(iconPath)) {
							noIconObjs.add(avo);
						}
					}
					netSenceDao.insertOrReplaceInTx(dbScenes);
					Collections.sort(scenes);
					Collections.sort(noIconObjs, new NetSceneAVOComparator());
					MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_SCENES_SUCCESS, 0, 0, scenes);

//					for (final AVObject avo : noIconObjs) {
//						// 如果尚未下载
//						loadSceneIcon(avo);
//					}
				} else {
					LOGD(">>>>>> loadNetScenes >>>>>> " + e.getMessage());
					MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_SCENES_FAILURE, 0, 0, null);
				}
			}
		});
	}

	public void loadSceneIcon(final AVObject avo) {
		loadIconFile(avo, NetSence.COLUMN_SENCE_NET_ICON, avo.getString(NetSence.COLUMN_SENCE_NAME) + ".png",
				new ILoadIconFileListener() {

					@Override
					public void onLoadFailed() {
						MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_FOOTAGE_ICON_FAILURE, 0, 0, null);
					}

					@Override
					public void onFileLoaded(Uri uri) {
//						com.canruoxingchen.uglypic.dao.NetSence ns = new com.canruoxingchen.uglypic.dao.NetSence(avo
//								.getObjectId(), uri.toString(), avo.getString(NetSence.COLUMN_SENCE_PARENT_ID), Integer
//								.parseInt(avo.getString(NetSence.COLUMN_SENCE_ORDER_NUM)), avo
//								.getString(NetSence.COLUMN_SENCE_NAME), avo.getString(NetSence.COLUMN_SENCE_DESCRIBE),
//								avo.getString(NetSence.COLUMN_INPUT_CONTENT),
//								avo.getString(NetSence.COLUMN_INPUT_RECT), avo
//										.getString(NetSence.COLUMN_INPUT_FONT_NAME), Integer.parseInt(numFormat(avo
//										.getString(NetSence.COLUMN_INPUT_FONT_SIZE))), parseColor(avo
//										.getString(NetSence.COLUMN_INPUT_FONT_COLOR)), Integer.parseInt(numFormat(avo
//										.getString(NetSence.COLUMN_INPUT_FONT_ALIGNMENT))), avo
//										.getString(NetSence.COLUMN_TIME_RECT), avo
//										.getString(NetSence.COLUMN_TIME_FONT_NAME), Integer.parseInt(numFormat(avo
//										.getString(NetSence.COLUMN_TIME_FONT_SIZE))), parseColor(avo
//										.getString(NetSence.COLUMN_TIME_FONT_COLOR)), Integer.parseInt(numFormat(avo
//										.getString(NetSence.COLUMN_TIME_FONT_ALIGNMENT))));
						final NetSenceDao netSenceDao = DaoManager.getInstance(mContext).getDao(NetSenceDao.class);
						com.canruoxingchen.uglypic.dao.NetSence ns = netSenceDao.load(avo.getObjectId());
						if(ns == null) {
							return;
						}
						ns.setSenceNetIcon(uri == null ? "" : uri.toString());
						netSenceDao.insertOrReplace(ns);
						NetSence netScene = new NetSence(avo.getObjectId(), ns.getSenceNetIcon(), ns.getSenceParentId(), 
								ns.getSenceOrderNum(), ns.getSenceName(), ns.getSenceDescribe(), ns.getInputContent(),
								ns.getInputRect(), ns.getInputFontName(), ns.getInputFontSize(),
								ns.getInputFontColor(), ns.getInputFontAlignment(), ns.getTimeRect(),
								ns.getTimeRect(), ns.getTimeFontSize(), ns.getTimeFontColor(), ns.getTimeFontAlignment());
						// 通知更新UI
						MessageCenter.getInstance(mContext).notifyHandlers(MSG_LOAD_FOOTAGE_ICON_SUCCESS, 0, 0,
								netScene);
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

	public void loadIconFile(AVObject avObject, final String key, final String name,
			final ILoadIconFileListener listener) {
		AVFile avFile = avObject.getAVFile(key);
		if(avFile == null) {
			return;
		}
		
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
