package com.canruoxingchen.uglypic.camera;

import android.annotation.SuppressLint;
import android.opengl.GLES20;

public final class PmCameraFbo {
    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8d65;

    private int mFrameBufferHandle = -1;
    private int mFrameBufferTextureHandler = -1;
    private int mWidth, mHeight;

    public void bind() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferHandle);
        GLES20.glViewport(0, 0, mWidth, mHeight);
    }

    public int getHeight() {
        return mHeight;
    }

    public int getTexture() {
        return mFrameBufferTextureHandler;
    }

    public int getWidth() {
        return mWidth;
    }

    @SuppressLint("InlinedApi")
    public void init(int width, int height, boolean textureExternalOES) {
        reset();

        mWidth = width;
        mHeight = height;

        int frameBuffer[] = new int[1];
        GLES20.glGenFramebuffers(1, frameBuffer, 0);
        mFrameBufferHandle = frameBuffer[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferHandle);

        int frameBufferTexture[] = new int[1];
        GLES20.glGenTextures(1, frameBufferTexture, 0);
        mFrameBufferTextureHandler = frameBufferTexture[0];
        int target = textureExternalOES ? GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D;
        GLES20.glBindTexture(target, frameBufferTexture[0]);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        if (target == GLES20.GL_TEXTURE_2D) {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, target, frameBufferTexture[0], 0);
        }
    }

    public void reset() {
        if (mFrameBufferHandle != -1) {
            int[] handle = { mFrameBufferHandle };
            GLES20.glDeleteFramebuffers(1, handle, 0);
            int[] texture = { mFrameBufferTextureHandler };
            GLES20.glDeleteTextures(texture.length, texture, 0);
            mFrameBufferHandle = -1;
            mFrameBufferTextureHandler = -1;
            mWidth = mHeight = 0;
        }
    }

}
