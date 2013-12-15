package com.canruoxingchen.uglypic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.canruoxingchen.uglypic.cache.AsyncImageView;
import com.canruoxingchen.uglypic.cache.ImageInfo;
import com.canruoxingchen.uglypic.camera.ImageProcessConstants;
import com.canruoxingchen.uglypic.camera.PmCamera;
import com.canruoxingchen.uglypic.camera.PmCameraData;
import com.canruoxingchen.uglypic.camera.PmCameraRender;
import com.canruoxingchen.uglypic.util.Logger;
import com.canruoxingchen.uglypic.util.jni.NativeImageUtil;

public class CameraActivity extends BaseActivity implements OnClickListener, OnTouchListener, LoaderCallbacks<Cursor> {
	private static final String TAG = CameraActivity.class.getSimpleName();

	private static final int REQUEST_CODE_GALLERY = 0x001;

	private static final int URL_LOADER_ALBUM_EXTERNAL = 1;

	public static final String INTENT_EXTRA_PICTURE_SIZE = "picture_size";
	public static final String INTENT_EXTRA_IS_SQUARE = "is_square";
	public static final String INTENT_EXTRA_IS_FRONT = "is_front";

	private static final int MESSAGE_FOCUSING = 0x001;
	private static final int MESSAGE_FOCUSED = 0x002;
	private static final int MESSAGE_FOCUS_FAILED = 0x003;
	private static final int MESSAGE_FOCUSED_SUCCESS = 0x004;

	private static final int FOCUS_VIEW_SIZE = 100;
	@SuppressWarnings("unused")
	private static final int BOTTOM_HEIGHT = 196;

	PmCamera mPmCamera;
	RendererObserver mRenderObserver = new RendererObserver();
	OrientationObserver mOrientationObserver;
	CameraObserver mCameraObserver = new CameraObserver();
	PmCameraData mPmCameraData = new PmCameraData();
	PmCameraRender mPmCameraRender;
	PmCameraHander mHandler;

	RelativeLayout mExitRL;
	ImageButton mFlashOnOffIB;
	ImageButton mCameraNextIB;
	ImageButton mCapturePhotoIB;
	AsyncImageView mChoosePhotoAiv;
	View mFocusIndicatorView;
	RelativeLayout mTopBarRL;
	RelativeLayout mBottomBarRL;

	String mCurrentPhotoPath;
	int mPictureMinSize = ImageProcessConstants.PICTURE_MIN_SIZE_LARGE;
	boolean mIsPreviewSquare = true;
	boolean mIsFrontCamera = false;

	FlashMode mCurrentFlashMode = FlashMode.OFF;

	// 相机可见区域宽高比
	float mPictureRatio = 1.0f;

