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
import us.hebi.quickbuf.ProtoSink;
import us.hebi.quickbuf.ProtoSource;
import protos.test.robo.ForeignMessage;
import protos.test.robo.TestAllTypes;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * ==== Array Input/Output ====
 * Benchmark                                            Mode  Cnt  Score   Error  Units
 * SerializationBenchmark.computeMessageSerializedSize  avgt   10  0.059 ± 0.003  us/op
 * SerializationBenchmark.computeStringSerializedSize   avgt   10  0.853 ± 0.028  us/op
 * SerializationBenchmark.readMessage                   avgt   10  0.500 ± 0.042  us/op
 * SerializationBenchmark.readMessageUnsafe             avgt   10  0.421 ± 0.021  us/op
 * SerializationBenchmark.readString                    avgt   10  0.920 ± 0.110  us/op
 * SerializationBenchmark.readStringUnsafe              avgt   10  0.756 ± 0.023  us/op
 * SerializationBenchmark.writeMessage                  avgt   10  0.247 ± 0.006  us/op
 * SerializationBenchmark.writeMessageUnsafe            avgt   10  0.123 ± 0.009  us/op
 * SerializationBenchmark.writeString                   avgt   10  1.294 ± 0.038  us/op
 * SerializationBenchmark.writeStringUnsafe             avgt   10  1.235 ± 0.029  us/op
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
public class SerializationBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + SerializationBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final TestAllTypes msg = TestAllTypes.newInstance()
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
            .addAllRepeatedFixed32(new int[5])
            .addAllRepeatedDouble(new double[5])
            .addRepeatedForeignMessage(ForeignMessage.newInstance().setC(512));
    byte[] msgBytes = msg.toByteArray();
    byte[] msgOutBuffer = new byte[msgBytes.length];
    final TestAllTypes msgIn = TestAllTypes.newInstance();

    final TestAllTypes stringMessage = TestAllTypes.newInstance()
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
                    "this is a pretty long \uD83D\uDCA9 string \n");
    byte[] stringMsgBytes = stringMessage.toByteArray();
    byte[] stringMsgOutBuffer = new byte[stringMessage.getSerializedSize()];

    final ProtoSink sink = ProtoSink.newSafeInstance();
    final ProtoSource source = ProtoSource.newSafeInstance();

    final ProtoSink unsafeSink = ProtoSink.newUnsafeInstance();
    final ProtoSource unsafeSource = ProtoSource.newUnsafeInstance();

    @Benchmark
    public TestAllTypes readMessage() throws IOException {
        return msgIn.clear().mergeFrom(source.wrap(msgBytes));
    }

    @Benchmark
    public int readString() throws IOException {
        stringMessage.clear().mergeFrom(source.wrap(stringMsgBytes));
        return stringMessage.getDefaultString().length() + stringMessage.getOptionalString().length();
    }

    @Benchmark
    public int computeMessageSerializedSize() throws IOException {
        return msg.getSerializedSize();
    }

    @Benchmark
    public int computeStringSerializedSize() throws IOException {
        return stringMessage.getSerializedSize();
    }

    @Benchmark
    public int writeMessage() throws IOException {
        msg.writeTo(sink.wrap(msgOutBuffer));
        return sink.position();
    }

    @Benchmark
    public int writeString() throws IOException {
        stringMessage.writeTo(sink.wrap(stringMsgOutBuffer));
        return sink.position();
    }

    @Benchmark
    public TestAllTypes readMessageUnsafe() throws IOException {
        return msgIn.clear().mergeFrom(unsafeSource.wrap(msgBytes));
    }

    @Benchmark
    public int readStringUnsafe() throws IOException {
        stringMessage.clear().mergeFrom(unsafeSource.wrap(stringMsgBytes));
        return stringMessage.getDefaultString().length() + stringMessage.getOptionalString().length();
    }

    @Benchmark
    public int writeMessageUnsafe() throws IOException {
        msg.writeTo(unsafeSink.wrap(msgOutBuffer));
        return unsafeSink.position();
    }

    @Benchmark
    public int writeStringUnsafe() throws IOException {
        stringMessage.writeTo(unsafeSink.wrap(stringMsgOutBuffer));
        return unsafeSink.position();
    }

}
