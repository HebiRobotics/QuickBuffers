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
 * === FlatBuffers 1.11.0
 * Benchmark                                Mode  Cnt  Score   Error  Units
 * BenchRunner.flatDirectDecodeAndTraverse  avgt   10  0.234 ± 0.012  us/op
 * BenchRunner.flatDirectEncode             avgt   10  0.457 ± 0.031  us/op
 * BenchRunner.flatHeapDecodeAndTraverse    avgt   10  0.381 ± 0.144  us/op
 * BenchRunner.flatHeapEncode               avgt   10  0.626 ± 0.050  us/op
 *
 * === FlatBuffers 1.10.0
 * Benchmark                                Mode  Cnt  Score   Error  Units
 * BenchRunner.flatDirectDecodeAndTraverse  avgt   10  0.321 ± 0.003  us/op
 * BenchRunner.flatDirectEncode             avgt   10  0.649 ± 0.006  us/op
 * BenchRunner.flatHeapDecodeAndTraverse    avgt   10  0.427 ± 0.008  us/op
 * BenchRunner.flatHeapEncode               avgt   10  0.821 ± 0.011  us/op
 *
 * === Unsafe Sink
 * BenchRunner.quickbufDecode             avgt   10  0.177 ± 0.008  us/op
 * BenchRunner.quickbufDecodeAndTraverse  avgt   10  0.302 ± 0.006  us/op
 * BenchRunner.quickbufEncode             avgt   10  0.221 ± 0.004  us/op
 *
 * === Heap Sink
 * BenchRunner.quickbufDecode             avgt   10  0.213 ± 0.007  us/op
 * BenchRunner.quickbufDecodeAndTraverse  avgt   10  0.346 ± 0.008  us/op
 * BenchRunner.quickbufEncode             avgt   10  0.268 ± 0.002  us/op
 *
 * @author Florian Enner
 * @since 23 Jan 2015
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
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
