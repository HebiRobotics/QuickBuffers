/*-
 * #%L
 * quickbuf-benchmarks
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * === JDK17
 * Benchmark                              Mode  Cnt    Score   Error  Units
 * JsonBenchmark.writeBase64              avgt   10  187,572 ± 3,836  us/op
 * JsonBenchmark.writeDoubleNumbers       avgt   10   17,491 ± 0,447  us/op
 * JsonBenchmark.writeFloatNumbers        avgt   10   12,641 ± 1,209  us/op
 * JsonBenchmark.writeIntNumbers          avgt   10    7,365 ± 0,505  us/op
 * JsonBenchmark.writeLongNumbers         avgt   10   10,928 ± 0,345  us/op
 * JsonBenchmark.writeStringAsciiBytes    avgt   10    0,724 ± 0,005  us/op
 * JsonBenchmark.writeStringAsciiChars    avgt   10    0,606 ± 0,019  us/op
 * JsonBenchmark.writeStringAsciiEncoded  avgt   10    0,389 ± 0,003  us/op
 * JsonBenchmark.writeStringUtf8Bytes     avgt   10    0,397 ± 0,005  us/op
 * JsonBenchmark.writeStringUtf8Chars     avgt   10    3,015 ± 0,026  us/op
 * JsonBenchmark.writeStringUtf8Encoded   avgt   10    0,384 ± 0,003  us/op
 *
 * ============= Floating point encoding (JDK17) =============
 * === Schubfach (default implementation)
 * Benchmark                         Mode  Cnt   Score   Error  Units
 * JsonBenchmark.writeDoubleNumbers  avgt   10  69,058 ± 1,600  us/op
 * JsonBenchmark.writeFloatNumbers   avgt   10  49,168 ± 2,137  us/op
 *
 * === Schubfach (default algorithm after direct integration)
 * Benchmark                         Mode  Cnt   Score   Error  Units
 * JsonBenchmark.writeDoubleNumbers  avgt   10  45,576 ± 0,204  us/op
 * JsonBenchmark.writeFloatNumbers   avgt   10  37,687 ± 0,286  us/op
 *
 * === Schubfach (custom without comma)
 * Benchmark                         Mode  Cnt   Score   Error  Units
 * JsonBenchmark.writeDoubleNumbers  avgt   10  35,870 ± 0,863  us/op
 * JsonBenchmark.writeFloatNumbers   avgt   10  28,251 ± 0,633  us/op
 *
 * == Fixed floating point (deprecated)
 * Benchmark                         Mode  Cnt   Score   Error  Units
 * JsonBenchmark.writeDoubleNumbers  avgt   10  17,468 ± 0,406  us/op
 * JsonBenchmark.writeFloatNumbers   avgt   10  13,166 ± 0,239  us/op
 *
 * @author Florian Enner
 * @since 13 Nov 2019
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class JsonBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + JsonBenchmark.class.getSimpleName() + ".*")
//                .jvmArgs("-Dquickbuf.json.float_encoding=minimal")
//                .jvmArgs("-Dquickbuf.json.float_encoding=no_comma")
//                .jvmArgs("-Dquickbuf.json.float_encoding=fixed")
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

    final Utf8String asciiBytes = Utf8String.newInstance(utf8Chars.toString());
    final Utf8String utf8Bytes = Utf8String.newInstance(asciiChars.toString());

    @Setup(Level.Trial)
    public void setupData() {
        rnd.setSeed(0);
        rndBytes.setLength(256 * 1024);
        rnd.nextBytes(rndBytes.array());
        for (int i = 0; i < rndInts.length(); i++) {
            rndInts.array()[i] = (int) randomDigitLong(7);
            rndLongs.array()[i] = randomDigitLong(18);
            rndFloats.array()[i] = roundFloat(rnd.nextFloat() * (byte) rnd.nextInt());
            rndDoubles.array()[i] = roundDouble(rnd.nextDouble() * (byte) rnd.nextInt());
        }
        asciiBytes.size();
        utf8Bytes.size();
    }

    private long randomDigitLong(int maxDigits) {
        int numDigits = rnd.nextInt(maxDigits + 1);
        long value = 0;
        for (int i = 0; i < numDigits; i++) {
            value = value * 10 + rnd.nextInt(10);
        }
        return value;
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
        return jsonSink.getBytes().length();
    }

    @Benchmark
    public int writeStringAsciiChars() throws IOException {
        jsonSink.clear().writeString(name, asciiChars);
        return jsonSink.getBytes().length();
    }

    @Benchmark
    public int writeStringUtf8Chars() throws IOException {
        jsonSink.clear().writeString(name, utf8Chars);
        return jsonSink.getBytes().length();
    }

    @Benchmark
    public int writeStringAsciiBytes() throws IOException {
        asciiBytes.copyFrom(asciiChars);
        asciiBytes.size();
        jsonSink.clear().writeString(name, asciiBytes);
        return jsonSink.getBytes().length();
    }

    @Benchmark
    public int writeStringUtf8Bytes() throws IOException {
        utf8Bytes.copyFrom(utf8Bytes);
        utf8Bytes.size();
        jsonSink.clear().writeString(name, utf8Bytes);
        return jsonSink.getBytes().length();
    }

    @Benchmark
    public int writeStringAsciiEncoded() throws IOException {
        jsonSink.clear().writeString(name, asciiBytes);
        return jsonSink.getBytes().length();
    }

    @Benchmark
    public int writeStringUtf8Encoded() throws IOException {
        jsonSink.clear().writeString(name, utf8Bytes);
        return jsonSink.getBytes().length();
    }

    @Benchmark
    public int writeIntNumbers() throws IOException {
        jsonSink.clear().writeRepeatedInt32(name, rndInts);
        return jsonSink.getBytes().length();
    }

    @Benchmark
    public int writeLongNumbers() throws IOException {
        jsonSink.clear().writeRepeatedInt64(name, rndLongs);
        return jsonSink.getBytes().length();
    }

    @Benchmark
    public int writeFloatNumbers() throws IOException {
        jsonSink.clear().writeRepeatedFloat(name, rndFloats);
        return jsonSink.getBytes().length();
    }

    @Benchmark
    public int writeDoubleNumbers() throws IOException {
        jsonSink.clear().writeRepeatedDouble(name, rndDoubles);
        return jsonSink.getBytes().length();
    }

}
