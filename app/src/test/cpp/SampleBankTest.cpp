#include <gtest/gtest.h>

#include "SampleBank.h"

using soundcheck::Sample;
using soundcheck::SampleBank;

TEST(SampleBankTest, MissingSampleIsNull) {
    SampleBank bank;
    EXPECT_EQ(bank.get(3), nullptr);
}

TEST(SampleBankTest, LoadedSampleCanBeRead) {
    SampleBank bank;
    ASSERT_TRUE(bank.load(3, {0.1f, 0.2f}));
    const Sample* sample = bank.get(3);
    ASSERT_NE(sample, nullptr);
    EXPECT_EQ(sample->frames.size(), 2u);
    EXPECT_FLOAT_EQ(sample->frames[1], 0.2f);
}

TEST(SampleBankTest, IdsOutsideTheBankAreRejected) {
    SampleBank bank;
    EXPECT_FALSE(bank.load(-1, {1.0f}));
    EXPECT_FALSE(bank.load(SampleBank::kMaxSamples, {1.0f}));
    EXPECT_EQ(bank.get(-1), nullptr);
    EXPECT_EQ(bank.get(SampleBank::kMaxSamples), nullptr);
}

TEST(SampleBankTest, ReplacingASampleKeepsTheOldOneReadable) {
    SampleBank bank;
    bank.load(0, {1.0f});
    const Sample* old = bank.get(0);
    bank.load(0, {2.0f});
    EXPECT_FLOAT_EQ(old->frames[0], 1.0f);
    EXPECT_FLOAT_EQ(bank.get(0)->frames[0], 2.0f);
}
