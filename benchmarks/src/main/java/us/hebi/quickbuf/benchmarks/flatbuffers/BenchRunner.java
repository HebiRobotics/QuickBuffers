/*-
 * #%L
 * quickbuf-benchmarks
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package us.hebi.quickbuf.benchmarks.flatbuffers;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import protos.benchmarks.flatbuffers.quickbuf.FooBarContainer;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Message size: FlatBuffers 344, QuickBuffers: 228
 *
 * === FlatBuffers 1.10.0 (JDK8)
 * Benchmark                                Mode  Cnt  Score   Error  Units
 * BenchRunner.flatDirectDecodeAndTraverse  avgt   10  0.321 ± 0.003  us/op
 * BenchRunner.flatDirectEncode             avgt   10  0.649 ± 0.006  us/op
 * BenchRunner.flatHeapDecodeAndTraverse    avgt   10  0.427 ± 0.008  us/op
 * BenchRunner.flatHeapEncode               avgt   10  0.821 ± 0.011  us/op
 *
 * === FlatBuffers 1.11.0 (JDK8)
 * Benchmark                                Mode  Cnt  Score   Error  Units
 * BenchRunner.flatDirectDecodeAndTraverse  avgt   10  0.234 ± 0.012  us/op
 * BenchRunner.flatDirectEncode             avgt   10  0.457 ± 0.031  us/op
 * BenchRunner.flatHeapDecodeAndTraverse    avgt   10  0.381 ± 0.144  us/op
 * BenchRunner.flatHeapEncode               avgt   10  0.626 ± 0.050  us/op
 *
 * === FlatBuffers 2.0.0 (JDK17)
 * Benchmark                                Mode  Cnt    Score    Error  Units
 * BenchRunner.flatDirectDecodeAndTraverse  avgt   20  222,815 ±  7,716  ns/op
 * BenchRunner.flatDirectEncode             avgt   20  467,350 ±  8,653  ns/op
 * BenchRunner.flatHeapDecodeAndTraverse    avgt   20  210,689 ±  7,518  ns/op
 * BenchRunner.flatHeapEncode               avgt   20  512,067 ± 25,371  ns/op
 *
 * === QuickBuffers rc1 (JDK17) - DirectSource
 * Benchmark                              Mode  Cnt    Score    Error  Units
 * BenchRunner.quickbufDecode             avgt   20  185,288 ±  2,578  ns/op
 * BenchRunner.quickbufDecodeAndTraverse  avgt   20  216,498 ±  5,685  ns/op
 * BenchRunner.quickbufEncode             avgt   20  264,051 ± 10,898  ns/op
 *
 * === QuickBuffers rc1 (JDK17) - ArraySource
 * Benchmark                              Mode  Cnt    Score    Error  Units
 * BenchRunner.quickbufDecode             avgt   20  166,178 ±  9,089  ns/op
 * BenchRunner.quickbufDecodeAndTraverse  avgt   20  199,232 ± 15,365  ns/op
 * BenchRunner.quickbufEncode             avgt   20  259,334 ± 28,094  ns/op
 *
 * @author Florian Enner
 * @since 23 Jan 2015
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(4)
@Warmup(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class BenchRunner {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + BenchRunner.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    public BenchRunner() {
        flatbuffers.encode(heapDecodeBuffer);
        flatbuffers.encode(directDecodeBuffer);
        position = heapDecodeBuffer.position();
    }

    FlatBuffersBench flatbuffers = new FlatBuffersBench();
    ByteBuffer heapDecodeBuffer = ByteBuffer.allocate(1024);
    ByteBuffer directDecodeBuffer = ByteBuffer.allocateDirect(1024);
    ByteBuffer heapEncodeBuffer = ByteBuffer.allocate(1024);
    ByteBuffer directEncodeBuffer = ByteBuffer.allocateDirect(1024);
    int position;

    @Benchmark
    public Object flatHeapEncode() {
        return flatbuffers.encode(heapEncodeBuffer);
    }

    @Benchmark
    public long flatHeapDecodeAndTraverse() {
        heapDecodeBuffer.position(position);
        return flatbuffers.traverse(flatbuffers.decode(heapDecodeBuffer));
    }

    @Benchmark
    public Object flatDirectEncode() {
        return flatbuffers.encode(directEncodeBuffer);
    }

    @Benchmark
    public long flatDirectDecodeAndTraverse() {
        directDecodeBuffer.position(position);
        return flatbuffers.traverse(flatbuffers.decode(directDecodeBuffer));
    }

    @Benchmark
    public int quickbufEncode() {
        return quickbuffers.encode();
    }

    @Benchmark
    public Object quickbufDecode() {
        return quickbuffers.decode();
    }

    @Benchmark
    public long quickbufDecodeAndTraverse() {
        return quickbuffers.traverse(quickbuffers.decode());
    }

    QuickBuffersBench quickbuffers = new QuickBuffersBench();
    final FooBarContainer quickbufMsg = QuickBuffersBench.setData(FooBarContainer.newInstance());

}
