cmake_minimum_required(VERSION 3.22.1)
project(soundcheck CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(oboe REQUIRED CONFIG)

add_library(soundcheck SHARED
    engine/SampleBank.cpp
    engine/Mixer.cpp
    engine/AudioEngine.cpp
    jni/NativeAudioEngine.cpp
)
target_include_directories(soundcheck PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(soundcheck log oboe::oboe)
target_link_options(soundcheck PRIVATE "-Wl,-z,max-page-size=16384")
