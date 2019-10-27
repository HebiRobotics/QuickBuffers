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

package us.hebi.robobuf.benchmarks.encoding;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import us.hebi.robobuf.ProtoSink;
import protos.test.robo.TestAllTypes;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark                                      Mode  Cnt  Score    Error  Units
 * PrimitiveWriteBenchmark.writeRepeatedDouble64  avgt   10  3.362 ±  0.045  us/op (x512)
 * PrimitiveWriteBenchmark.writeRepeatedInt32     avgt   10  3.344 ±  0.030  us/op (x512)
 *
 * PrimitiveWriteBenchmark.writeDouble64          avgt   10  0.026 ±  0.001  us/op
 * PrimitiveWriteBenchmark.writeFloat32           avgt   10  0.024 ±  0.001  us/op
 * PrimitiveWriteBenchmark.writeVarint32_1        avgt   10  0.023 ±  0.001  us/op
 * PrimitiveWriteBenchmark.writeVarint32_5        avgt   10  0.028 ±  0.001  us/op
 * PrimitiveWriteBenchmark.writeVarint64_1        avgt   10  0.022 ±  0.001  us/op
 * PrimitiveWriteBenchmark.writeVarint64_10       avgt   10  0.032 ±  0.002  us/op
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
public class PrimitiveWriteBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + PrimitiveWriteBenchmark.class.getSimpleName() + ".*Repeated.*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final Random rnd = new Random();
    final TestAllTypes msg = TestAllTypes.newInstance();
    byte[] outputBuffer = new byte[10 * 1024];
    final ProtoSink sink = ProtoSink.newInstance(outputBuffer);
    final double[] doubleArray = new double[512];
    final int[] intArray = new int[512];

    @Setup(Level.Trial)
    public void setupData() {
        msg.clear();
        for (int i = 0; i < doubleArray.length; i++) {
            doubleArray[i] = rnd.nextDouble();
            intArray[i] = rnd.nextInt();
        }
    }

    @Benchmark
    public int writeVarint32_1() throws IOException {
        return write(msg.setOptionalInt32(42));
    }

    @Benchmark
    public int writeVarint32_5() throws IOException {
        return write(msg.setOptionalInt32(Integer.MAX_VALUE));
    }

    @Benchmark
    public int writeVarint64_1() throws IOException {
        return write(msg.setOptionalInt64(42));
    }

    @Benchmark
    public int writeVarint64_10() throws IOException {
        return write(msg.setOptionalInt64(Long.MAX_VALUE));
    }

    @Benchmark
    public int writeFloat32() throws IOException {
        return write(msg.setOptionalFloat(42));
    }

    @Benchmark
    public int writeDouble64() throws IOException {
        return write(msg.setOptionalDouble(42));
    }

    @Benchmark
    public int writeRepeatedDouble64() throws IOException {
        msg.getMutableRepeatedDouble().copyFrom(doubleArray);
        return write(msg);
    }

    @Benchmark
    public int writeRepeatedInt32() throws IOException {
        msg.getMutableRepeatedDouble().copyFrom(doubleArray);
        return write(msg);
    }

    private int write(TestAllTypes msg) throws IOException {
        sink.reset();
        msg.writeTo(sink);
        return sink.position();
    }

}
