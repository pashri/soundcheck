#include "SampleBank.h"

#include <utility>

namespace soundcheck {

namespace {
bool inRange(int32_t id) { return id >= 0 && id < SampleBank::kMaxSamples; }
}  // namespace

SampleBank::SampleBank() {
    for (auto& slot : slots_) slot.store(nullptr, std::memory_order_relaxed);
}

bool SampleBank::load(int32_t id, std::vector<float> frames) {
    if (!inRange(id)) return false;
    auto sample = std::make_unique<Sample>(Sample{std::move(frames)});
    slots_[id].store(sample.get(), std::memory_order_release);
    owned_.push_back(std::move(sample));
    return true;
}

const Sample* SampleBank::get(int32_t id) const {
    return inRange(id) ? slots_[id].load(std::memory_order_acquire) : nullptr;
}

}  // namespace soundcheck
