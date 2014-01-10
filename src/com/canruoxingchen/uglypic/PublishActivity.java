/**
 * 
 */
package com.canruoxingchen.uglypic;

import java.io.File;
import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;

import com.canruoxingchen.uglypic.concurrent.ThreadPoolManager;
import com.canruoxingchen.uglypic.sns.SnsHelper;
import com.canruoxingchen.uglypic.statistics.StatisticsUtil;
import com.canruoxingchen.uglypic.util.ImageUtils;
import com.canruoxingchen.uglypic.util.UiUtils;
import com.canruoxingchen.uglypic.view.SlipButton;
import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboHandler;
import com.sina.weibo.sdk.constant.WBConstants;

/**
 * 
 * 发布页面
 * 
 * @author wsf
 * 
 */
public class PublishActivity extends BaseActivity implements OnClickListener, IWeiboHandler.Response {

	private static final String EXTRA_ORIGIN = "origin";
	private static final String EXTRA_RESULT = "result";

	private static final String SHARE_CONTENT = "@丑图秀秀";

	public static final String KEY_ORIGIN = EXTRA_ORIGIN;
	public static final String KEY_RESULT = EXTRA_RESULT;

	private static final int REQUEST_CODE_VIEW_PHOTO = 1;

	private static final int SHARE_TYPE_LOCAL = 1;
	private static final int SHARE_TYPE_WEIBO = 2;
	private static final int SHARE_TYPE_WEIXIN = 3;
	private static final int SHARE_TYPE_FRIENDS = 4;
	// 只是查看
	private static final int SHARE_TYPE_VIEW = 5;

	private static final int THUMBNAIL_MERGE = 1;
	private static final int THUMBNAIL_NORMAL = 2;

	private static final int THUMB_WIDTH = 100;
	private static final int THUMB_HEIGHT = 100;

	private static final int MERGED_WIDTH_MAX = 600;
	private static final int TEXT_HEIGHT = 30;
	private static final int TEXT_COLOR = 0xFFE26E26;

	private String mOriginPath;
	private String mResultPath;

	private String mSharePathSingle;
	private String mSharePathMerge;

	private ImageView mIvImage;
	private EditText mEtDesc;

	private SlipButton mSlipBtn = null;
	private View mViewToWeibo;
	private View mViewToWeixin;
	private View mViewToFriends;
	private View mViewBack;
	private View mViewFinish;

	private SnsHelper mSnsHelper;

	private boolean mMerged = false;

	private MyHandler mHandler;

	private Bitmap mSingleThumbnail = null;
	private Bitmap mMergedThumbnail = null;

	private static class MyHandler extends Handler {

		WeakReference<PublishActivity> mActivity;

