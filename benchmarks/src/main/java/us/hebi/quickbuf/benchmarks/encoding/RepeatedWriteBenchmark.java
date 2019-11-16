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

package us.hebi.quickbuf.benchmarks.encoding;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import protos.test.robo.RepeatedPackables;
import protos.test.robo.TestAllTypes;
import us.hebi.quickbuf.ProtoMessage;
import us.hebi.quickbuf.ProtoSink;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * ==== Array Sink ==== (256x each type)
 * Benchmark                                        Mode  Cnt  Score   Error  Units
 * RepeatedWriteBenchmark.writeNonPackedDouble      avgt   10  1.649 ± 0.044  us/op
 * RepeatedWriteBenchmark.writeNonPackedFloat       avgt   10  0.874 ± 0.021  us/op
 * RepeatedWriteBenchmark.writeNonPackedInt32       avgt   10  2.317 ± 0.098  us/op
 * RepeatedWriteBenchmark.writeNonPackedInt64       avgt   10  2.968 ± 0.093  us/op
 * RepeatedWriteBenchmark.writeNonPackedIntFixed32  avgt   10  0.799 ± 0.015  us/op
 * RepeatedWriteBenchmark.writeNonPackedIntFixed64  avgt   10  1.515 ± 0.029  us/op
 * RepeatedWriteBenchmark.writePackedDouble         avgt   10  1.000 ± 0.023  us/op
 * RepeatedWriteBenchmark.writePackedFloat          avgt   10  0.432 ± 0.004  us/op
 * RepeatedWriteBenchmark.writePackedInt32          avgt   10  2.275 ± 0.061  us/op
 * RepeatedWriteBenchmark.writePackedInt64          avgt   10  3.169 ± 0.056  us/op
 * RepeatedWriteBenchmark.writePackedIntFixed32     avgt   10  0.324 ± 0.005  us/op
 * RepeatedWriteBenchmark.writePackedIntFixed64     avgt   10  0.846 ± 0.016  us/op
 *
 * ==== Unsafe Array Sink ==== (256x each type)
 * Benchmark                                        Mode  Cnt  Score   Error  Units
 * RepeatedWriteBenchmark.writeNonPackedDouble      avgt   10  0.449 ± 0.007  us/op
 * RepeatedWriteBenchmark.writeNonPackedFloat       avgt   10  0.439 ± 0.009  us/op
 * RepeatedWriteBenchmark.writeNonPackedInt32       avgt   10  1.924 ± 0.060  us/op
 * RepeatedWriteBenchmark.writeNonPackedInt64       avgt   10  2.674 ± 0.077  us/op
 * RepeatedWriteBenchmark.writeNonPackedIntFixed32  avgt   10  0.478 ± 0.016  us/op
 * RepeatedWriteBenchmark.writeNonPackedIntFixed64  avgt   10  0.472 ± 0.005  us/op
 * RepeatedWriteBenchmark.writePackedDouble         avgt   10  0.065 ± 0.003  us/op
 * RepeatedWriteBenchmark.writePackedFloat          avgt   10  0.044 ± 0.002  us/op
 * RepeatedWriteBenchmark.writePackedInt32          avgt   10  1.956 ± 0.029  us/op
 * RepeatedWriteBenchmark.writePackedInt64          avgt   10  2.610 ± 0.045  us/op
 * RepeatedWriteBenchmark.writePackedIntFixed32     avgt   10  0.048 ± 0.001  us/op
 * RepeatedWriteBenchmark.writePackedIntFixed64     avgt   10  0.063 ± 0.001  us/op
 *
 * @author Florian Enner
 * @since 16 Aug 2019
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class RepeatedWriteBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + RepeatedWriteBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final Random rnd = new Random();
    final TestAllTypes msg = TestAllTypes.newInstance();
    byte[] outputBuffer = new byte[10 * 1024];
    final ProtoSink sink = ProtoSink.newInstance(outputBuffer);
    final static int size = 256;
    final double[] doubleArray = new double[size];
    final float[] floatArray = new float[size];
    final int[] intArray = new int[size];
    final long[] longArray = new long[size];

    final RepeatedPackables.Packed packed = RepeatedPackables.Packed.newInstance();
    final RepeatedPackables.NonPacked nonPacked = RepeatedPackables.NonPacked.newInstance();

    @Setup(Level.Trial)
    public void setupData() {
        msg.clear();
        for (int i = 0; i < doubleArray.length; i++) {
            doubleArray[i] = rnd.nextDouble();
            intArray[i] = rnd.nextInt();
            floatArray[i] = rnd.nextFloat();
            longArray[i] = rnd.nextLong();
        }
    }

    @Benchmark
    public int writeNonPackedDouble() throws IOException {
        return write(nonPacked.clear().addAllDoubles(doubleArray));
    }

    @Benchmark
    public int writeNonPackedInt32() throws IOException {
        return write(nonPacked.clear().addAllInt32S(intArray));
    }

    @Benchmark
    public int writeNonPackedInt64() throws IOException {
        return write(nonPacked.clear().addAllInt64S(longArray));
    }

    @Benchmark
    public int writeNonPackedIntFixed32() throws IOException {
        return write(nonPacked.clear().addAllFixed32S(intArray));
    }

    @Benchmark
    public int writeNonPackedIntFixed64() throws IOException {
        return write(nonPacked.clear().addAllFixed64S(longArray));
    }

    @Benchmark
    public int writeNonPackedFloat() throws IOException {
        return write(nonPacked.clear().addAllFloats(floatArray));
    }

    @Benchmark
    public int writePackedDouble() throws IOException {
        return write(packed.clear().addAllDoubles(doubleArray));
    }

    @Benchmark
    public int writePackedInt32() throws IOException {
        return write(packed.clear().addAllInt32S(intArray));
    }

    @Benchmark
    public int writePackedInt64() throws IOException {
        return write(packed.clear().addAllInt64S(longArray));
    }

    @Benchmark
    public int writePackedIntFixed32() throws IOException {
        return write(packed.clear().addAllFixed32S(intArray));
    }

    @Benchmark
    public int writePackedIntFixed64() throws IOException {
        return write(packed.clear().addAllFixed64S(longArray));
    }

    @Benchmark
    public int writePackedFloat() throws IOException {
        return write(packed.clear().addAllFloats(floatArray));
    }

    private int write(ProtoMessage msg) throws IOException {
        sink.reset();
        msg.writeTo(sink);
        return sink.position();
    }

}
