package us.hebi.robobuf.benchmarks.encoding;

import com.google.protobuf.CodedOutputStream;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import us.hebi.robobuf.ProtoSink;
import us.hebi.robobuf.java.ForeignMessage;
import us.hebi.robobuf.java.TestAllTypes;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark                          Mode  Cnt  Score   Error  Units
 * MessageJavaBenchmark.readMessage   avgt   10  1.018 ± 0.043  us/op
 * MessageJavaBenchmark.writeMessage  avgt   10  0.757 ± 0.062  us/op
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
public class MessageJavaBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + MessageJavaBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final TestAllTypes.Builder outputMsg = TestAllTypes.newBuilder()
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
            .addAllRepeatedDouble(Arrays.asList(0d, 0d, 0d, 0d))
            .addRepeatedForeignMessage(ForeignMessage.newBuilder().setC(512));
    byte[] inputData = outputMsg.build().toByteArray();

    //    final TestAllTypes inputMsg = new TestAllTypes();
    byte[] outputBuffer = new byte[inputData.length];
    final ProtoSink sink = ProtoSink.newInstance(outputBuffer);

    @Benchmark
    public TestAllTypes readMessage() throws IOException {
        return TestAllTypes.parseFrom(inputData);
    }

    @Benchmark
    public int writeMessage() throws IOException {
        TestAllTypes msg = outputMsg.build();
        int serializedSize = msg.getSerializedSize();
        msg.writeTo(CodedOutputStream.newInstance(outputBuffer, 0, serializedSize));
        return serializedSize;
    }

}
