#pragma once

#include <array>
#include <atomic>
#include <cstdint>
#include <memory>
#include <vector>

namespace soundcheck {

// One mono sound at the engine's sample rate.
struct Sample {
    std::vector<float> frames;
};

// Samples by id. load() runs on the control thread; get() is safe on the audio thread.
// A replaced sample stays allocated until the bank is destroyed, because a voice may still
// be playing it.
class SampleBank {
public:
    static constexpr int32_t kMaxSamples = 256;

    SampleBank();

    bool load(int32_t id, std::vector<float> frames);
    const Sample* get(int32_t id) const;

private:
    std::array<std::atomic<const Sample*>, kMaxSamples> slots_;
    std::vector<std::unique_ptr<Sample>> owned_;
};

}  // namespace soundcheck
