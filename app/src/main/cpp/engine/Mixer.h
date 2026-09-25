#pragma once

#include <array>
#include <atomic>
#include <cstddef>
#include <cstdint>
#include <limits>

#include "EventQueue.h"
#include "SampleBank.h"

namespace soundcheck {

// A request from the control thread to the audio thread.
struct Command {
    enum class Type : int32_t { Schedule, CancelFrom, Silence, FadeOut };
    Type type;
    int64_t frame;             // Schedule: start frame. CancelFrom: first frame to cancel.
    int32_t sampleId;          // Schedule only.
    float gain;                // Schedule only.
    float rate = 1.0f;         // Schedule only: sample frames per output frame; 2 is an octave up.
    int64_t lengthFrames = 0;  // Schedule only: frames until the fade-out; 0 plays it all.
};

// Mixes scheduled samples into audio blocks and counts the frames rendered. The count lives
// here rather than in the Oboe stream so it survives the stream being reopened. A scheduled
// sound waits in a pending list and takes a voice only when its start frame arrives.
class Mixer {
public:
    static constexpr int32_t kMaxVoices = 64;
    static constexpr size_t kMaxPending = 256;
    static constexpr size_t kQueueCapacity = 1024;
    static constexpr int64_t kReleaseFrames = 4800;  // 100 ms fade-out at 48 kHz

    explicit Mixer(const SampleBank& bank);

    // Control thread; callers must not push from two threads at once.
    bool push(const Command& command);

    // Audio thread: fills `out` with `numFrames` interleaved frames.
    void render(float* out, int32_t numFrames, int32_t channelCount);

    // Any thread: the next frame render() will produce. Never goes backwards.
    int64_t framePosition() const;

    // Any thread: a diagnostic counter for tests, not read by the app. Sounds lost so far,
    // because the pending list was full or an older sound's voice was taken to make room.
    int64_t droppedCount() const;

private:
    static constexpr int64_t kNever = std::numeric_limits<int64_t>::max();

    struct Voice {
        const Sample* sample = nullptr;
        int64_t startFrame = 0;
        int64_t releaseFrame = kNever;  // the output frame its fade-out starts on
        double cursor = 0.0;            // read position in the sample, in sample frames
        float gain = 0.0f;
        float rate = 1.0f;
        bool active = false;
    };

    void apply(const Command& command, int64_t blockStart);
    void cancelPendingFrom(int64_t frame);
    void startDue(int64_t blockEnd);
    void startVoice(const Command& command);
    Voice& voiceToUse();
    void mixVoice(Voice& voice, float* out, int32_t numFrames, int32_t channelCount,
                  int64_t blockStart);

    const SampleBank& bank_;
    EventQueue<Command, kQueueCapacity> queue_;
    std::array<Command, kMaxPending> pending_{};
    size_t pendingCount_ = 0;
    std::array<Voice, kMaxVoices> voices_{};
    std::atomic<int64_t> position_{0};
    std::atomic<int64_t> dropped_{0};
};

}  // namespace soundcheck
