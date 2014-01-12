package com.canruoxingchen.uglypic.camera;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;

public class PmCamera {
    private final Context mContext;

    private Camera mCamera;
    private int mCameraId;
    private final Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
    private PmCameraData mQmCameraData;
    private SurfaceTexture mSurfaceTexture;

    private int mPictureMinSize = ImageProcessConstants.PICTURE_MIN_SIZE_LARGE;
    private double mRatio;

    private Size mPreviewSize;
    private Size mPictureSize;

    private String mCurrentFlashMode = Parameters.FLASH_MODE_AUTO;

    private boolean mNeedRefreshPreviewSize = false;
    
    //当前zoom的位置
	private int mCurrentZoomIndex = 0;
	private volatile boolean mIsZooming = false;
	private List<Integer> mZoomRatios = new ArrayList<Integer>();
	private boolean mIsZoomSupported = false;
	private boolean mIsSmoothZoomSupported = false;
	private int mZoomMax = 0;
	
    
    public PmCamera(Context context) {
        mContext = context;
    }

    public int getOrientation() {
        if (mCameraInfo == null || mQmCameraData == null) {
            return 0;
        }

        if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            return (mCameraInfo.orientation - mQmCameraData.mDeviceOrientation + 360) % 360;
        } else {
            return (mCameraInfo.orientation + mQmCameraData.mDeviceOrientation) % 360;
        }
    }

    public void updateAspectRatioPreview() {
        if (mCamera == null || mQmCameraData == null) {
            return;
        }

        Camera.Size size = mCamera.getParameters().getPreviewSize();
        int w = size.height;
        int h = size.width;
        mQmCameraData.mPreviewWidth = w;
        mQmCameraData.mPreviewHeight = h;

        mQmCameraData.mAspectRatioPreview[0] = (float) Math.min(w, h) / w;
        mQmCameraData.mAspectRatioPreview[1] = (float) Math.min(w, h) / h;
    }

    public boolean isCameraFront() {
        return mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT;
    }

    /**
     * Must be called from Activity.onPause(). Stops preview and releases Camera
     * instance.
     */
    public void onPause() {
        mSurfaceTexture = null;
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * Should be called from Activity.onResume(). Recreates Camera instance.
     */
    public void onResume() {
        openCamera();
    }

    private void openCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        if (mCameraId >= 0) {
            Camera.getCameraInfo(mCameraId, mCameraInfo);
            mCamera = Camera.open(mCameraId);
            Camera.Parameters params = mCamera.getParameters();
            if (params.getSupportedFocusModes().contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            if (!isCameraFront() && params.getSupportedFlashModes().contains(mCurrentFlashMode)) {
                params.setFlashMode(mCurrentFlashMode);
            }
            
            //调焦参数
            mIsSmoothZoomSupported = params.isSmoothZoomSupported();
            mIsZoomSupported = params.isZoomSupported();
            if(mIsZoomSupported) {
            	mCamera.setZoomChangeListener(mZoomChangeListener);
            	mZoomRatios = params.getZoomRatios();
            	mZoomMax = params.getMaxZoom();
            } else {
            	mZoomRatios = new ArrayList<Integer>();
            	mZoomMax = 0;
            }
            // Camera.Area focusArea = new Camera.Area(new Rect(-100, -100, 100,
            // 100), 1000);
            // List<Camera.Area> focusList = new ArrayList<Camera.Area>();
            // focusList.add(focusArea);
            // if (params.getMaxNumFocusAreas() > 0) {
            // params.setFocusAreas(focusList);
            // }
            // if (params.getMaxNumMeteringAreas() > 0) {
            // params.setMeteringAreas(focusList);
            // }
            Size size = getOptimalPreviewSize(mNeedRefreshPreviewSize);
            params.setPreviewSize(size.width, size.height);
            size = getOptimalPictureSize(mNeedRefreshPreviewSize);
            params.setPictureSize(size.width, size.height);
            if (params.getSupportedPictureFormats().contains(ImageFormat.JPEG)) {
                params.setPictureFormat(ImageFormat.JPEG);
            }
            mCamera.setParameters(params);
            mNeedRefreshPreviewSize = false;

            // int orientation = mCameraInfo.orientation;
            // if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            // orientation = 360 - orientation;
            // }
            // mCamera.setDisplayOrientation(orientation);
            mCamera.setDisplayOrientation(90);

            updateAspectRatioPreview();

            try {
                if (mSurfaceTexture != null) {
                    mCamera.setPreviewTexture(mSurfaceTexture);
                    mCamera.startPreview();
                }
            } catch (Exception ex) {
            }
        }
    }

    public void toggleFlashMode(String flashMode) {
        if (mCamera == null || flashMode == null) {
            return;
        }

        Camera.Parameters params = mCamera.getParameters();
        List<String> supportedFashModes = params.getSupportedFlashModes();
        if (supportedFashModes != null && supportedFashModes.contains(flashMode)) {
            params.setFlashMode(flashMode);
            mCamera.setParameters(params);

            mCurrentFlashMode = flashMode;
        }
    }

    public boolean hasFrontCamera() {
        boolean flag = false;
        CameraInfo info = new CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; ++i) {
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public void setCameraFront(boolean frontFacing) {
        int facing = frontFacing ? CameraInfo.CAMERA_FACING_FRONT : CameraInfo.CAMERA_FACING_BACK;
        // 切换摄像头强制刷新mPreivewSize和mPictureSize
        if (mCameraInfo == null || facing != mCameraInfo.facing) {
            mNeedRefreshPreviewSize = true;
        }

        mCameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; ++i) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == facing) {
                mCameraId = i;
                break;
            }
        }

        openCamera();
    }

    public void setCameraDisplayOrientation(int deviceOrientation) {
        if (mCamera == null) {
            return;
        }
        int result;
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCameraInfo.orientation + deviceOrientation) % 360;
            result = (360 - result) % 360;
        } else {
            result = (mCameraInfo.orientation - deviceOrientation + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    /**
     * 相机预览策略：取相机预览宽高比和手机屏幕宽高比最近似的一组，并且预览宽高的小值与屏幕宽最接近
     * 
     * @param refresh
     *            为true则重新计算，不取缓存
     * 
     * @return
     */
    private Size getOptimalPreviewSize(boolean refresh) {
        if (mCamera == null) {
            return null;
        }

        if (mPreviewSize != null && !refresh) {
            return mPreviewSize;
        }

        int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = mContext.getResources().getDisplayMetrics().heightPixels;
        int screenMin = Math.min(screenHeight, screenWidth);
        double screenRatio = (double) screenMin / Math.max(screenWidth, screenHeight);

        Size optimalSize = null;
        double minRatioDiff = Double.MAX_VALUE, minRatioTemp;
        double minPreviewDiff = Double.MAX_VALUE, minPreviewTemp;
        for (Size size : mCamera.getParameters().getSupportedPreviewSizes()) {
            minRatioTemp = Math.abs((double) Math.min(size.width, size.height) / Math.max(size.width, size.height) - screenRatio);
            if (minRatioTemp > minRatioDiff) {
                continue;
            }
            minPreviewTemp = Math.abs(Math.min(size.width, size.height) - screenMin);
            if (Math.abs(minRatioTemp - minRatioDiff) > 0.001 || minPreviewTemp <= minPreviewDiff) {
                optimalSize = size;
                minPreviewDiff = minPreviewTemp;
            }
            minRatioDiff = minRatioTemp;
        }
        mRatio = (double) Math.min(optimalSize.width, optimalSize.height) / Math.max(optimalSize.width, optimalSize.height);
        mPreviewSize = optimalSize;
        return optimalSize;
    }

    /**
     * PictureSize宽高比例要与预览的宽高比例一致，否则保存照片会出现与预览不一致的bug，这里取最接近的
     * 在宽高比符合要求的情况下，PictureSize的短边尽量取大于mPictureMinSize的最近接值，如果没有大于的值，则取小于的最接近值
     * 
     * @param refresh
     *            为true则重新计算，不取缓存
     * 
     * @return
     */
    private Size getOptimalPictureSize(boolean refresh) {
        if (mCamera == null) {
            return null;
        }

        if (mPictureSize != null && !refresh) {
            return mPictureSize;
        }

        Size optimalSize = null;
        double minRatioDiff = Double.MAX_VALUE, minRatioTemp;
        int minPictureDiff = Integer.MAX_VALUE, minPictureTemp;
        for (Size size : mCamera.getParameters().getSupportedPictureSizes()) {
            minRatioTemp = Math.abs((double) Math.min(size.width, size.height) / Math.max(size.width, size.height) - mRatio);
            if (minRatioTemp > minRatioDiff) {
                continue;
            }
            minPictureTemp = Math.min(size.width, size.height) - mPictureMinSize;
            if ((Math.abs(minRatioTemp - minRatioDiff) > 0.001)
                    || ((minPictureTemp >= 0 && (minPictureDiff < 0 || minPictureTemp < minPictureDiff)) || (minPictureTemp < 0 && minPictureDiff < 0 && minPictureTemp > minPictureDiff))) {
                optimalSize = size;
                minPictureDiff = minPictureTemp;
            }
            minRatioDiff = minRatioTemp;
        }
        mPictureSize = optimalSize;
        return optimalSize;
    }

    public void setPreviewTexture(SurfaceTexture surfaceTexture) throws IOException {
        mSurfaceTexture = surfaceTexture;
        mCamera.setPreviewTexture(surfaceTexture);
    }

    public void setQmCameraData(PmCameraData qmCameraData) {
        mQmCameraData = qmCameraData;
    }

    public void setPictureMinSize(int pictureMinSize) {
        mPictureMinSize = pictureMinSize;
    }

    public Size getPreviewSize() {
        return mPreviewSize;
    }

    public Size getPicturesize() {
        return mPictureSize;
    }

    public void startPreview() {
        mCamera.startPreview();
    }

    public void stopPreview() {
        mCamera.stopPreview();
    }
    
    public boolean isZooming() {
    	return mIsZooming;
    }
    
    public boolean smoothZoom(int index) {
    	if(mCamera != null && mIsZoomSupported && mIsSmoothZoomSupported) {
    		mCamera.stopSmoothZoom();
    		mCamera.startSmoothZoom(index);
    		return true;
    	}
    	return false;
    }
   
    public int getMaxZoom() {
    	return mZoomMax;
    }
    
 // 放大
 	public void zoomIn() {
 		if  (mCamera != null
 				&& mIsZoomSupported
 				&& mZoomRatios != null
 				&& mZoomRatios.size() > 0) {
 			Camera.Parameters params = mCamera.getParameters();
 			if (mCurrentZoomIndex + 1 < params.getMaxZoom()) {
 				++mCurrentZoomIndex;
 				int ratio = mZoomRatios
 						.get(mCurrentZoomIndex);
 				params.setZoom(mCurrentZoomIndex);
 				mCamera.setParameters(params);
 			}
 		}
 	}

 	// 缩小
 	public void zoomOut() {
 		if (mCamera != null
 				&& mIsZoomSupported
 				&& mZoomRatios != null
 				&& mZoomRatios.size() > 0) {
 			if (mCurrentZoomIndex - 1 >= 0) {
 				--mCurrentZoomIndex;
 				Camera.Parameters params = mCamera.getParameters();
 				params.setZoom(mCurrentZoomIndex);
 				mCamera.setParameters(params);
 			}
 		}
 	}

    private void switchFocusMode(String focusMode) {
        if (mCamera == null || focusMode == null) {
            return;
        }
        Parameters params = mCamera.getParameters();
        if (params.getFocusMode().equals(focusMode)) {
            return;
        }
        if (params.getSupportedFocusModes().contains(focusMode)) {
            params.setFocusMode(focusMode);
            mCamera.setParameters(params);
        }
    }

    public void autoFocus(Observer observer) {
        switchFocusMode(Parameters.FOCUS_MODE_AUTO);
        if (mCamera != null) {
            // autoFocus fail RuntimeException
            try {
                mCamera.autoFocus(new CameraObserver(observer));
            } catch (Exception e) {
                // sorry, nothing I can do
            }
        }
    }

    public void takenPicture(Observer observer) {
        if (mCamera == null || observer == null) {
            return;
        }

        // 如果是前置摄像头直接拍照，无需对焦
        if (isCameraFront()) {
            CameraObserver cameraObserver = new CameraObserver(observer, true);
            try {
                mCamera.takePicture(cameraObserver, null, cameraObserver);
            } catch (Exception e) {
                observer.onException();
            }
        } else {
            switchFocusMode(Parameters.FOCUS_MODE_AUTO);
            try {
                mCamera.autoFocus(new CameraObserver(observer, true));
            } catch (Exception e) {
                observer.onException();
            }
        }
    }

    private final class CameraObserver implements Camera.ShutterCallback, Camera.AutoFocusCallback, Camera.PictureCallback {

        private Observer mObserver;
        private boolean mIsTakenPicture;

        CameraObserver(Observer observer) {
            this(observer, false);
        }

        CameraObserver(Observer observer, boolean isTakenPicture) {
            mObserver = observer;
            mIsTakenPicture = isTakenPicture;
        }

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            // switchFocusMode必须在takePicture之前，否则拍照会出问题
            switchFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.cancelAutoFocus();
            mObserver.onAutoFocus(success);
            if (mIsTakenPicture) {
                // takePicture fail RuntimeException
                try {
                    mCamera.takePicture(this, null, this);
                } catch (Exception e) {
                    mObserver.onException();
                }
            }
        }

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mObserver.onPictureTaken(data);
        }

        @Override
        public void onShutter() {
            mObserver.onShutter();
        }
    }

    public interface Observer {
        public void onAutoFocus(boolean success);

        public void onPictureTaken(byte[] jpeg);

        public void onShutter();

        public void onException();
    }
    


	private final Camera.OnZoomChangeListener mZoomChangeListener = new Camera.OnZoomChangeListener() {

		@Override
		public void onZoomChange(int zoomValue, boolean stopped, Camera camera) {
			// mCurrentZoomIndex = zoomValue;
			mIsZooming = !stopped;
		}
	};

}
