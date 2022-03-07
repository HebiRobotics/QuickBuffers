/*-
 * #%L
 * benchmarks
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

package us.hebi.quickbuf.benchmarks.encoding;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import protos.test.quickbuf.ForeignMessage;
import protos.test.quickbuf.TestAllTypes;
import us.hebi.quickbuf.ProtoSink;
import us.hebi.quickbuf.ProtoSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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
 * == w/ wrapper sinks
 * Benchmark                                                      Mode  Cnt  Score   Error  Units
 * SerializationBenchmarkQuickbuf.writeMessage                    avgt   10  0,125 ± 0,011  us/op
 * SerializationBenchmarkQuickbuf.writeMessageToByteBuffer        avgt   10  0,306 ± 0,004  us/op
 * SerializationBenchmarkQuickbuf.writeMessageToDirectByteBuffer  avgt   10  0,322 ± 0,006  us/op
 * SerializationBenchmarkQuickbuf.writeMessageToOutputStream      avgt   10  1,795 ± 0,020  us/op
 * SerializationBenchmarkQuickbuf.writeMessageUnsafe              avgt   10  0,083 ± 0,006  us/op
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
public class SerializationBenchmarkQuickbuf {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + SerializationBenchmarkQuickbuf.class.getSimpleName() + ".*")
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
    final ProtoSource source = ProtoSource.newInstance();

    final ProtoSink unsafeSink = ProtoSink.newUnsafeInstance();
    final ProtoSource unsafeSource = ProtoSource.newUnsafeInstance();

    final ByteArrayOutputStream baos = new ByteArrayOutputStream(stringMsgOutBuffer.length);
    final ProtoSink streamSink = ProtoSink.wrap(baos);
    final ProtoSink byteBufferSink = ProtoSink.wrap(ByteBuffer.allocate(stringMsgOutBuffer.length));
    final ProtoSink directByteBufferSink = ProtoSink.wrap(ByteBuffer.allocateDirect(stringMsgOutBuffer.length));

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

    @Benchmark
    public int writeMessageToOutputStream() throws IOException {
        baos.reset();
        msg.writeTo(streamSink);
        return baos.size();
    }

    @Benchmark
    public int writeMessageToByteBuffer() throws IOException {
        msg.writeTo(byteBufferSink.reset());
        return byteBufferSink.position();
    }

    @Benchmark
    public int writeMessageToDirectByteBuffer() throws IOException {
        msg.writeTo(directByteBufferSink.reset());
        return directByteBufferSink.position();
    }

}
