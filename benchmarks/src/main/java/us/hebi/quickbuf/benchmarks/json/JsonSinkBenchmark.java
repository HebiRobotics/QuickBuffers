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

package us.hebi.quickbuf.benchmarks.json;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import protos.benchmarks.real_logic.quickbuf.Examples.Car;
import protos.benchmarks.real_logic.quickbuf.Fix.MarketDataIncrementalRefreshTrades;
import us.hebi.quickbuf.JsonSink;
import us.hebi.quickbuf.compat.GsonSink;
import us.hebi.quickbuf.compat.JacksonSink;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import static us.hebi.quickbuf.benchmarks.comparison.SbeThroughputBenchmarkQuickbuf.*;

/**
 * === Quickbuf RC1 (JDK 17)
 * Benchmark                                   Mode  Cnt     Score     Error   Units
 * JsonSinkBenchmark.testGsonCarEncode        thrpt   20   252,798 ±   2,027  ops/ms
 * JsonSinkBenchmark.testGsonMarketEncode     thrpt   20   439,188 ±   3,255  ops/ms
 * JsonSinkBenchmark.testJacksonCarEncode     thrpt   20   396,907 ±   2,105  ops/ms
 * JsonSinkBenchmark.testJacksonMarketEncode  thrpt   20   621,604 ±   9,139  ops/ms
 *
 * JsonSinkBenchmark.testJsonCarEncode        thrpt   20  1435,850 ±  40,645  ops/ms
 * JsonSinkBenchmark.testJsonMarketEncode     thrpt   20  3602,384 ± 142,406  ops/ms
 *
 * @author Florian Enner
 * @since 28 Nov 2019
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class JsonSinkBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + JsonSinkBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final JsonSink jsonSink = JsonSink.newInstance().setWriteEnumsAsInts(true).reserve(2048);
    final MarketDataIncrementalRefreshTrades marketData = MarketDataIncrementalRefreshTrades.newInstance();
    final Car car = Car.newInstance();
    private final StringWriter encodeString = new StringWriter();

    @Benchmark
    public Object testJsonMarketEncode() throws IOException {
        return jsonSink.clear()
                .writeMessage(buildMarketData(marketData))
                .getBytes();
    }

    @Benchmark
    public Object testJsonCarEncode() throws IOException {
        return jsonSink.clear()
                .writeMessage(buildCarData(car))
                .getBytes();
    }

    @Benchmark
    public Object testGsonMarketEncode() throws IOException {
        encodeString.getBuffer().setLength(0);
        return GsonSink.newStringWriter(encodeString)
                .setWriteEnumsAsInts(true)
                .writeMessage(buildMarketData(marketData))
                .getChars();
    }

    @Benchmark
    public Object testGsonCarEncode() throws IOException {
        encodeString.getBuffer().setLength(0);
        return GsonSink.newStringWriter(encodeString)
                .setWriteEnumsAsInts(true)
                .writeMessage(buildCarData(car))
                .getChars();
    }

    @Benchmark
    public Object testJacksonMarketEncode() throws IOException {
        encodeString.getBuffer().setLength(0);
        return JacksonSink.newStringWriter(encodeString)
                .setWriteEnumsAsInts(true)
                .writeMessage(buildMarketData(marketData))
                .getChars();
    }

    @Benchmark
    public Object testJacksonCarEncode() throws IOException {
        encodeString.getBuffer().setLength(0);
        return JacksonSink.newStringWriter(encodeString)
                .setWriteEnumsAsInts(true)
                .writeMessage(buildCarData(car))
                .getChars();
    }

}
