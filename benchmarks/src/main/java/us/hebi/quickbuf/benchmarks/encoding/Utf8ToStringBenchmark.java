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
import us.hebi.quickbuf.ProtoUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * === JDK8
 * Benchmark                               Mode  Cnt  Score   Error  Units
 * Utf8ToStringBenchmark.readBuiltinAscii  avgt   10  0,332 ± 0,020  us/op
 * Utf8ToStringBenchmark.readBuiltinUtf8   avgt   10  0,617 ± 0,015  us/op
 * Utf8ToStringBenchmark.readManualAscii   avgt   10  0,468 ± 0,028  us/op
 * Utf8ToStringBenchmark.readManualUtf8    avgt   10  0,692 ± 0,046  us/op
 *
 * === JDK17
 * Benchmark                               Mode  Cnt  Score   Error  Units
 * Utf8ToStringBenchmark.readBuiltinAscii  avgt   10  0,068 ± 0,001  us/op
 * Utf8ToStringBenchmark.readBuiltinUtf8   avgt   10  0,676 ± 0,016  us/op
 * Utf8ToStringBenchmark.readManualAscii   avgt   10  0,525 ± 0,021  us/op
 * Utf8ToStringBenchmark.readManualUtf8    avgt   10  0,658 ± 0,003  us/op
 *
 * @author Florian Enner
 * @since 26 Nov 2019
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class Utf8ToStringBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + Utf8ToStringBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    byte[] asciiBytes = ("" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n" +
            "this is a pretty long ascii string \n").getBytes(StandardCharsets.US_ASCII);

    byte[] utf8Bytes = ("" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n" +
            "this is a pretty long \uD83D\uDCA9 string \n").getBytes(StandardCharsets.UTF_8);

    final StringBuilder builder = new StringBuilder(128);

    @Benchmark
    public String readBuiltinAscii() throws IOException {
        return new String(asciiBytes, 0, asciiBytes.length, StandardCharsets.UTF_8);
    }

    @Benchmark
    public String readBuiltinUtf8() throws IOException {
        return new String(utf8Bytes, 0, utf8Bytes.length, StandardCharsets.UTF_8);
    }

    @Benchmark
    public String readManualAscii() throws IOException {
        ProtoUtil.decodeUtf8(asciiBytes, 0, asciiBytes.length, builder);
        return builder.toString();
    }

    @Benchmark
    public String readManualUtf8() throws IOException {
        ProtoUtil.decodeUtf8(utf8Bytes, 0, utf8Bytes.length, builder);
        return builder.toString();
    }

}
