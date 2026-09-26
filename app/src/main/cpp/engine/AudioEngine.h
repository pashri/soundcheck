#pragma once

#include <atomic>
#include <memory>
#include <mutex>
#include <vector>

#include <oboe/Oboe.h>

#include "Mixer.h"
#include "SampleBank.h"

namespace soundcheck {

constexpr int32_t kSampleRate = 48000;

// Owns the Oboe output stream and feeds it from the Mixer. If the device output changes
// (headphones, Bluetooth), Oboe closes the stream and this reopens it; the Mixer, and so the
// frame count and every scheduled sound, carries on untouched. If the reopen fails,
// hasFailed() says so until the next successful start().
class AudioEngine : public oboe::AudioStreamDataCallback,
                    public oboe::AudioStreamErrorCallback {
public:
    AudioEngine() = default;
    ~AudioEngine() override;

    bool start();
    void stop();
    bool loadSample(int32_t id, std::vector<float> frames);
    bool push(const Command& command);
    int64_t framePosition() const;
    bool hasFailed() const;

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData,
                                          int32_t numFrames) override;
    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) override;

private:
    oboe::Result openAndStart();

    SampleBank bank_;
    Mixer mixer_{bank_};
    std::shared_ptr<oboe::AudioStream> stream_;
    std::mutex controlLock_;    // serialises control-thread callers; never taken in onAudioReady
    bool wantRunning_ = false;  // guarded by controlLock_
    std::atomic<bool> failed_{false};
};

}  // namespace soundcheck
