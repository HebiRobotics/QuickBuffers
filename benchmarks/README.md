# QuickBuffers - Benchmarks
  
Below is a comparison with Google's official bindings for a variety of datasets. The performance depends a lot on the specific data format and content, so the results may not be representative for your use case. All tests were run on OpenJDK 17 using JMH on an AMD Ryzen 9 3900x. The protobuf-java benchmarks used version `3.21.12`.

## Benchmark 1 - SBE dataset

The first benchmark was copied from [Small Binary Encoding's](https://mechanical-sympathy.blogspot.com/2014/05/simple-binary-encoding.html) Car (140 byte) and MarketData (64 byte) throughput benchmarks. It tests manual creation of messages and encodes and decodes them from a byte array, which is representative of sending and receiving individual messages over a network.

<!-- car multiplier: 140 * 1000 / (1024*1024) = 0.1335 = -->
<!-- market multiplier: 64 * 1000 / (1024*1024) = 0.061 = -->
<!-- market multiplier: 84 * 1000 / (1024*1024) = 0.080 = -->

| Protobuf Binary          | Size [bytes] | QuickBuffers [msg/s] | Protobuf-Java [msg/s] | Ratio 
|:-------------------------|-------------:|---------------------:|----------------------:|------:|
| **Car**         |
| Encode                   |          140 |     3.40M (454 MB/s) |      1.25M (167 MB/s) |   2.7 
| Decode                   |          140 |     3.36M (449 MB/s) |       1.1M (145 MB/s) |   3.1 
| **Market Data (varint)** |
| Encode                   |           64 |    12.48M (761 MB/s) |      5.62M (342 MB/s) |   2.2 
| Decode                   |           64 |     9.20M (561 MB/s) |      2.46M (150 MB/s) |   3.7 

Note that the throughput is heavily impacted by the chosen data types. Variable length integers are comparatively expensive to work with, so using fixed-width types can often significantly increase throughput at the cost of a larger message size. In the market data case, changing the number encoding from varint to fixed-width types could more than double the decoding throughput. In some cases it may even make sense to use [Groups](https://developers.google.com/protocol-buffers/docs/proto#groups) as they are encoded with a start and end tag and can be streamed without requiring computing the size ahead of time.

| Protobuf Binary          | Size [bytes] | QuickBuffers [msg/s] | Protobuf-Java [msg/s] | Ratio
|:-------------------------|-------------:|---------------------:|----------------------:|------:|
| **Market Data (fixed)** |               
| Encode                  |           84 |     17.0M (1.3 GB/s) |       7.2M (576 MB/s) |   2.4 
| Decode                  |           84 |     20.0M (1.6 GB/s) |       4.8M (384 MB/s) |   4.2 

We also benchmarked the built-in JSON encoding with Protobuf-Java's JsonFormat Printer. This is an unfair comparison of generated code against a reflective approach, so a big speedup is to be expected.

<!-- car mutliplier: 559 * 1000 / (1024*1024) = 0.5331 = -->
<!-- market multiplier: 435 * 1000 / (1024*1024) = 0.415 = -->

| Protobuf JSON      | Size [bytes] | QuickBuffers [msg/s] | Protobuf-Java [msg/s] | Ratio 
|:-------------------|-------------:|---------------------:|----------------------:|------:|
| Car Encode         |          559 |     1.44M (765 MB/s) |       0.12M (62 MB/s) |  12.3 
| Market Data Encode |          435 |    3.60M ( 1.5 GB/s) |       0.16M (67 MB/s) |  22.2

## Benchmark 2 - File streams

The second benchmark reads and writes streams of delimited protobuf messages with varying contents, which is representative for streaming log files. This benchmark does not trigger lazy-parsing of strings, so it is primarily indicative of forwarding use cases. This is a best case scenario for protobuf-java serialization as it omits all the overhead related to building the objects. All datasets were loaded into memory and decoded from a byte array.

|     Dataset      | Content         | Size [bytes/msg] | QuickBuffers [ms/log] |     Java [ms/log] | Ratio 
|:----------------:|:----------------|-----------------:|----------------------:|------------------:|------:|
|     **Read**     |
|        1         | sensor feedback |              220 |     118 ms (773 MB/s) | 432 ms (211 MB/s) |   3.7 
|        2         | sensor info     |              650 |      67 ms (892 MB/s) | 225 ms (266 MB/s) |   3.4 
|        3         | car data        |              140 |      23 ms (456 MB/s) |  70 ms (150 MB/s) |   3.0 
|        4         | market data     |               64 |      18 ms (583 MB/s) |  68 ms (154 MB/s) |   3.8 
|        5         | packed doubles  |              64M |    5.8 ms (11.6 GB/s) | 68 ms ( 1.0 GB/s) |  11.6 
|    **Write**     |                 |
|        1         | sensor feedback |              220 |     89 ms ( 1.0 GB/s) | 137 ms (666 MB/s) |   1.5 
|        2         | sensor info     |              650 |     49 ms ( 1.2 GB/s) |  75 ms (797 MB/s) |   1.5 
|        3         | car data        |              140 |      19 ms (552 MB/s) |  23 ms (466 MB/s) |   1.2 
|        4         | market data     |               64 |      14 ms (749 MB/s) |  20 ms (524 MB/s) |   1.4 
|        5         | packed doubles  |              64M |    5.6 ms (12.0 GB/s) | 40 ms ( 1.7 GB/s) |   7.1 
| **Read + Write** |
|        1         | sensor feedback |              220 |     207 ms (441 MB/s) |  569ms (160 MB/s) |   2.7 
|        2         | sensor info     |              650 |     116 ms (515 MB/s) | 300 ms (199 MB/s) |   2.6 
|        3         | car data        |              140 |      42 ms (250 MB/s) |  93 ms (113 MB/s) |   2.2 
|        4         | market data     |               64 |      32 ms (328 MB/s) |  88 ms (119 MB/s) |   2.8 
|        5         | packed doubles  |              64M |   11.4 ms ( 5.9 GB/s) | 108 ms (621 MB/s) |   9.5 

<!-- | 3  | ms (  MB/s) | ms (  MB/s)  | ms (  MB/s) | 0 -->

<!-- 
set1 = @(value) round(87*1024*1024 ./ (value*1E3));
set2 = @(value) round(57*1024*1024 ./ (value*1E3));
set3 = @(value) round(10*1024*1024 ./ (value*1E3));
set4 = @(value) round(10*1024*1024 ./ (value*1E3));
set5 = @(value) round(64*1024*1024 ./ (value*1E3)); 
-->

**Dataset contents**

* Dataset 1 contains a series of delimited ~220 byte messages containing sensor measurements from production data. Messages consist mostly of **scalar data types** and a relatively small amount of nesting. No strings, repeated, or unknown fields. Only a small subset of defined fields is populated. (87 MB)
* Dataset 2  contains a series of delimited ~650 byte messages containing sensor data and hardware information from production data. The messages are a superset of dataset 1 with **additional strings (mostly small and ascii) and more nesting**. No unknown or repeated fields. About half of the defined fields are populated. (57 MB)
* Dataset 3 contains ~147k **car messages** generated by the **SbeBenchmark** (10 MB)
* Dataset 4 contains ~73k **market messages** generated by the  **SbeBenchmark** (10 MB)
* Dataset 5 contains a single artificial message with one **packed double field** (`repeated double values = 1 [packed=true]`). It only encodes a repeated type with fixed size, so it should be representative of the best-case scenario memory throughput (on little-endian systems this can map to memcpy). (64 MB)
   
The benchmark code can be found in the `benchmarks` module. The `Write` results are derived from `Write = ((Read + Write) - Read)`, which is not necessarily composable.
   
## Benchmark 3 - FlatBuffers

For the last benchmark we compared QuickBuffers against the Java bindings of Google's [FlatBuffers](https://google.github.io/flatbuffers/) project and ported its [official C++ benchmark](https://google.github.io/flatbuffers/flatbuffers_benchmarks.html). 

Contrary to the official C++ benchmark that shows tremendous performance benefits over Protobuf-Lite (which is much slower than the regular version), the Java version is not nearly as optimized, and actually performs slower for most use cases. Recent JDK improvements have improved the performance of `ByteBuffer`, but the overhead is still enough to counter any benefits gained by removing the decoding step. 

Moreover, the benchmark was deliberately setup to favor FlatBuffers by structuring the content as a worst case for Protobuf. It uses deep levels of nesting and inappropriate varint types for very large numbers. For example, an `int64` type is chosen for a field that always maps to the largest size (10 bytes) and goes through the most expensive path. Choosing a flatter hierarchy with appropriate data types would speed things up considerably.

That being said, it may be worth exploring for use cases that require random access to small subsets of data.

|                                          | QuickBuffers (1.0.0/jdk17) | FlatBuffers (2.0.0/jdk17) | FlatBuffers (1.11.0/jdk8) | FlatBuffers (1.10.0/jdk8) | Ratio 
|:-----------------------------------------|---------------------------:|--------------------------:|--------------------------:|--------------------------:|------:|
| **DirectByteBuffer**                     |                    [ns/op] |                   [ns/op] |                   [ns/op] |                   [ns/op] |
| Decode                                   |                        185 |                         0 |                         0 |                         0 |   0.0 
| Traverse                                 |                         31 |                       223 |                       234 |                       321 |   7.2 
| Decode + Traverse                        |                        216 |                       223 |                       234 |                       321 |   1.0 
| Encode                                   |                        264 |                       467 |                       457 |                       649 |   1.8 
| Encode + Decode + Traverse               |                        480 |                       690 |                       691 |                       970 |   1.4 
| **HeapByteBuffer**                       |                    [ns/op] |                   [ns/op] |                   [ns/op] |                   [ns/op] |
| Decode                                   |                        166 |                         0 |                         0 |                         0 |   0.0 
| Traverse                                 |                         33 |                       211 |                       381 |                       427 |   6.4 
| Decode + Traverse                        |                        199 |                       211 |                       381 |                       427 |   1.1 
| Encode                                   |                        259 |                       512 |                       626 |                       821 |   2.0 
| Encode + Decode + Traverse               |                        458 |                       723 |                      1007 |                      1248 |   1.6 
| **Other**                                
| Serialized Size                          |                  228 bytes |                 344 bytes |                 344 bytes |                 344 bytes |   1.5 
| Transient memory allocated during decode |                    0 bytes |                   0 bytes |                   0 bytes |                   0 bytes |     1 

