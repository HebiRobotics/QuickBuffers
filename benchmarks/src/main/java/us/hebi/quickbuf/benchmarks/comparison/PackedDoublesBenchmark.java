/*-
 * #%L
 * benchmarks
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

package us.hebi.quickbuf.benchmarks.comparison;

import com.google.protobuf.CodedOutputStream;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import us.hebi.quickbuf.ProtoSink;
import us.hebi.quickbuf.ProtoSource;
import protos.test.quickbuf.RepeatedPackables.Packed;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Serializes a single massive packed double array. Represents best case scenario.
 * Represents the last dataset in the Readme benchmarks.
 *
 * === QuickBuffers (Unsafe) ===
 * Benchmark                              Mode  Cnt    Score    Error  Units
 * PackedDoublesBenchmark.readQuick       avgt   10   7.061 ± 0.167  ms/op
 * PackedDoublesBenchmark.readWriteQuick  avgt   10  13.618 ± 0.337  ms/op
 *
 * === QuickBuffers (Safe) ===
 * PackedDoublesBenchmark.readQuick       avgt   10  29.202 ± 0.397  ms/op
 * PackedDoublesBenchmark.readWriteQuick  avgt   10  74.527 ± 1.353  ms/op
 *
 * === Java (Some Unsafe) ===
 * PackedDoublesBenchmark.readProto       avgt   10  103.989 ± 37.389  ms/op
 * PackedDoublesBenchmark.readWriteProto  avgt   10  119.322 ± 15.138  ms/op
 *
 * === JavaLite (Some Unsafe) ===
 * PackedDoublesBenchmark.readProto       avgt   10   91.523 ± 42.055  ms/op --
 * PackedDoublesBenchmark.readWriteProto  avgt   10  112.942 ± 32.927  ms/op --
 *
 * @author Florian Enner
 * @since 13 Oct 2019
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class PackedDoublesBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + PackedDoublesBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final byte[] input = Packed.newInstance().addAllDoubles(new double[8 * 1024 * 1024]).toByteArray();
    final byte[] output = new byte[input.length + 100];

    final ProtoSource source = ProtoSource.newInstance();
    final ProtoSink sink = ProtoSink.newInstance();

    final Packed message = Packed.newInstance();

    @Benchmark
    public Object readQuick() throws IOException {
        source.wrap(input);
        return message.clear().mergeFrom(source);
    }

    @Benchmark
    public int readWriteQuick() throws IOException {
        message.clear().mergeFrom(source.wrap(input)).writeTo(sink.wrap(output));
        return sink.position();
    }

    @Benchmark
    public Object readProto() throws IOException {
        return protos.test.protobuf.RepeatedPackables.Packed.parseFrom(input);
    }

    @Benchmark
    public int readWriteProto() throws IOException {
        CodedOutputStream out = CodedOutputStream.newInstance(output);
        protos.test.protobuf.RepeatedPackables.Packed.parseFrom(input)
                .writeTo(out);
        return out.getTotalBytesWritten();
    }

}
