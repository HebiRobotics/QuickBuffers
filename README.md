<!-- 
<p align="center">
  <img src="https://hebirobotics.github.io/QuickBuffers/icon.png"  alt="QuickBuffers icon">
</p>
-->

# QuickBuffers - Fast Protocol Buffers without Allocations

QuickBuffers is a Java implementation of [Google's Protocol Buffers](https://developers.google.com/protocol-buffers/) that has been developed for low latency use cases in zero-allocation environments. The API follows Protobuf-Java where feasible to simplify migration.

The main highlights are

 * **Allocation-free** in steady state. All parts of the API are mutable and reusable.
 * **No reflections**. GraalVM native-images and ProGuard obfuscation ([config](#proguard-configuration)) are supported out of the box
 * **Faster** encoding and decoding [speed](./benchmarks)
 * **Smaller** code size than protobuf-javalite
 * **Built-in JSON** marshalling compliant with the [Proto3 mapping](https://developers.google.com/protocol-buffers/docs/proto3#json)
 * **Improved order** for optimized [sequential memory access](https://github.com/HebiRobotics/QuickBuffers/wiki/Serialization-Order)
 * **Optional accessors** as an opt-in feature (java8)

QuickBuffers fully [conforms](./conformance) to the [proto2 specification](https://developers.google.com/protocol-buffers/docs/proto) and is compatible with all java versions from 6 through 20. So far we have decided against explicitly implementing the proto3 deviations due to some of their [design decisions](proto3.md), but proto3 messages can be generated and are wire compatible. The current limitations include

* [Extensions](https://developers.google.com/protocol-buffers/docs/proto#extensions) are embedded directly into the extended message, so support is limited to generation time.
* [Maps](https://developers.google.com/protocol-buffers/docs/proto#maps) are implemented as a [repeated field](https://developers.google.com/protocol-buffers/docs/proto#backwards) that matches the wire representation.
* Unsigned integer types within the sign range are encoded as negative JSON numbers
* The JSON marshalling does not special case the built-in [proto3 types](https://developers.google.com/protocol-buffers/docs/proto3) such as timestamp and duration
* [Services](https://developers.google.com/protocol-buffers/docs/proto#services) are currently ignored

## Getting started

In order to use QuickBuffers you need to generate messages and add the corresponding runtime dependency. The runtime can be found at the Maven coordinates below.

```xml
<properties>
  <quickbuf.version>1.0.0</quickbuf.version>
  <quickbuf.options>indent=4,java8_optional=true</quickbuf.options>
</properties>
```

```XML
<dependency>
  <groupId>us.hebi.quickbuf</groupId>
  <artifactId>quickbuf-runtime</artifactId>
  <version>${quickbuf.version}</version>
</dependency>
```

The message generator `protoc-gen-quickbuf` is set up as a plugin for the protocol buffers compiler `protoc`. The easiest way to generate messages is to use the [protoc-jar-maven-plugin](https://github.com/os72/protoc-jar-maven-plugin).

```xml
<!-- Downloads protoc w/ plugin and generates messages -->
<!-- Default settings expect .proto files to be in src/main/protobuf -->
<plugin>  
  <groupId>com.github.os72</groupId>
  <artifactId>protoc-jar-maven-plugin</artifactId>
  <version>3.11.4</version>
  <executions>
    <execution>
      <phase>generate-sources</phase>
      <goals>
        <goal>run</goal>
      </goals>
      <configuration>
        <protocVersion>3.21.12</protocVersion>

        <outputTargets>
          <outputTarget>
            <type>quickbuf</type>
            <pluginArtifact>us.hebi.quickbuf:protoc-gen-quickbuf:${quickbuf.version}</pluginArtifact>
            <outputOptions>${quickbuf.options}</outputOptions>
          </outputTarget>
        </outputTargets>

      </configuration>
    </execution>
  </executions>
</plugin>
```

The generator features several options that can be supplied as a comma-separated list. The default values are marked bold.

| Option                   | Value                             | Description                                                                                                                                                                                                                                                                                                       |
|:-------------------------|:----------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **indent**               | **2**, 4, 8, tab                  | sets the indentation in generated files                                                                                                                                                                                                                                                                           |
| **replace_package**      | (pattern)=replacement             | replaces the Java package of the generated messages to avoid name collisions with messages generated by `--java_out`.                                                                                                                                                                                             |
| **input_order**          | **quickbuf**, number, none        | improves decoding performance when parsing messages that were serialized in a known order. `number` matches protobuf-java, and `none` disables this optimization (not recommended).                                                                                                                               |
| **output_order**         | **quickbuf**, number              | `number` matches protobuf-java serialization to pass conformance tests that require binary equivalence (not recommended).                                                                                                                                                                                         |
| **store_unknown_fields** | **false**, true                   | generates code to retain unknown fields that were encountered during parsing. This allows messages to be routed without losing information, even if the schema is not fully known. Unknown fields are stored in binary form and are ignored in equality checks.                                                   |
| **enforce_has_checks**   | **false**, true                   | throws an exception when accessing fields that were not set                                                                                                                                                                                                                                                       |                          
| **allocation**           | **eager**, lazy, lazymsg          | changes the allocation strategy for nested types. `eager` allocates up-front and results in fewer runtime-allocations, but it may be wasteful and prohibits recursive type declarations. `lazy` waits until the field is actually needed. `lazymsg` acts lazy for nested messages, and eager for everything else. |
| **extensions**           | **embedded**, disabled            | `embedded` adds extensions from within a single protoc call directly to the extended message. This requires extensions to be known at generation time. Some plugins may do a separate request per file, so it may require an import to combine multiple files.                                                    |
| **java8_optional**       | **false**, true                   | creates `tryGet` methods that are short for `return if(hasField()) ? Optional.of(getField()) : Optional.absent()`. Requires a runtime with Java 8 or higher.                                                                                                                                                      |                               

## Manual installation

Alternatively, you can also manually execute `protoc` with the `quickbuf` plugin. The plugin can be installed as a package or run as a standalone executable.

**Package installation**

The easiest option is to go to the [download](https://hebirobotics.github.io/QuickBuffers/download.html) site and install the appropriate package. The `protoc-gen-quickbuf` executable is automatically added to the path. There are also commandline installation options that work well on CI.

For unsupported platforms you can download the Java wrapper scripts in `protoc-gen-quickbuf-${version}.zip` and place them on the path. This requires a Java 8 or higher runtime.

**Standalone executable**

If you prefer a standalone executable, you can go to the [Releases](https://github.com/HebiRobotics/QuickBuffers/releases) section and download the `protoc-gen-quickbuf-${version}-${arch}.exe` for your system. The plugin path needs to be manually specified by adding the `--plugin-protoc-gen-quickbuf=${pathToExe}` parameter. Depending on your system you may also need to set the executable bit and remove quarantine flags (macOS).

```bash
sudo xattr -r -d com.apple.quarantine protoc-gen-quickbuf
sudo chmod +x protoc-gen-quickbuf
```

**Run protoc**

You can download protoc from [here](https://repo1.maven.org/maven2/com/google/protobuf/protoc/) and run it with `--quickbuf_out=<options>:./path/to/output`. For example, 

```bash
protoc --quickbuf_out=indent=4,input_order=quickbuf:<output_directory> <proto_files>
```

## Reading and writing messages

We tried to keep the public API as close to Google's `protobuf-java` as possible, so most use cases should require very few changes. The Java related file options are all supported and behave the same way<!--(`java_package`, `java_outer_classname`, `java_multiple_files`, `java_generate_equals_and_hash`)-->.

```protobuf
// .proto definition
message RootMessage {
  optional string text = 1;
  optional NestedMessage nested_message = 2;
  repeated Person people_list = 3;
}

message NestedMessage {
  optional double value = 1;
}

message Person {
  optional uint32 id = 1;
  optional string name = 2;
}
```

The main difference is that there are no extra builder classes and that all message contents are mutable. The `getMutable()` accessors set the has flag and provide access to the nested references.

```Java
// Use fluent-style to set values
RootMessage msg = RootMessage.newInstance()
        .setText("Hello World");

// Use getMutable() to set nested messages
msg.getMutableNestedMessage()
        .setValue(1.0);

// Write repeated values into the internally allocated list
RepeatedMessage<Person> people = msg.getMutablePeopleList().reserve(4);
for (int i = 0; i < 4; i++) {
    Person person = people.next()
        .setId(i)
        .setName("person " + i);
}
```

Messages can be read from a `ProtoSource` and written to a `ProtoSink`. `newInstance` instantiates optimized implementations for accessing contiguous blocks of memory such as `byte[]` and `ByteBuffer`. Reads and writes do not modify the `ByteBuffer` state, so positions and limits need to be manually if needed.

```Java
// Convenience wrappers
byte[] buffer = msg.toByteArray();
RootMessage result = RootMessage.parseFrom(buffer);
assertEquals(result, msg);
```

The internal state can be reset with the `setInput` and `setOutput` methods. `ProtoMessage::getSerializedSize` sets an internally cached size, so it should always be called before serialization if there were any changes.

```Java
 // Reusable objects
byte[] buffer = new byte[512];
ProtoSink sink = ProtoSink.newArraySink();
ProtoSource source = ProtoSource.newArraySource();

// Stream messages
for (int i = 0; i < 100; i++) {
    int length = msg.getSerializedSize();
    msg.writeTo(sink.setOutput(buffer, 0, length));
    result.clearQuick().mergeFrom(source.setInput(buffer, 0, length));
}
```

Additionally, there are also (non-optimized) convenience wrappers for `InputStream`, `OutputStream`, and `ByteBuffer`.

```Java
ProtoSink.newInstance(new ByteArrayOutputStream());
ProtoSource.newInstance(new ByteArrayInputStream(bytes));
```

Keep in mind that mutability comes at the cost of thread-safety, so contents should be cloned with `ProtoMessage::clone` or copied with `ProtoMessage::copyFrom` before being passed to another thread.

**Direct Source/Sink**

Depending on platform support for `sun.misc.Unsafe`, the `DirectSource` and `DirectSink` implementations allow working with off-heap memory. This is intended for reducing unnecessary memory copies when working with direct NIO buffers. Besides not needing to copy data, there is no performance benefit compared to working with heap arrays.

```Java
// Write to direct buffer
ByteBuffer directBuffer = ByteBuffer.allocateDirect(msg.getSerializedSize());
ProtoSink directSink = ProtoSink.newDirectSink();
msg.writeTo(directSink.setOutput(directBuffer));
directBuffer.limit(directSink.getTotalBytesWritten());

// Read from direct buffer
ProtoSource directSource = ProtoSource.newDirectSource();
RootMessage result = RootMessage.parseFrom(directSource.setInput(directBuffer));
assertEquals(msg, result);
```

**JSON Source/Sink**

ProtoMessages also support reading from and writing to JSON as specified in the [proto3 mapping](https://developers.google.com/protocol-buffers/docs/proto3#json).

```Java
// Set some contents
RootMessage msg = RootMessage.newInstance();
msg.setText("👍 QuickBuffers \uD83D\uDC4D");
msg.getMutablePeopleList().next()
    .setId(0)
    .setName("First Name");
msg.getMutablePeopleList().next()
    .setId(1)
    .setName("Last Name");

// Print as prettified json
System.out.println(msg);
```

The default toString method for all messages returns prettified json. The above prints:

```text
{
  "text": "👍 QuickBuffers 👍",
  "peopleList": [
    {
      "id": 0,
      "name": "First Name"
    },
    {
      "id": 1,
      "name": "Last Name"
    }
  ]
}
```

More fine grained control is exposed via the `JsonSink` and `JsonSource` interfaces

```Java
// json options
JsonSink sink = JsonSink.newInstance()
    .setPrettyPrinting(false)
    .setWriteEnumsAsInts(false)
    .setPreserveProtoFieldNames(false);

// use ProtoMessage::writeTo or JsonSink::writeMessage to serialize the contents
msg.writeTo(sink.clear());
RepeatedByte bytes = sink.getBytes();

// use ProtoMessage::parseFrom or JsonSource::parseMessage to parse the contents
JsonMessage result = JsonSource.newInstance(bytes)
    .setIgnoreUnknownFields(true)
    .parseMessage(JsonMessage.getFactory());
```

Parts can be combined to convert an incoming protobuf stream to outgoing json and vice-versa

```java
msg.clearQuick()
    .mergeFrom(protoSource.setInput(input))
    .writeTo(jsonSink.clear());
```

The default implementation encodes the minimal representation accepted by the protobuf spec, i.e., floating point numbers do not append a trailing zero, and long integers are encoded without quotes. Alternative implementations based on GSON and Jackson can be found in the `quickbuf-compat` artifact.

## Building from source

The project can be built with `mvn package` using jdk 8 through jdk 20.

`mvn clean package --projects generator,runtime -am` omits building the benchmarks.

Note that the `package` goal is always required, and that `mvn clean test` is not enough to work. This limitation is introduced by the plugin mechanism of `protoc`, which exchanges information with plugins via protobuf messages on `std::in` and `std::out`. Using `std::in` makes it comparatively easy to get schema information, but it is quite difficult to set up unit tests and debug plugins during development. To enable standard tests, the `parser` module contains a tiny protoc-plugin that stores the raw request from `std::in` inside a file that can be loaded during testing and development of the actual generator plugin. This makes the `generator` module dependent on the packaged output of the `parser` module.

## Detailed accessors for different types

All nested object types such as message or repeated fields have `getField()` and `getMutableField()` accessors. Both return the same internal storage object, but `getField()` should be considered read-only. Once a field is cleared, it should also no longer be modified.

### Primitive fields

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

### Message fields

Nested message types are `final` and allocated during construction time. The recommended way to set nested message content is by accessing the internal store with `getMutableNestedMessage()`. Setting content using `setNestedMessage(NestedMessage.newInstance())` copies the data, but does not change the internal reference.

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


### String fields

`String` types are internally stored as `Utf8String` that are lazily parsed and can be set with `CharSequence`. Since Java `String` objects are immutable, there are additional access methods to allow for decoding characters into a reusable `StringBuilder` instance, as well as for using a custom `Utf8Decoder` that can implement interning.

```proto
// .proto
message SimpleMessage {
    optional string optional_string = 2;
}
```

```Java
// simplified generated code
public final class SimpleMessage {
    public SimpleMessage setOptionalString(CharSequence value);
    public SimpleMessage clearOptionalString(); // sets length = 0
    public boolean hasOptionalString();
    public String getOptionalString(); // lazily converted string
    public Utf8String getOptionalStringBytes(); // internal representation -> treat as read-only
    public Utf8String getMutableOptionalStringBytes(); // internal representation -> may be modified until has state is cleared

    private final Utf8String optionalString = Utf8String.newEmptyInstance();
}
```

```Java
// Get characters
SimpleMessage msg = SimpleMessage.newInstance().setOptionalString("my-text");

StringBuilder chars = new StringBuilder();
msg.getOptionalStringBytes().getChars(chars); // chars now contains "my-text"
```

### Repeated fields

Repeated scalar fields work mostly the same as String fields, but the internal `array()` can be accessed directly if needed. Repeated messages and object types provide a `next()` method that adds one element and provides a mutable reference to it.

```proto
// .proto
message SimpleMessage {
    repeated double repeated_double   = 42;
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

<!-- 
## More information

### Mutability

Our main reason for creating this project was that all commonly available Protobuf implementations (Java, JavaLite, Wire) favor immutable messages, and that they can't be used without resulting in significant amounts of allocations. While this is not a problem for most applications, the GC pressure becomes an issue when working with complex nested messages at very high rates and with very low deadlines. Allocations can also become a performance bottleneck when iterating over large files with millions or more protobuf entries. QuickBuffers considers all message contents to be mutable and reusable. 

### Eager Allocation

The use cases we are targeting often care less about allocations during startup, but it is often important that there are no allocations in steady state. Thus, all object-type fields inside a message are `final` and are allocated immediately at object instantiation. This also makes it more likely that messages are allocated in a contiguous block and that the serialization can be done with a more sequential access pattern.

Unfortunately, we currently have no way of knowing an appropriate initial size for repeated fields, so they are initialized empty and may grow as needed. In the future, we may add custom options to specify a default and/or maximum size.  (`TODO`)

Be aware that this prevents the definition of cycles in the message definitions.

-->

## Proguard configuration

There are no reflections, so none of the fields need to be preserved or special cased. However, Proguard may warn about missing methods when obfuscating against an older runtime. This is related to an intentional workaround, so the warnings can just be disabled by adding the line below to the `proguard.conf` file.

```text
-dontwarn us.hebi.quickbuf.JdkMethods
```

## Acknowledgements

Many internals and large parts of the generated API are based on [Protobuf-Java](https://github.com/protocolbuffers/protobuf). The encoding of floating point numbers during JSON serialization is based on [Schubfach](https://github.com/c4f7fcce9cb06515/Schubfach/) [[Giu2020](https://drive.google.com/open?id=1luHhyQF9zKlM8yJ1nebU0OgVYhfC6CBN)]. Many other JSON parts were inspired by [dsl-json](https://github.com/ngs-doo/dsl-json), [jsoniter](https://jsoniter.com/), and [jsoniter-scala](https://github.com/plokhotnyuk/jsoniter-scala).