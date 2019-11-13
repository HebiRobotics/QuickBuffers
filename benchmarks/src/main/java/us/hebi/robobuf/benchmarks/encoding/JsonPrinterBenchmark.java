package us.hebi.robobuf.benchmarks.encoding;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import us.hebi.robobuf.*;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * === JDK8 ===
 * Benchmark                                Mode  Cnt    Score    Error  Units
 * JsonPrinterBenchmark.writeBase64         avgt   10  245.396 ± 14.421  us/op
 * JsonPrinterBenchmark.writeDoubleNumbers  avgt   10   21.968 ±  2.064  us/op
 * JsonPrinterBenchmark.writeFloatNumbers   avgt   10   22.826 ±  0.934  us/op
 * JsonPrinterBenchmark.writeIntNumbers     avgt   10   13.828 ±  0.425  us/op
 * JsonPrinterBenchmark.writeLongNumbers    avgt   10   21.310 ±  1.117  us/op
 * JsonPrinterBenchmark.writeStringAscii    avgt   10    1.023 ±  0.043  us/op
 * JsonPrinterBenchmark.writeStringUtf8     avgt   10    1.782 ±  0.115  us/op
 *
 * === JDK12 ===
 * Benchmark                                Mode  Cnt    Score   Error  Units
 * JsonPrinterBenchmark.writeBase64         avgt   10  194.622 ± 3.784  us/op
 * JsonPrinterBenchmark.writeDoubleNumbers  avgt   10   22.275 ± 0.365  us/op
 * JsonPrinterBenchmark.writeFloatNumbers   avgt   10   23.061 ± 0.499  us/op
 * JsonPrinterBenchmark.writeIntNumbers     avgt   10   13.454 ± 0.511  us/op
 * JsonPrinterBenchmark.writeLongNumbers    avgt   10   19.711 ± 1.085  us/op
 * JsonPrinterBenchmark.writeStringAscii    avgt   10    0.942 ± 0.039  us/op
 * JsonPrinterBenchmark.writeStringUtf8     avgt   10    1.932 ± 0.157  us/op
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
public class JsonPrinterBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + JsonPrinterBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final JsonPrinter printer = JsonPrinter.newInstance();
    final byte[] key = "\"field\":".getBytes();
    final StringBuilder asciiChars = new StringBuilder().append(
            "this is an ascii string that we can encode entirely in the fast path 0123456789" +
                    "this is an ascii string that we can encode entirely in the fast path 0123456789" +
                    "this is an ascii string that we can encode entirely in the fast path 0123456789" +
                    "this is an ascii string that we can encode entirely in the fast path 0123456789" +
                    "this is an ascii string that we can encode entirely in the fast path 0123456789" +
                    "this is an ascii string that we can encode entirely in the fast path 0123456789" +
                    "this is an ascii string that we can encode entirely in the fast path 0123456789" +
                    "this is an ascii string that we can encode entirely in the fast path 0123456789" +
                    "this is an ascii string that we can encode entirely in the fast path 0123456789" +
                    "this is an ascii string that we can encode entirely in the fast path 0123456789");
    final StringBuilder utf8Chars = new StringBuilder().append("utf8\uD83D\uDCA9").append(asciiChars);
    final RepeatedByte rndBytes = RepeatedByte.newEmptyInstance().setLength(256 * 1024);
    final RepeatedInt rndInts = RepeatedInt.newEmptyInstance().setLength(1024);
    final RepeatedLong rndLongs = RepeatedLong.newEmptyInstance().setLength(1024);
    final RepeatedFloat rndFloats = RepeatedFloat.newEmptyInstance().setLength(1024);
    final RepeatedDouble rndDoubles = RepeatedDouble.newEmptyInstance().setLength(1024);
    final Random rnd = new Random();

    @Setup(Level.Trial)
    public void setupData() {
        rnd.setSeed(0);
        rndBytes.setLength(256 * 1024);
        rnd.nextBytes(rndBytes.array());
        for (int i = 0; i < rndInts.length(); i++) {
            rndInts.array()[i] = rnd.nextInt();
            rndLongs.array()[i] = rnd.nextLong();
            rndFloats.array()[i] = rnd.nextFloat();
            rndDoubles.array()[i] = rnd.nextDouble();
        }
    }

    @Benchmark
    public int writeBase64() throws IOException {
        printer.clear().print(key, rndBytes);
        return printer.getBuffer().length();
    }

    @Benchmark
    public int writeStringAscii() throws IOException {
        printer.clear().print(key, asciiChars);
        return printer.getBuffer().length();
    }

    @Benchmark
    public int writeStringUtf8() throws IOException {
        printer.clear().print(key, utf8Chars);
        return printer.getBuffer().length();
    }

    @Benchmark
    public int writeIntNumbers() throws IOException {
        printer.clear().print(key, rndInts);
        return printer.getBuffer().length();
    }

    @Benchmark
    public int writeLongNumbers() throws IOException {
        printer.clear().print(key, rndLongs);
        return printer.getBuffer().length();
    }

    @Benchmark
    public int writeFloatNumbers() throws IOException {
        printer.clear().print(key, rndFloats);
        return printer.getBuffer().length();
    }

    @Benchmark
    public int writeDoubleNumbers() throws IOException {
        printer.clear().print(key, rndDoubles);
        return printer.getBuffer().length();
    }

}
