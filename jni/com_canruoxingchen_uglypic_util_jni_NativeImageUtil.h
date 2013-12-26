/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_canruoxingchen_uglypic_util_jni_NativeImageUtil */

#ifndef _Included_com_canruoxingchen_uglypic_util_jni_NativeImageUtil
#define _Included_com_canruoxingchen_uglypic_util_jni_NativeImageUtil
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_canruoxingchen_uglypic_util_jni_NativeImageUtil
 * Method:    oesOnDrawFrame
 * Signature: (I[F)I
 */
JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_oesOnDrawFrame
  (JNIEnv *, jobject, jint, jfloatArray);

/*
 * Class:     com_canruoxingchen_uglypic_util_jni_NativeImageUtil
 * Method:    normalOnDrawFrame
 * Signature: (II[F[F[FF[F)I
 */
JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_normalOnDrawFrame
  (JNIEnv *, jobject, jint, jint, jfloatArray, jfloatArray, jfloatArray, jfloat, jfloatArray);

/*
 * Class:     com_canruoxingchen_uglypic_util_jni_NativeImageUtil
 * Method:    saveCameraPhoto
 * Signature: (Ljava/lang/String;Ljava/lang/String;IIII)I
 */
JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_saveCameraPhoto
  (JNIEnv *, jobject, jstring, jstring, jint, jint, jint, jint);

/*
 * Class:     com_canruoxingchen_uglypic_util_jni_NativeImageUtil
 * Method:    saveCameraPhotoWithoutGL
 * Signature: (Ljava/lang/String;Ljava/lang/String;IIIIIII)I
 */
JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_saveCameraPhotoWithoutGL
  (JNIEnv *, jobject, jstring, jstring, jint, jint, jint, jint, jint, jint, jint);

/*
 * Class:     com_canruoxingchen_uglypic_util_jni_NativeImageUtil
 * Method:    cropPhoto
 * Signature: (Ljava/lang/String;Ljava/lang/String;[F[FFII)I
 */
JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_cropPhoto
  (JNIEnv *, jobject, jstring, jstring, jfloatArray, jfloatArray, jfloat, jint, jint);

/*
 * Class:     com_canruoxingchen_uglypic_util_jni_NativeImageUtil
 * Method:    mergePhoto
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIII)I
 */
JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_mergePhoto
  (JNIEnv *, jobject, jstring, jstring, jstring, jint, jint, jint, jint);

/*
 * Class:     com_canruoxingchen_uglypic_util_jni_NativeImageUtil
 * Method:    processGaussianBlur
 * Signature: (Landroid/graphics/Bitmap;)I
 */
JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_processGaussianBlur
  (JNIEnv *, jobject, jobject);

/*
 * Class:     com_canruoxingchen_uglypic_util_jni_NativeImageUtil
 * Method:    cropPhotoWithoutGL
 * Signature: (Ljava/lang/String;Ljava/lang/String;IIIIII)I
 */
JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_cropPhotoWithoutGL
  (JNIEnv *, jobject, jstring, jstring, jint, jint, jint, jint, jint, jint);

/*
 * Class:     com_canruoxingchen_uglypic_util_jni_NativeImageUtil
 * Method:    onDestroy
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_onDestroy
  (JNIEnv *, jobject);

/*
 * Class:     com_canruoxingchen_uglypic_util_jni_NativeImageUtil
 * Method:    test
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_test
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
