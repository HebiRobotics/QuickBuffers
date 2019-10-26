# RoboBuffers - Fast Protocol Buffers without Allocations

RoboBuffers is a Java implementation of [Google's Protocol Buffers v2](https://developers.google.com/protocol-buffers) that has been developed for low latency and high throughput use cases. It can be used in zero-allocation environments and supports off-heap use cases.
 
The main differences to Protobuf-Java are
 * Message contents are mutable
 * Nested types are allocated eagerly
 * The serialization order matches the memory layout (i.e. sequential access pattern)
 
This library hasn't gone through an official release yet, so the public API should be considered a work-in-progress that is subject to change. It currently supports all of the Proto2 types with the exception of `OneOf`, `Maps`, `Extensions`, and `Services`.

## Performance
  
Below is a comparison with Google's official bindings for a variefy of datasets. Note that the performance depends a lot on the specific data format and content, so the results may not be representative for your use case. All tests were run using JMH on JDK8 on an Intel NUC8i7BEH.

The first benchmark was copied from [Small Binary Encoding's](https://mechanical-sympathy.blogspot.com/2014/05/simple-binary-encoding.html) Car (140 byte) and MarketData (64 byte) throughput benchmarks. It tests manual creation of messages and encodes and decodes them from a byte array, which is similar to sending and receiving individual messages.

<!-- car mutliplier: 140 * 1000 / (1024*1024) = 0.1335 = -->
<!-- market multiplier: 64 * 1000 / (1024*1024) = 0.061 = -->

| Test [msg/ms] | RoboBuffers | Protobuf-Java | Ratio
| :----------- | :-----------: | :-----------: | :-----------: |
| Car Encode  | 2728 (364 MB/s) | 1125 (150 MB/s) |  2.4  
| Car Decode  | 2042 (273 MB/s) | 1166s (149 MB/s) |  1.8  
| Market Data Encode  | 6654 (406 MB/s) | 3712 (226 MB/s) |  1.8  
| Market Data Decode  | 5977 (365 MB/s) | 3282 (200 MB/s) |  1.8  

Note that this test was done using the original SBE .proto definitions. If the varint types are adapted to a less expensive encoding, e.g., `fixed64/32` instead of `int64/32`, the market data numbers change to encoding 8245 msgs/ms (+23%) and decoding 6583 msgs/ms (+10%). By additionally inlining the small nested message fields it'd go to 3-4x the original throughput of Protobuf-Java. The choice of type can have a huge impact on the performance.

We also ran benchmarks for reading and writing streams of delimited protobuf messages with varying contents, which is similar to reading sequentially from a log file. All datasets were loaded into memory and decoded from a byte array. Neither benchmark triggers Protobuf-Java's lazy-parsing of strings, so the results may be slightly off. The benchmark code can be found in the `benchmarks` directory.

|  | RoboBuffers<p>(Unsafe) | RoboBuffers<p>(without Unsafe) | Java`[1]`| JavaLite`[1]` | `[2]`
| ----------- | -----------: | -----------: | -----------: | -----------: | ----------- |
| **Read**  | | 
| 1  | 173ms (502 MB/s) | 212ms (410 MB/s) |  344ms (253 MB/s)  | 567ms (153 MB/s) | 2.0
| 2  | 102ms (559 MB/s)` | 118ms (483 MB/s) | 169ms (337 MB/s)  | 378ms (150 MB/s) | 1.7
| 3  | 34ms (297 MB/s) | 44ms (226 MB/s) | 65ms (153 MB/s)  | 147ms (68 MB/s) | 1.9
| 4  | 25ms (400 MB/s) | 28ms (353 MB/s) | 47ms (214 MB/s)  | 155ms (65 MB/s) | 1.9
| 5 | 9.8ms (6.5 GB/s) | 44ms (1.5 GB/s) |  103ms (621 MB/s)  | 92ms (696 MB/s) | 10.5
|  **Write**`[3]`  | | |
| 1 | 118ms (737 MB/s)  | 165ms (527 MB/s) | 157ms (554 MB/s)  | 718ms (121 MB/s)  | 1.3
| 2 | 71ms (802 MB/s)  | 101ms (564 MB/s) | 137ms (416 MB/s)  | 308ms (188 MB/s) | 1.9
| 3  | 23ms (435 MB/s) | 29ms (344 MB/s) | 29ms (344 MB/s)  | 101ms (99 MB/s) | 1.3
| 4  | 16ms (625 MB/s) | 23ms (434 MB/s) | 42ms (238 MB/s)  | 97ms (103 MB/s) | 2.6
| 5 | 6.2ms (10 GB/s)  | 46ms (1.4 GB/s) | 16ms (4.0 GB/s)  | 21ms (3.0 GB/s) | 2.5
| **Read + Write** |  | 
| 1  | 291ms (299 MB/s) | 377ms (231 MB/s) | 501ms (174 MB/s)  | 1285 ms (68 MB/s) | 1.7
| 2 | 173ms (329 MB/s) | 219ms (260 MB/s) | 306ms (186 MB/s)  | 686 ms (83 MB/s) | 1.8
| 3  | 57ms (176 MB/s) | 73ms (138 MB/s) | 94ms (106 MB/s)  | 248ms (40 MB/s) | 1.6
| 4  | 41ms (244 MB/s) | 51ms (196 MB/s) | 89ms (112 MB/s)  | 252ms (40 MB/s) | 2.2
| 5  | 16ms (4.0 GB/s) | 90ms (711 MB/s) | 119ms (537 MB/s)  | 113ms (566 MB/s) | 7.4

<!-- | 3  | ms (  MB/s) | ms (  MB/s) | ms (  MB/s)  | ms (  MB/s) | 0 -->

* `[1]` Version 3.9.1 (makes use of `sun.misc.Unsafe` when available)
* `[2]` `Java / RoboBuffers (Unsafe)`
* `[3]` Derived from `Write = ((Read + Write) - Read)` which is not necessarily composable

 * Dataset Contents
   * Dataset 1 (87 MB) contains a series of delimited ~220 byte messages (production data). A lot of **scalar data types** and a relatively small amount of nesting. No strings, repeated, or unknown fields. Only a small subset of fields is populated.
   * Dataset 2 (57 MB) contains a series of delimited ~650 byte messages (**production data**). Similar data to dataset 1, but with strings (mostly small and ascii) and more nesting. No unknown or repeated fields. Only about half the fields are populated.
   * Dataset 3 (10 MB) contains ~147k messages generated by the **CarBenchmark**
   * Dataset 4 (10 MB) contains ~73k messages generated by the  **MarketDataBenchmark**
   * Dataset 5 (64 MB) contains a single artificial message with one (64 MB) **packed double field** (`repeated double values = 1 [packed=true]`). It only encodes a repeated type with fixed size, so it should be representative of the best-case scenario memory throughput (on little-endian systems this can map to memcpy).

## Getting Started

There have been no releases yet, so currently users need to build from source.

### Building from Source

The project can be built with `mvn package` using JDK8 through JDK11. The runtime sources are compiled against Java 1.6.

Note that protoc plugins get started by the `protoc` executable and exchange information via protobuf messages on `std::in` and `std::out`. While this makes it fairly simple to get the schema information, it makes it quite difficult to setup unit tests and debug plugins during development. To work around this, the `parser` module contains a tiny protoc-plugin that stores the raw request from `std::in` inside a file that can be loaded in unit tests during development of the actual generator plugin.

For this reason the `generator` modules requires the packaged output of the `parser` module, so you always need to run the `package` goal. `mvn clean test` will not work.

### Generating Messages

The code generator is setup as a `protoc` plugin. In order to call it, you need to

* Download `protoc` and add the directory to the `$PATH` (tested with `protoc-3.7.0` through `protoc-3.9.2`)
* Place the files below in the same directory or somewhere else on the `$PATH`. Protoc does have an option to define a plugin path, but it does not seem to work with scripts.
  * `generator/target/protoc-gen-robobuf`
  * `generator/target/protoc-gen-robobuf.bat`
  * `generator/target/protoc-gen-robobuf-<version>.jar`
* Call `protoc` with `--robobuf_out=<options>:./path/to/generate`

Currently available options are

* **indent** = `2`|`4`|`8`|`tab` (Sets the indentation in generated files. Default = 2)

* **replacePackage** = `regex|replacement` (Allows replacing the Java package to avoid name collisions with messages generated by `--java_out`)

* **input_order** enables an optimization that improves decoding performance when parsing messages that arrive in an expected serialization order.
  * **robobuf:** expects fields to arrive sorted by type and their ascending number (default)
  * **number:** expects fields to arrive sorted by their ascending number (official implementations)
  * **random:** disables this optimization (not recommended)

For example, 
```bash
protoc --robobuf_out= \
    indent=4, \
    input_order=robobuf, \
    replacePackage=us.hebi.java|us.hebi.robo: \
    ./path/to/generate`.
``` 

## Usage Examples

We tried to keep the public API as close to Google's official Java bindings as possible, so for many use cases the required changes should be minimal.

All nested object types (e.g. messages, repeated fields, etc.) have `getField()` and `getMutableField()` accessors. Both return the internal storage, but the `getField()` getter should be considered read-only.

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
    public NestedMessage getNestedMessage(); // internal message -> treat as read-only
    public NestedMessage getMutableNestedMessage(); // internal message -> may be modified until has state is cleared

    private final NestedMessage nestedMessage = NestedMessage.newInstance();
}
```

```Java
// (1) setting nested values via 'set' (does a data copy!)
msg.setNestedMessage(NestedMessage().newInstance().setPrimitiveValue(0));

// (2) modify the internal store directly (recommended)
RootMessage msg = RootMessage.newInstance();
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
    public StringBuilder getOptionalString(); // internal store -> treat as read-only
    public StringBuilder getMutableOptionalString(); // internal store -> may be modified 

    private final StringBuilder optionalString = new StringBuilder(0);
}
```

```Java
// Set and append to a string field
SimpleMessage msg = SimpleMessage.newInstance();
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
    public RepeatedDouble getRepeatedDouble(); // internal store -> treat as read-only
    public RepeatedDouble getMutableRepeatedDouble(); // internal store -> may be modified 

    private final RepeatedDouble repeatedDouble = RepeatedDouble.newEmptyInstance();
}
```

Note that repeated stores can currently only expand, but we may add something similar to `StringBuilder::trimToSize` to get rid of unneeded memory (`TODO`).

### Serialization

Messages can be read from a `ProtoSource` and written to a `ProtoSink`. At the moment we only support contiguous blocks of memory, i.e., `byte[]`, and we we have no immediate plans on supporting `ByteBuffer` or `InputStream` due to the significant performance penalties involved.

```Java
// Create data
RootMessage msg = RootMessage().newInstance
    .setPrimitiveValue(2);

