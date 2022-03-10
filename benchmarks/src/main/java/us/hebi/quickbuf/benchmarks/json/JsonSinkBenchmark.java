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

import com.fasterxml.jackson.core.JsonFactory;
import com.google.gson.Gson;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import protos.benchmarks.real_logic.quickbuf.Examples.Car;
import protos.benchmarks.real_logic.quickbuf.Fix.MarketDataIncrementalRefreshTrades;
import us.hebi.quickbuf.compat.GsonSink;
import us.hebi.quickbuf.compat.JacksonSink;
import us.hebi.quickbuf.JsonSink;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import static us.hebi.quickbuf.benchmarks.comparison.SbeThroughputBenchmarkQuickbuf.*;

/**
 * === JDK 8
 * Benchmark                                   Mode  Cnt     Score     Error   Units
 * JsonSinkBenchmark.testGsonCarEncode        thrpt   10   252,810 ±  39,957  ops/ms
 * JsonSinkBenchmark.testGsonMarketEncode     thrpt   10   450,060 ±  63,790  ops/ms
 * JsonSinkBenchmark.testJacksonCarEncode     thrpt   10   364,245 ±   7,705  ops/ms
 * JsonSinkBenchmark.testJacksonMarketEncode  thrpt   10   475,648 ±  12,104  ops/ms
 * JsonSinkBenchmark.testJsonCarEncode        thrpt   10  1374,798 ±  24,455  ops/ms
 * JsonSinkBenchmark.testJsonMarketEncode     thrpt   10  3413,242 ± 143,734  ops/ms
 *
 * === JDK17
 * Benchmark                                   Mode  Cnt     Score     Error   Units
 * JsonSinkBenchmark.testGsonCarEncode        thrpt   10   242,962 ±   4,886  ops/ms
 * JsonSinkBenchmark.testGsonMarketEncode     thrpt   10   425,402 ±   6,106  ops/ms
 * JsonSinkBenchmark.testJacksonCarEncode     thrpt   10   323,485 ±   9,031  ops/ms
 * JsonSinkBenchmark.testJacksonMarketEncode  thrpt   10   433,531 ±   7,282  ops/ms
 * JsonSinkBenchmark.testJsonCarEncode        thrpt   10  1410,040 ±  12,219  ops/ms
 * JsonSinkBenchmark.testJsonMarketEncode     thrpt   10  3600,006 ± 139,574  ops/ms
 *
 * @author Florian Enner
 * @since 28 Nov 2019
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class JsonSinkBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + JsonSinkBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final JsonSink jsonSink = JsonSink.newInstance().reserve(2048);
    final MarketDataIncrementalRefreshTrades marketData = MarketDataIncrementalRefreshTrades.newInstance();
    final Car car = Car.newInstance();
    static final Gson gson = new Gson();

    private ByteArrayOutputStream encodeBuffer = new ByteArrayOutputStream();
    JsonFactory jsonFactory = new JsonFactory();

    @Benchmark
    public int testJsonMarketEncode() throws IOException {
        return jsonSink.clear()
                .writeMessage(buildMarketData(marketData))
                .getBuffer()
                .length();
    }

    @Benchmark
    public int testJsonCarEncode() throws IOException {
        return jsonSink.clear()
                .writeMessage(buildCarData(car))
                .getBuffer()
                .length();
    }

    @Benchmark
    public Object testGsonMarketEncode() throws IOException {
        StringWriter string = new StringWriter();
        new GsonSink(gson.newJsonWriter(string))
                .writeMessage(buildMarketData(marketData));
        return string.getBuffer();
    }

    @Benchmark
    public Object testGsonCarEncode() throws IOException {
        StringWriter string = new StringWriter();
        new GsonSink(gson.newJsonWriter(string))
                .writeMessage(buildCarData(car));
        return string.getBuffer();
    }

    @Benchmark
    public Object testJacksonMarketEncode() throws IOException {
        encodeBuffer.reset();
        new JacksonSink(jsonFactory.createGenerator(encodeBuffer))
                .writeMessage(buildMarketData(marketData));
        return encodeBuffer.size();
    }

    @Benchmark
    public Object testJacksonCarEncode() throws IOException {
        encodeBuffer.reset();
        new JacksonSink(jsonFactory.createGenerator(encodeBuffer))
                .writeMessage(buildCarData(car));
        return encodeBuffer.size();
    }

}
