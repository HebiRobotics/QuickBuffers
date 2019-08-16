package us.hebi.robobuf.benchmarks.wrapper;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import us.hebi.robobuf.benchmarks.UnsafeUtil;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static us.hebi.robobuf.benchmarks.UnsafeUtil.*;

/**
 * @author Florian Enner
 * @since 31 Jul 2019
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
public class SequentialIterationBenchmark {

    /*
     * ==== Note ====
     * This benchmark measures the performance difference between iterating an array member field
     * vs using a wrapper class that abstracts access. It seems that the JIT inlines wrappers
     * and that it can optimize loops the same way as iterating a standard array.
     *
     * // baseline
     * SequentialIterationBenchmark.iterateArray              avgt   20  129.479 ± 1.348  us/op
     *
     * // single byte at a time
     * SequentialIterationBenchmark.iterateArrayWrapper       avgt   20  128.926 ± 1.754  us/op
     * SequentialIterationBenchmark.iterateHeapWrapper        avgt   20  137.475 ± 3.327  us/op
     * SequentialIterationBenchmark.iterateOffHeapWrapper     avgt   20  129.735 ± 3.364  us/op
     *
     * // 4 bytes at a time
     * SequentialIterationBenchmark.iterateArrayWrapperInt    avgt   20  123.921 ± 2.214  us/op
     * SequentialIterationBenchmark.iterateHeapWrapperInt     avgt   20   32.392 ± 0.654  us/op
     * SequentialIterationBenchmark.iterateOffHeapWrapperInt  avgt   20   31.602 ± 0.192  us/op
     *
     * ==== Note ====
     * With the above code the unsafe loop is optimized the same as a standard array iteration,
     * but there are non-obvious modifications that can have huge performance impacts, e.g.,
     * if the 'remaining' variable changes from int to long, the performance goes down 50%. I
     * assume that this causes the JIT to not pick up on the array pattern and optimize it
     * differently.
     *
     * Benchmark                                              Mode  Cnt    Score   Error  Units
     * SequentialIterationBenchmark.iterateHeapWrapper        avgt   20  298.005 ± 4.628  us/op
     * SequentialIterationBenchmark.iterateOffHeapWrapper     avgt   20  215.658 ± 3.641  us/op
     * SequentialIterationBenchmark.iterateHeapWrapperInt     avgt   20   72.876 ± 0.904  us/op
     * SequentialIterationBenchmark.iterateOffHeapWrapperInt  avgt   20   65.007 ± 1.134  us/op
     *
     */

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + SequentialIterationBenchmark.class.getSimpleName() + ".*WrapperInt")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    Random rnd = new Random(0);
    byte[] bytes = new byte[512 * 1024];
    ByteBuffer directBuffer = (ByteBuffer) ByteBuffer.allocateDirect(bytes.length);
    long nativeAddress = UnsafeUtil.getDirectAddress(directBuffer);

    @Setup(Level.Iteration)
    public void setup() {
        rnd.nextBytes(bytes);
        directBuffer.clear();
        directBuffer.put(bytes);
    }

    @Benchmark
    public long iterateArray() {
        long sum = 0;
        for (int i = 0; i < bytes.length; i++) {
            sum += bytes[i];
        }
        return sum;
    }

    @Benchmark
    public long iterateArrayWrapper() {
        SourceWrappers.ArrayWrapper source = new SourceWrappers.ArrayWrapper(bytes);
        long result = 0;
        while (source.hasNext()) {
            result += source.next();
        }
        return result;
    }

    @Benchmark
    public long iterateArrayWrapperInt() {
        SourceWrappers.ArrayWrapper source = new SourceWrappers.ArrayWrapper(bytes);
        long result = 0;
        while (source.hasNext(4)) {
            result += source.nextInt();
        }
        return result;
    }

    @Benchmark
    public long iterateHeapWrapper() {
        SourceWrappers.UnsafeWrapper source = new SourceWrappers.UnsafeWrapper(bytes, BYTE_ARRAY_OFFSET, bytes.length);
        long result = 0;
        while (source.hasNext()) {
            result += source.next();
        }
        return result;
    }

    @Benchmark
    public long iterateHeapWrapperInt() {
        SourceWrappers.UnsafeWrapper source = new SourceWrappers.UnsafeWrapper(bytes, BYTE_ARRAY_OFFSET, bytes.length);
        long result = 0;
        while (source.hasNext(4)) {
            result += source.nextInt();
        }
        return result;
    }

    @Benchmark
    public long iterateOffHeapWrapper() {
        SourceWrappers.UnsafeWrapper source = new SourceWrappers.UnsafeWrapper(null, nativeAddress, bytes.length);
        long result = 0;
        while (source.hasNext()) {
            result += source.next();
        }
        return result;
    }

    @Benchmark
    public long iterateOffHeapWrapperInt() {
        SourceWrappers.UnsafeWrapper source = new SourceWrappers.UnsafeWrapper(null, nativeAddress, bytes.length);
        long result = 0;
        while (source.hasNext(4)) {
            result += source.nextInt();
        }
        return result;
    }

}
