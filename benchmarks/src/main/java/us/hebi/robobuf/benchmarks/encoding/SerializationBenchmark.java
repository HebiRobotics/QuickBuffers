package us.hebi.robobuf.benchmarks.encoding;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import us.hebi.robobuf.ProtoSink;
import us.hebi.robobuf.ProtoSource;
import us.hebi.robobuf.robo.ForeignMessage;
import us.hebi.robobuf.robo.TestAllTypes;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * ==== Array Input/Output ====
 * Benchmark                                      Mode  Cnt  Score   Error  Units
 * MessageBenchmark.computeMessageSerializedSize  avgt   10  0.063 ± 0.003  us/op
 * MessageBenchmark.computeStringSerializedSize   avgt   10  0.876 ± 0.031  us/op
 * MessageBenchmark.readMessage                   avgt   10  0.511 ± 0.030  us/op
 * MessageBenchmark.readMessageUnsafe             avgt   10  0.484 ± 0.084  us/op
 * MessageBenchmark.readString                    avgt   10  0.917 ± 0.022  us/op
 * MessageBenchmark.readStringUnsafe              avgt   10  1.170 ± 0.570  us/op
 * MessageBenchmark.writeMessage                  avgt   10  0.274 ± 0.040  us/op
 * MessageBenchmark.writeMessageUnsafe            avgt   10  0.141 ± 0.006  us/op
 * MessageBenchmark.writeString                   avgt   10  1.335 ± 0.036  us/op
 * MessageBenchmark.writeStringUnsafe             avgt   10  1.602 ± 0.059  us/op
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

    final TestAllTypes msg = new TestAllTypes()
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
            .addRepeatedForeignMessage(new ForeignMessage().setC(512));
    byte[] msgBytes = msg.toByteArray();
    byte[] msgOutBuffer = new byte[msgBytes.length];
    final TestAllTypes msgIn = new TestAllTypes();

    final TestAllTypes stringMessage = new TestAllTypes()
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

    final ProtoSink sink = ProtoSink.createInstance();
    final ProtoSource source = ProtoSource.createInstance();

    final ProtoSink unsafeSink = ProtoSink.createUnsafe();
    final ProtoSource unsafeSource = ProtoSource.createUnsafe();

    @Benchmark
    public TestAllTypes readMessage() throws IOException {
        return msgIn.clear().mergeFrom(source.setInput(msgBytes));
    }

    @Benchmark
    public TestAllTypes readString() throws IOException {
        return stringMessage.clear().mergeFrom(source.setInput(stringMsgBytes));
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
        msg.writeTo(sink.setOutput(msgOutBuffer));
        return sink.position();
    }

    @Benchmark
    public int writeString() throws IOException {
        stringMessage.writeTo(sink.setOutput(stringMsgOutBuffer));
        return sink.position();
    }

    @Benchmark
    public TestAllTypes readMessageUnsafe() throws IOException {
        return msgIn.clear().mergeFrom(unsafeSource.setInput(msgBytes));
    }

    @Benchmark
    public TestAllTypes readStringUnsafe() throws IOException {
        return stringMessage.clear().mergeFrom(unsafeSource.setInput(stringMsgBytes));
    }

    @Benchmark
    public int writeMessageUnsafe() throws IOException {
        msg.writeTo(unsafeSink.setOutput(msgOutBuffer));
        return unsafeSink.position();
    }

    @Benchmark
    public int writeStringUnsafe() throws IOException {
        stringMessage.writeTo(unsafeSink.setOutput(stringMsgOutBuffer));
        return unsafeSink.position();
    }

}
