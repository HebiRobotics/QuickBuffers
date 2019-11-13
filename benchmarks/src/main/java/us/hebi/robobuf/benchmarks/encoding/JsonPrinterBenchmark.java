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
 * Benchmark                         Mode  Cnt    Score   Error  Units
 * JsonBenchmark.writeAsciiString    avgt   10    1.042 ± 0.011  us/op
 * JsonBenchmark.writeBase64         avgt   10  247.056 ± 4.119  us/op
 * JsonBenchmark.writeDoubleNumbers  avgt   10  253.975 ± 5.162  us/op
 * JsonBenchmark.writeFloatNumbers   avgt   10  116.836 ± 0.814  us/op
 * JsonBenchmark.writeIntNumbers     avgt   10   15.412 ± 0.826  us/op
 * JsonBenchmark.writeLongNumbers    avgt   10   32.334 ± 1.547  us/op
 * JsonBenchmark.writeUtf8String     avgt   10    1.777 ± 0.025  us/op
 *
 * === JDK12 ===
 * Benchmark                         Mode  Cnt    Score    Error  Units
 * JsonBenchmark.writeAsciiString    avgt   10    0.952 ±  0.013  us/op
 * JsonBenchmark.writeBase64         avgt   10  202.268 ±  2.230  us/op
 * JsonBenchmark.writeDoubleNumbers  avgt   10  255.907 ± 10.073  us/op
 * JsonBenchmark.writeFloatNumbers   avgt   10  107.907 ±  2.278  us/op
 * JsonBenchmark.writeIntNumbers     avgt   10   20.976 ±  0.444  us/op
 * JsonBenchmark.writeLongNumbers    avgt   10   30.664 ±  0.495  us/op
 * JsonBenchmark.writeUtf8String     avgt   10    1.901 ±  0.031  us/op
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
    public int writeAsciiString() throws IOException {
        printer.clear().print(key, asciiChars);
        return printer.getBuffer().length();
    }

    @Benchmark
    public int writeUtf8String() throws IOException {
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
