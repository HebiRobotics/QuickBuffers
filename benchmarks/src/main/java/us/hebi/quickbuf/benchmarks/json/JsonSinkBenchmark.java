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
import us.hebi.quickbuf.JsonSink;
import us.hebi.quickbuf.compat.GsonSink;
import us.hebi.quickbuf.compat.JacksonSink;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import static us.hebi.quickbuf.benchmarks.comparison.SbeThroughputBenchmarkQuickbuf.*;

/**
 * === JDK 8
 * Benchmark                                   Mode  Cnt     Score     Error   Units
 * JsonSinkBenchmark.testGsonCarEncode        thrpt   10   260,005 ±  40,651  ops/ms
 * JsonSinkBenchmark.testGsonMarketEncode     thrpt   10   454,905 ±  64,111  ops/ms
 * JsonSinkBenchmark.testJacksonCarEncode     thrpt   10   385,079 ±   3,634  ops/ms
 * JsonSinkBenchmark.testJacksonMarketEncode  thrpt   10   496,499 ±   2,560  ops/ms
 * JsonSinkBenchmark.testJsonCarEncode        thrpt   10  1413,708 ±  10,256  ops/ms
 * JsonSinkBenchmark.testJsonMarketEncode     thrpt   10  3223,580 ±  76,672  ops/ms
 * <p>
 * === JDK17
 * Benchmark                                   Mode  Cnt     Score     Error   Units
 * JsonSinkBenchmark.testGsonCarEncode        thrpt   10   244.408 ±   2.071  ops/ms
 * JsonSinkBenchmark.testGsonMarketEncode     thrpt   10   418.432 ±   3.404  ops/ms
 * JsonSinkBenchmark.testJacksonCarEncode     thrpt   10   327.974 ±   3.657  ops/ms
 * JsonSinkBenchmark.testJacksonMarketEncode  thrpt   10   429.415 ±   6.475  ops/ms
 * JsonSinkBenchmark.testJsonCarEncode        thrpt   10  1376.977 ±  23.032  ops/ms
 * JsonSinkBenchmark.testJsonMarketEncode     thrpt   10  3269.737 ±  45.623  ops/ms
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

    final JsonSink jsonSink = JsonSink.newInstance().setWriteEnumsAsInts(true).reserve(2048);
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
                .setWriteEnumsAsInts(true)
                .writeMessage(buildMarketData(marketData));
        return string.getBuffer();
    }

    @Benchmark
    public Object testGsonCarEncode() throws IOException {
        StringWriter string = new StringWriter();
        new GsonSink(gson.newJsonWriter(string))
                .setWriteEnumsAsInts(true)
                .writeMessage(buildCarData(car));
        return string.getBuffer();
    }

    @Benchmark
    public Object testJacksonMarketEncode() throws IOException {
        encodeBuffer.reset();
        new JacksonSink(jsonFactory.createGenerator(encodeBuffer))
                .setWriteEnumsAsInts(true)
                .writeMessage(buildMarketData(marketData));
        return encodeBuffer.size();
    }

    @Benchmark
    public Object testJacksonCarEncode() throws IOException {
        encodeBuffer.reset();
        new JacksonSink(jsonFactory.createGenerator(encodeBuffer))
                .setWriteEnumsAsInts(true)
                .writeMessage(buildCarData(car));
        return encodeBuffer.size();
    }

}
