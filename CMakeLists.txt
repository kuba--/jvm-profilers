cmake_minimum_required(VERSION 2.8 FATAL_ERROR)
project(jvm-profiler)

# uncomment to make a debug build (including source positions and symbols)
set(CMAKE_BUILD_TYPE DEBUG)

# Binaries will end up in ./build
set(OUTDIR ${PROJECT_BINARY_DIR}/build)
set(LIBRARY_OUTPUT_PATH ${OUTDIR})

find_package(JNI REQUIRED)
if (JNI_FOUND)
    message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message (STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
    message (STATUS "JAVA_INCLUDE_PATH=${JAVA_INCLUDE_PATH}")
    message (STATUS "JAVA_INCLUDE_PATH2=${JAVA_INCLUDE_PATH2}")
endif()
INCLUDE_DIRECTORIES(${JAVA_INCLUDE_PATH})
INCLUDE_DIRECTORIES(${JAVA_INCLUDE_PATH2})

# include of gcc 4.8 headers specifically to work around
# https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=729933
# equivalent in ubuntu 13.10
include_directories("/usr/include/i386-linux-gnu/c++/4.8/")

##########################################################
# Compiler Options

set(GLOBAL_WARNINGS "-Wall -Wformat-security -Wno-char-subscripts")
set(GLOBAL_WARNINGS "${GLOBAL_WARNINGS} -Wno-sign-compare -Wno-strict-overflow -Wwrite-strings")
set(GLOBAL_WARNINGS "${GLOBAL_WARNINGS} -Wnon-virtual-dtor -Woverloaded-virtual")

set(GLOBAL_COPTS "-fdiagnostics-show-option -fno-omit-frame-pointer -fno-strict-aliasing")
set(GLOBAL_COPTS "${GLOBAL_COPTS} -funsigned-char -fno-asynchronous-unwind-tables -msse2 -g -D__STDC_FORMAT_MACROS -O2")

# Platform Specific
if(${CMAKE_SYSTEM_NAME} MATCHES "Darwin")
    set(GLOBAL_COPTS "${GLOBAL_COPTS} -DTARGET_RT_MAC_CFM=0")
    if("${CMAKE_SIZEOF_VOID_P}" EQUAL "8")
        message("-- Detected 64 bit Mac")
        set(GLOBAL_COPTS "${GLOBAL_COPTS} -D__LP64__=1")
    endif()
endif()

# Compiler Specific
if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "Clang")
    message("-- using Clang")
    set(GLOBAL_WARNINGS, "${GLOBAL_WARNINGS} -Weverything -Wno-c++98-compat-pedantic -Wno-padded")
    set(global_warNINGS, "${GLOBAL_WARNINGS} -Wno-missing-prototypes -Wno-weak-vtables -Wno-global-constructors")
elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
    message("-- using GCC")
    set(GLOBAL_COPTS "${GLOBAL_COPTS} -mfpmath=sse")
    set(GLOBAL_WARNINGS, "${GLOBAL_WARNINGS} -Wframe-larger-than=16384 -Wno-unused-but-set-variable")
    set(GLOBAL_WARNINGS, "${GLOBAL_WARNINGS} -Wunused-but-set-parameter -Wvla -Wno-conversion-null")
    set(GLOBAL_WARNINGS, "${GLOBAL_WARNINGS} -Wno-unknown-pragmas -Wno-builtin-macro-redefined -Wl,-fatal_warnings")
endif()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${GLOBAL_WARNINGS} ${GLOBAL_COPTS} -pthread -std=c++0x")

##########################################################
# Build

add_library(perfmap SHARED
    perf-map-agent/src/perf-map-agent.c
    perf-map-agent/src/perf-map-file.c
    perf-map-agent/src/perf-map-file.h)

add_library(honestprof SHARED
    honest-profiler/src/agent.cpp
    honest-profiler/src/circular_queue.cpp
    honest-profiler/src/circular_queue.h
    honest-profiler/src/control.cpp
    honest-profiler/src/globals.h
    honest-profiler/src/log_writer.cpp
    honest-profiler/src/log_writer.h
    honest-profiler/src/signal_handler.cpp
    honest-profiler/src/signal_handler.h
    honest-profiler/src/processor.cpp
    honest-profiler/src/processor.h
    honest-profiler/src/profiler.cpp
    honest-profiler/src/profiler.h
    honest-profiler/src/stacktraces.h)

find_package(Java REQUIRED)
include(UseJava)

set(CMAKE_JAVA_INCLUDE_PATH ${JAVA_INCLUDE_PATH}/../lib/tools.jar)
set(CMAKE_JAVA_TARGET_OUTPUT_DIR ${OUTDIR})

message(STATUS "LIBS: ${Java_JAR_EXECUTABLE} ${Java_INCLUDE_DIRS} incl: ${CMAKE_JAVA_INCLUDE_PATH} output_dir: ${CMAKE_JAVA_TARGET_OUTPUT_DIR}")
add_jar(agent Attach.java ENTRY_POINT Attach OUTPUT_DIR ${OUTDIR})
