#include <gtest/gtest.h>

#include "EventQueue.h"

using soundcheck::EventQueue;

TEST(EventQueueTest, PopFailsWhenEmpty) {
    EventQueue<int, 4> queue;
    int out = 0;
    EXPECT_FALSE(queue.pop(out));
}

TEST(EventQueueTest, PopsItemsInTheOrderTheyWerePushed) {
    EventQueue<int, 4> queue;
    queue.push(1);
    queue.push(2);
    int out = 0;
    ASSERT_TRUE(queue.pop(out));
    EXPECT_EQ(out, 1);
    ASSERT_TRUE(queue.pop(out));
    EXPECT_EQ(out, 2);
}

TEST(EventQueueTest, RefusesItemsWhenFull) {
    EventQueue<int, 4> queue;
    for (int i = 0; i < 4; ++i) ASSERT_TRUE(queue.push(i));
    EXPECT_FALSE(queue.push(4));
}

TEST(EventQueueTest, KeepsWorkingAfterWrappingAround) {
    EventQueue<int, 4> queue;
    for (int i = 0; i < 10; ++i) {
        ASSERT_TRUE(queue.push(i));
        int out = -1;
        ASSERT_TRUE(queue.pop(out));
        EXPECT_EQ(out, i);
    }
}
