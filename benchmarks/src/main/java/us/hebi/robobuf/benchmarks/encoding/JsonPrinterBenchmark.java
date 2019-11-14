/*-
 * #%L
 * robobuf-benchmarks
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

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
 * Benchmark                                Mode  Cnt    Score   Error  Units
 * JsonPrinterBenchmark.writeBase64         avgt   10  235.938 ± 5.254  us/op
 * JsonPrinterBenchmark.writeDoubleNumbers  avgt   10   21.629 ± 0.246  us/op
 * JsonPrinterBenchmark.writeFloatNumbers   avgt   10   21.220 ± 0.476  us/op
 * JsonPrinterBenchmark.writeIntNumbers     avgt   10   13.147 ± 0.402  us/op
 * JsonPrinterBenchmark.writeLongNumbers    avgt   10   21.321 ± 0.656  us/op
 * JsonPrinterBenchmark.writeStringAscii    avgt   10    1.003 ± 0.007  us/op
 * JsonPrinterBenchmark.writeStringUtf8     avgt   10    1.730 ± 0.025  us/op
 *
 * === JDK12 ===
 * Benchmark                                Mode  Cnt    Score   Error  Units
 * JsonPrinterBenchmark.writeBase64         avgt   10  193.091 ± 3.674  us/op
 * JsonPrinterBenchmark.writeDoubleNumbers  avgt   10   22.055 ± 0.440  us/op
 * JsonPrinterBenchmark.writeFloatNumbers   avgt   10   21.044 ± 0.551  us/op
 * JsonPrinterBenchmark.writeIntNumbers     avgt   10   13.168 ± 0.271  us/op
 * JsonPrinterBenchmark.writeLongNumbers    avgt   10   20.170 ± 0.550  us/op
 * JsonPrinterBenchmark.writeStringAscii    avgt   10    0.910 ± 0.018  us/op
 * JsonPrinterBenchmark.writeStringUtf8     avgt   10    1.937 ± 0.165  us/op
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
