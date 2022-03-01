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
import us.hebi.quickbuf.compat.GsonSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static us.hebi.quickbuf.benchmarks.comparison.SbeThroughputBenchmarkQuickbuf.*;

/**
 * === JDK 17
 * Benchmark                                         Mode  Cnt    Score   Error   Units
 * JsonSourceBenchmark.testGsonCarDecode_string     thrpt   10  269,341 ± 9,123  ops/ms
 * JsonSourceBenchmark.testGsonMarketDecode_string  thrpt   10  419,436 ± 4,950  ops/ms
 *
 * @author Florian Enner
 * @since 01 Mär 2022
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class JsonSourceBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + JsonSourceBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    public JsonSourceBenchmark() {
        marketBytes = JsonSink.newInstance()
                .writeMessage(buildMarketData(marketData))
                .getBuffer()
                .toArray();
        carBytes = JsonSink.newInstance()
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
    public Object testGsonMarketDecode_string() throws IOException {
        return marketData.clearQuick().mergeFrom(new GsonSource(marketString));
    }

    @Benchmark
    public Object testGsonCarDecode_string() throws IOException {
        return carData.clearQuick().mergeFrom(new GsonSource(carString));
    }

}
