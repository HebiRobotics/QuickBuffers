# RoboBuffers - Fast Protocol Buffers without Allocations

RoboBuffers is a Java implementation of [Google's Protocol Buffers v2](https://developers.google.com/protocol-buffers) that has been developed for low latency and high throughput use cases. It can be used in zero-allocation environments and supports off-heap use cases.

The main differences to Protobuf-Java are

 * Message contents are mutable
 * The [serialization order](https://github.com/HebiRobotics/RoboBuffers/wiki/Serialization-Order) was optimized for sequential memory access
 * There is currently no support for `OneOf`, `Maps`, `Extensions`, and `Services`
 * Nested types are instantiated eagerly

For performance comparisons please refer to [benchmarks](./benchmarks).

## Getting Started

This library hasn't gone through an official release yet, so the public API should be considered a work-in-progress that is subject to change. Please let us know in case you have any feedback.

### Generating Messages

The code generator is setup as a `protoc` plugin. In order to call it, you need to

* Download an appropriate [protoc.exe](https://repo1.maven.org/maven2/com/google/protobuf/protoc/) and add the directory to the `$PATH` (tested with `protoc-3.7.0` through `protoc-3.9.1`)
* Download [protoc-gen-robobuf-1.0-alpha2](https://github.com/HebiRobotics/RoboBuffers/releases/download/1.0-alpha2/protoc-gen-robobuf-1.0-alpha2.zip) and extract the files into the same directory or somewhere else on the `$PATH`. 
  * Running the plugin requires Java8 or higher to be installed
  * Protoc does have an option to define a plugin path, but it does not seem to work with the wrapper scripts
* Call `protoc` with `--robobuf_out=<options>:./path/to/generate`

Currently available options are

* **`indent=2|4|8|tab`** (default = 2)
  * sets the indentation in generated files
* **`replacePackage=regex|replacement`**
  * allows replacing the Java package to avoid name collisions with messages generated by `--java_out`
* **`input_order=robobuf|number|none`**
  * enables an optimization that improves decoding performance when parsing messages that arrive in an expected serialization order.
  * `robobuf` expects fields to arrive sorted by type and their ascending number (default)
  * `number` expects fields to arrive sorted by their ascending number (official implementations)
  * `none` disables this optimization (not recommended)
* **`store_unknown_fields=true|false`** (default = false)
  * stores unknown fields that it encounter during parsing. This allows messages to be passed on without losing information even if the schema is not fully known.
  * the unknown data is stored in binary form, so individual fields cannot be accessed directly. 
  * unknown fields are ignored when comparing with `equals`.
* **`json_use_proto_name=true|false`** (default = false)
  * changes the serialized json field names from the default lowerCamelCase (e.g. `myField` or the optional `json_name` override) to the field names in the original proto definition, e.g., `my_field`. [Compatible parsers](https://developers.google.com/protocol-buffers/docs/proto3#json) should be able to parse both cases.

For example, 
```bash
protoc --robobuf_out= \
    indent=4, \
    input_order=robobuf, \
    replacePackage=us.hebi.java|us.hebi.robo: \
    ./path/to/generate`.
``` 

### Runtime Library

The generated messages require a runtime library. Released versions will be on Maven Central. The runtime is compatible with Java 6 and higher.

```XML
<dependency>
  <groupId>us.hebi.robobuf</groupId>
  <artifactId>robobuf-runtime</artifactId>
  <version>1.0-alpha2</version>
</dependency>
```

<details>
<summary>Building from Source</summary><p>

The project can be built with `mvn package` using JDK8 through JDK11.

Note that protoc plugins get started by the `protoc` executable and exchange information via protobuf messages on `std::in` and `std::out`. While this makes it fairly simple to get the schema information, it makes it quite difficult to setup unit tests and debug plugins during development. To work around this, the `parser` module contains a tiny protoc-plugin that stores the raw request from `std::in` inside a file that can be loaded in unit tests during development of the actual generator plugin.

For this reason the `generator` modules requires the packaged output of the `parser` module, so you always need to run the `package` goal. `mvn clean test` will not work.

</p></details> 

## Basic Usage

We tried to keep the public API as close to Google's official Java bindings as possible, so for many use cases the required changes should be minimal.

All nested object types (e.g. messages, repeated fields, etc.) have `getField()` and `getMutableField()` accessors. Both return the internal storage, but the `getField()` getter should be considered read-only.


<details>
<summary>Primitive Fields</summary><p>

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

</p></details> 

<details>
<summary>Message Fields</summary><p>

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

</p></details> 

<details>
<summary>String Fields</summary><p>

`String` objects are immutable, so fields accept `CharSequence` and return `StringBuilder` objects instead.

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

</p></details> 

<details>
<summary>Repeated Fields</summary><p>

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

</details>

### Serialization

Messages can be read from a `ProtoSource` and written to a `ProtoSink`. At the moment we only support contiguous blocks of memory, i.e., `byte[]`, and we we have no immediate plans on supporting `ByteBuffer` or `InputStream` due to the significant performance penalties involved.

```Java
// Create data
RootMessage msg = RootMessage.newInstance()
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

<details>
<summary>Off-Heap Addressing</summary><p>

Depending on platform support, the implementation may make use of `sun.misc.Unsafe`. If you 
are familiar with Unsafe, you may also request an UnsafeSource instance that will allow you to use off-heap addresses. Use with caution!

```Java
long address = /* DirectBuffer::address */;
ProtoSource source = ProtoSource.newUnsafeInstance();
source.setInput(null, address, length)
```

</details>

<!-- 
## More Information

### Mutability

Our main reason for creating this project was that all commonly available Protobuf implementations (Java, JavaLite, Wire) favor immutable messages, and that they can't be used without resulting in significant amounts of allocations. While this is not a problem for most applications, the GC pressure becomes an issue when working with complex nested messages at very high rates and with very low deadlines. Allocations can also become a performance bottleneck when iterating over large files with millions or more protobuf entries. RoboBuffers considers all message contents to be mutable and reusable. 

### Eager Allocation

The use cases we are targeting often care less about allocations during startup, but it is often important that there are no allocations in steady state. Thus, all object-type fields inside a message are `final` and are allocated immediately at object instantiation. This also makes it more likely that messages are allocated in a contiguous block and that the serialization can be done with a more sequential access pattern.

Unfortunately, we currently have no way of knowing an appropriate initial size for repeated fields, so they are initialized empty and may grow as needed. In the future, we may add custom options to specify a default and/or maximum size.  (`TODO`)

Be aware that this prevents the definition of cycles in the message definitions.

-->
