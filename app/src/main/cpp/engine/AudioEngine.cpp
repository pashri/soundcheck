#include "AudioEngine.h"

#include <utility>

#include <android/log.h>

namespace soundcheck {

namespace {
constexpr const char* kTag = "SoundcheckEngine";
}  // namespace

AudioEngine::~AudioEngine() { stop(); }

bool AudioEngine::start() {
    std::lock_guard<std::mutex> lock(controlLock_);
    wantRunning_ = true;
    if (stream_) return true;
    return openAndStart() == oboe::Result::OK;
}

void AudioEngine::stop() {
    std::lock_guard<std::mutex> lock(controlLock_);
    wantRunning_ = false;
    if (!stream_) return;
    stream_->stop();
    stream_->close();
    stream_.reset();
}

bool AudioEngine::loadSample(int32_t id, std::vector<float> frames) {
    std::lock_guard<std::mutex> lock(controlLock_);
    return bank_.load(id, std::move(frames));
}

bool AudioEngine::push(const Command& command) {
    std::lock_guard<std::mutex> lock(controlLock_);
    return mixer_.push(command);
}

int64_t AudioEngine::framePosition() const { return mixer_.framePosition(); }

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream* stream, void* audioData,
                                                   int32_t numFrames) {
    mixer_.render(static_cast<float*>(audioData), numFrames, stream->getChannelCount());
    return oboe::DataCallbackResult::Continue;
}

void AudioEngine::onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) {
    __android_log_print(ANDROID_LOG_WARN, kTag, "Output closed (%s), reopening",
                        oboe::convertToText(error));
    std::lock_guard<std::mutex> lock(controlLock_);
    if (stream != stream_.get()) return;
    stream_.reset();
    if (wantRunning_) openAndStart();
}

oboe::Result AudioEngine::openAndStart() {
    oboe::AudioStreamBuilder builder;
    oboe::Result result = builder.setDirection(oboe::Direction::Output)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Shared)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(oboe::ChannelCount::Stereo)
            ->setSampleRate(kSampleRate)
            ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Medium)
            ->setUsage(oboe::Usage::Media)
            ->setContentType(oboe::ContentType::Music)
            ->setDataCallback(this)
            ->setErrorCallback(this)
            ->openStream(stream_);
    if (result == oboe::Result::OK) result = stream_->requestStart();
    if (result != oboe::Result::OK) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "Output failed to start: %s",
                            oboe::convertToText(result));
        if (stream_) stream_->close();
        stream_.reset();
    }
    return result;
}

}  // namespace soundcheck
