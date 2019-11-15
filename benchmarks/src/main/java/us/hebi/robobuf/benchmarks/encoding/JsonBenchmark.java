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
 * Benchmark                         Mode  Cnt    Score   Error  Units
 * JsonBenchmark.writeBase64         avgt   10  255.828 ± 2.703  us/op
 * JsonBenchmark.writeDoubleNumbers  avgt   10   18.893 ± 0.835  us/op
 * JsonBenchmark.writeFloatNumbers   avgt   10   16.851 ± 0.701  us/op
 * JsonBenchmark.writeIntNumbers     avgt   10   11.586 ± 0.557  us/op
 * JsonBenchmark.writeLongNumbers    avgt   10   19.261 ± 0.757  us/op
 * JsonBenchmark.writeStringAscii    avgt   10    0.738 ± 0.011  us/op
 * JsonBenchmark.writeStringUtf8     avgt   10    1.826 ± 0.029  us/op
 *
 * === JDK12 ===
 * Benchmark                         Mode  Cnt    Score   Error  Units
 * JsonBenchmark.writeBase64         avgt   10  215.028 ± 2.393  us/op
 * JsonBenchmark.writeDoubleNumbers  avgt   10   19.084 ±  0.246  us/op
 * JsonBenchmark.writeFloatNumbers   avgt   10   16.200 ±  0.205  us/op
 * JsonBenchmark.writeIntNumbers     avgt   10   12.432 ±  0.611  us/op
 * JsonBenchmark.writeLongNumbers    avgt   10   17.695 ±  0.313  us/op
 * JsonBenchmark.writeStringAscii    avgt   10    0.618 ± 0.017  us/op
 * JsonBenchmark.writeStringUtf8     avgt   10    2.002 ± 0.070  us/op
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

    final JsonSink jsonSink = JsonSink.newInstance();
    final byte[] key = "\"field\":" .getBytes();
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
        jsonSink.clear().writeField(key, rndBytes);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeStringAscii() throws IOException {
        jsonSink.clear().writeField(key, asciiChars);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeStringUtf8() throws IOException {
        jsonSink.clear().writeField(key, utf8Chars);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeIntNumbers() throws IOException {
        jsonSink.clear().writeField(key, rndInts);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeLongNumbers() throws IOException {
        jsonSink.clear().writeField(key, rndLongs);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeFloatNumbers() throws IOException {
        jsonSink.clear().writeField(key, rndFloats);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeDoubleNumbers() throws IOException {
        jsonSink.clear().writeField(key, rndDoubles);
        return jsonSink.getBuffer().length();
    }

}
