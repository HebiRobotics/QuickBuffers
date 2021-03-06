# QuickBuffers - Benchmarks
  
Below is a comparison with Google's official bindings for a variety of datasets. The performance depends a lot on the specific data format and content, so the results may not be representative for your use case. All tests were run on JDK8 using JMH on an Intel NUC8i7BEH.

## Benchmark 1 - SBE dataset

The first benchmark was copied from [Small Binary Encoding's](https://mechanical-sympathy.blogspot.com/2014/05/simple-binary-encoding.html) Car (140 byte) and MarketData (64 byte) throughput benchmarks. It tests manual creation of messages and encodes and decodes them from a byte array, which is similar to sending and receiving individual messages.

<!-- car mutliplier: 140 * 1000 / (1024*1024) = 0.1335 = -->
<!-- market multiplier: 64 * 1000 / (1024*1024) = 0.061 = -->

| Test [msg/ms] | QuickBuffers | Protobuf-Java | Ratio
| :----------- | :-----------: | :-----------: | :-----------: |
| Car Encode  | 3649 (487 MB/s) | 985 (132 MB/s) |  3.7  
| Car Decode  | 2329 (311 MB/s) | 1271 (170 MB/s) |  1.8  
| Market Data Encode  | 13177 (804 MB/s) | 3700 (226 MB/s) |  3.6  
| Market Data Decode  | 9805 (598 MB/s) | 3306 (202 MB/s) |  3.0  

Note that this test was done using the original SBE .proto definitions. If the varint types are changed to a less expensive encoding, e.g., `fixed64/32` instead of `int64/32`, the results improve by 30-50%. By additionally inlining the small nested fields it'd result in more than 5x the original message throughput. Overall, be aware that there is a significant trade-off between wire size and encoding speed.

We also compared the built-in JSON encoding and found that for this particular benchmark the message throughput is on par with Protobuf-Java. However, at 559 byte (car) and 435 byte (market) the uncompressed binary sizes are significantly larger.

<!-- car mutliplier: 559 * 1000 / (1024*1024) = 0.5331 = -->
<!-- market multiplier: 435 * 1000 / (1024*1024) = 0.415 = -->

| Test [msg/ms] | QuickBuffers (JSON) | Protobuf-Java (Binary) | Ratio
| :----------- | :-----------: | :-----------: | :-----------: |
| Car Encode  | 1599 (852 MB/s) | 985 |  1.6  
| Market Data Encode  | 3691 (1.5 GB/s) | 3700 |  1.0 

## Benchmark 2 - File Streams

We also ran benchmarks for reading and writing streams of delimited protobuf messages with varying contents, which is similar to reading sequentially from a log file. All datasets were loaded into memory and decoded from a byte array. This benchmark does not trigger lazy-parsing of strings, so it is primarily indicative of forwarding use cases. The benchmark code can be found in the `benchmarks` directory.

|  | QuickBuffers<p>(Unsafe) | QuickBuffers<p>(without Unsafe) | Java`[1]`| JavaLite`[1]` | `[2]`
| ----------- | -----------: | -----------: | -----------: | -----------: | ----------- |
| **Read**  | | 
| 1  | 144ms (604 MB/s) | 149ms (584 MB/s) |  344ms (253 MB/s)  | 567ms (153 MB/s) | 2.4
| 2  | 79ms (722 MB/s)` | 90ms (633 MB/s) | 169ms (337 MB/s)  | 378ms (150 MB/s) | 2.1
| 3  | 30ms (333 MB/s) | 35ms (286 MB/s) | 65ms (153 MB/s)  | 147ms (68 MB/s) | 2.2
| 4  | 21ms (476 MB/s) | 21ms (476 MB/s) | 47ms (214 MB/s)  | 155ms (65 MB/s) | 2.2
| 5 | 7ms (9.1 GB/s) | 29ms (2.2 GB/s) |  103ms (621 MB/s)  | 92ms (696 MB/s) | 14.7
|  **Write**`[3]`  | | |
| 1 | 99ms (879 MB/s)  | 155ms (561 MB/s) | 157ms (554 MB/s)  | 718ms (121 MB/s)  | 1.6
| 2 | 58ms (983 MB/s)  | 79ms (722 MB/s) | 137ms (416 MB/s)  | 308ms (188 MB/s) | 2.4
| 3  | 17ms (588 MB/s) | 21ms (476 MB/s) | 29ms (344 MB/s)  | 101ms (99 MB/s) | 1.7
| 4  | 14ms (714 MB/s) | 17ms (588 MB/s) | 42ms (238 MB/s)  | 97ms (103 MB/s) | 3.0
| 5 | 6.6ms (9.7 GB/s)  | 45ms (1.4 GB/s) | 16ms (4.0 GB/s)  | 21ms (3.0 GB/s) | 2.4
| **Read + Write** |  | 
| 1  | 243ms (358 MB/s) | 304ms (286 MB/s) | 501ms (174 MB/s)  | 1285 ms (68 MB/s) | 2.1
| 2 | 137ms (416 MB/s) | 169ms (337 MB/s) | 306ms (186 MB/s)  | 686 ms (83 MB/s) | 2.2
| 3  | 47ms (213 MB/s) | 56ms (179 MB/s) | 94ms (106 MB/s)  | 248ms (40 MB/s) | 2.0
| 4  | 35ms (286 MB/s) | 38ms (263 MB/s) | 89ms (112 MB/s)  | 252ms (40 MB/s) | 2.5
| 5  | 14ms (4.7 GB/s) | 75ms (859 MB/s) | 119ms (537 MB/s)  | 113ms (566 MB/s) | 8.5

<!-- | 3  | ms (  MB/s) | ms (  MB/s) | ms (  MB/s)  | ms (  MB/s) | 0 -->

* `[1]` Version 3.9.1 (makes use of `sun.misc.Unsafe` when available)
* `[2]` `Java / QuickBuffers (Unsafe)`
* `[3]` Derived from `Write = ((Read + Write) - Read)` which is not necessarily composable

 * Dataset Contents
   * Dataset 1 (87 MB) contains a series of delimited ~220 byte messages (production data). A lot of **scalar data types** and a relatively small amount of nesting. No strings, repeated, or unknown fields. Only a small subset of fields is populated.
   * Dataset 2 (57 MB) contains a series of delimited ~650 byte messages (**production data**). Similar data to dataset 1, but with strings (mostly small and ascii) and more nesting. No unknown or repeated fields. Only about half the fields are populated.
   * Dataset 3 (10 MB) contains ~147k messages generated by the **CarBenchmark**
   * Dataset 4 (10 MB) contains ~73k messages generated by the  **MarketDataBenchmark**
   * Dataset 5 (64 MB) contains a single artificial message with one (64 MB) **packed double field** (`repeated double values = 1 [packed=true]`). It only encodes a repeated type with fixed size, so it should be representative of the best-case scenario memory throughput (on little-endian systems this can map to memcpy).
   
## Benchmark 3 - FlatBuffers
   
We also compared QuickBuffers against the Java bindings of Google's [FlatBuffers](https://google.github.io/flatbuffers/) project and ported its [official C++ benchmark](https://google.github.io/flatbuffers/flatbuffers_benchmarks.html).
   
   
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
   
While the official C++ benchmark shows tremendous performance benefits over Protobuf, the Java implementation has unfortunately been lagging behind a bit. Recent versions have seen some significant performance improvements, but encoding and traversing a `ByteBuffer` still results in more overhead than may be expected.

It is also worth noting that the benchmark was created with a bias for FlatBuffers. The original data is mostly comprised of large varint numbers (e.g. a 10 byte int64) and repeated messages with multiple levels of nesting, which is a particularly bad case for Protobuf. Messages with a flatter hierarchy and more fixed-size scalar types should fare much better.