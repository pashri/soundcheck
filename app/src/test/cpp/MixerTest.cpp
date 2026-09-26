#include <gtest/gtest.h>

#include <limits>
#include <vector>

#include "Mixer.h"

using soundcheck::Command;
using soundcheck::Mixer;
using soundcheck::SampleBank;

namespace {

constexpr int32_t kOnes = 0;      // {1, 1, 1}
constexpr int32_t kHalves = 1;    // {0.5, 0.5}
constexpr int32_t kNegative = 2;  // {-1}
constexpr int32_t kRamp = 3;      // {0, 0.25, 0.5, 0.75, 1}
constexpr int32_t kLong = 4;      // 10 000 frames of 1

Command schedule(int64_t frame, int32_t id, float gain = 1.0f) {
    return {Command::Type::Schedule, frame, id, gain};
}

Command note(int64_t frame, int32_t id, float rate, int64_t lengthFrames) {
    return {Command::Type::Schedule, frame, id, 1.0f, rate, lengthFrames};
}

Command cancelFrom(int64_t frame) { return {Command::Type::CancelFrom, frame, 0, 0.0f}; }

Command silence() { return {Command::Type::Silence, 0, 0, 0.0f}; }

Command fadeOut() { return {Command::Type::FadeOut, 0, 0, 0.0f}; }

class MixerTest : public ::testing::Test {
protected:
    void SetUp() override {
        bank.load(kOnes, {1.0f, 1.0f, 1.0f});
        bank.load(kHalves, {0.5f, 0.5f});
        bank.load(kNegative, {-1.0f});
        bank.load(kRamp, {0.0f, 0.25f, 0.5f, 0.75f, 1.0f});
        bank.load(kLong, std::vector<float>(10000, 1.0f));
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

TEST_F(MixerTest, SchedulesBeyondTheVoiceLimitTakeOverTheOldestVoice) {
    for (int32_t i = 0; i < Mixer::kMaxVoices + 10; ++i) {
        mixer.push(schedule(0, kHalves, 0.01f));
    }
    EXPECT_NEAR(render(1)[0], 0.5f * 0.01f * Mixer::kMaxVoices, 1e-4f);
}

TEST_F(MixerTest, HalfRatePlaysAnOctaveDownWithInterpolatedValues) {
    mixer.push(note(0, kRamp, 0.5f, 0));
    auto out = render(10);
    EXPECT_EQ(out, (std::vector<float>{0.0f, 0.125f, 0.25f, 0.375f, 0.5f, 0.625f, 0.75f,
                                       0.875f, 1.0f, 0.0f}));
}

TEST_F(MixerTest, DoubleRatePlaysAnOctaveUpBySkippingFrames) {
    mixer.push(note(0, kRamp, 2.0f, 0));
    auto out = render(5);
    EXPECT_EQ(out, (std::vector<float>{0.0f, 0.5f, 1.0f, 0.0f, 0.0f}));
}

TEST_F(MixerTest, ASoundIsHeldForItsLengthThenFadesOutOverTheRelease) {
    mixer.push(note(0, kLong, 1.0f, 100));
    auto out = render(5000);
    EXPECT_EQ(out[99], 1.0f);
    EXPECT_EQ(out[100], 1.0f);
    EXPECT_FLOAT_EQ(out[100 + Mixer::kReleaseFrames / 2], 0.5f);
    EXPECT_NEAR(out[100 + Mixer::kReleaseFrames - 1], 1.0f / Mixer::kReleaseFrames, 1e-6f);
    EXPECT_EQ(out[100 + Mixer::kReleaseFrames], 0.0f);
}

TEST_F(MixerTest, SoundsWaitingToStartDoNotTakeVoices) {
    for (int64_t i = 1; i <= 100; ++i) mixer.push(schedule(i * 10, kOnes));
    int32_t sounding = 0;
    for (int32_t block = 0; block < 101; ++block) {  // 10-frame blocks, as a device renders
        for (float value : render(10)) sounding += value == 1.0f ? 1 : 0;
    }
    EXPECT_EQ(sounding, 100 * 3);
    EXPECT_EQ(mixer.droppedCount(), 0);
}

TEST_F(MixerTest, WhenEveryVoiceIsBusyTheOldestSoundGivesUpItsVoice) {
    mixer.push(schedule(0, kLong));
    for (int32_t i = 1; i < Mixer::kMaxVoices; ++i) mixer.push(schedule(1, kLong, 0.001f));
    render(2);
    mixer.push(schedule(2, kLong, 0.001f));
    EXPECT_NEAR(render(1)[0], 0.001f * Mixer::kMaxVoices, 1e-5f);
    EXPECT_EQ(mixer.droppedCount(), 1);
}

TEST_F(MixerTest, AFullPendingListDropsTheExtraSoundsAndCountsThem) {
    for (size_t i = 0; i < Mixer::kMaxPending + 5; ++i) mixer.push(schedule(100, kOnes));
    render(1);
    EXPECT_EQ(mixer.droppedCount(), 5);
}

TEST_F(MixerTest, FadeOutFadesPlayingSoundsAndDropsWaitingOnes) {
    mixer.push(schedule(0, kLong));
    mixer.push(schedule(1000, kOnes));
    render(10);
    mixer.push(fadeOut());
    auto out = render(5000);  // frames 10..5009
    EXPECT_EQ(out[0], 1.0f);
    EXPECT_NEAR(out[990], 1.0f - 990.0f / Mixer::kReleaseFrames, 1e-6f);
    EXPECT_EQ(out[Mixer::kReleaseFrames], 0.0f);
}

TEST_F(MixerTest, CancelFromDropsOnlyTheWaitingSoundsFromItsFrame) {
    mixer.push(schedule(100, kOnes));
    mixer.push(schedule(200, kOnes));
    mixer.push(cancelFrom(150));
    auto out = render(300);
    EXPECT_EQ(out[100], 1.0f);
    EXPECT_EQ(out[200], 0.0f);
}

TEST_F(MixerTest, ARateOfZeroIsIgnoredRatherThanPlayingForever) {
    mixer.push(note(0, kOnes, 0.0f, 0));
    EXPECT_EQ(render(1)[0], 0.0f);
}

TEST_F(MixerTest, ARateOfNaNIsIgnoredRatherThanCorruptingTheCursor) {
    mixer.push(note(0, kOnes, std::numeric_limits<float>::quiet_NaN(), 0));
    EXPECT_EQ(render(1)[0], 0.0f);
}

TEST_F(MixerTest, CancelFromDropsAWaitingSoundScheduledExactlyOnItsFrame) {
    mixer.push(schedule(100, kOnes));
    mixer.push(cancelFrom(100));
    auto out = render(101);
    EXPECT_EQ(out[100], 0.0f);
}
