package us.hebi.robobuf.benchmarks.encoding;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import us.hebi.robobuf.JsonPrinter;
import us.hebi.robobuf.RepeatedByte;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * === JDK8 ===
 * Benchmark                  Mode  Cnt    Score    Error  Units
 * JsonBenchmark.writeBase64  avgt   10  248.695 ± 6.371  us/op
 *
 * === JDK12 ===
 * Benchmark                  Mode  Cnt    Score   Error  Units
 * JsonBenchmark.writeBase64  avgt   10  204.745 ± 5.070  us/op
 *
 * @author Florian Enner
 * @since 13 Nov 2019
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class JsonBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + JsonBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final JsonPrinter printer = JsonPrinter.newInstance();
    final byte[] key = "\"field\":".getBytes();
    final RepeatedByte rndBytes = RepeatedByte.newEmptyInstance();
    final Random rnd = new Random();

    @Setup(Level.Trial)
    public void setupData() {
        rndBytes.setLength(256 * 1024);
        rnd.nextBytes(rndBytes.array());
    }

    @Benchmark
    public int writeBase64() throws IOException {
        printer.clear().print(key, rndBytes);
        return printer.getBuffer().length();
    }

}
