package us.hebi.robobuf.benchmarks.encoding;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import us.hebi.robobuf.ProtoMessage;
import us.hebi.robobuf.ProtoSink;
import protos.test.robo.RepeatedPackables;
import protos.test.robo.TestAllTypes;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * ==== Array Sink ==== (256x each type)
 * Benchmark                                        Mode  Cnt  Score   Error  Units
 * RepeatedWriteBenchmark.writeNonPackedDouble      avgt   10  1.660 ± 0.026  us/op
 * RepeatedWriteBenchmark.writeNonPackedFloat       avgt   10  0.978 ± 0.021  us/op
 * RepeatedWriteBenchmark.writeNonPackedInt32       avgt   10  2.320 ± 0.100  us/op
 * RepeatedWriteBenchmark.writeNonPackedInt64       avgt   10  3.048 ± 0.253  us/op
 * RepeatedWriteBenchmark.writeNonPackedIntFixed32  avgt   10  0.984 ± 0.036  us/op
 * RepeatedWriteBenchmark.writeNonPackedIntFixed64  avgt   10  1.603 ± 0.065  us/op
 * RepeatedWriteBenchmark.writePackedDouble         avgt   10  0.831 ± 0.016  us/op
 * RepeatedWriteBenchmark.writePackedFloat          avgt   10  0.442 ± 0.023  us/op
 * RepeatedWriteBenchmark.writePackedInt32          avgt   10  2.354 ± 0.074  us/op
 * RepeatedWriteBenchmark.writePackedInt64          avgt   10  3.268 ± 0.205  us/op
 * RepeatedWriteBenchmark.writePackedIntFixed32     avgt   10  0.329 ± 0.011  us/op
 * RepeatedWriteBenchmark.writePackedIntFixed64     avgt   10  0.772 ± 0.064  us/op
 *
 * ==== Unsafe Array Sink ==== (256x each type)
 * Benchmark                                        Mode  Cnt  Score   Error  Units
 * RepeatedWriteBenchmark.writeNonPackedDouble      avgt   10  0.742 ± 0.020  us/op
 * RepeatedWriteBenchmark.writeNonPackedFloat       avgt   10  0.710 ± 0.013  us/op
 * RepeatedWriteBenchmark.writeNonPackedInt32       avgt   10  2.322 ± 0.082  us/op
 * RepeatedWriteBenchmark.writeNonPackedInt64       avgt   10  2.951 ± 0.205  us/op
 * RepeatedWriteBenchmark.writeNonPackedIntFixed32  avgt   10  0.699 ± 0.031  us/op
 * RepeatedWriteBenchmark.writeNonPackedIntFixed64  avgt   10  0.716 ± 0.026  us/op
 * RepeatedWriteBenchmark.writePackedDouble         avgt   10  0.097 ± 0.003  us/op
 * RepeatedWriteBenchmark.writePackedFloat          avgt   10  0.056 ± 0.002  us/op
 * RepeatedWriteBenchmark.writePackedInt32          avgt   10  2.292 ± 0.085  us/op
 * RepeatedWriteBenchmark.writePackedInt64          avgt   10  3.230 ± 0.113  us/op
 * RepeatedWriteBenchmark.writePackedIntFixed32     avgt   10  0.061 ± 0.005  us/op
 * RepeatedWriteBenchmark.writePackedIntFixed64     avgt   10  0.099 ± 0.003  us/op
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