// Serialize into existing byte array
byte[] buffer = new byte[msg.getSerializedSize()];
ProtoSink sink = ProtoSink.newInstance(buffer);
msg.writeTo(sink);

// Serialize to byte array using helper method
assertArrayEquals(msg.toByteArray(), buffer);

// Read from byte array into an existing message
ProtoSource source = ProtoSource.newInstance(buffer);
assertEquals(msg, RootMessage().newInstance.mergeFrom(source));
```

Note that `ProtoMessage::getSerializedSize` sets an internally cached size, so it should always be called before serialization.

Depending on platform support, the implementation may make use of `sun.misc.Unsafe`. If you 
are familiar with Unsafe, you may also request an UnsafeSource instance that will allow you to use off-heap addresses. Use with caution!

```Java
long address = /* DirectBuffer::address */;
ProtoSource source = ProtoSource.newUnsafeInstance();
source.setInput(null, address, length)
```

<!-- 
## More Information

### Mutability

Our main reason for creating this project was that all commonly available Protobuf implementations (Java, JavaLite, Wire) favor immutable messages, and that they can't be used without resulting in significant amounts of allocations. While this is not a problem for most applications, the GC pressure becomes an issue when working with complex nested messages at very high rates and with very low deadlines. Allocations can also become a performance bottleneck when iterating over large files with millions or more protobuf entries. RoboBuffers considers all message contents to be mutable and reusable. 

### Eager Allocation

The use cases we are targeting often care less about allocations during startup, but it is often important that there are no allocations in steady state. Thus, all object-type fields inside a message are `final` and are allocated immediately at object instantiation. This also makes it more likely that messages are allocated in a contiguous block and that the serialization can be done with a more sequential access pattern.

Unfortunately, we currently have no way of knowing an appropriate initial size for repeated fields, so they are initialized empty and may grow as needed. In the future, we may add custom options to specify a default and/or maximum size.  (`TODO`)

Be aware that this prevents the definition of cycles in the message definitions.

-->

## Sequential Serialization Order

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

## Acknowledgements

The serialization and deserialization code was based on Google's now-abandoned `Protobuf-JavaNano` implementation. The Utf8 related methods were modified from `Guava` and `Protobuf-Java`.
