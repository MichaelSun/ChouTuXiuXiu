LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
	LOCAL_CFLAGS := -DHAVE_NEON=1
endif

LOCAL_MODULE    := ndk
LOCAL_SRC_FILES := rrimagelib.c rrutil_exif.c main.cpp libnsgif.c
LOCAL_LDLIBS +=  -lm -llog -ljnigraphics -lz -lEGL -lGLESv2 -landroid 

LOCAL_STATIC_LIBRARIES += libjpeg
LOCAL_STATIC_LIBRARIES += libpng
LOCAL_STATIC_LIBRARIES += libexif
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libjpeg
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libpng
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libexif

include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH)/libjpeg/Android.mk $(LOCAL_PATH)/libpng/Android.mk $(LOCAL_PATH)/libexif/Android.mk 
