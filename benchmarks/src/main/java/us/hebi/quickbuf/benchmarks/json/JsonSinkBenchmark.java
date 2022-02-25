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
import us.hebi.quickbuf.GsonSink;
import us.hebi.quickbuf.JacksonSink;
import us.hebi.quickbuf.JsonSink;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import static us.hebi.quickbuf.benchmarks.comparison.SbeThroughputBenchmarkQuickbuf.*;

/**
 * === JDK 8
 * Benchmark                                   Mode  Cnt     Score    Error   Units
 * JsonSinkBenchmark.testGsonCarEncode        thrpt   10   188.928 ±  65.934  ops/ms
 * JsonSinkBenchmark.testGsonMarketEncode     thrpt   10   396.440 ± 170.741  ops/ms
 * JsonSinkBenchmark.testJacksonCarEncode     thrpt   10   417.064 ±   5.880  ops/ms
 * JsonSinkBenchmark.testJacksonMarketEncode  thrpt   10   595.311 ±  21.524  ops/ms
 * JsonSinkBenchmark.testJsonCarEncode        thrpt   10  1490.562 ±  16.291  ops/ms
 * JsonSinkBenchmark.testJsonMarketEncode     thrpt   10  3438.101 ±  35.577  ops/ms
 *
 * === JDK13
 * JsonSinkBenchmark.testGsonCarEncode        thrpt   10   219.425 ±  3.166  ops/ms
 * JsonSinkBenchmark.testGsonMarketEncode     thrpt   10   475.153 ±  9.760  ops/ms
 * JsonSinkBenchmark.testJacksonCarEncode     thrpt   10   338.599 ±  6.220  ops/ms
 * JsonSinkBenchmark.testJacksonMarketEncode  thrpt   10   478.171 ± 10.901  ops/ms
 * JsonSinkBenchmark.testJsonCarEncode        thrpt   10  1400.559 ± 36.754  ops/ms
 * JsonSinkBenchmark.testJsonMarketEncode     thrpt   10  3446.400 ± 53.235  ops/ms
 *
 * === before FieldName layer of indirection
 * JsonSinkBenchmark.testJsonCarEncode        thrpt   10  1604.781 ± 25.897  ops/ms
 * JsonSinkBenchmark.testJsonMarketEncode     thrpt   10  3624.096 ± 24.779  ops/ms
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
