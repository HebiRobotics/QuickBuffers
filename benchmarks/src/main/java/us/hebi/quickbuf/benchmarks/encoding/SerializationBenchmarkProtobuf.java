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
import protos.test.java.ForeignMessage;
import protos.test.java.TestAllTypes;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * ==== Protobuf-Javalite (3.9.1) ====
 * Benchmark                                Mode  Cnt  Score   Error  Units
 * SerializationJavaBenchmark.readMessage   avgt   10  1.139 ± 0.170  us/op
 * SerializationJavaBenchmark.readString    avgt   10  1.321 ± 0.053  us/op
 * SerializationJavaBenchmark.writeMessage  avgt   10  0.763 ± 0.106  us/op
 * SerializationJavaBenchmark.writeString   avgt   10  1.548 ± 0.093  us/op
 *
 * ==== Protobuf-Java (3.9.1) ====
 * Benchmark                                Mode  Cnt  Score   Error  Units
 * SerializationJavaBenchmark.readMessage   avgt   10  0.705 ± 0.049  us/op (probably has some unparsed lazy fields)
 * SerializationJavaBenchmark.readString    avgt   10  1.614 ± 0.121  us/op
 * SerializationJavaBenchmark.writeMessage  avgt   10  0.303 ± 0.055  us/op
 * SerializationJavaBenchmark.writeString   avgt   10  1.317 ± 0.050  us/op
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
public class SerializationBenchmarkProtobuf {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + SerializationBenchmarkProtobuf.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final TestAllTypes msg = TestAllTypes.newBuilder()
            .setOptionalBool(true)
            .setOptionalDouble(100.0d)
            .setOptionalFloat(101.0f)
            .setOptionalFixed32(102)
            .setOptionalFixed64(103)
            .setOptionalSfixed32(104)
            .setOptionalSfixed64(105)
            .setOptionalSint32(106)
            .setOptionalSint64(107)
            .setOptionalInt32(108)
            .setOptionalInt64(109)
            .setOptionalUint32(110)
            .setOptionalUint64(111)
            .setDefaultString("ascii string")
            .setOptionalString("non-ascii \uD83D\uDCA9 string")
            .setDefaultNestedEnum(TestAllTypes.NestedEnum.FOO)
            .addAllRepeatedFixed32(Arrays.asList(0, 0, 0, 0))
            .addAllRepeatedDouble(Arrays.asList(0d, 0d, 0d, 0d, 0d))
            .addRepeatedForeignMessage(ForeignMessage.newBuilder().setC(512).build())
            .build();
    byte[] msgBytes = msg.toByteArray();
    byte[] msgOutBuffer = new byte[msgBytes.length];

    final TestAllTypes stringMessage = TestAllTypes.newBuilder()
            .setOptionalString("" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n" +
                    "this is a pretty long ascii string \n")
            .setDefaultString("" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n" +
                    "this is a pretty long \uD83D\uDCA9 string \n")
            .build();
    byte[] stringMsgBytes = stringMessage.toByteArray();
    byte[] stringMsgOutBuffer = new byte[stringMessage.getSerializedSize()];

    @Benchmark
    public TestAllTypes readMessage() throws IOException {
        return TestAllTypes.parseFrom(msgBytes);
    }

    @Benchmark
    public int readString() throws IOException {
        // Actually access strings because protobuf does lazy parsing
        TestAllTypes msg = TestAllTypes.parseFrom(stringMsgBytes);
        return msg.getDefaultString().length() + msg.getOptionalString().length();
    }

    @Benchmark
    public byte[] writeMessage() throws IOException {
        return msg.toByteArray();
    }

    @Benchmark
    public byte[] writeString() throws IOException {
        return stringMessage.toByteArray();
    }

}
