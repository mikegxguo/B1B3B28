LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := framework
LOCAL_MODULE_TAGS := optional
include $(BUILD_MULTI_PREBUILT)

include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := B1B3B28
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_PRIVILEGED_MODULE := true
LOCAL_CERTIFICATE := platform

LOCAL_DEX_PREOPT := false
#LOCAL_PREBUILT_JNI_LIBS := lib/arm64-v8a/libnative-lib.so
LOCAL_PREBUILT_JNI_LIBS := lib/arm64-v8a/libqlmodem.so
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

include $(BUILD_PACKAGE)
