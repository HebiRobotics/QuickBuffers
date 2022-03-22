# QuickBuffers - Benchmarks
  
Below is a comparison with Google's official bindings for a variety of datasets. The performance depends a lot on the specific data format and content, so the results may not be representative for your use case. All tests were run on OpenJDK 17 using JMH on an AMD Ryzen 9 3900x.

## Benchmark 1 - SBE dataset

The first benchmark was copied from [Small Binary Encoding's](https://mechanical-sympathy.blogspot.com/2014/05/simple-binary-encoding.html) Car (140 byte) and MarketData (64 byte) throughput benchmarks. It tests manual creation of messages and encodes and decodes them from a byte array, which is similar to sending and receiving individual messages.

<!-- car multiplier: 140 * 1000 / (1024*1024) = 0.1335 = -->
<!-- market multiplier: 64 * 1000 / (1024*1024) = 0.061 = -->

| Test [msg/ms] | QuickBuffers (rc1) | Protobuf-Java (3.19.4) | Ratio
| :----------- | :-----------: | :-----------: | :-----------: |
| Car Encode  | 3404 (454 MB/s) | 1207 (161 MB/s) |  2.8 
| Car Decode  | 3362 (449 MB/s) | 1318 (176 MB/s) |  2.6  
| Market Data Encode  | 12478 (761 MB/s) | 5957 (363 MB/s) |  2.1  
| Market Data Decode  | 9201 (561 MB/s) | 3390 (207 MB/s) |  2.7  

Note that this test was done using the original SBE .proto definitions. If the varint types are changed to a less expensive encoding, e.g., `fixed64/32` instead of `int64/32`, the results improve by 30-50%. By additionally inlining the small nested fields it'd result in more than 5x the original message throughput. Overall, be aware that there is a significant trade-off between wire size and encoding speed.

We also compared the throughput of the built-in JSON encoding with the binary encoding of Protobuf-Java. At 559 byte (car) and 435 byte (market) the uncompressed binary sizes are of course significantly larger.

<!-- car mutliplier: 559 * 1000 / (1024*1024) = 0.5331 = -->
<!-- market multiplier: 435 * 1000 / (1024*1024) = 0.415 = -->

| Test [msg/ms] | QuickBuffers (JSON) | Protobuf-Java (Binary) | Ratio
| :----------- | :-----------: | :-----------: | :-----------: |
| Car Encode  | 1435 (765 MB/s) | 1207 |  1.2  
| Market Data Encode  | 3602 (1.5 GB/s) | 5957 |  0.6 

## Benchmark 2 - File Streams

We also ran benchmarks for reading and writing streams of delimited protobuf messages with varying contents, which is similar to reading sequentially from a log file. All datasets were loaded into memory and decoded from a byte array. This benchmark does not trigger lazy-parsing of strings, so it is primarily indicative of forwarding use cases. This is a best case scenario for Protobuf as it omits all the overhead related to building the objects.

|  | QuickBuffers (rc1) |  Java (3.19.4) | JavaLite (3.19.4) | Ratio
| :-----------: | :-----------: | :-----------: | :-----------: | :-----------: |
| **Read**   |  
| 1  | 118ms (773 MB/s) |   265ms (344 MB/s)  | 523ms (174 MB/s) | 2.2
| 2  | 67ms (892 MB/s) |  148ms (404 MB/s)  | 313ms (191 MB/s) | 2.2
| 3  | 23ms (456 MB/s) |  54ms (194 MB/s)  | 104ms (101 MB/s) | 2.4
| 4  | 18ms (583 MB/s) | 43ms (244 MB/s)  | 131ms (80 MB/s) | 2.4
| 5 | 5.8ms (11.6 GB/s) |   59ms (1.1 GB/s)  | 63ms (1.0 GB/s) | 10.2
|  **Write**  | |
| 1 | 89ms (1.0 GB/s)  |  123ms (742 MB/s)  | 535ms (171 MB/s)  | 1.4
| 2 | 49ms (1.2 GB/s)  |  77ms (776 MB/s)  | 253ms (236 MB/s) | 1.6
| 3  | 19ms (552 MB/s) |  25ms (419 MB/s)  | 69ms (152 MB/s) | 1.3
| 4  | 14ms (749 MB/s) |  31ms (338 MB/s)  | 90ms (117 MB/s) | 2.2
| 5 | 6.6ms (10.1 GB/s)  | 41ms (1.6 GB/s)  | 39ms (1.7 GB/s) | 6.2
| **Read + Write**   | 
| 1  | 207ms (441 MB/s) |  388ms (235 MB/s)  | 1058ms (86 MB/s) | 1.9
| 2 | 116ms (515 MB/s) |  225ms (266 MB/s)  | 566ms (106 MB/s) | 1.9
| 3  | 42ms (250 MB/s) |  79ms (133 MB/s)  | 173ms (61 MB/s) | 1.9
| 4  | 32ms (328 MB/s) |  74ms (142 MB/s)  | 221ms (47 MB/s) | 2.3
| 5  | 11.4ms (5.9 GB/s) |  100ms (671 MB/s)  | 102ms (658 MB/s) | 8.8

<!-- | 3  | ms (  MB/s) | ms (  MB/s)  | ms (  MB/s) | 0 -->

<!-- 
set1 = @(value) round(87*1024*1024 ./ (value*1E3));
set2 = @(value) round(57*1024*1024 ./ (value*1E3));
set3 = @(value) round(10*1024*1024 ./ (value*1E3));
set4 = @(value) round(10*1024*1024 ./ (value*1E3));
set5 = @(value) round(64*1024*1024 ./ (value*1E3)); 
-->

The benchmark code can be found in the `benchmarks` directory. The `Write` results are derived from `Write = ((Read + Write) - Read)`, which is not necessarily composable. The dataset contents are as follows

* Dataset 1 contains a series of delimited ~220 byte messages (production data). A lot of **scalar data types** and a relatively small amount of nesting. No strings, repeated, or unknown fields. Only a small subset of fields is populated. (87 MB)
* Dataset 2  contains a series of delimited ~650 byte messages (**production data**). Similar data to dataset 1, but with strings (mostly small and ascii) and more nesting. No unknown or repeated fields. Only about half the fields are populated. (57 MB)
* Dataset 3 contains ~147k **car messages** generated by the **SbeBenchmark** (10 MB)
* Dataset 4 contains ~73k **market messages** generated by the  **SbeBenchmark** (10 MB)
* Dataset 5 contains a single artificial message with one **packed double field** (`repeated double values = 1 [packed=true]`). It only encodes a repeated type with fixed size, so it should be representative of the best-case scenario memory throughput (on little-endian systems this can map to memcpy). (64 MB)
   
## Benchmark 3 - FlatBuffers

We also compared QuickBuffers against the Java bindings of Google's [FlatBuffers](https://google.github.io/flatbuffers/) project and ported its [official C++ benchmark](https://google.github.io/flatbuffers/flatbuffers_benchmarks.html).

First, it is worth noting that the benchmark was created with a strong bias for FlatBuffers. The data is setup as a worst case for Protobuf and uses deep levels of nesting and large int64 numbers that result in 10 byte varints. Using a flatter hierarchy and fixed size scalar types would speed it up considerably.

**TODO: results are from JDK8. Need to be updated.**

|  | QuickBuffers | FlatBuffers (v1.11.0) | FlatBuffers (v1.10.0) | Ratio`[1]`
| :----------- | :-----------: | :-----------: | :-----------: | :-----------: |
| **UnsafeSource / DirectByteBuffer [ns/op]**  
| Decode             | 177 | 0 | 0 |  0.0 
| Traverse           | 125 | 234 | 321 |  1.9
| Encode             | 233 | 457 | 649 |  2.0
| Encode + Decode + Traverse | 523 | 691 | 970 |  1.3
| **ArraySource / HeapByteBuffer [ns/op]**  
| Decode             | 213 | 0 | 0 |  0.0  
| Traverse           | 133 | 381 | 427 |  2.9
| Encode             | 268 | 626 | 821 |  2.3
| Encode + Decode + Traverse | 614 | 1007 | 1248 |  1.6
| **Other**  
| Serialized Size   | 228 bytes | 344 bytes | 344 bytes |  1.5
| Transient memory allocated during decode   | 0 bytes | 0 bytes | 0 bytes | 1

* `[1]` `FlatBuffers v1.11.0 / QuickBuffers`
* `[2]` `Traverse = (Decode + Traverse) - Decode` (includes lazy utf8 parsing)

Overall, while the official C++ benchmark shows tremendous performance benefits over Protobuf, the Java implementation has unfortunately been lagging behind a bit. Recent versions have seen some significant performance improvements, but encoding and traversing a `ByteBuffer` still results in significant overhead.