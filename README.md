# RoboBuffers - Protocol Buffers without Allocations

RoboBuffers is a Java implementation of Google's Protocol Buffers v2 that is designed to be allocation-free in steady-state. It has been designed for real-time communications in allocation-constrained environments such as often found in the robotics or banking world.

It currently supports all of the Proto2 syntax, with the exception of
* ~~Extensions~~ (ignored)
* ~~Maps~~ ([workaround](https://developers.google.com/protocol-buffers/docs/proto#simple))
* ~~OneOf~~ (ignored)
* ~~Services~~ (ignored)
* ~~Recursive Message Definitions~~ (runtime error)

This library hasn't gone through an official release yet, so the public API should be considered a work-in-progress that is subject to change. That being said, the internals are partly based on Google's abandoned Protobuf-JavaNano library and we have been using the precursor of this project in production for several years, so we are quite confident that the serialization works correctly.

We realize that GPLv3 is not suitable for all use cases. Please contact us at info@hebirobotics.com if you are interested in a commercial license.

## Differentiators to Protobuf-Java

**Field Ordering according to Memory Layout**

The JVM is allowed to re-order the location of fields in memory. For example, the class below may actually be laid out as `[aLong][anInt][reference]`. Please see [Know Thy Java Object Memory Layout](http://psy-lob-saw.blogspot.com/2013/05/know-thy-java-object-memory-layout.html) for more information on this topic.

```Java
class Example { 
    Object reference;
    int anInt;
    long aLong;
}
``` 

Typically the field order follows the `.proto` file definition, but this can cause serialization code to walk the memory in a semi-random pattern. Since the ordering is not part of the protobuf specification, RoboBuffers chose to order the fields such that the access is as sequential as possible.

**Eager Allocation**

All object types inside a messages are `final` and are being allocated immediately at object instantiation. The reasoning behind this is that nested messages are likely to be allocated directly after the parent, resulting in a quasi-contiguous layout and improved sequential access of nested messages. Given that all objects likely have the same lifecycle, the objects are also expected to stay together after a GC.

Note that this prevents recursive object definitions and allocates memory even for messages that won't end up being used.

Repeated fields are currently allocated with the backing array being empty and may grow over time as necessary. `TODO:` At some point, we may add a custom option to initialize the repeated store to a user settable default or max size.

**Mutability**

In order to get rid of memory allocations all message contents are considered mutable. Among other things, you should be aware that

* Protobuf's `String` type maps to the reusable `StringBuilder` Java type
* When sharing messages across threads, you should copy the contents via `message.copyFrom(other)` to prevent concurrent modifications
* Messages should not be used as keys in Hashing structures (e.g. `HashMap`)

## Getting Started

There have been no releases yet, so currently users need to build from source.

### Developer Info

Protoc plugins get called by `protoc` and exchange information via protobuf messages on `std::in` and `std::out`. While this makes it fairly simple to get the schema information, it makes it quite difficult to debug plugins or setup unit tests.

To work around this, the `parser` module contains a tiny protoc-plugin that stores the raw request from `std::in` inside a file that can be loaded in unit tests during development.

The `compiler` module contains the actual protoc-plugin and code generator. The `runtime` module contains the runtime libraries needed when using the generated messages. The `benchmark` module contains performance benchmarks in order to test performance implications of different strategies.

### Building from Source

The sources currently use Java 6 syntax (internal constraint that may be updated to 7+) while the tests make use of Java 8. Since JDK12 only builds 7+, you currently need to build it with JDK8 or JDK11.

`mvn package`

Because the unit tests in the `compiler` module require the packaged output of the `parser` module, you always need to run the `package` goal. `mvn clean test` will not work. All the binary dependencies for unit tests such as an appropriate `protoc` executable should be downloaded automatically.

### Generating Messages

The code generator is setup as a `protoc` plugin. In order to call it, you need to

* Download `protoc` and add the directory to the `$PATH` (tested with `protoc-3.7.0` through `protoc-3.9.2`)
* Place from (`./compiler/target/`) `protoc-gen-robobuf`, `protoc-gen-robobuf.bat`, and `protoc-gen-robobuf-<version>.jar` in the same directory or somewhere else on the `$PATH`. Protoc does have an option to define a plugin path, but we've never gotten that to work properly.
* Call `protoc` with `--robobuf_out=<options>:./path/to/generate`

Currently available options are

* **indent** = `2`|`4`|`8`|`tab` (Sets the indentation in generated files. Default = 2)
* **replacePackage** = `regex|replacement` (Allows replacing the Java package to avoid name collisions with messages generated by `--java_out`)

For example, `protoc --robobuf_out=indent=4,replacePackage=us.hebi.java|us.hebi.robo:./path/to/generate`.

## Usage Examples

We tried to keep the public API as close to Google's official Java bindings as possible, so for many use cases the required changes should be minimal.

### Primitive Fields

All primitive values generate the same accessors as in Protobuf-Java's `Builder` classes

```proto
// .proto
message SimpleMessage {
    optional int32 primitive_value = 1;
}
```

```Java
// generated code
public final class SimpleMessage {
    public SimpleMessage setPrimitiveValue(int value);
    public SimpleMessage clearPrimitiveValue();
    public boolean hasPrimitiveValue();
    public int getPrimitiveValue();
}
```

### Message Fields

All nested message types are `final` and allocated beforehand. They can be accessed via `getNestedMessage()` or `getMutableNestedMessage()` accessors. `getMutableField()` returns the internal field, and also sets the appropriate has-bit.

```proto
// .proto
message SimpleMessage {
    optional int32 primitive_value = 1;
}
message RootMessage {
    optional SimpleMessage nested_message = 1;
}
```

```Java
// Set a field in the nested message
RootMessage msg = new RootMessage();
msg.getMutableNestedMessage().setPrimitiveValue(0);
```

You can also call `setNestedMessage(nestedMessage)`, but be aware that it will do a copy of the contained data rather than storing the reference.

```Java
// Set a field in the nested message
msg.setNestedMessage(new NestedMessage().setPrimitiveValue(0)); // does a copy!
```

The `clearNestedMessage()` method clears the has bit as well the backing data.

### String Fields

Since `String` objects are immutable, we use the built-in `CharSequence` and `StringBuilder` classes instead.

```proto
// .proto
message SimpleMessage {
    optional string optional_string = 2;
}
```

```Java
// generated code
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
// generated code
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

### Mashalling

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
long address = /* ... */;
ProtoSource source = ProtoSource.createUnsafe();
source.setInput(null, address, length)
```

## Acknowledgements

The serialization and deserialization code is heavily based on Google's abandoned `Protobuf-JavaNano` implementation. The Utf8 related methods were adapted from `Guava/Protobuf-Java`.