# Serialization Order

Google's [Protobuf-Java](https://github.com/protocolbuffers/protobuf/tree/master/java) implementation defines fields in the same order as in the `.proto` definition, and it serializes them in the order of ascending field numbers. For example, the following definition would be serialized as `field[1,4,2,3,5]`.

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

The relevant parts in the generated Java class look like the following snippet. At first glance it looks like it would traverse the fields in a serial pattern, but it is actually accessing them in a semi-random memory access pattern that can significantly degrade performance.

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

The reason is that JVMs are allowed to re-order the location of fields in memory. While the typical assumption is that the memory layout matches the declaration order, i.e., `field [1,2,3,4,5]`, on JDK8 it would actually be changed to `field [4,5,1,2,3]`. [Know Thy Java Object Memory Layout](http://psy-lob-saw.blogspot.com/2013/05/know-thy-java-object-memory-layout.html) has for more information on this topic. On JDK8 the internal runtime memory layout looks like:
 
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

The [Protobuf specification](https://developers.google.com/protocol-buffers/docs/encoding#order) does not specify a particular field order, so `QuickBuffers` instead declares and serializes fields sorted by their type as well as the ascending number. This results in a predictable memory layout and a sequential access pattern during serialization.

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

The resulting object layout looks as follows:

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
