#include "Mixer.h"

#include <algorithm>

namespace soundcheck {

Mixer::Mixer(const SampleBank& bank) : bank_(bank) {}

bool Mixer::push(const Command& command) { return queue_.push(command); }

void Mixer::render(float* out, int32_t numFrames, int32_t channelCount) {
    Command command{};
    while (queue_.pop(command)) apply(command);
    const int32_t samples = numFrames * channelCount;
    std::fill(out, out + samples, 0.0f);
    const int64_t blockStart = position_.load(std::memory_order_relaxed);
    for (Voice& voice : voices_) {
        if (voice.active) mixVoice(voice, out, numFrames, channelCount, blockStart);
    }
    for (int32_t i = 0; i < samples; ++i) out[i] = std::clamp(out[i], -1.0f, 1.0f);
    position_.store(blockStart + numFrames, std::memory_order_release);
}

int64_t Mixer::framePosition() const { return position_.load(std::memory_order_acquire); }

void Mixer::apply(const Command& command) {
    switch (command.type) {
        case Command::Type::Schedule:
            startVoice(command);
            break;
        case Command::Type::CancelFrom:
            for (Voice& voice : voices_) {
                if (voice.cursor == 0 && voice.startFrame >= command.frame) voice.active = false;
            }
            break;
        case Command::Type::Silence:
            for (Voice& voice : voices_) voice.active = false;
            break;
    }
}

void Mixer::startVoice(const Command& command) {
    const Sample* sample = bank_.get(command.sampleId);
    if (sample == nullptr || sample->frames.empty()) return;
    auto free = std::find_if(voices_.begin(), voices_.end(),
                             [](const Voice& voice) { return !voice.active; });
    if (free == voices_.end()) return;  // every voice busy: drop the new one, cut nothing off
    *free = Voice{sample, command.frame, 0, command.gain, true};
}

void Mixer::mixVoice(Voice& voice, float* out, int32_t numFrames, int32_t channelCount,
                     int64_t blockStart) {
    const int64_t first = std::max<int64_t>(voice.startFrame - blockStart, 0);
    if (first >= numFrames) return;
    const auto& frames = voice.sample->frames;
    const auto length = static_cast<int64_t>(frames.size());
    for (int64_t i = first; i < numFrames && voice.cursor < length; ++i, ++voice.cursor) {
        const float value = frames[static_cast<size_t>(voice.cursor)] * voice.gain;
        for (int32_t channel = 0; channel < channelCount; ++channel) {
            out[i * channelCount + channel] += value;
        }
    }
    if (voice.cursor >= length) voice.active = false;
}

}  // namespace soundcheck
