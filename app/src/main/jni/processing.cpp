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
#include <string.h>
#include <jni.h>
#include <inttypes.h>
#include <android/log.h>
#include <essentia/algorithmfactory.h>
#include <essentia/pool.h>
#include <fstream>
#include <vector>
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, "hell-libs::", __VA_ARGS__))

/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   app/src/main/java/com/example/hellolibs/MainActivity.java
 */
extern "C" {
using namespace std;
using namespace essentia;
using namespace essentia::standard;
JNIEXPORT jint JNICALL
    Java_com_pcom_nextpl_ListActivity_process(JNIEnv *env, jobject thiz, jint rcmsize, jfloatArray rcm, jobject globalPitch) {


        vector<Real> sig(rcmsize);
        jfloat* inrcm = env->GetFloatArrayElements(rcm, NULL);
        for (int i = 0; i < rcmsize; i++)
            sig[i] = inrcm[i];
        essentia::init();
//        ofstream f;
//        f.open(s);
//        f.close();
//        cout << s << endl;
//        FILE* file = fopen(buf,"w+");
//        if (file != NULL)
//            fclose(file);
//        Pool pool;
//        string u = "privet";
//        pool.set("test.test", u);
//        s = pool.value<string>("test.test");
        AlgorithmFactory& factory = standard::AlgorithmFactory::instance();
        Algorithm* melodia = factory.create("MultiPitchKlapuri");

        melodia->input("signal").set(sig);

        vector<vector<Real> > pitch;
        melodia->output("pitch").set(pitch);
        melodia->compute();
//        jint r = -1;
        jint r = pitch.size();
        delete melodia;
        jclass arrayListClass = env->GetObjectClass(globalPitch);
        jclass floatClass = env->FindClass("java/lang/Float");
        jmethodID addId = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
        for (int i = 0; i < pitch.size(); i++)
        {
            jobject voices = env->NewObject(arrayListClass, env->GetMethodID(arrayListClass, "<init>", "()V"));
            for (int j = 0; j < pitch[i].size(); j++)
            {
                jobject val = env->NewObject(floatClass, env->GetMethodID(floatClass, "<init>", "(F)V"), (jfloat) pitch[i][j]);
                env->CallBooleanMethod(voices, addId, val);
                env->DeleteLocalRef(val);
            }
            env->CallBooleanMethod(globalPitch, addId, voices);
            env->DeleteLocalRef(voices);
        }
        essentia::shutdown();
        return r;
    }

}