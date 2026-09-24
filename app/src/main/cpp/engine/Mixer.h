#pragma once

#include <array>
#include <atomic>
#include <cstddef>
#include <cstdint>

#include "EventQueue.h"
#include "SampleBank.h"

namespace soundcheck {

// A request from the control thread to the audio thread.
struct Command {
    enum class Type : int32_t { Schedule, CancelFrom, Silence };
    Type type;
    int64_t frame;     // Schedule: start frame. CancelFrom: first frame to cancel.
    int32_t sampleId;  // Schedule only.
    float gain;        // Schedule only.
};

// Mixes scheduled samples into audio blocks and counts the frames rendered. The count lives
// here rather than in the Oboe stream so it survives the stream being reopened.
class Mixer {
public:
    static constexpr int32_t kMaxVoices = 64;
    static constexpr size_t kQueueCapacity = 1024;

    explicit Mixer(const SampleBank& bank);

    // Control thread; callers must not push from two threads at once.
    bool push(const Command& command);

    // Audio thread: fills `out` with `numFrames` interleaved frames.
    void render(float* out, int32_t numFrames, int32_t channelCount);

    // Any thread: the next frame render() will produce. Never goes backwards.
    int64_t framePosition() const;

private:
    struct Voice {
        const Sample* sample = nullptr;
        int64_t startFrame = 0;
        int64_t cursor = 0;
        float gain = 0.0f;
        bool active = false;
    };

    void apply(const Command& command);
    void startVoice(const Command& command);
    void mixVoice(Voice& voice, float* out, int32_t numFrames, int32_t channelCount,
                  int64_t blockStart);

    const SampleBank& bank_;
    EventQueue<Command, kQueueCapacity> queue_;
    std::array<Voice, kMaxVoices> voices_{};
    std::atomic<int64_t> position_{0};
};

}  // namespace soundcheck
