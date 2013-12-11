package com.canruoxingchen.uglypic.camera;

public class PmCameraData {
	public final float mAspectRatio[] = new float[2];
	public final float mAspectRatioPreview[] = new float[2];
	public final float[] mOrientationM = new float[16];
	public int mPreviewWidth;
	public int mPreviewHeight;
	public int mGLSurfaceWidth;
	public int mGLSurfaceHeight;

	public int mDeviceOrientation;

	public byte[] mImageData;
	public long mImageTime;

	public float mCropScale = 1.0f;
	public float mCropTranslate[] = new float[] { 0.0f, 0.0f };

	public void reset() {
		mCropScale = 1.0f;
		mCropTranslate[0] = 0.0f;
		mCropTranslate[1] = 0.0f;
	}
}