		MyHandler(PublishActivity activity) {
			mActivity = new WeakReference<PublishActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case R.id.msg_weibo_auth_success: {
				PublishActivity activity = mActivity.get();
				if (activity != null) {
					activity.saveCurrentImage(SHARE_TYPE_WEIBO);
				}
				break;
			}
			case R.id.msg_weibo_share_success: {
				UiUtils.toastMessage(UglyPicApp.getAppExContext(), R.string.publish_share_to_weibo_success);
				
				break;
			}
			case R.id.msg_weibo_share_failure: {
				UiUtils.toastMessage(UglyPicApp.getAppExContext(), R.string.publish_share_to_weibo_failure);
				break;
			}
			case R.id.msg_photo_export_failure: {
				TipsDialog.getInstance().dismiss();
				UiUtils.toastMessage(UglyPicApp.getAppExContext(), R.string.publish_exporting_failure);
				break;
			}
			case R.id.msg_photo_export_success: {
				TipsDialog.getInstance().dismiss();
				String path = (String) msg.obj;
				PublishActivity activity = mActivity.get();
				if (activity != null) {
					if (activity.mMerged) {
						activity.mSharePathMerge = path;
					} else {
						activity.mSharePathSingle = path;
					}
					activity.share(msg.arg1, path);
				}
				break;
			}
			case R.id.msg_photo_merge_thumbnail_success: {
				PublishActivity activity = mActivity.get();
				if (activity != null) {
					if (msg.arg1 == THUMBNAIL_MERGE) {
						activity.mMergedThumbnail = (Bitmap) msg.obj;
						if (activity.mMerged) {
							activity.mIvImage.setImageBitmap(activity.mMergedThumbnail);
						}
					} else {
						activity.mSingleThumbnail = (Bitmap) msg.obj;
						if (!activity.mMerged) {
							activity.mIvImage.setImageBitmap(activity.mSingleThumbnail);
						}
					}
				}
			}
			}
		}
	}

	public static void start(Activity context, int requestCode, String originPath, String resultPath) {
		Intent intent = new Intent(context, PublishActivity.class);
		intent.putExtra(EXTRA_ORIGIN, originPath);
		intent.putExtra(EXTRA_RESULT, resultPath);
		context.startActivityForResult(intent, requestCode);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		if (intent != null) {
			mOriginPath = intent.getStringExtra(EXTRA_ORIGIN);
			mResultPath = intent.getStringExtra(EXTRA_RESULT);
		}

		if (savedInstanceState != null) {
			mOriginPath = savedInstanceState.getString(KEY_ORIGIN);
			mResultPath = savedInstanceState.getString(KEY_RESULT);
		}

		mHandler = new MyHandler(this);

		initUI();
		initListers();

		// mAivImage.setImageInfo(ImageInfo.obtain(Uri.fromFile(new
		// File(mResultPath)).toString()));

		loadThumbnail(THUMBNAIL_NORMAL);

		mSnsHelper = new SnsHelper(this);
		mSnsHelper.onCreate(this, savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mIvImage.setImageDrawable(null);
		if (mSingleThumbnail != null && !mSingleThumbnail.isRecycled()) {
			mSingleThumbnail.recycle();
			mSingleThumbnail = null;
		}

		if (mMergedThumbnail != null && !mMergedThumbnail.isRecycled()) {
			mMergedThumbnail.recycle();
			mMergedThumbnail = null;
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		setResult(RESULT_FIRST_USER);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		mSnsHelper.onNewIntent(this, intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		mSnsHelper.authorizeCallBack(requestCode, resultCode, data);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_ORIGIN, mOriginPath);
		outState.putString(KEY_RESULT, mResultPath);
	}

	@Override
	protected void onResume() {
		super.onResume();
		MessageCenter msgCenter = MessageCenter.getInstance(this);
		msgCenter.registerMessage(R.id.msg_weibo_share_failure, mHandler);
		msgCenter.registerMessage(R.id.msg_weibo_auth_failure, mHandler);
		msgCenter.registerMessage(R.id.msg_weibo_share_success, mHandler);
		msgCenter.registerMessage(R.id.msg_weibo_auth_success, mHandler);
		msgCenter.registerMessage(R.id.msg_photo_export_failure, mHandler);
		msgCenter.registerMessage(R.id.msg_photo_export_success, mHandler);
		msgCenter.registerMessage(R.id.msg_photo_merge_thumbnail_success, mHandler);
	}

	@Override
	protected void onPause() {
		super.onPause();
		MessageCenter msgCenter = MessageCenter.getInstance(this);
		msgCenter.unregisterMessage(R.id.msg_weibo_share_failure, mHandler);
		msgCenter.unregisterMessage(R.id.msg_weibo_auth_failure, mHandler);
		msgCenter.unregisterMessage(R.id.msg_weibo_share_success, mHandler);
		msgCenter.unregisterMessage(R.id.msg_weibo_auth_success, mHandler);
		msgCenter.unregisterMessage(R.id.msg_photo_export_failure, mHandler);
		msgCenter.unregisterMessage(R.id.msg_photo_export_success, mHandler);
		msgCenter.unregisterMessage(R.id.msg_photo_merge_thumbnail_success, mHandler);
	}

	@Override
	protected void initUI() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.publish);

		mIvImage = (ImageView) findViewById(R.id.share_pic);
		mEtDesc = (EditText) findViewById(R.id.share_desc);
		mSlipBtn = (SlipButton) findViewById(R.id.publish_slip_btn);
		mViewToWeibo = findViewById(R.id.publish_share_to_weibo);
		mViewToWeixin = findViewById(R.id.publish_share_to_weixin);
		mViewToFriends = findViewById(R.id.publish_share_to_friends);
		mViewBack = findViewById(R.id.publish_back);
		mViewFinish = findViewById(R.id.publish_finish);
	}

	@Override
	protected void initListers() {
		mSlipBtn.SetOnChangedListener(new SlipButton.OnChangedListener() {

			@Override
			public void OnChanged(View v, boolean checkState) {
				if (checkState) {
					mMerged = true;
					if (mMergedThumbnail != null && !mMergedThumbnail.isRecycled()) {
						mIvImage.setImageBitmap(mMergedThumbnail);
					} else {
						recycleBmp(mMergedThumbnail);
						loadThumbnail(THUMBNAIL_MERGE);
					}
				} else {
					mMerged = false;

					if (mSingleThumbnail != null && !mSingleThumbnail.isRecycled()) {
						mIvImage.setImageBitmap(mSingleThumbnail);
					} else {
						recycleBmp(mSingleThumbnail);
						loadThumbnail(THUMBNAIL_MERGE);
					}
				}
			}
		});

		mViewToWeibo.setOnClickListener(this);
		mViewToWeixin.setOnClickListener(this);
		mViewToFriends.setOnClickListener(this);
		mViewBack.setOnClickListener(this);
		mViewFinish.setOnClickListener(this);
		mIvImage.setOnClickListener(this);

		mEtDesc.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				final File mergeFile = mSharePathMerge == null ? null : new File(mSharePathMerge);
				final File singleFile = mSharePathSingle == null ? null : new File(mSharePathSingle);
				ThreadPoolManager.getInstance().execute(new Runnable() {

					@Override
					public void run() {
						if (mergeFile != null && mergeFile.exists()) {
							mergeFile.delete();
						}
						if (singleFile != null && singleFile.exists()) {
							singleFile.delete();
						}
					}
				});
				mSharePathMerge = null;
				mSharePathSingle = null;
			}
		});
	}

	private void shareToWeibo() {
		if (mSnsHelper.hasWeiboAuthorized()) {
			// mSnsHelper.shareToWeibo(mEtDesc.getText().toString(), mMerged ?
			// mOriginPath : mResultPath);
			saveCurrentImage(SHARE_TYPE_WEIBO);
		} else {
			mSnsHelper.authorizeToWeibo(this);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.publish_back:
			setResult(RESULT_FIRST_USER);
			finish();
			break;
		case R.id.publish_finish: {
			// finish();
			saveCurrentImage(SHARE_TYPE_LOCAL);
			break;
		}
		case R.id.publish_share_to_weibo:
			shareToWeibo();
			break;
		case R.id.publish_share_to_weixin:
			saveCurrentImage(SHARE_TYPE_WEIXIN);
			break;
		case R.id.publish_share_to_friends:
			saveCurrentImage(SHARE_TYPE_FRIENDS);
			break;
		case R.id.share_pic:
			saveCurrentImage(SHARE_TYPE_VIEW);
			break;
		}
	}

	private void share(int shareType, String imagePath) {
		switch (shareType) {
		case SHARE_TYPE_LOCAL:
			ImageUtils.saveBitmapToGallery(this, Uri.fromFile(new File(imagePath)), 0);
			ViewUglyPicActivity.start(this, REQUEST_CODE_VIEW_PHOTO, imagePath, false);

			// 统计图片
			if (!TextUtils.isEmpty(mResultPath)) {
				StatisticsUtil.saveFile(this, mResultPath);
			}
			finish();
			break;
		case SHARE_TYPE_WEIBO:
			mSnsHelper.shareToWeibo(SHARE_CONTENT, imagePath);
			break;
		case SHARE_TYPE_WEIXIN:
			mSnsHelper.shareToWeixin(SHARE_CONTENT, imagePath);
			break;
		case SHARE_TYPE_FRIENDS:
			mSnsHelper.shareToFriends(SHARE_CONTENT, imagePath);
			break;
		case SHARE_TYPE_VIEW:
			ViewUglyPicActivity.start(this, REQUEST_CODE_VIEW_PHOTO, imagePath, true);
			break;
		}
	}

	private String saveImage(String content, boolean merge) {
		Bitmap oriBitmap = ImageUtils.decode(UglyPicApp.getAppExContext(), Uri.fromFile(new File(mOriginPath)));
		if (oriBitmap == null) {
			// messageCenter.notifyHandlers(R.id.msg_photo_export_failure);
			return "";
		}
		int width = oriBitmap.getWidth() > MERGED_WIDTH_MAX ? MERGED_WIDTH_MAX : oriBitmap.getWidth();
		if (!merge) {
			recycleBmp(oriBitmap);
		}
		int nPic = merge ? 2 : 1;
		int height = TextUtils.isEmpty(content) ? MERGED_WIDTH_MAX * nPic + TEXT_HEIGHT : MERGED_WIDTH_MAX * nPic
				+ TEXT_HEIGHT * 2;
		int start = 0;
		Bitmap image = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(image);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Style.FILL);
		paint.setColor(Color.WHITE);
		paint.setTextSize(20);
		canvas.drawRect(0, 0, width, height, paint);
		if (merge) {
			canvas.drawBitmap(oriBitmap, new Rect(0, 0, oriBitmap.getWidth(), oriBitmap.getHeight()), new Rect(0, 0,
					width, width), paint);
			start += width;
			recycleBmp(oriBitmap);
		}

		paint.setColor(Color.BLACK);
		if (!TextUtils.isEmpty(content)) {
			start += TEXT_HEIGHT;
			canvas.drawText(content, 0, start - 5, paint);
		}
		Bitmap result = ImageUtils.scaleDecode(mResultPath, width, width);
		if (result == null) {
			recycleBmp(image);
			return "";
		}
		canvas.drawBitmap(result, new Rect(0, 0, result.getWidth(), result.getHeight()), new Rect(0, start, width,
				width + start), paint);
		start += width;
		float textWidth = paint.measureText(SHARE_CONTENT);
		paint.setColor(TEXT_COLOR);
		canvas.drawText(SHARE_CONTENT, width - textWidth, height - 5, paint);
		recycleBmp(result);
		try {
			return ImageUtils.saveBitmapForLocalPath(UglyPicApp.getAppExContext(), image, 0, false);
		} finally {
			recycleBmp(image);
		}
	}

	private void recycleBmp(Bitmap bmp) {
		if (bmp != null && !bmp.isRecycled()) {
			bmp.recycle();
		}
		bmp = null;
	}

	// 保存当前图片
	private void saveCurrentImage(final int shareType) {
		// mEditorPanel.destroyDrawingCache();
		final MessageCenter messageCenter = MessageCenter.getInstance(UglyPicApp.getAppExContext());
		TipsDialog.getInstance().show(this, R.drawable.tips_loading, R.string.photo_editor_saving, true, false);
		final String content = mEtDesc.getText().toString() == null ? "" : mEtDesc.getText().toString().trim();
		final boolean merged = mMerged;
		// 判断是否已经存了图片，如果已存，则不再存
		if (merged) {
			if (mSharePathMerge != null) {
				messageCenter.notifyHandlers(R.id.msg_photo_export_success, shareType, 0, mSharePathMerge);
			}
		} else {
			if (mSharePathSingle != null) {
				messageCenter.notifyHandlers(R.id.msg_photo_export_success, shareType, 0, mSharePathSingle);
			}
		}
		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				String path = saveImage(content, merged);
				if (TextUtils.isEmpty(path)) {
					messageCenter.notifyHandlers(R.id.msg_photo_export_failure);
				} else {
					messageCenter.notifyHandlers(R.id.msg_photo_export_success, shareType, 0, path);
				}
			}
		});
	}

	private void loadThumbnail(final int type) {
		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				if (type == THUMBNAIL_MERGE) {
					Bitmap origin = ImageUtils.scaleDecode(mOriginPath, THUMB_WIDTH, THUMB_HEIGHT);
					Bitmap result = ImageUtils.scaleDecode(mResultPath, THUMB_WIDTH, THUMB_HEIGHT);
					try {
						if (origin != null && result != null) {
							Bitmap image = Bitmap.createBitmap(THUMB_WIDTH, THUMB_HEIGHT * 2, Config.ARGB_8888);
							Canvas canvas = new Canvas(image);
							Paint paint = new Paint();
							paint.setAntiAlias(true);
							canvas.drawBitmap(origin, new Rect(0, 0, origin.getWidth(), origin.getHeight()), new Rect(
									0, 0, THUMB_WIDTH, THUMB_HEIGHT), paint);
							canvas.drawBitmap(result, new Rect(0, 0, result.getWidth(), result.getHeight()), new Rect(
									0, THUMB_HEIGHT, THUMB_WIDTH, THUMB_HEIGHT * 2), paint);
							MessageCenter.getInstance(UglyPicApp.getAppExContext()).notifyHandlers(
									R.id.msg_photo_merge_thumbnail_success, type, 0, image);
						} else {

						}
					} finally {
						recycleBmp(origin);
						recycleBmp(result);
					}
				} else {
					Bitmap result = ImageUtils.scaleDecode(mResultPath, THUMB_WIDTH, THUMB_HEIGHT);
					try {
						if (result != null) {
							MessageCenter.getInstance(UglyPicApp.getAppExContext()).notifyHandlers(
									R.id.msg_photo_merge_thumbnail_success, type, 0, result);
						}
					} finally {
						// recycleBmp(result);
					}
				}
			}
		});
	}

	@Override
	public void onResponse(BaseResponse response) {
		switch (response.errCode) {
		case WBConstants.ErrorCode.ERR_OK:
			MessageCenter.getInstance(UglyPicApp.getAppExContext()).notifyHandlers(R.id.msg_weibo_share_success);
			break;
		case WBConstants.ErrorCode.ERR_FAIL:
			MessageCenter.getInstance(UglyPicApp.getAppExContext()).notifyHandlers(R.id.msg_weibo_share_failure);
			break;
		}
	}
}
