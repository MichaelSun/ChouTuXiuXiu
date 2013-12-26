package com.canruoxingchen.uglypic.util.jni;

import android.content.Context;
import android.graphics.Bitmap;

public class NativeImageUtil {
    private Context mContext;
    private static NativeImageUtil gInstance;

    static {
        System.loadLibrary("ndk");
    }

    private NativeImageUtil(Context context) {
        mContext = context.getApplicationContext();
    }

    public synchronized static NativeImageUtil getInstance(Context context) {
        if (gInstance == null) {
            gInstance = new NativeImageUtil(context);
        }

        return gInstance;
    }

    private Context getContext() {
        return mContext;
    }

    /**
     * 渲染GL_TEXTURE_EXTERNAL_OES纹理
     * 
     * @param oesTextureId
     *            与SurfaceTexture相对应的GL_TEXTURE_EXTERNAL_OES纹理Id
     * @param mTransformM
     *            SurfaceTexture中获取到的顶点变换参数
     *            {@link android.graphics.SurfaceTexture#getTransformMatrix}
     * @return
     */
    public native int oesOnDrawFrame(int oesTextureId, float[] mTransformM);

    /**
     * 选择相对的滤镜对textureId的纹理进行渲染
     * 
     * @param filterType
     *            滤镜类型
     * @param textureId
     *            渲染的图片在GPU中的纹理Id
     * @param uAspectRatio
     *            顶点变换参数，影响图片大小和位置
     * @param uAspectRatioPreview
     *            定点变换参数，影响图片大小和位置
     * @param uTranslate
     *            图片裁剪过程坐标的水平竖直移动
     * @param uScale
     *            图片裁剪过程缩放值
     * 
     * @return
     */
    public native int normalOnDrawFrame(int filterType, int textureId, float[] uAspectRatio, float[] uAspectRatioPreview, float[] uTranslate, float uScale,
            float[] mOrientationM);

    /**
     * 将inFilePath指向的文件滤镜处理，输出到outFilePath指向的文件中
     * 
     * @param inFilePath
     *            输入文件全路径
     * @param outFilePath
     *            输出文件全路径，没有则创建
     * @param orientation
     *            图片旋转角度修正
     * @param minWidth
     *            短边的长度最大值
     * @param mNeedMirror
     *            是否镜像（即是否前置摄像头）
     * @param isSquare
     *            是否方形图
     * @return
     */
    public native int saveCameraPhoto(String inFilePath, String outFilePath, int orientation, int minWidth, int mNeedMirror, int isSquare);

    /**
     * 处理相机数据
     * 
     * @param inFilePath
     *            输入文件路径
     * @param outFilePath
     *            输出文件路径
     * @param orientation
     *            转正需要的旋转角度
     * @param mirror
     *            是否镜像处理
     * @param left
     *            裁剪区域左上角位置(相对于图片转正之后的)
     * @param top
     *            裁剪区域左上角位置(相对于图片转正之后的)
     * @param width
     *            裁剪区域宽度
     * @param height
     *            裁剪区域高度
     * @param minWidth
     *            最小边的最大长度
     * @return
     */
    public native int saveCameraPhotoWithoutGL(String inFilePath, String outFilePath, int orientation, int mirror, int left, int top, int width, int height,
            int minWidth);
    
    /**
     * 裁剪放缩图片
     * 
     * @param inFilePath
     *            输入文件全路径
     * @param outFilePath
     *            输出文件全路径
     * @param mAspectRatio
     *            viewPort比例
     * @param mTranslate
     *            平移参数
     * @param mScale
     *            缩放参数
     * @param minWidth
     *            短边的长度最大值
     * @param isSquare
     *            裁剪结果是否方形
     * 
     * @return
     */
    public native int cropPhoto(String inFilePath, String outFilePath, float[] mAspectRatio, float[] mTranslate, float mScale, int minWidth, int isSquare);
    
    public native int mergePhoto(String origPath, String processedPath, String outFilePath, int origWidth, int origHeight, int processedWidth, int processedHeight);

    /**
     * 将传入的bitmap做高斯模糊处理，会改变传入的图
     * 
     * @param bitmap
     * @return 成功返回1，失败返回0
     */
    public native int processGaussianBlur(Bitmap bitmap);

    /**
     * 裁剪图片
     * 
     * @param inFilePath
     *            输入路径
     * @param outFilePath
     *            输出路径
     * @param minWidth
     *            最小边的最大长度
     * @param left
     *            按rotation旋转后裁剪区域左边局相对于原图左边距的距离占整个宽度的百分比
     * @param top
     *            按rotation旋转后裁剪区域上边距相对于原图上边距的距离占整个高度的百分比
     * @param width
     *            按rotation旋转后裁剪区域的宽度
     * @param height
     *            按rotation旋转后裁剪区域的高度
     * @param rotation
     *            图片转正需要旋转的角度
     * @return
     */
    public native int cropPhotoWithoutGL(String inFilePath, String outFilePath, int minWidth, int left, int top, int width, int height, int rotation);

    /**
     * 销毁gl参数
     * 
     * @return
     */
    public native int onDestroy();

    /**
     * Useless method, just use to test native code.
     */
    public native void test();
}