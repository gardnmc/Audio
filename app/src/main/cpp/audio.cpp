// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("audio");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("audio")
//      }
//    }


/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <jni.h>

#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_audio_AudioRecordActivity_Java_1com_1example_1audio_1AudioRecordActivity_1stringFromJNI(
        JNIEnv *env, jobject thiz) {
    std::string hello = "Hello from JNI.";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_audio_AudioRecordActivity_test(JNIEnv *env, jobject thiz, jint i) {
    // TODO: implement test()
    return (i+100);
}

extern "C"
JNIEXPORT jshort JNICALL
Java_com_example_audio_AudioRecordActivity_get_1max(JNIEnv *env, jobject thiz, jobject dta, jint size) {
    int i=0;
    int8_t bl = 0,  bh = 0;
    jshort max = 0;
    short v;
    //int8_t *buffer  = new int8_t [size ];

    int8_t * buffer = static_cast<int8_t *>(env->GetDirectBufferAddress(dta));
    //memcpy(buffer, data, size);

    for (i = 0; i < size; i+=2) {
        bl =  buffer[i];
        bh = buffer[i+1];
        v = bl + (bh <<  8 );
        if (v > max) {
            max = v;
        }
    }
//    len = (*env).GetArrayLength( dta);

    return (max);
}