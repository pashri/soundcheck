#include <gtest/gtest.h>

#include <vector>

#include "Mixer.h"

using soundcheck::Command;
using soundcheck::Mixer;
using soundcheck::SampleBank;

namespace {

constexpr int32_t kOnes = 0;      // {1, 1, 1}
constexpr int32_t kHalves = 1;    // {0.5, 0.5}
constexpr int32_t kNegative = 2;  // {-1}

Command schedule(int64_t frame, int32_t id, float gain = 1.0f) {
    return {Command::Type::Schedule, frame, id, gain};
}

Command cancelFrom(int64_t frame) { return {Command::Type::CancelFrom, frame, 0, 0.0f}; }

Command silence() { return {Command::Type::Silence, 0, 0, 0.0f}; }

class MixerTest : public ::testing::Test {
protected:
    void SetUp() override {
        bank.load(kOnes, {1.0f, 1.0f, 1.0f});
        bank.load(kHalves, {0.5f, 0.5f});
        bank.load(kNegative, {-1.0f});
    }

    std::vector<float> render(int32_t frames, int32_t channels = 1) {
        std::vector<float> out(static_cast<size_t>(frames * channels), -9.0f);
        mixer.render(out.data(), frames, channels);
        return out;
    }

    SampleBank bank;
    Mixer mixer{bank};
};

}  // namespace

TEST_F(MixerTest, SilenceRendersZerosAndAdvancesThePosition) {
    for (float value : render(8)) EXPECT_EQ(value, 0.0f);
    EXPECT_EQ(mixer.framePosition(), 8);
}

TEST_F(MixerTest, PositionKeepsCountingAcrossBlocksOfAnySize) {
    render(3);
    render(7);
    render(1);
    EXPECT_EQ(mixer.framePosition(), 11);
}

TEST_F(MixerTest, ScheduledSampleStartsOnItsExactFrame) {
    mixer.push(schedule(5, kOnes));
    auto out = render(10);
    EXPECT_EQ(out[4], 0.0f);
    EXPECT_EQ(out[5], 1.0f);
    EXPECT_EQ(out[7], 1.0f);
    EXPECT_EQ(out[8], 0.0f);
}

TEST_F(MixerTest, SampleCrossingABlockBoundaryContinuesInTheNextBlock) {
    mixer.push(schedule(3, kOnes));
    auto first = render(4);
    auto second = render(4);
    EXPECT_EQ(first[3], 1.0f);
    EXPECT_EQ(second[0], 1.0f);
    EXPECT_EQ(second[1], 1.0f);
    EXPECT_EQ(second[2], 0.0f);
}

TEST_F(MixerTest, StereoOutputCarriesTheSampleOnBothChannels) {
    mixer.push(schedule(0, kHalves));
    auto out = render(2, 2);
    EXPECT_EQ(out, (std::vector<float>{0.5f, 0.5f, 0.5f, 0.5f}));
}

TEST_F(MixerTest, GainScalesTheSample) {
    mixer.push(schedule(0, kHalves, 0.5f));
    EXPECT_FLOAT_EQ(render(1)[0], 0.25f);
}

TEST_F(MixerTest, OverlappingSamplesAddUp) {
    mixer.push(schedule(0, kHalves));
    mixer.push(schedule(0, kHalves, 0.5f));
    EXPECT_FLOAT_EQ(render(1)[0], 0.75f);
}

TEST_F(MixerTest, OutputIsClampedToFullScale) {
    mixer.push(schedule(0, kOnes));
    mixer.push(schedule(0, kOnes));
    mixer.push(schedule(1, kNegative));
    mixer.push(schedule(1, kNegative));
    mixer.push(schedule(1, kNegative));
    mixer.push(schedule(1, kNegative));
    auto out = render(2);
    EXPECT_FLOAT_EQ(out[0], 1.0f);
    EXPECT_FLOAT_EQ(out[1], -1.0f);
}

TEST_F(MixerTest, SoundScheduledInThePastPlaysAtTheStartOfTheNextBlock) {
    render(10);
    mixer.push(schedule(4, kHalves));
    EXPECT_FLOAT_EQ(render(2)[0], 0.5f);
}

TEST_F(MixerTest, CancelFromDropsFutureSoundsButLetsPlayingOnesFinish) {
    mixer.push(schedule(0, kOnes));
    mixer.push(schedule(10, kOnes));
    render(1);
    mixer.push(cancelFrom(1));
    auto out = render(12);  // frames 1..12
    EXPECT_EQ(out[0], 1.0f);
    EXPECT_EQ(out[1], 1.0f);
    EXPECT_EQ(out[2], 0.0f);
    EXPECT_EQ(out[9], 0.0f);
}

TEST_F(MixerTest, SilenceStopsEverythingAtOnce) {
    mixer.push(schedule(0, kOnes));
    mixer.push(schedule(2, kOnes));
    render(1);
    mixer.push(silence());
    for (float value : render(4)) EXPECT_EQ(value, 0.0f);
}

TEST_F(MixerTest, UnknownSampleIsIgnored) {
    mixer.push(schedule(0, 42));
    EXPECT_EQ(render(1)[0], 0.0f);
}

TEST_F(MixerTest, SchedulesBeyondTheVoiceLimitAreDroppedSafely) {
    for (int32_t i = 0; i < Mixer::kMaxVoices + 10; ++i) {
        mixer.push(schedule(0, kHalves, 0.01f));
    }
    EXPECT_NEAR(render(1)[0], 0.5f * 0.01f * Mixer::kMaxVoices, 1e-4f);
}
