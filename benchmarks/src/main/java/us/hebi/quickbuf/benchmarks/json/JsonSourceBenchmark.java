/*-
 * #%L
 * quickbuf-benchmarks
 * %%
 * Copyright (C) 2019 - 2022 HEBI Robotics
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
import us.hebi.quickbuf.JsonSource;
import us.hebi.quickbuf.compat.GsonSource;
import us.hebi.quickbuf.compat.JacksonSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static us.hebi.quickbuf.benchmarks.comparison.SbeThroughputBenchmarkQuickbuf.*;

/**
 * === JDK 17 (no fallthrough)
 * Benchmark                                     Mode  Cnt    Score    Error   Units
 * JsonSourceBenchmark.testGsonCarDecode        thrpt   10  243,683 ±  9,024  ops/ms
 * JsonSourceBenchmark.testGsonMarketDecode     thrpt   10  379,985 ± 11,020  ops/ms
 * JsonSourceBenchmark.testJacksonCarDecode     thrpt   10  199,277 ±  2,230  ops/ms
 * JsonSourceBenchmark.testJacksonMarketDecode  thrpt   10  288,962 ±  1,458  ops/ms
 * <p>
 * === JDK 17 (with fallthrough)
 * Benchmark                                     Mode  Cnt    Score   Error   Units
 * JsonSourceBenchmark.testGsonCarDecode        thrpt   10  259,449 ± 4,185  ops/ms
 * JsonSourceBenchmark.testGsonMarketDecode     thrpt   10  411,372 ± 4,356  ops/ms
 * JsonSourceBenchmark.testJacksonCarDecode     thrpt   10  190,302 ± 3,499  ops/ms
 * JsonSourceBenchmark.testJacksonMarketDecode  thrpt   10  277,333 ± 4,390  ops/ms
 *
 * === JDK 8 (with fallthrough)
 * Benchmark                                     Mode  Cnt    Score    Error   Units
 * JsonSourceBenchmark.testGsonCarDecode        thrpt   10  253,099 ± 23,003  ops/ms
 * JsonSourceBenchmark.testGsonMarketDecode     thrpt   10  416,122 ±  8,009  ops/ms
 * JsonSourceBenchmark.testJacksonCarDecode     thrpt   10  203,651 ± 10,489  ops/ms
 * JsonSourceBenchmark.testJacksonMarketDecode  thrpt   10  312,991 ± 11,147  ops/ms
 * JsonSourceBenchmark.testJsonCarDecode        thrpt   10  293,815 ± 14,114  ops/ms
 * JsonSourceBenchmark.testJsonMarketDecode     thrpt   10  475,158 ± 27,611  ops/ms
 *
 * @author Florian Enner
 * @since 01 Mär 2022
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class JsonSourceBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + JsonSourceBenchmark.class.getSimpleName() + ".*Json.*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    public JsonSourceBenchmark() {
        marketBytes = JsonSink.newInstance()
                .setWriteEnumsAsInts(true)
                .writeMessage(buildMarketData(marketData))
                .getBuffer()
                .toArray();
        carBytes = JsonSink.newInstance()
                .setWriteEnumsAsInts(true)
                .writeMessage(buildCarData(carData))
                .getBuffer()
                .toArray();

        marketString = new String(marketBytes, StandardCharsets.UTF_8);
        carString = new String(carBytes, StandardCharsets.UTF_8);
    }

    private final MarketDataIncrementalRefreshTrades marketData = MarketDataIncrementalRefreshTrades.newInstance();
    private final byte[] marketBytes;
    private final String marketString;

    private final Car carData = Car.newInstance();
    private final byte[] carBytes;
    private final String carString;

    @Benchmark
    public Object testJsonMarketDecode() throws IOException {
        return marketData.clearQuick().mergeFrom(JsonSource.newInstance(marketBytes));
    }

    @Benchmark
    public Object testJsonCarDecode() throws IOException {
        return carData.clearQuick().mergeFrom(JsonSource.newInstance(carBytes));
    }

    @Benchmark
    public Object testGsonMarketDecode() throws IOException {
        return marketData.clearQuick().mergeFrom(new GsonSource(marketString));
    }

    @Benchmark
    public Object testGsonCarDecode() throws IOException {
        return carData.clearQuick().mergeFrom(new GsonSource(carString));
    }

    @Benchmark
    public Object testJacksonMarketDecode() throws IOException {
        return marketData.clearQuick().mergeFrom(new JacksonSource(marketString));
    }

    @Benchmark
    public Object testJacksonCarDecode() throws IOException {
        return carData.clearQuick().mergeFrom(new JacksonSource(carString));
    }

}
