# Protobuf Conformance Tests
  
Google offers a [conformance test suite](https://github.com/protocolbuffers/protobuf/blob/main/conformance/README.md) that contains thousands of automated unit tests to check whether an implementation conforms to the specification. The suite is setup similarly to protoc plugins where a native executable calls the plugin and communicates via `std::in` and `std::out`. Due to [limitations with pipes](https://github.com/protocolbuffers/protobuf/blob/main/conformance/conformance.proto#L38-L55) in Windows and macOS, the tester can currently only be run on Linux.

QuickBuffers currently passes all required and recommended tests and provides options to produce binary equivalent output to Protobuf-Java. The test implementation is in [ConformanceQuickbuf.java](src/main/java/us/hebi/quickbuf/conformance/ConformanceQuickbuf.java).

**Compile the QuickBuffers test executable**

Due to some issues with Java wrapper scripts, we found it best to compile the Java code to a native executable. This also tests GraalVM integration and makes sure that there no accidental runtime reflections. 

```shell
# full packaging and native compilation
mvn clean package --projects conformance -am -Pnative

# native compilation without prior packaging
mvn native:compile-no-fork -pl conformance -Pnative
```

The generated executable is in `${quickbufDir}/conformance/target/ConformanceQuickbuf.exe`

**Compile the native C++ runner+**

The `conformance_test_runner` executable needs to be compiled on Linux using the commands below. Depending on the CPU this can take tens of minutes.

```shell
git clone https://github.com/protocolbuffers/protobuf.git
cd protobuf
git submodule update --init --recursive
cmake . -Dprotobuf_BUILD_CONFORMANCE=ON && cmake --build .
```

**Execute the conformance_test_runner**

```shell
cd ${protobufDir}
cp ${quickbufDir}/conformance/target/ConformanceQuickbuf.exe ${protobufDir}/
chmod +x ConformanceQuickbuf.exe
./conformance_test_runner --enforce_recommended ConformanceQuickbuf.exe
```

The output should list 0 failures. Unsupported features such as the text format and some Google internal formats are skipped.

```text
user:~/protobuf$ ./conformance_test_runner --enforce_recommended ConformanceQuickbuf.exe
WARNING: All log messages before absl::InitializeLog() is called are written to STDERR
I0000 00:00:1675465291.622071   18024 conformance_test_runner.cc:315] ConformanceQuickbuf.exe

CONFORMANCE TEST BEGIN ====================================

CONFORMANCE SUITE PASSED: 653 successes, 1376 skipped, 0 expected failures, 0 unexpected failures.

WARNING: All log messages before absl::InitializeLog() is called are written to STDERR
I0000 00:00:1675465292.115120   22087 conformance_test_runner.cc:315] ConformanceQuickbuf.exe

CONFORMANCE TEST BEGIN ====================================

CONFORMANCE SUITE PASSED: 0 successes, 120 skipped, 0 expected failures, 0 unexpected failures.

ConformanceQuickbuf: received EOF from test runner after 120 tests. Bytes read: 12581, Bytes written: 1216
ConformanceQuickbuf: received EOF from test runner after 2030 tests. Bytes read: 166171, Bytes written: 55441
```