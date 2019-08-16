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
 * @author Florian Enner
 * @since 16 Aug 2019
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
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
    final ProtoSink sink = ProtoSink.newInstance(outputBuffer);

    @Benchmark
    public TestAllTypes readMessage() throws IOException {
        return inputMsg.clear().mergeFrom(ProtoSource.newInstance(inputData));
    }

    @Benchmark
    public int writeMessage() throws IOException {
        int serializedSize = outputMsg.getSerializedSize();
        ProtoSink sink = ProtoSink.newInstance(outputBuffer, 0, serializedSize);
        outputMsg.writeTo(sink);
        return sink.position();
    }

}
