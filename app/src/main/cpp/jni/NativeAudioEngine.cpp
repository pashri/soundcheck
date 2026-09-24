#include <jni.h>

#include <utility>
#include <vector>

#include "engine/AudioEngine.h"

using soundcheck::AudioEngine;
using soundcheck::Command;

namespace {
AudioEngine* engineFrom(jlong handle) { return reinterpret_cast<AudioEngine*>(handle); }
jboolean toJni(bool value) { return value ? JNI_TRUE : JNI_FALSE; }
}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_org_pashri_soundcheck_audio_NativeAudioEngine_nativeCreate(JNIEnv*, jobject) {
    return reinterpret_cast<jlong>(new AudioEngine());
}

JNIEXPORT jboolean JNICALL
Java_org_pashri_soundcheck_audio_NativeAudioEngine_nativeStart(JNIEnv*, jobject, jlong handle) {
    return toJni(engineFrom(handle)->start());
}

JNIEXPORT void JNICALL
Java_org_pashri_soundcheck_audio_NativeAudioEngine_nativeStop(JNIEnv*, jobject, jlong handle) {
    engineFrom(handle)->stop();
}

JNIEXPORT jboolean JNICALL
Java_org_pashri_soundcheck_audio_NativeAudioEngine_nativeLoadSample(
        JNIEnv* env, jobject, jlong handle, jint id, jfloatArray pcm) {
    std::vector<float> frames(static_cast<size_t>(env->GetArrayLength(pcm)));
    env->GetFloatArrayRegion(pcm, 0, static_cast<jsize>(frames.size()), frames.data());
    return toJni(engineFrom(handle)->loadSample(id, std::move(frames)));
}

JNIEXPORT jboolean JNICALL
Java_org_pashri_soundcheck_audio_NativeAudioEngine_nativeSchedule(
        JNIEnv*, jobject, jlong handle, jint id, jlong frame, jfloat gain) {
    return toJni(engineFrom(handle)->push({Command::Type::Schedule, frame, id, gain}));
}

JNIEXPORT void JNICALL
Java_org_pashri_soundcheck_audio_NativeAudioEngine_nativeCancelFrom(
        JNIEnv*, jobject, jlong handle, jlong frame) {
    engineFrom(handle)->push({Command::Type::CancelFrom, frame, 0, 0.0f});
}

JNIEXPORT void JNICALL
Java_org_pashri_soundcheck_audio_NativeAudioEngine_nativeSilence(JNIEnv*, jobject, jlong handle) {
    engineFrom(handle)->push({Command::Type::Silence, 0, 0, 0.0f});
}

JNIEXPORT jlong JNICALL
Java_org_pashri_soundcheck_audio_NativeAudioEngine_nativeFramePosition(
        JNIEnv*, jobject, jlong handle) {
    return engineFrom(handle)->framePosition();
}

}  // extern "C"
