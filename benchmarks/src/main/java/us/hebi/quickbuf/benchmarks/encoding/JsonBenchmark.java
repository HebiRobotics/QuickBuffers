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
 * Benchmark                              Mode  Cnt  Score   Error  Units
 * JsonBenchmark.writeStringAsciiBytes    avgt   10  0,985 ± 0,008  us/op
 * JsonBenchmark.writeStringAsciiChars    avgt   10  0,767 ± 0,022  us/op
 * JsonBenchmark.writeStringAsciiEncoded  avgt   10  0,420 ± 0,006  us/op
 * JsonBenchmark.writeStringUtf8Bytes     avgt   10  0,442 ± 0,005  us/op
 * JsonBenchmark.writeStringUtf8Chars     avgt   10  2,307 ± 0,047  us/op
 * JsonBenchmark.writeStringUtf8Encoded   avgt   10  0,418 ± 0,009  us/op
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
            rndInts.array()[i] = rnd.nextInt();
            rndLongs.array()[i] = rnd.nextLong();
            rndFloats.array()[i] = roundFloat(rnd.nextFloat() * (byte) rnd.nextInt());
            rndDoubles.array()[i] = roundDouble(rnd.nextDouble() * (byte) rnd.nextInt());
        }
        asciiBytes.size();
        utf8Bytes.size();
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
    public int writeStringAsciiChars() throws IOException {
        jsonSink.clear().writeString(name, asciiChars);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeStringUtf8Chars() throws IOException {
        jsonSink.clear().writeString(name, utf8Chars);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeStringAsciiBytes() throws IOException {
        asciiBytes.copyFrom(asciiChars);
        asciiBytes.size();
        jsonSink.clear().writeString(name, asciiBytes);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeStringUtf8Bytes() throws IOException {
        utf8Bytes.copyFrom(utf8Bytes);
        utf8Bytes.size();
        jsonSink.clear().writeString(name, utf8Bytes);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeStringAsciiEncoded() throws IOException {
        jsonSink.clear().writeString(name, asciiBytes);
        return jsonSink.getBuffer().length();
    }

    @Benchmark
    public int writeStringUtf8Encoded() throws IOException {
        jsonSink.clear().writeString(name, utf8Bytes);
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
