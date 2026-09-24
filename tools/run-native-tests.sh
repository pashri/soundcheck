#!/usr/bin/env bash
# Builds and runs the C++ engine tests on this Mac, using the Android SDK's CMake and Ninja.
set -euo pipefail

sdk="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
bin="$sdk/cmake/3.22.1/bin"
root="$(cd "$(dirname "$0")/.." && pwd)"
build="$root/app/build/native-tests"

"$bin/cmake" -S "$root/app/src/test/cpp" -B "$build" -G Ninja \
    -DCMAKE_MAKE_PROGRAM="$bin/ninja" -DCMAKE_BUILD_TYPE=Debug
"$bin/cmake" --build "$build"
"$build/engine_tests"
