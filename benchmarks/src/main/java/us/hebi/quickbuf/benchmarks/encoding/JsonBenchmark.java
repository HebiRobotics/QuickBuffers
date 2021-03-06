/*-
 * #%L
 * quickbuf-benchmarks
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

package us.hebi.quickbuf.benchmarks.encoding;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import us.hebi.quickbuf.*;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * === JDK8 ===
 * Benchmark                         Mode  Cnt    Score   Error  Units
 * JsonBenchmark.writeBase64         avgt   10  255.828 ± 2.703  us/op
 * JsonBenchmark.writeDoubleNumbers  avgt   10   22.074 ± 0.553  us/op
 * JsonBenchmark.writeFloatNumbers   avgt   10   17.536 ± 0.324  us/op
 * JsonBenchmark.writeIntNumbers     avgt   10   10.845 ± 0.240  us/op
 * JsonBenchmark.writeLongNumbers    avgt   10   16.440 ± 0.339  us/op
 * JsonBenchmark.writeStringAscii    avgt   10    0.738 ± 0.011  us/op
 * JsonBenchmark.writeStringUtf8     avgt   10    1.826 ± 0.029  us/op
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
    final FieldName name = FieldName.forField("field");
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
            rndFloats.array()[i] = roundFloat(rnd.nextFloat() * (byte) rnd.nextInt());
            rndDoubles.array()[i] = roundDouble(rnd.nextDouble() * (byte) rnd.nextInt());
        }
    }

    private static float roundFloat(float value) {
        final float factor = 1E5f;
        return ((long) (value * factor + 0.5)) / factor;
    }

    private static double roundDouble(double value) {
        final double factor = 1E8;
        return ((long) (value * factor + 0.5)) / factor;
    }

    @Benchmark
    public int writeBase64() throws IOException {
        jsonSink.clear().writeBytes(name, rndBytes);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeStringAscii() throws IOException {
        jsonSink.clear().writeString(name, asciiChars);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeStringUtf8() throws IOException {
        jsonSink.clear().writeString(name, utf8Chars);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeIntNumbers() throws IOException {
        jsonSink.clear().writeRepeatedInt32(name, rndInts);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeLongNumbers() throws IOException {
        jsonSink.clear().writeRepeatedInt64(name, rndLongs);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeFloatNumbers() throws IOException {
        jsonSink.clear().writeRepeatedFloat(name, rndFloats);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeDoubleNumbers() throws IOException {
        jsonSink.clear().writeRepeatedDouble(name, rndDoubles);
        return jsonSink.getBuffer().length();
    }

}
