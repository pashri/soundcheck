cmake_minimum_required(VERSION 3.22.1)
project(soundcheck_engine_tests CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)
FetchContent_Declare(
    googletest
    URL https://github.com/google/googletest/archive/refs/tags/v1.15.2.tar.gz
)
FetchContent_MakeAvailable(googletest)

set(ENGINE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../main/cpp/engine)

add_executable(engine_tests
    EventQueueTest.cpp
    SampleBankTest.cpp
    MixerTest.cpp
    ${ENGINE_DIR}/SampleBank.cpp
    ${ENGINE_DIR}/Mixer.cpp
)
target_include_directories(engine_tests PRIVATE ${ENGINE_DIR})
target_compile_options(engine_tests PRIVATE -Wall -Wextra)
target_link_libraries(engine_tests PRIVATE GTest::gtest_main)
