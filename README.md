# RoboBuffers - Fast Protocol Buffers without Allocations

RoboBuffers is a Java implementation of Google's Protocol Buffers v2 that has been developed for low latency and high throughput use cases. It is allocation free in steady state and can be used in zero-allocation environments.

It currently supports all of the Proto2 syntax, with the exception of
* Extensions (ignored)
* Maps ([workaround](https://developers.google.com/protocol-buffers/docs/proto#simple))
* OneOf (ignored)
* Services (ignored)
* Recursive Message Definitions (out of memory runtime error)

Note that this library hasn't gone through an official release yet, so the public API should be considered a work-in-progress that is subject to change. Feedback is always welcome.

## Performance / Benchmarks
  
Below are benchmark results for a comparison between `RoboBuffers` and Google's `Protobuf-JavaLite` for different datasets. The tests were run on a single thread on a JDK8 runtime running on an Intel NUC8i7BEH.

Note that the performance depends a lot on the specific data format, so the results may not be representative for your use case.

|  | RoboBuffers (Unsafe) | RoboBuffers (No-Unsafe) | JavaLite (v3.9.1`[1]`) | Relative`[2]`
| ----------- | ----------- | ----------- | ----------- | ----------- |
| **Read**  | | |
| 1  | 494ms (176 MB/s) | 521ms (167 MB/s) | 567ms (153 MB/s) | 1.1
| 2  | 161ms (354 MB/s) | 177ms (322 MB/s) | 378ms (150 MB/s) | 2.3
| 3 | 9.8ms (6.5 GB/s) | 44ms (1.5 GB/s) |  92ms (696 MB/s) | 9.4
|  **Write**`[3]`  | | |
| 1 | 111ms (784 MB/s)  | 152 ms (572 MB/s) | 718ms (121 MB/s)  | 6.5
| 2 | 71 ms (803 MB/s)  | 99 ms (576 MB/s) | 308ms (188 MB/s) | 4.3
| 3 | 6.2 ms (10 GB/s)  | 46 ms (1.4 GB/s) | 21 ms (3.0 GB/s) | 3.4
| **Read + Write** |  | |
| 1  | 605 ms (144 MB/s) | 673ms (129 MB/s) | 1285 ms (68 MB/s) | 2.1
| 2 | 232 ms (245 MB/s) | 276ms (206 MB/s) | 686 ms (83 MB/s) | 3.0
| 3  | 16 ms (4.0 GB/s) | 90ms (711 MB/s) | 113 ms (566 MB/s) | 7.1

* `[1]` Also makes (limited) use of `sun.misc.Unsafe`
* `[2]` `Javalite / RoboBuffers (Unsafe)`
* `[3]` Derived from `Write = ((Read + Write) - Read)` which is not necessarily composable

Each dataset contains delimited protobuf messages with contents that were extracted from a production log. All datasets were then loaded into a `byte[]` and decoded from memory. The benchmark decodes each contained message and then serializes it into another output `byte[]`. 
 
 * Dataset 1 (87 MB) contains a series of delimited ~220 byte messages. Only primitive data types and a relatively small amount of nesting. No strings, repeated, or unknown fields. Only a small subset of fields are populated. This should be a realistic of a bad-case scenario.
 * Dataset 2 (57 MB) contains a series of delimited ~650 byte messages. Similar data to dataset 1, but with strings (mostly small and ascii) and more nesting. No unknown or repeated fields. Only a subset of fields is populated.
 * Dataset 3 (64 MB) contains a single artificial message with one (64 MB) packed double field (`repeated double values = 1 [packed=true]`). It only encodes a repeated type with known width (no varint), so it should be representative of a best-case scenario (on little-endian systems this can map to memcpy).
 
 The benchmarking code looks as below. We did not test the default `Protobuf-Java` bindings separately because the benchmark would not trigger the lazy String parsing. However, considering that `Protobuf-JavaLite` uses the same library, the time should be about the same as decoding with `Protobuf-Java` and accessing all lazy fields.

```Java
// Code for Protobuf-Java
@Benchmark
public int readWriteDelimited() throws IOException {
    CodedOutputStream sink = CodedOutputStream.newInstance(output);
    CodedInputStream source = CodedInputStream.newInstance(datasets[dataset]);
    while (!source.isAtEnd()) {
        // read delimited from source
        final int length = source.readRawVarint32();
        int limit = source.pushLimit(length);
        RootMessage msg = RootMessage.parseFrom(source);
        source.popLimit(limit);

        // write delimited to output
        sink.writeUInt32NoTag(msg.getSerializedSize());
        msg.writeTo(sink);
    }
    return sink.getTotalBytesWritten();
}

private byte[] output = /* large array */;
private byte[][] datasets = /* load into memory */;
```

## Highlights

### Mutability

Our main reason for creating this project was that all available Protobuf implementations (Java, JavaLite, Wire) favor immutable messages, and that they can't be used without resulting in significant amounts of allocations. While this is not a problem for most applications, the GC pressure becomes an issue when working with complex nested messages at very high rates and with very low deadlines. Allocations can also become a performance bottleneck when iterating over large files with millions or more protobuf entries. RoboBuffers considers all message contents to be mutable and reusable. 

### Eager Allocation

The use cases we are targeting often care less about allocations during startup, but it is often important that there are no allocations in steady state. Thus, all object-type fields inside a message are `final` and are allocated immediately at object instantiation. This also makes it more likely that messages are allocated in a contiguous block and that the serialization can be done with a more sequential access pattern.

Unfortunately, we currently have no way of knowing an appropriate initial size for repeated fields, so they are initialized empty and may grow as needed. In the future, we may add custom options to specify a default and/or maximum size.  (`TODO`)

Be aware that this prevents the definition of cycles in the message definitions.

### Serialization Order

`Protobuf-Java` defines fields in the same order as in the `.proto` file, and it serializes them in the order of ascending field numbers. Unfortunately, this results in poor semi-random memory access patterns. `RoboBuffers` instead orders fields primarily by their type, and serializes them in a way that results in a fully sequential access pattern.

One thing to be aware of is that the JVM is allowed to do many optimizations, including re-ordering the location of fields in memory. For example, take the following proto definition

```proto
// Proto definition
message ExampleMessage {
    optional int32 field1 = 1;
    optional int32 field2 = 3;
    optional string field3 = 4;
    optional double field4 = 2;
    optional int64 field5 = 5;
}
```

This would generate code that roughly looks as below. While the typical assumption is that the memory layout matches the declaration order,  i.e., `field [1,2,3,4,5]`, on JDK8 it would actually be changed to `field [4,5,1,2,3]`. We recommend reading [Know Thy Java Object Memory Layout](http://psy-lob-saw.blogspot.com/2013/05/know-thy-java-object-memory-layout.html) for more information on this topic.

```Java
// Generated Protobuf-Java Message (simplified)
class ExampleMessage extends GeneratedMessageV3 { 
    private int field1_;
    private int field2_;
    private String field3_;
    private double field4_;
    private long field5_;

    @Override
    public void writeTo(CodedOutputStream output) throws IOException {
        // removed bitfield checks                          // offset 40
        output.writeInt32(1, field1_);                      // offset 44
        output.writeDouble(2, field4_);                     // offset 24
        output.writeInt32(3, field2_);                      // offset 48
        GeneratedMessageV3.writeString(output, 4, field3_); // offset 56 & ref jump
        output.writeInt64(5, field5_);                      // offset 32
    }
    
}
``` 

By sorting field types according to the re-ordering rules, we can pre-order fields and serialize with a purely sequential memory access pattern. Note that the main benefit for serializing by number is that new fields get added in a predictable location, but the same is still true when sorting by type first.

```java
// Generated RoboBuffers Message (simplified)
class ExampleMessage extends ProtoMessage { 
    private double field4;
    private long field5;
    private int field1;
    private int field2;
    private final StringBuilder field3 = new StringBuilder(0);

    @Override
    public void writeTo(ProtoSink output) throws IOException {
        // removed bitfield checks       // offset 16
        output.writeRawByte((byte) 17);  // tag
        output.writeDoubleNoTag(field4); // offset 24
        output.writeRawByte((byte) 40);  // tag
        output.writeInt64NoTag(field5);  // offset 32
        output.writeRawByte((byte) 8);   // tag
        output.writeInt32NoTag(field1);  // offset 40
        output.writeRawByte((byte) 24);  // tag
        output.writeInt32NoTag(field2);  // offset 44
        output.writeRawByte((byte) 34);  // tag
        output.writeStringNoTag(field3); // offset 48 & ref jump
    }
   
}
```

<!-- Protobuf-Java generated message
```text
UnittestFieldOrder$ExampleMessage object internals:
 OFFSET  SIZE             TYPE DESCRIPTION                               VALUE
      0    12                  (object header)                           N/A
     12     4              int AbstractMessageLite.memoizedHashCode      N/A
     16     4              int AbstractMessage.memoizedSize              N/A
     20     4  UnknownFieldSet GeneratedMessageV3.unknownFields          N/A
     24     8           double ExampleMessage.field4_                    N/A
     32     8             long ExampleMessage.field5_                    N/A
     40     4              int ExampleMessage.bitField0_                 N/A
     44     4              int ExampleMessage.field1_                    N/A
     48     4              int ExampleMessage.field2_                    N/A
     52     1             byte ExampleMessage.memoizedIsInitialized      N/A
     53     3                  (alignment/padding gap)                  
     56     4           Object ExampleMessage.field3_                    N/A
     60     4                  (loss due to the next object alignment)
Instance size: 64 bytes
Space losses: 3 bytes internal + 4 bytes external = 7 bytes total
```
-->

<!-- RoboBuffers generated message
```text
us.hebi.robobuf.robo.UnittestFieldOrder$ExampleMessage object internals:
OFFSET  SIZE                      TYPE DESCRIPTION                               VALUE
   0    12                           (object header)                           N/A
  12     4                       int ProtoMessage.cachedSize                   N/A
  16     4                       int ProtoMessage.bitField0_                   N/A
  20     4                       int ProtoMessage.bitField1_                   N/A
  24     8                    double ExampleMessage.field4                     N/A
  32     8                      long ExampleMessage.field5                     N/A
  40     4                       int ExampleMessage.field1                     N/A
  44     4                       int ExampleMessage.field2                     N/A
  48     4   java.lang.StringBuilder ExampleMessage.field3                     N/A
  52     4                           (loss due to the next object alignment)
Instance size: 56 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total
```
-->

## Getting Started

There have been no releases yet, so currently users need to build from source.

### Project Info

Protoc plugins get executed by the `protoc` process and exchange information via protobuf messages on `std::in` and `std::out`. While this makes it fairly simple to get the schema information, it makes it quite difficult to setup unit tests and debug plugins during development.

To work around this, the `parser` module contains a tiny protoc-plugin that stores the raw request from `std::in` inside a file that can be loaded in unit tests during development. The `compiler` module contains the 'real' protoc-plugin that will generate the messages. 

The `runtime` module contains the runtime libraries needed for using the generated messages, and the `benchmark` module contains performance benchmarks in order to test performance implications of different strategies.

### Building from Source

The sources currently use Java 6 syntax (internal constraint that may be updated to 7+) while the tests make use of Java 8. Since JDK12 only builds 7+, you currently need to build it with JDK8 or JDK11.

`mvn package`

Because the unit tests in the `compiler` module require the packaged output of the `parser` module, you always need to run the `package` goal. `mvn clean test` will not work. All the binary dependencies for unit tests such as an appropriate `protoc` executable should be downloaded automatically.

### Generating Messages

The code generator is setup as a `protoc` plugin. In order to call it, you need to

* Download `protoc` and add the directory to the `$PATH` (tested with `protoc-3.7.0` through `protoc-3.9.2`)
* Place the files below in the same directory or somewhere else on the `$PATH`. Protoc does have an option to define a plugin path, but it does not seem to work with scripts.
  * `compiler/target/protoc-gen-robobuf`
  * `compiler/target/protoc-gen-robobuf.bat`
  * `compiler/target/protoc-gen-robobuf-<version>.jar`
* Call `protoc` with `--robobuf_out=<options>:./path/to/generate`

Currently available options are

* **indent** = `2`|`4`|`8`|`tab` (Sets the indentation in generated files. Default = 2)

* **replacePackage** = `regex|replacement` (Allows replacing the Java package to avoid name collisions with messages generated by `--java_out`)

* **input_order** enables an (experimental) fall-through optimization for reading messages that arrive with a known serialization order.
  * **"robobuf"** expects fields to arrive sorted by type and their ascending number (default)
  * **"number"** expects fields to arrive sorted by their ascending number. Used by most implementations.
  * **"random"**: disables this optimization

For example, 
```bash
protoc --robobuf_out= \
    indent=4, \
    input_order=robobuf, \
    replacePackage=us.hebi.java|us.hebi.robo: \
    ./path/to/generate`.
``` 

## Examples

We tried to keep the public API as close to Google's official Java bindings as possible, so for many use cases the required changes should be minimal.

### Primitive Fields

All primitive values generate the same accessors and behavior as Protobuf-Java's `Builder` classes

```proto
// .proto
message SimpleMessage {
    optional int32 primitive_value = 1;
}
```

```Java
// simplified generated code
public final class SimpleMessage {
    public SimpleMessage setPrimitiveValue(int value);
    public SimpleMessage clearPrimitiveValue();
    public boolean hasPrimitiveValue();
    public int getPrimitiveValue();

    private int primitiveValue;
}
```

### Message Fields

Nested message types are `final` and allocated during construction time. Setting the field copies the internal data, but does not change the reference, so the best way to set nested message content is by directly accessing the internal store with `getMutableNestedMessage()`.

```proto
// .proto
message NestedMessage {
    optional int32 primitive_value = 1;
}
message RootMessage {
    optional NestedMessage nested_message = 1;
}
```

```Java
// simplified generated code
public final class RootMessage {
    public RootMessage setNestedMessage(NestedMessage value); // copies contents to internal message
    public RootMessage clearNestedMessage(); // clears has bit as well as the backing object
    public boolean hasNestedMessage();
    public NestedMessage getNestedMessage(); // internal message
    public NestedMessage getMutableNestedMessage(); // also sets has bit

    private final NestedMessage nestedMessage = new NestedMessage();
}
```

```Java
// (1) setting nested values via 'set' (does a data copy!)
msg.setNestedMessage(new NestedMessage().setPrimitiveValue(0));

// (2) modify the internal store directly (recommended)
RootMessage msg = new RootMessage();
msg.getMutableNestedMessage().setPrimitiveValue(0);
```

### String Fields

`String` objects are immutable, so we use the built-in `CharSequence` and `StringBuilder` classes instead.

```proto
// .proto
message SimpleMessage {
    optional string optional_string = 2;
}
```

```Java
// simplified generated code
public final class SimpleMessage {
    public SimpleMessage setOptionalString(CharSequence value); // copies data
    public SimpleMessage clearOptionalString(); // sets length = 0
    public boolean hasOptionalString();
    public StringBuilder getOptionalString(); // internal store
    public StringBuilder getMutableOptionalString(); // also sets has bit

    private final StringBuilder optionalString = new StringBuilder(0);
}
```

```Java
// Set and append to a string field
SimpleMessage msg = new SimpleMessage();
msg.setOptionalString("my-");
msg.getMutableOptionalString()
    .append("text"); // field is now 'my-text'
```

If you receive messages with many identical Strings, you may want to use a `StringInterner` to share already existing references.

### Repeated Fields

Note: Our own use cases make very little use of repeated fields, so we expect that the API can probably be improved significantly. (i.e. please let us know if you have any better ideas)

They currently work mostly the same as String fields, e.g.,

```proto
// .proto
message SimpleMessage {
    repeated   double repeated_double   = 42;
}
```

```Java
// simplified generated code
public final class SimpleMessage {
    public SimpleMessage addRepeatedDouble(double value); // adds one value
    public SimpleMessage addAllRepeatedDouble(double... values); // adds N values
    public SimpleMessage clearRepeatedDouble(); // sets length = 0
    public boolean hasRepeatedDouble();
    public RepeatedDouble getRepeatedDouble(); // internal store
    public RepeatedDouble getMutableRepeatedDouble(); // also sets has bit

    private final RepeatedDouble repeatedDouble = new RepeatedDouble();
}
```

Note that repeated stores can currently only expand, but we may add something similar to `StringBuilder::trimToSize` to get rid of unneeded memory (`TODO`).

### Serialization

Messages can be read from a `ProtoSource` and written to a `ProtoSink`. At the moment we only support contiguous blocks of memory, i.e., `byte[]`, and we we have no immediate plans on supporting `ByteBuffer` or `InputStream` due to the significant performance penalties involved.

```Java
// Create data
RootMessage msg = new RootMessage()
    .setPrimitiveValue(2);

// Serialize into existing byte array
byte[] buffer = new byte[msg.getSerializedSize()];
ProtoSink sink = ProtoSink.createFastest().setOutput(buffer);
msg.writeTo(sink);

// Serialize to byte array using helper method
assertArrayEquals(msg.toByteArray(), buffer);

// Read from byte array into an existing message
ProtoSource source = ProtoSource.createFastest().setInput(buffer);
assertEquals(msg, new RootMessage().mergeFrom(source));
```

Note that `ProtoMessage::getSerializedSize` sets an internally cached size, so it should always be called before serialization.

Depending on platform support, the implementation may make use of `sun.misc.Unsafe`. If you 
are familiar with Unsafe, you may also request an UnsafeSource instance that will allow you to use off-heap addresses. Use with caution!

```Java
long address = /* DirectBuffer::address */;
ProtoSource source = ProtoSource.createUnsafe();
source.setInput(null, address, length)
```

## Acknowledgements

The serialization and deserialization code was based on Google's now-abandoned `Protobuf-JavaNano` implementation. The Utf8 related methods were modified from `Guava` and `Protobuf-Java`.