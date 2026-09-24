#pragma once

#include <array>
#include <atomic>
#include <cstddef>

namespace soundcheck {

// Single-producer, single-consumer ring buffer. push() runs only on the control thread and
// pop() only on the audio thread; neither blocks nor allocates.
template <typename T, size_t Capacity>
class EventQueue {
    static_assert((Capacity & (Capacity - 1)) == 0, "Capacity must be a power of two");

public:
    bool push(const T& item) {
        const size_t write = write_.load(std::memory_order_relaxed);
        const size_t read = read_.load(std::memory_order_acquire);
        if (write - read == Capacity) return false;
        items_[write & (Capacity - 1)] = item;
        write_.store(write + 1, std::memory_order_release);
        return true;
    }

    bool pop(T& out) {
        const size_t read = read_.load(std::memory_order_relaxed);
        const size_t write = write_.load(std::memory_order_acquire);
        if (read == write) return false;
        out = items_[read & (Capacity - 1)];
        read_.store(read + 1, std::memory_order_release);
        return true;
    }

private:
    std::array<T, Capacity> items_{};
    std::atomic<size_t> write_{0};
    std::atomic<size_t> read_{0};
};

}  // namespace soundcheck
