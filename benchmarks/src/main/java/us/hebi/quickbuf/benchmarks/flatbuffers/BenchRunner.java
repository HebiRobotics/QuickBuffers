/*-
 * #%L
 * quickbuf-benchmarks
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
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
 * BenchRunner.quickbufDecode             avgt   10  0.237 ± 0.015  us/op
 * BenchRunner.quickbufDecodeAndTraverse  avgt   10  0.259 ± 0.014  us/op
 * BenchRunner.quickbufEncode             avgt   10  0.233 ± 0.007  us/op
 *
 * === Heap Sink
 * BenchRunner.quickbufDecode             avgt   10  0.278 ± 0.017  us/op
 * BenchRunner.quickbufDecodeAndTraverse  avgt   10  0.301 ± 0.004  us/op
 * BenchRunner.quickbufEncode             avgt   10  0.274 ± 0.005  us/op
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
        this.position = heapDecodeBuffer.position();
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
    public long quickbufTraverse() {
        return quickbuffers.traverse(quickbufMsg);
    }

    @Benchmark
    public long quickbufDecodeAndTraverse() {
        return quickbuffers.traverse(quickbuffers.decode());
    }

    QuickBuffersBench quickbuffers = new QuickBuffersBench();
    final FooBarContainer quickbufMsg = QuickBuffersBench.setData(FooBarContainer.newInstance());

}
