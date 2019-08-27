package us.hebi.robobuf.benchmarks.comparison;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.benchmarks.BenchmarkMessage2;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import us.hebi.robobuf.ProtoSink;
import us.hebi.robobuf.ProtoSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * This benchmark was supposed to be similar to Google's provided Caliper benchmarks
 * in protobuf/benchmarks/datasets. Unfortunately, of the 4 datasets in tag 3.9.1:
 *
 * - #3 and #4 don't compile because protoc can't figure out dependencies (even when
 * only using the officially provided generator)
 * - #1 can't be parsed by Java/Lite because of missing required fields
 * - #2 works, but it is mostly measuring unknown fields, which is meaningless
 * because robobuf ignores them (serialized size 85 KB vs 52 Byte)
 *
 * @author Florian Enner
 * @since 27 Aug 2019
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(0)
@Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class GoogleProtobufBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + GoogleProtobufBenchmark.class.getSimpleName() + ".*write.*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    private static byte[] loadDataset(String resource) {
        InputStream inputStream = GoogleProtobufBenchmark.class.getResourceAsStream(resource);
        try {
            byte[] data = new byte[inputStream.available()];
            if (inputStream.read(data) != data.length)
                throw new AssertionError();
            return data;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public GoogleProtobufBenchmark() {
        try {
            gMsg = BenchmarkMessage2.GoogleMessage2.parseFrom(dataset2);
            rMsg = com.google.protobuf.robo.BenchmarkMessage2.GoogleMessage2.parseFrom(dataset2);
        } catch (IOException e) {
            throw new AssertionError();
        }
    }

    final byte[] dataset2 = loadDataset("/datasets/google_message2/dataset.google_message2.pb");
    final BenchmarkMessage2.GoogleMessage2 gMsg;
    final com.google.protobuf.robo.BenchmarkMessage2.GoogleMessage2 rMsg;

    byte[] output = new byte[dataset2.length];
    final ProtoSource arraySource = ProtoSource.createInstance();
    final ProtoSource unsafeSource = ProtoSource.createUnsafe();
    final ProtoSink arraySink = ProtoSink.createInstance();
    final ProtoSink unsafeSink = ProtoSink.createUnsafe();
    com.google.protobuf.robo.BenchmarkMessage2.GoogleMessage2 msg2 = new com.google.protobuf.robo.BenchmarkMessage2.GoogleMessage2();

    @Benchmark
    public BenchmarkMessage2.GoogleMessage2 parseDataset2_Lite() throws IOException {
        return BenchmarkMessage2.GoogleMessage2.parseFrom(dataset2);
    }

    @Benchmark
    public com.google.protobuf.robo.BenchmarkMessage2.GoogleMessage2 parseDataset2_RobobufArray() throws IOException {
        return msg2.clear().mergeFrom(arraySource.setInput(dataset2));
    }

    @Benchmark
    public com.google.protobuf.robo.BenchmarkMessage2.GoogleMessage2 parseDataset2_RobobufUnsafe() throws IOException {
        return msg2.clear().mergeFrom(unsafeSource.setInput(dataset2));
    }

    @Benchmark
    public int writeDataset2_Lite() throws IOException {
        CodedOutputStream out = CodedOutputStream.newInstance(output);
        gMsg.writeTo(out);
        return out.getTotalBytesWritten();
    }

    @Benchmark
    public int writeDataset2_RobobufArray() throws IOException {
        arraySink.setOutput(output);
        rMsg.writeTo(arraySink);
        return arraySink.position();
    }

    @Benchmark
    public int writeDataset2_RobobufUnsafe() throws IOException {
        unsafeSink.setOutput(output);
        rMsg.writeTo(unsafeSink);
        return arraySink.position();
    }

}
