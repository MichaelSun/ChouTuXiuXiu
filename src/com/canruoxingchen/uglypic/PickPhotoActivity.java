/**
 * PhotoSelectionActivity.java
 */
package com.canruoxingchen.uglypic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.canruoxingchen.uglypic.concurrent.ThreadPoolManager;
import com.canruoxingchen.uglypic.util.FileUtils;
import com.canruoxingchen.uglypic.util.ImageUtils;
import com.canruoxingchen.uglypic.util.Logger;
import com.canruoxingchen.uglypic.util.UiUtils;

/**
 * 选择照片Activity，隔离选择来源，统一给出Uri作为返回
 * 
 * @author wsf
 * 
 */
public class PickPhotoActivity extends BaseActivity {

	private static final String TAG = "ChoosePhotoActivity";

	private static final String EXTRA_PHOTO_SRC = "photo_src";

	private static final String KEY_PHOTO_SRC = EXTRA_PHOTO_SRC;
	private static final String KEY_PHOTO_TMP_PATH = "photo_tmp_path";
	private static final String KEY_HAS_SELECTED_PHOTO = "has_selected_photo";

	private static final int PHOTO_SRC_CAMERA = 1;
	private static final int PHOTO_SRC_GALLERY = 2;

	private static final int REQUEST_CODE_CAMERA = 111;
	private static final int REQUEST_CODE_GALLERY = 112;
	private static final int REQUEST_CODE_PHOTO_EDIT = 4;
	// private static final int REQUEST_CODE_MODEL = 4;

	private static final String PICASA_PREFIX = "content://com.google.android.gallery3d.provider/picasa";

	/*-照片来源*/
	private int mPhotoSrc = PHOTO_SRC_CAMERA;
	/*-拍照时用到的临时文件*/
	private File mCameraTmpFile = null;

	/**
	 * 从相册中选择
	 * 
	 * @param activity
	 * @param requestCode
	 */
	public static void startForPhotoFromGallery(Activity activity, int requestCode) {
		Intent intent = new Intent(activity, PickPhotoActivity.class);
		intent.putExtra(EXTRA_PHOTO_SRC, PHOTO_SRC_GALLERY);
		activity.startActivityForResult(intent, requestCode);
	}

