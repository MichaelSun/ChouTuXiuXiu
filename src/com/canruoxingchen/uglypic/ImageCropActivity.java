package com.canruoxingchen.uglypic;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.canruoxingchen.uglypic.camera.ImageProcessConstants;
import com.canruoxingchen.uglypic.camera.PmCameraUtils;
import com.canruoxingchen.uglypic.concurrent.ThreadPoolManager;
import com.canruoxingchen.uglypic.util.FileUtils;
import com.canruoxingchen.uglypic.util.jni.NativeImageUtil;
import com.canruoxingchen.uglypic.view.CropImageView;

public class ImageCropActivity extends BaseActivity implements OnClickListener {

	private static final int MESSAGE_FINISH_CHOOSE_PHOTO = 0x005;

	private static final int REQUEST_CODE_CHOOSE_POHTO = 0x001;

	CropImageView mCropView;
	RelativeLayout mExitRL;
	TextView mDoneTv;

	volatile String mImagePath;
	boolean mNeedCrop = true;
	int mMinPictureSize = ImageProcessConstants.PICTURE_MIN_SIZE_LARGE;

	CropHandler mHandler;

	String mPublisherText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

		setContentView(R.layout.activity_image_crop);

		mHandler = new CropHandler(this);

		mCropView = (CropImageView) findViewById(R.id.iv_crop);
		mCropView.setIsCrop(true);

		mExitRL = (RelativeLayout) findViewById(R.id.rl_exit);
		mDoneTv = (TextView) findViewById(R.id.tv_accept_crop);
		mExitRL.setOnClickListener(this);
		mDoneTv.setOnClickListener(this);

		startPhotoPicker();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mDoneTv != null) {
			mDoneTv.setClickable(true);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQUEST_CODE_CHOOSE_POHTO:
			if (resultCode == RESULT_OK) {
				TipsDialog.getInstance().show(this, R.drawable.tips_loading, R.string.tips_photo_loading, true, false);

				mImagePath = data == null ? null :
					PmCameraUtils.getImagePathFromUri(ImageCropActivity.this, data.getData());

				if (mImagePath == null) {
					setResult(RESULT_CANCELED);
					finish();
				} else if (isFileSizeInvalid(mImagePath)) {
					mHandler.sendEmptyMessage(MESSAGE_FINISH_CHOOSE_PHOTO);
				} else {
					// file size invalid
					ThreadPoolManager.getInstance().execute(new SaveRunnable());
					setResult(RESULT_FIRST_USER);
					finish();
				}
			} else if (resultCode == RESULT_CANCELED) {
				setResult(RESULT_CANCELED);
				finish();
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		TipsDialog.getInstance().dismiss();
	}

	private void startPhotoPicker() {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_PICK);
		intent.setType("image/*");
		startActivityForResult(intent, REQUEST_CODE_CHOOSE_POHTO);
	}

	/**
	 * 检测文件大小
	 * 
	 * @param filePath
	 *            文件绝对路径
	 * @return true文件合法，false文件不合法
	 */
	private boolean isFileSizeInvalid(String filePath) {
		boolean flag = true;
		if (filePath == null) {
			flag = false;
		} else {
			long fileSize = PmCameraUtils.getFileSize(filePath);
			if (fileSize < ImageProcessConstants.MIN_FILE_SIZE || fileSize > ImageProcessConstants.MAX_FILE_SIZE) {
				flag = false;
			} else {
				Options options = new Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(filePath, options);
				if (Math.min(options.outWidth, options.outHeight) < ImageProcessConstants.MIN_PIXEL) {
					flag = false;
				}
			}
		}

		return flag;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.rl_exit:
			finish();
			break;
		case R.id.tv_accept_crop:
			mDoneTv.setClickable(false);

			Thread thread = new Thread(new SaveRunnable());
			thread.setPriority(Thread.MAX_PRIORITY);
			thread.start();
			break;
		default:
			break;
		}
	}

	private static class CropHandler extends Handler {
		private final WeakReference<ImageCropActivity> mActivity;

		public CropHandler(ImageCropActivity activity) {
			mActivity = new WeakReference<ImageCropActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			ImageCropActivity activity = mActivity.get();
			if (activity == null) {
				return;
			}

			switch (msg.what) {
			case MESSAGE_FINISH_CHOOSE_PHOTO:
				TipsDialog.getInstance().dismiss();
				activity.mCropView.setImagePath(activity.mImagePath);
				break;
			default:
				break;
			}
		}
	}

	final class SaveRunnable implements Runnable {

		@SuppressLint("DefaultLocale")
		@Override
		public void run() {
			String savePath = FileUtils.createSdCardFile(System.currentTimeMillis() + ".jpg");
			File filePath = savePath == null ? getCacheDir() : new File(savePath);

			if (!filePath.exists() || !filePath.canWrite()) {
				TipsDialog.getInstance().show(ImageCropActivity.this, R.drawable.tips_saved, R.string.tips_no_sdcard,
						true);
				return;
			}

			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(System.currentTimeMillis());
			String pictureName = String.format("PmCamera_%d%02d%02d_%02d%02d%02d", calendar.get(Calendar.YEAR),
					calendar.get(Calendar.MONTH) + (1 - Calendar.JANUARY), calendar.get(Calendar.DATE),
					calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
			final File outFile = new File(filePath, pictureName + ".JPEG");
			final String outFilePath = outFile.getAbsolutePath();

			if (mNeedCrop) {
				Rect rect = mCropView.getCropArea();
				int orientation = mCropView.getOrientation();
				NativeImageUtil.getInstance(ImageCropActivity.this).cropPhotoWithoutGL(mImagePath, outFilePath,
						mMinPictureSize, rect.left, rect.top, rect.right, rect.bottom, orientation);
			} else {
				int orientation = PmCameraUtils.getExifOrientation(mImagePath);
				NativeImageUtil.getInstance(ImageCropActivity.this).cropPhotoWithoutGL(mImagePath, outFilePath,
						mMinPictureSize, 0, 0, 0, 0, orientation);
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// startPublisher(outFilePath);
					PhotoEditor.start(ImageCropActivity.this, Uri.fromFile(new File(mImagePath)));
					finish();
				}
			});
		}
	}

	public static void start(Activity activity, int requestCode) {
		Intent intent = new Intent(activity, ImageCropActivity.class);
		activity.startActivityForResult(intent, requestCode);
	}


	@Override
	protected void initUI() {

	}

	@Override
	protected void initListers() {

	}

}
