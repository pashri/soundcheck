#include "Mixer.h"

#include <algorithm>
#include <vector>

namespace soundcheck {

namespace {

// The sample's value at a fractional read position, on a straight line between the two
// neighbouring frames.
float interpolate(const std::vector<float>& frames, double position) {
    const auto index = static_cast<size_t>(position);
    const size_t next = std::min(index + 1, frames.size() - 1);
    const auto fraction = static_cast<float>(position - static_cast<double>(index));
    return frames[index] + (frames[next] - frames[index]) * fraction;
}

}  // namespace

Mixer::Mixer(const SampleBank& bank) : bank_(bank) {}

bool Mixer::push(const Command& command) { return queue_.push(command); }

void Mixer::render(float* out, int32_t numFrames, int32_t channelCount) {
    const int64_t blockStart = position_.load(std::memory_order_relaxed);
    Command command{};
    while (queue_.pop(command)) apply(command, blockStart);
    startDue(blockStart + numFrames);
    const int32_t samples = numFrames * channelCount;
    std::fill(out, out + samples, 0.0f);
    for (Voice& voice : voices_) {
        if (voice.active) mixVoice(voice, out, numFrames, channelCount, blockStart);
    }
    for (int32_t i = 0; i < samples; ++i) out[i] = std::clamp(out[i], -1.0f, 1.0f);
    position_.store(blockStart + numFrames, std::memory_order_release);
}

int64_t Mixer::framePosition() const { return position_.load(std::memory_order_acquire); }

int64_t Mixer::droppedCount() const { return dropped_.load(std::memory_order_relaxed); }

void Mixer::apply(const Command& command, int64_t blockStart) {
    switch (command.type) {
        case Command::Type::Schedule:
            if (pendingCount_ == kMaxPending) {
                dropped_.fetch_add(1, std::memory_order_relaxed);
            } else {
                pending_[pendingCount_++] = command;
            }
            break;
        case Command::Type::CancelFrom:
            cancelPendingFrom(command.frame);
            break;
        case Command::Type::Silence:
            pendingCount_ = 0;
            for (Voice& voice : voices_) voice.active = false;
            break;
        case Command::Type::FadeOut:
            pendingCount_ = 0;
            for (Voice& voice : voices_) {
                voice.releaseFrame = std::min(voice.releaseFrame, blockStart);
            }
            break;
    }
}

void Mixer::cancelPendingFrom(int64_t frame) {
    size_t kept = 0;
    for (size_t i = 0; i < pendingCount_; ++i) {
        if (pending_[i].frame < frame) pending_[kept++] = pending_[i];
    }
    pendingCount_ = kept;
}

void Mixer::startDue(int64_t blockEnd) {
    size_t kept = 0;
    for (size_t i = 0; i < pendingCount_; ++i) {
        if (pending_[i].frame < blockEnd) {
            startVoice(pending_[i]);
        } else {
            pending_[kept++] = pending_[i];
        }
    }
    pendingCount_ = kept;
}

void Mixer::startVoice(const Command& command) {
    const Sample* sample = bank_.get(command.sampleId);
    if (sample == nullptr || sample->frames.empty() || !(command.rate > 0.0f)) return;
    const int64_t release =
            command.lengthFrames > 0 ? command.frame + command.lengthFrames : kNever;
    voiceToUse() = Voice{sample, command.frame, release, 0.0, command.gain, command.rate, true};
}

Mixer::Voice& Mixer::voiceToUse() {
    auto free = std::find_if(voices_.begin(), voices_.end(),
                             [](const Voice& voice) { return !voice.active; });
    if (free != voices_.end()) return *free;
    dropped_.fetch_add(1, std::memory_order_relaxed);
    return *std::min_element(voices_.begin(), voices_.end(),
                             [](const Voice& a, const Voice& b) {
                                 return a.startFrame < b.startFrame;
                             });
}

void Mixer::mixVoice(Voice& voice, float* out, int32_t numFrames, int32_t channelCount,
                     int64_t blockStart) {
    const auto& frames = voice.sample->frames;
    const auto last = static_cast<double>(frames.size() - 1);
    for (int64_t i = std::max<int64_t>(voice.startFrame - blockStart, 0); i < numFrames; ++i) {
        const int64_t released = blockStart + i - voice.releaseFrame;
        const float envelope =
                released < 0 ? 1.0f : 1.0f - static_cast<float>(released) / kReleaseFrames;
        if (voice.cursor > last || envelope <= 0.0f) {
            voice.active = false;
            return;
        }
        const float value = interpolate(frames, voice.cursor) * voice.gain * envelope;
        for (int32_t channel = 0; channel < channelCount; ++channel) {
            out[i * channelCount + channel] += value;
        }
        voice.cursor += voice.rate;
    }
    if (voice.cursor > last) voice.active = false;
}

}  // namespace soundcheck