	/**
	 * 使用相机拍照
	 * 
	 * @param activity
	 * @param requestCode
	 */
	public static void startForPhotoFromCamera(Activity activity, int requestCode) {

		if (FileUtils.checkSdCardUserful()) {
			Intent intent = new Intent(activity, PickPhotoActivity.class);
			intent.putExtra(EXTRA_PHOTO_SRC, PHOTO_SRC_CAMERA);
			activity.startActivityForResult(intent, requestCode);
		} else {
			UiUtils.toastMessage(activity, R.string.no_sdcard_hint);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		if (intent == null) {
			if (!recoverState(savedInstanceState)) {
				finish();
				return;
			}
			LOGD("--------------------------activity created");
		} else {
			mPhotoSrc = intent.getIntExtra(EXTRA_PHOTO_SRC, 0);
			LOGD("--------------------------get photo src from intent : " + mPhotoSrc);

			boolean hasSelectedPhoto = false;
			if (savedInstanceState != null) {
				hasSelectedPhoto = savedInstanceState.getBoolean(KEY_HAS_SELECTED_PHOTO);
			}
			if (!hasSelectedPhoto) {
				LOGD("--------------------------choose photo from : " + mPhotoSrc);
				choosePhoto(mPhotoSrc);
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		TipsDialog.getInstance().dismiss();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (outState != null) {
			outState.putInt(KEY_PHOTO_SRC, mPhotoSrc);
			outState.putBoolean(KEY_HAS_SELECTED_PHOTO, true);
			if (mCameraTmpFile != null) {
				outState.putString(KEY_PHOTO_TMP_PATH, mCameraTmpFile.getAbsolutePath());
				LOGD("save instance state: " + mCameraTmpFile.getAbsolutePath());
			}
		}

	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		recoverState(savedInstanceState);
	}

	private boolean recoverState(Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			return false;
		}

		mPhotoSrc = savedInstanceState.getInt(KEY_PHOTO_SRC);
		String cameraPhotoPath = savedInstanceState.getString(KEY_PHOTO_TMP_PATH);
		if (!TextUtils.isEmpty(cameraPhotoPath)) {
			LOGD("camera photo path: " + cameraPhotoPath);
			mCameraTmpFile = new File(cameraPhotoPath);
		}

		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {

			TipsDialog.getInstance().showProcess(this, getString(R.string.choose_photo_waiting));

			switch (requestCode) {
			case REQUEST_CODE_CAMERA:
				handleCameraResult(data);
				break;
			case REQUEST_CODE_GALLERY:
				handleGalleryResult(data);
				break;
			case REQUEST_CODE_PHOTO_EDIT:
				if (data == null) {
					setResult(RESULT_CANCELED);
					finish();
				} else {
					saveEdittedPhoto(data.getData());
					finish();
				}
				break;
			}
		} else {
			finish();
		}

		// finish();
	}

	private void choosePhoto(int src) {
		switch (src) {
		case PHOTO_SRC_CAMERA:
			choosePhotoFromCamera();
			break;
		case PHOTO_SRC_GALLERY:
			choosePhotoFromGallery();
			break;
		default:
			finish();
			break;
		}
	}

	// 从图库选择照片
	private void choosePhotoFromGallery() {
		Intent intent = new Intent(Intent.ACTION_PICK, null);
		intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
		startActivityForResult(intent, REQUEST_CODE_GALLERY);
	}

	// 启动相机拍照
	private void choosePhotoFromCamera() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		String imgName = System.currentTimeMillis() + ".jpg";
		mCameraTmpFile = ImageUtils.getTempImageFile(imgName);
		if (mCameraTmpFile != null) {
			if (mCameraTmpFile.exists()) {
				mCameraTmpFile.delete();
			}
			// 在某些手机上，例如Galaxy S3，有时无法直接创建文件，需要自己创建
			try {
				mCameraTmpFile.createNewFile();
			} catch (IOException e) {

			}
			intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCameraTmpFile));
			// 该参数不一定好使
			// 默认启动前置摄像头貌似有问题，有时需要关闭两次
			// intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
			startActivityForResult(intent, REQUEST_CODE_CAMERA);
		} else {
			UiUtils.toastMessage(this, R.string.storage_not_enough);
			finish();
		}
	}

	private void saveEdittedPhoto(Uri uri) {
		if (uri != null) {
			Intent result = new Intent();
			result.setData(uri);
			setResult(RESULT_OK, result);
		} else {
			setResult(RESULT_CANCELED);
		}
	}

	/*-**************************************************
	 * 处理选择结果
	 * **************************************************/
	private void handleCameraResult(Intent data) {

		if (data == null || data.getParcelableExtra("data") == null) {
			if (mCameraTmpFile != null) {
				// 启动编辑界面
				Intent result = new Intent();
				result.setData(Uri.fromFile(mCameraTmpFile));
				setResult(RESULT_OK, result);
				finish();
			}
		} else {
			Bitmap bitmap = data.getParcelableExtra("data");
			FileOutputStream fos = null;
			if (bitmap == null) {
				setResult(RESULT_CANCELED);
				finish();
				return;
			}

			if (mCameraTmpFile != null) {
				try {
					fos = new FileOutputStream(mCameraTmpFile);
					bitmap.compress(CompressFormat.JPEG, 100, fos);
					bitmap.recycle();
				} catch (FileNotFoundException e) {
					// e.printStackTrace();
				} finally {
					FileUtils.closeQuietly(fos);
				}
			}

			Intent result = new Intent();
			result.setData(Uri.fromFile(mCameraTmpFile));
			setResult(RESULT_OK, result);
			finish();
		}
	}

	private void handleGalleryResult(Intent data) {

		if (data != null && data.getData() != null) {
			if (data.getData().toString().startsWith(PICASA_PREFIX)) {
				// PICASA 在4.2支持有问题，暂时不适配

				// 对于Picasa的照片，先复制一份，然后再关闭
				ThreadPoolManager.getInstance()
						.execute(
								new CopyFileTask(this, data.getData()));
			} else {
					Intent result = new Intent();
					result.setData(data.getData());
					setResult(RESULT_OK, result);
					finish();
			}
		}
	}

	private static void LOGD(String logMe) {
		Logger.d(TAG, logMe);
	}

	private class CopyFileTask implements Runnable {

		private WeakReference<PickPhotoActivity> mActivity;

		private InputStream mSrc = null;

		private Uri mSrcUri = null;

		public CopyFileTask(PickPhotoActivity activity, Uri uri) {
			this.mActivity = new WeakReference<PickPhotoActivity>(activity);
			ContentResolver resolver = activity.getContentResolver();
			try {
				mSrc = resolver.openInputStream(uri);
			} catch (FileNotFoundException e) {
				LOGD(e.getMessage());
			}
			mSrcUri = uri;
		}

		@Override
		public void run() {
			File destFile = null;
			if (mSrcUri != null && mSrc != null) {
				destFile = ImageUtils.getTempImageFile(System.currentTimeMillis() + "_img");
				if (destFile != null) {
					LOGD("[[Copy File]]>>>> From: " + mSrcUri.toString() + " to: " + destFile.getPath());
					FileOutputStream fos = null;
					try {
						fos = new FileOutputStream(destFile);
						FileUtils.copyFile(mSrc, fos);
					} catch (FileNotFoundException e) {
						LOGD(e.getMessage());
					} finally {
						FileUtils.closeQuietly(mSrc);
						FileUtils.closeQuietly(fos);
					}
					LOGD("[[Copy File Finished]]>>>> From: " + mSrcUri.toString() + " to: " + destFile.getPath());
				} // end of if (destFile != null) {
			}

			Activity activity = mActivity.get();
			final Uri destUri = (destFile == null ? null : Uri.fromFile(destFile));
			if (activity != null) {
				activity.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (destUri != null) {
							Intent intent = new Intent();
							intent.setData(destUri);
							setResult(RESULT_OK, intent);
						}
						finish();
					}
				});
			}
		}
	}

	@Override
	protected void initUI() {

	}

	@Override
	protected void initListers() {

	}
}
