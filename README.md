# QuickBuffers - Fast Protocol Buffers without Allocations

QuickBuffers is a Java implementation of [Google's Protocol Buffers v2](https://developers.google.com/protocol-buffers) that has been developed for low latency use cases in zero-allocation environments. The API follows Protobuf-Java where feasible to simplify migration.

The main differences are

 * All parts of the API are mutable and reusable
 * Substantial improvements to both encoding and decoding speed ([benchmarks](./benchmarks))
 * No reflection API or any use of reflections. ProGuard obfuscation does not require configuration.
 * Significantly smaller code size than the `java` and `javalite` options
 * Built-in JSON serialization that matches the [Proto3 JSON Mapping](https://developers.google.com/protocol-buffers/docs/proto3#json) (experimental)
 * Different [serialization order](https://github.com/HebiRobotics/QuickBuffers/wiki/Serialization-Order) that optimizes for sequential memory access

Partly supported features
* `Maps` can be used with a [workaround](https://developers.google.com/protocol-buffers/docs/proto3#backwards-compatibility)
* Unknown fields are retained as raw bytes and cannot be accessed as fields

 Unsupported Features
 * `Extensions` 
 * `Services`

## Runtime Library

You can find the latest release on Maven Central at the coordinates below. The runtime is compatible with Java 6 and higher.

```XML
<dependency>
  <groupId>us.hebi.quickbuf</groupId>
  <artifactId>quickbuf-runtime</artifactId>
  <version>1.0-rc1</version>
</dependency>
```

<details>
<summary>Building from Source</summary><p>

The project can be built with `mvn package` using JDK8 through JDK17.

Note that protoc plugins get started by the `protoc` executable and exchange information via protobuf messages on `std::in` and `std::out`. While this makes it fairly simple to get the schema information, it makes it quite difficult to setup unit tests and debug plugins during development. To work around this, the `parser` module contains a tiny protoc-plugin that stores the raw request from `std::in` inside a file that can be loaded in unit tests during development of the actual generator plugin.

For this reason the `generator` modules requires the packaged output of the `parser` module, so you always need to run the `package` goal. `mvn clean test` will not work. `mvn clean package --projects generator,runtime -am` omits building the benchmarks.

</p></details> 

## Generating Messages

The code generator is setup as a `protoc` plugin that gets called by the official protobuf compiler. You can either generate the message sources manually, or use build system plugins to generate the sources automatically each time.

<details>
<summary>Manual Generation</summary><p>

* Download an appropriate `protoc.exe` from [Maven Central](https://repo1.maven.org/maven2/com/google/protobuf/protoc/) and add the directory to the `$PATH` (tested with `protoc-3.7.0` through `protoc-3.19.4`)
* Download [protoc-gen-quickbuf](https://github.com/HebiRobotics/QuickBuffers/releases/download/1.0-rc1/protoc-gen-quickbuf-1.0-rc1.zip) and extract the files into the same directory or somewhere else on the `$PATH`.
  * Running the plugin requires Java8 or higher to be installed
  * Protoc does have an option to define a plugin path, but it does not seem to work with the wrapper scripts
* Call `protoc` with `--quickbuf_out=<options>:./path/to/generate`

</p></details>

<details>
<summary>Maven Configuration</summary><p>

The configuration below downloads the QuickBuffers generator plugin, puts it on the correct path, and executes protoc using the `protoc-jar-maven-plugin`. The default settings assume that the proto files are located in `src/main/protobuf`.

```XML
<build>
    <plugins>

       <!-- Downloads QuickBuffers generator plugin -->
        <plugin>
            <artifactId>maven-antrun-plugin</artifactId>
            <version>1.8</version>
            <executions>
                <execution>
                    <id>download-quickbuf-plugin</id>
                    <phase>generate-sources</phase>
                    <configuration>
                        <tasks>
                            <taskdef resource="net/sf/antcontrib/antcontrib.properties"
                                     classpathref="maven.plugin.classpath"/>

                            <!-- Download plugin files -->
                            <get src="https://github.com/HebiRobotics/QuickBuffers/releases/download/1.0-rc1/protoc-gen-quickbuf-1.0-rc1.zip"
                                 dest="${project.basedir}/protoc-gen-quickbuf.zip" skipexisting="true" verbose="on"/>
                            <unzip src="${project.basedir}/protoc-gen-quickbuf.zip" dest="${project.basedir}" overwrite="false"/>

                            <!--
                            The executing directory does not end up on the $PATH on Linux, so we need to use the
                            pluginPath protoc option. Unfortunately, this requires us to specify the (full) absolute
                            path to the OS-dependent executable. Typically this would be done via OS-specific Maven
                            Profiles, but that would ruin the copy & paste experience and require changes in multiple
                            sections of the pom file. As a workaround, we can select the correct file and give it a
                            common name that we can use in the plugin path. Unfortunately, after some tests we found
                            that Windows only accepts .exe and .bat files, and .exe does not work with scripts.
                            Thus, the file needs to have a .bat extension.
                             -->
                            <if>
                                <os family="windows"/>
                                <then>
                                    <copy file="${project.basedir}\protoc-gen-quickbuf.bat"
                                          tofile="${project.basedir}\protoc-gen-quickbuf-plugin.bat"/>
                                </then>
                                <else>
                                    <copy file="${project.basedir}/protoc-gen-quickbuf"
                                          tofile="${project.basedir}/protoc-gen-quickbuf-plugin.bat"/>

                                    <!-- Unzip does not preserve permissions, so we need to fix the executable bits -->
                                    <chmod perm="775" file="${project.basedir}/protoc-gen-quickbuf"/>
                                    <chmod perm="775" file="${project.basedir}/protoc-gen-quickbuf-plugin.bat"/>
                                </else>
                            </if>
                        </tasks>
                    </configuration>
                    <goals>
                        <goal>run</goal>
                    </goals>
                </execution>
            </executions>
            <dependencies>
                <dependency>
                    <groupId>ant-contrib</groupId>
                    <artifactId>ant-contrib</artifactId>
                    <version>1.0b3</version>
                    <exclusions>
                        <exclusion>
                            <groupId>ant</groupId>
                            <artifactId>ant</artifactId>
                        </exclusion>
                    </exclusions>
                </dependency>
            </dependencies>
        </plugin>

        <!-- Calls protoc.exe and generate messages -->
        <plugin>
            <groupId>com.github.os72</groupId>
            <artifactId>protoc-jar-maven-plugin</artifactId>
            <version>3.8.0</version>
            <executions>
                <execution>
                    <phase>generate-sources</phase>
                    <goals>
                        <goal>run</goal>
                    </goals>
                    <configuration>
                        <protocVersion>3.19.4</protocVersion>

                        <!-- plugin configuration, options, etc. -->
                        <outputTargets>
                            <outputTarget>
                                <pluginPath>${project.basedir}/protoc-gen-quickbuf-plugin.bat</pluginPath>
                                <type>quickbuf</type>
                                <outputOptions>store_unknown_fields=false</outputOptions>
                            </outputTarget>
                        </outputTargets>

                    </configuration>
                </execution>
            </executions>
        </plugin>

    </plugins>
</build>
```

</p></details> 

Currently available options are

| Option | Value | Description |
| :----------- | :----------- | :----------- |
| **indent** | **2**, 4, 8, tab | sets the indentation in generated files |
| **replace_package** | (pattern)&#124;replacement | replaces the Java package of the generated messages to avoid name collisions with messages generated by `--java_out`. |
| **input_order** | **quickbuf**, number, none | improves decoding performance when parsing messages that were serialized in a known order. `number` matches protobuf-java, and `none` disables this optimization (not recommended). |
| **store_unknown_fields** | **false**, true  | generates code to retain unknown fields that were encountered during parsing. This allows messages to be routed without losing information, even if the schema is not fully known. Unknown fields are stored in binary form and are ignored in equality checks. |
| **enforce_has_checks** | **false**, true  | throws an exception when accessing fields that were not set |                               
| **java8_optional** | **false**, true  |  creates `tryGet` methods that are short for `return if(hasField()) ? Optional.of(getField()) : Optional.absent()`. Requires a runtime with Java 8 or higher. |                               

For example, 
```bash
protoc --quickbuf_out=indent=4,input_order=quickbuf:<output_directory> <proto_files>
``` 

## Message Fields

We tried to keep the public API as close to Google's `Protobuf-Java` as possible, so most use cases should require very few changes. The main difference is that there are no builders, and that all message contents are mutable.

All nested object types such as message or repeated fields have `getField()` and `getMutableField()` accessors. Both return the same internal storage object, but `getField()` should be considered read-only. Once a field is cleared, it should also no longer be modified.

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
SimpleMessage msg = SimpleMessage.newInstance()
    .setOptionalString("my-text");

StringBuilder chars = new StringBuilder();
msg.getOptionalStringBytes().getChars(chars); // chars now contains "my-text"
```

</p></details> 

<details>
<summary>Repeated Fields</summary><p>

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

Note that repeated stores can currently only expand, but we may add something similar to `StringBuilder::trimToSize` to get rid of unneeded memory (`TODO`).

</details>

### Reading and Writing Messages

Messages can be read from a `ProtoSource` and written to a `ProtoSink`. The implementations are optimized for accessing contiguous blocks of memory such as `byte[]`, but there are (non-optimized) convenience wrappers for working with `InputStream`, `OutputStream`, and `ByteBuffer`.

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

Depending on platform support for `sun.misc.Unsafe`, the `DirectSource` and `DirectSink` implementations allow working with off-heap memory.

Note that this is only intended for reducing unnecessary memory copies when working with direct NIO buffers. Contrary to popular belief, there are no performance benefits when using byte-wise actions of `Unsafe` over working with regular `byte[]` arrays. Many cases actually perform slightly slower.

```Java
// Create message
SimpleMessage msg = SimpleMessage.newInstance();
msg.setRequiredField(1);

// Write to direct buffer
ByteBuffer directBuffer = ByteBuffer.allocateDirect(msg.getSerializedSize());
ProtoSink directSink = ProtoSink.newDirectSink();
directSink.setOutput(directBuffer);
msg.writeTo(directSink);
directBuffer.limit(directSink.getTotalBytesWritten());

// Read from direct buffer
ProtoSource directSource = ProtoSource.newDirectSource();
directSource.setInput(directBuffer);
SimpleMessage msg2 = SimpleMessage.parseFrom(directSource);
assertEquals(msg, msg2);
```

</details>

<!-- 
## More Information

### Mutability

Our main reason for creating this project was that all commonly available Protobuf implementations (Java, JavaLite, Wire) favor immutable messages, and that they can't be used without resulting in significant amounts of allocations. While this is not a problem for most applications, the GC pressure becomes an issue when working with complex nested messages at very high rates and with very low deadlines. Allocations can also become a performance bottleneck when iterating over large files with millions or more protobuf entries. QuickBuffers considers all message contents to be mutable and reusable. 

### Eager Allocation

The use cases we are targeting often care less about allocations during startup, but it is often important that there are no allocations in steady state. Thus, all object-type fields inside a message are `final` and are allocated immediately at object instantiation. This also makes it more likely that messages are allocated in a contiguous block and that the serialization can be done with a more sequential access pattern.

Unfortunately, we currently have no way of knowing an appropriate initial size for repeated fields, so they are initialized empty and may grow as needed. In the future, we may add custom options to specify a default and/or maximum size.  (`TODO`)

Be aware that this prevents the definition of cycles in the message definitions.

-->
