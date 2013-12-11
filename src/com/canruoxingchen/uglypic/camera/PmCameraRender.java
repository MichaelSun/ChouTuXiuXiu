package com.canruoxingchen.uglypic.camera;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;

import com.canruoxingchen.uglypic.util.jni.NativeImageUtil;

public class PmCameraRender extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    Context mContext;

    final PmCameraFbo mFboExternal = new PmCameraFbo();
    final PmCameraFbo mFboOffscreen = new PmCameraFbo();
    Observer mObserver;

    PmCameraData mPmCameraData;
    SurfaceTexture mSurfaceTexture;
    boolean mSurfaceTextureUpdate;
    final float[] mSurfaceTextureTransformM = new float[16];

    boolean mNeedReInitialize = true;
    // frameAvailable之后才开始draw
    boolean mHasFrameBuffer = false;

    public PmCameraRender(Context context) {
        super(context);

        init(context);
    }

    public PmCameraRender(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        mHasFrameBuffer = false;
    }

    private void init(Context context) {
        mContext = context;

        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @SuppressLint("InlinedApi")
    @Override
    public void onDrawFrame(GL10 unused) {
        if (mNeedReInitialize) {
            mNeedReInitialize = false;

            Matrix.setIdentityM(mPmCameraData.mOrientationM, 0);
            mFboExternal.init(0, 0, true);
            mFboOffscreen.init(mPmCameraData.mPreviewWidth, mPmCameraData.mPreviewHeight, false);

            SurfaceTexture oldSurfaceTexture = mSurfaceTexture;
            mSurfaceTexture = new SurfaceTexture(mFboExternal.getTexture());
            mSurfaceTexture.setOnFrameAvailableListener(this);
            if (mObserver != null) {
                mObserver.onSurfaceTextureCreated(mSurfaceTexture);
            }
            if (oldSurfaceTexture != null) {
                oldSurfaceTexture.release();
            }
        }

        if (mSurfaceTextureUpdate) {
            mSurfaceTextureUpdate = false;
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(mSurfaceTextureTransformM);

            mFboOffscreen.bind();

            NativeImageUtil.getInstance(mContext).oesOnDrawFrame(mFboExternal.getTexture(), mSurfaceTextureTransformM);
        }

        if (mHasFrameBuffer) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glViewport(0, 0, mPmCameraData.mGLSurfaceWidth, mPmCameraData.mGLSurfaceHeight);

            float ratioX = mPmCameraData.mAspectRatio[0] / mPmCameraData.mAspectRatioPreview[0];
            float ratioY = mPmCameraData.mAspectRatio[1] / mPmCameraData.mAspectRatioPreview[1];

            NativeImageUtil.getInstance(mContext).normalOnDrawFrame(0, mFboOffscreen.getTexture(), mPmCameraData.mAspectRatio, mPmCameraData.mAspectRatioPreview,
                    new float[] { mPmCameraData.mCropTranslate[0] + 1.0f - ratioX, mPmCameraData.mCropTranslate[1] + 1.0f - ratioY }, mPmCameraData.mCropScale,
                    mPmCameraData.mOrientationM);
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (!mHasFrameBuffer) {
            mHasFrameBuffer = true;
        }
        mSurfaceTextureUpdate = true;
        requestRender();
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        mPmCameraData.mGLSurfaceWidth = width;
        mPmCameraData.mGLSurfaceHeight = height;

        mPmCameraData.mAspectRatio[0] = (float) Math.min(width, height) / width;
        mPmCameraData.mAspectRatio[1] = (float) Math.min(width, height) / height;

        mNeedReInitialize = true;
    }

    @Override
    public synchronized void onSurfaceCreated(GL10 unused, EGLConfig config) {
        mFboExternal.reset();
        mFboOffscreen.reset();
    }

    public void setObserver(Observer observer) {
        mObserver = observer;
    }

    public void setPmCameraData(PmCameraData pmCameraData) {
        mPmCameraData = pmCameraData;
    }

    public interface Observer {
        public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture);
    }

}
