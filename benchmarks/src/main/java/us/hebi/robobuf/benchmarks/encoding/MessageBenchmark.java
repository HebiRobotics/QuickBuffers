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
 * Benchmark                      Mode  Cnt  Score   Error  Units
 * MessageBenchmark.readMessage   avgt   10  0.536 ± 0.024  us/op
 * MessageBenchmark.writeMessage  avgt   10  0.310 ± 0.008  us/op
 *
 * Benchmark                     Mode  Cnt  Score   Error  Units
 * MessageBenchmark.readMessage  avgt   10  0.515 ± 0.015  us/op
 * MessageBenchmark.readString   avgt   10  1.114 ± 0.069  us/op - new String()
 * MessageBenchmark.readString   avgt   10  0.863 ± 0.009  us/op - decodeUtf8Array()
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
public class MessageBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + MessageBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final TestAllTypes outputMsg = new TestAllTypes()
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
    byte[] inputData = outputMsg.toByteArray();

    final TestAllTypes inputMsg = new TestAllTypes();
    byte[] outputBuffer = new byte[inputData.length];
    final ProtoSink sink = ProtoSink.createInstance();
    final ProtoSource source = ProtoSource.createInstance();

    @Benchmark
    public TestAllTypes readMessage() throws IOException {
        return inputMsg.clear().mergeFrom(source.setInput(inputData));
    }

    @Benchmark
    public int writeMessage() throws IOException {
        int serializedSize = outputMsg.getSerializedSize();
        outputMsg.writeTo(sink.setOutput(outputBuffer));
        return sink.position();
    }

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
    byte[] stringInput = stringMessage.toByteArray();
    ProtoSource stringSource = ProtoSource.wrapArray(stringInput);
    byte[] stringOutput = new byte[stringMessage.getSerializedSize()];
    ProtoSink stringSink = ProtoSink.createInstance();

    /**
     * Benchmark                      Mode  Cnt  Score   Error  Units
     * MessageBenchmark.writeMessage  avgt   10  0.308 ± 0.005  us/op
     * MessageBenchmark.writeString   avgt   10  1.380 ± 0.089  us/op
     *
     *
     * Benchmark                      Mode  Cnt  Score   Error  Units
     * MessageBenchmark.writeMessage  avgt   10  0.236 ± 0.039  us/op
     * MessageBenchmark.writeString   avgt   10  1.240 ± 0.072  us/op
     *
     * @return
     * @throws IOException
     */
    @Benchmark
    public int writeString() throws IOException {
        ProtoSink sink = this.stringSink.setOutput(stringOutput);
        stringMessage.writeTo(sink);
        return sink.position();
    }

    @Benchmark
    public TestAllTypes readString() throws IOException {
        return stringMessage.clear().mergeFrom(stringSource.setInput(stringInput));
    }

}