	String mPublisherText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_camera);

		Intent intent = getIntent();
		if (intent != null) {
			mPictureMinSize = intent.getIntExtra(INTENT_EXTRA_PICTURE_SIZE,
					ImageProcessConstants.PICTURE_MIN_SIZE_LARGE);
			mIsPreviewSquare = intent.getBooleanExtra(INTENT_EXTRA_IS_SQUARE, true);
			mIsFrontCamera = intent.getBooleanExtra(INTENT_EXTRA_IS_FRONT, false);

			mPictureMinSize = ImageProcessConstants.PICTURE_MIN_SIZE_LARGE;
			mIsPreviewSquare = true;
			// mIsFrontCamera = true;
		}

		mPmCamera = new PmCamera(this.getApplicationContext());
		mPmCamera.setPictureMinSize(mPictureMinSize);
		mPmCamera.setQmCameraData(mPmCameraData);
		mPmCamera.setCameraFront(mPmCamera.hasFrontCamera() ? mIsFrontCamera : false);

		mOrientationObserver = new OrientationObserver(this);
		mHandler = new PmCameraHander(this);

		initView();

		mPmCameraRender.setPmCameraData(mPmCameraData);
		mPmCameraRender.setObserver(mRenderObserver);

		switchFlashMode();

		needShowFlashButton();
		needShowSwitchCameraButton();

		this.getLoaderManager().initLoader(URL_LOADER_ALBUM_EXTERNAL, null, this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		NativeImageUtil.getInstance(this).onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();

		mPmCamera.onPause();
		mPmCameraRender.onPause();
		mOrientationObserver.disable();
	}

	@Override
	protected void onResume() {
		super.onResume();

		mPmCamera.onResume();
		mOrientationObserver.enable();
		mPmCameraRender.onResume();

		mFlashOnOffIB.setEnabled(true);
		mCameraNextIB.setEnabled(true);
		mChoosePhotoAiv.setEnabled(true);
		mCapturePhotoIB.setEnabled(true);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.rl_exit:
			finish();
			break;
		case R.id.ib_flash_on_off:
			switchFlashMode();
			break;
		case R.id.ib_camera_next:
			mPmCamera.setCameraFront(!mPmCamera.isCameraFront());
			needFocusWhenTouch();
			needShowFlashButton();
			break;
		case R.id.ib_capture:
			mOrientationObserver.disable();
			mFlashOnOffIB.setEnabled(false);
			mCameraNextIB.setEnabled(false);
			mChoosePhotoAiv.setEnabled(false);
			mCapturePhotoIB.setEnabled(false);
			mPmCamera.takenPicture(mCameraObserver);
			break;
		case R.id.ib_choose_photo:
			startPhotoPicker();
			break;
		default:
			break;
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (!canFocus(event.getX(), event.getY())) {
			return true;
		}

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			float density = getResources().getDisplayMetrics().density;
			mFocusIndicatorView.setX(event.getX() - density * FOCUS_VIEW_SIZE / 2);
			mFocusIndicatorView.setY(event.getY() - density * FOCUS_VIEW_SIZE / 2 + mTopBarRL.getHeight());
			mFocusIndicatorView.requestLayout();

			mHandler.sendEmptyMessage(MESSAGE_FOCUSING);
			break;
		case MotionEvent.ACTION_UP:
			ScaleAnimation animation = new ScaleAnimation(1.0f, 0.5f, 1.0f, 0.5f, event.getX(), event.getY());
			animation.setDuration(300);
			animation.setFillBefore(false);
			animation.setFillAfter(true);
			mFocusIndicatorView.startAnimation(animation);
			mPmCamera.autoFocus(mCameraObserver);
			break;
		default:
			break;
		}

		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_FIRST_USER) {
			if (requestCode == REQUEST_CODE_GALLERY) {
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						TipsDialog.getInstance().show(CameraActivity.this, R.drawable.tips_fail,
								R.string.tips_photo_size_invalid, true);
					}
				}, 1000);
			}
		} else if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_CODE_GALLERY) {
				// setResult(RESULT_OK, data);
				finish();
				PhotoEditor.start(this, data.getData());
			}
		} else if (resultCode == RESULT_CANCELED) {

		}
	}

	private void startPhotoPicker() {
		// TODO: 从相册选图
		PickPhotoActivity.startForPhotoFromGallery(this, REQUEST_CODE_GALLERY);
		// TODO
	}

	private void initView() {
		mExitRL = (RelativeLayout) findViewById(R.id.rl_exit);
		mPmCameraRender = (PmCameraRender) findViewById(R.id.camera_surface);
		mFlashOnOffIB = (ImageButton) findViewById(R.id.ib_flash_on_off);
		mCameraNextIB = (ImageButton) findViewById(R.id.ib_camera_next);
		mCapturePhotoIB = (ImageButton) findViewById(R.id.ib_capture);
		mChoosePhotoAiv = (AsyncImageView) findViewById(R.id.ib_choose_photo);
		mFocusIndicatorView = (View) findViewById(R.id.focus_indicator);
		mTopBarRL = (RelativeLayout) findViewById(R.id.rl_topbar);
		mBottomBarRL = (RelativeLayout) findViewById(R.id.rl_bottombar);
		mFlashOnOffIB.setOnClickListener(this);
		mCameraNextIB.setOnClickListener(this);
		mChoosePhotoAiv.setOnClickListener(this);
		mCapturePhotoIB.setOnClickListener(this);
		mExitRL.setOnClickListener(this);

		needFocusWhenTouch();

		int width = getResources().getDisplayMetrics().widthPixels;
		int height = getResources().getDisplayMetrics().heightPixels;
		float density = getResources().getDisplayMetrics().density;
		int topBarHeight = mTopBarRL.getLayoutParams().height;
		if (mIsPreviewSquare) {
			mPictureRatio = 1.0f;
			mBottomBarRL.getLayoutParams().height = height - topBarHeight - width;
			mBottomBarRL.setBackgroundColor(Color.parseColor("#FFFFFFFF"));
		} else {
			mBottomBarRL.setBackgroundColor(Color.parseColor("#00000000"));
			mBottomBarRL.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			((RelativeLayout.LayoutParams) mBottomBarRL.getLayoutParams()).bottomMargin = (int) (30 * density);

			Size previewSize = mPmCamera.getPreviewSize();
			int instrictHeight = previewSize.width * width / previewSize.height;
			int surfaceHeight;
			if (instrictHeight < height - topBarHeight) {
				surfaceHeight = instrictHeight;
			} else {
				surfaceHeight = height - topBarHeight;
			}
			mPictureRatio = (float) width / surfaceHeight;
		}
	}

	public PmCameraRender getPmCameraRender() {
		return mPmCameraRender;
	}

	public PmCameraData getPmCameraData() {
		return mPmCameraData;
	}

	/**
	 * 在相机预览可见范围点击才能对焦
	 * 
	 * @return
	 */
	private boolean canFocus(float x, float y) {
		int width = getResources().getDisplayMetrics().widthPixels;
		float height = width / mPictureRatio;
		if (x < 0 || x > width || y < 0 || y > height) {
			return false;
		}

		return true;
	}

	private void needShowFlashButton() {
		if (mPmCamera.isCameraFront()) {
			mFlashOnOffIB.setVisibility(View.GONE);
		} else {
			mFlashOnOffIB.setVisibility(View.VISIBLE);
		}
	}

	private void needShowSwitchCameraButton() {
		if (mPmCamera.hasFrontCamera()) {
			mCameraNextIB.setVisibility(View.VISIBLE);
		} else {
			mCameraNextIB.setVisibility(View.GONE);
		}
	}

	private void needFocusWhenTouch() {
		if (mPmCamera.isCameraFront()) {
			mPmCameraRender.setOnTouchListener(null);
		} else {
			mPmCameraRender.setOnTouchListener(this);
		}
	}

	private void switchFlashMode() {
		mCurrentFlashMode = mCurrentFlashMode.next();
		mFlashOnOffIB.setImageResource(mCurrentFlashMode.getResource());
		mPmCamera.toggleFlashMode(mCurrentFlashMode.getMode());
	}

	private void setImageResult(String filePath) {
		// Intent intent = new Intent();
		// if (!TextUtils.isEmpty(filePath)) {
		// intent.setData(Uri.fromFile(new File(filePath)));
		// }
		// intent.putExtra(ImageProcessConstants.TAG_IMAGE_PATH, filePath);
		// setResult(RESULT_OK, intent);
		// finish();
		finish();
		PhotoEditor.start(this, Uri.fromFile(new File(filePath)));
	}

	public enum FlashMode {
		ON(0, Parameters.FLASH_MODE_ON, R.drawable.camera_flash_on), OFF(1, Parameters.FLASH_MODE_OFF,
				R.drawable.camera_flash_off), AUTO(2, Parameters.FLASH_MODE_AUTO, R.drawable.camera_flash_auto);

		private int id;
		private String mode;
		private int resource;

		private FlashMode(int id, String mode, int resource) {
			this.id = id;
			this.mode = mode;
			this.resource = resource;
		}

		public int getId() {
			return this.id;
		}

		public String getMode() {
			return this.mode;
		}

		public int getResource() {
			return this.resource;
		}

		public FlashMode next() {
			FlashMode modes[] = FlashMode.values();
			int index = (id + 1) % modes.length;
			return modes[index];
		}
	}

	private class RendererObserver implements PmCameraRender.Observer {
		@Override
		public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture) {
			try {
				mPmCamera.stopPreview();
				mPmCamera.setPreviewTexture(surfaceTexture);
				mPmCamera.startPreview();
			} catch (final Exception ex) {
				LOGD("onSurfaceTextureCreated error: " + ex.getMessage());
			}
		}
	}

	private class OrientationObserver extends OrientationEventListener {
		public OrientationObserver(Context context) {
			super(context, SensorManager.SENSOR_DELAY_NORMAL);
			disable();
		}

		@Override
		public void onOrientationChanged(int orientation) {
			orientation = (((orientation + 45) / 90) * 90) % 360;
			mPmCameraData.mDeviceOrientation = orientation;

			mCapturePhotoIB.setRotation(-orientation);
			mCameraNextIB.setRotation(-orientation);
			mFlashOnOffIB.setRotation(-orientation);
			mChoosePhotoAiv.setRotation(-orientation);
		}
	}

	private final class CameraObserver implements PmCamera.Observer {
		@Override
		public void onAutoFocus(boolean success) {
			mHandler.sendEmptyMessage(success ? MESSAGE_FOCUSED : MESSAGE_FOCUS_FAILED);
			mHandler.sendEmptyMessageDelayed(MESSAGE_FOCUSED_SUCCESS, 1000L);
		}

		@Override
		public void onPictureTaken(final byte[] data) {
			mPmCameraData.mImageData = data;
			mPmCameraData.mImageTime = System.currentTimeMillis();

			mPmCamera.onPause();

			// 保存图片
			Thread thread = new Thread(new SaveRunnable());
			thread.setPriority(Thread.MAX_PRIORITY);
			thread.start();
		}

		@Override
		public void onShutter() {
		}

		@Override
		public void onException() {
			mCapturePhotoIB.setEnabled(true);
		}
	}

	private static class PmCameraHander extends Handler {
		private final WeakReference<CameraActivity> mActivity;

		public PmCameraHander(CameraActivity activity) {
			mActivity = new WeakReference<CameraActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			CameraActivity activity = mActivity.get();
			if (activity == null) {
				return;
			}

			switch (msg.what) {
			case MESSAGE_FOCUSING:
				activity.mFocusIndicatorView.setVisibility(View.VISIBLE);
				break;
			case MESSAGE_FOCUS_FAILED:
				break;
			case MESSAGE_FOCUSED:
				break;
			case MESSAGE_FOCUSED_SUCCESS:
				activity.mFocusIndicatorView.setAnimation(null);
				activity.mFocusIndicatorView.setVisibility(View.GONE);
				break;
			default:
				break;
			}
		}
	}

	private final class SaveRunnable implements Runnable {
		@SuppressLint("DefaultLocale")
		@Override
		public void run() {
			boolean isDCIM = false;
			// 默认手机系统相册路径为DCIM，不存在则取cacheDir，(不处理系统相册路径不为DCIM的情况)
			File filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
			filePath = new File(filePath, "/Camera/");
			if (filePath.exists() || filePath.mkdirs()) {
				isDCIM = true;
			} else {
				filePath = getCacheDir();
			}
			if (!filePath.exists() || !filePath.canWrite()) {
				TipsDialog.getInstance()
						.show(CameraActivity.this, R.drawable.tips_saved, R.string.tips_no_sdcard, true);
				return;
			}

			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(mPmCameraData.mImageTime);
			String pictureName = String.format("PmCamera_%d%02d%02d_%02d%02d%02d", calendar.get(Calendar.YEAR),
					calendar.get(Calendar.MONTH) + (1 - Calendar.JANUARY), calendar.get(Calendar.DATE),
					calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
			final File outFile = new File(filePath, pictureName + ".jpg");
			final String outFilePath = outFile.getAbsolutePath();

			filePath = new File(filePath, pictureName + "_OUT");
			try {
				filePath.createNewFile();
				FileOutputStream fos = new FileOutputStream(filePath);
				fos.write(mPmCameraData.mImageData);
				fos.close();
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}

			Size size = mPmCamera.getPicturesize();
			int w = size.height;
			int h = (int) (w / mPictureRatio);
			int left = 0, top = 0;
			int width = w, height = h;
			int orientation = mPmCamera.getOrientation();
			boolean isFrontCamera = mPmCamera.isCameraFront();
			switch (orientation) {
			case 90:
				left = 0;
				top = isFrontCamera ? size.width - height : 0;
				break;
			case 180:
				left = size.width - h;
				top = 0;
				width = h;
				height = w;
				break;
			case 270:
				left = 0;
				top = isFrontCamera ? 0 : size.width - height;

				break;
			default:
				width = h;
				height = w;
				break;
			}

			NativeImageUtil.getInstance(CameraActivity.this).saveCameraPhotoWithoutGL(filePath.getAbsolutePath(),
					outFilePath, orientation, isFrontCamera ? 1 : 0, left, top, width, height, mPictureMinSize);
			filePath.delete();

			// 如果在cacheDir，则不写入
			// broadcast和头像保存，其余不保存
			if (isDCIM) {
				ContentValues v = new ContentValues();
				v.put(MediaColumns.TITLE, pictureName);
				v.put(MediaColumns.DISPLAY_NAME, pictureName);
				v.put(ImageColumns.DESCRIPTION, "Taken with PmCamera.");
				v.put(MediaColumns.DATE_ADDED, calendar.getTimeInMillis());
				v.put(ImageColumns.DATE_TAKEN, calendar.getTimeInMillis());
				v.put(MediaColumns.DATE_MODIFIED, calendar.getTimeInMillis());
				v.put(MediaColumns.MIME_TYPE, "image/jpeg");
				v.put(ImageColumns.ORIENTATION, 0);
				v.put(MediaColumns.DATA, outFilePath);

				File parent = outFile.getParentFile();
				String path = parent.toString().toLowerCase(Locale.ENGLISH);
				String name = parent.getName().toLowerCase(Locale.ENGLISH);
				v.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
				v.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
				v.put(MediaColumns.SIZE, outFile.length());

				ContentResolver c = getContentResolver();
				c.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// 拍照成功，统计
					setImageResult(outFilePath);
				}
			});
		}
	}

	/**
	 * 进入拍照模式
	 * 
	 * @param activity
	 * @param requestCode
	 * @param from
	 */
	public static void startCameraMode(Activity activity, int requestCode) {
		Intent intent = new Intent(activity, CameraActivity.class);
		intent.putExtra(INTENT_EXTRA_PICTURE_SIZE, ImageProcessConstants.PICTURE_MIN_SIZE_SMALL);
		activity.startActivityForResult(intent, requestCode);
	}

	@Override
	protected void initUI() {

	}

	@Override
	protected void initListers() {

	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		LOGD("load created >>>>>>>> ");
		// 查询SD卡上所有的照片
		String[] projection = new String[] { MediaStore.Images.Media.DATA };

		// 按照时间排序返回
		return new CursorLoader(CameraActivity.this, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
				null, MediaStore.Images.Media.DATE_ADDED + " DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		LOGD("load finished >>>>>>>> ");
		if (data != null) {
			LOGD("load finished >>>>>>>> 1");
			if (data.moveToFirst()) {
				LOGD("load finished >>>>>>>> 2");
				do {
					int indexOfUrl = data.getColumnIndex(MediaStore.Images.Media.DATA);
					String url = data.getString(indexOfUrl);
					if (!TextUtils.isEmpty(url) && (new File(url).exists())) {
						LOGD("load finished >>>>>>>> 3  url=" + url);
						mChoosePhotoAiv.setImageInfo(ImageInfo.obtain(Uri.fromFile(new File(url)).toString()));
						break;
					}
				} while (data.moveToNext());
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

	}

	private void LOGD(String logMe) {
		Logger.d("CameraActivity", logMe);
	}

}
