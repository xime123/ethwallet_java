#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring

JNICALL
Java_juzix_com_juwallet_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
