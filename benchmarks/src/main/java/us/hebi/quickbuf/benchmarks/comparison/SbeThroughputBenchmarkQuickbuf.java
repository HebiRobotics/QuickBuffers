/*-
 * #%L
 * benchmarks
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

package us.hebi.quickbuf.benchmarks.comparison;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import protos.benchmarks.real_logic.quickbuf.Examples;
import protos.benchmarks.real_logic.quickbuf.Examples.Car;
import protos.benchmarks.real_logic.quickbuf.Fix;
import protos.benchmarks.real_logic.quickbuf.Fix.MarketDataIncrementalRefreshTrades;
import us.hebi.quickbuf.JsonSink;
import us.hebi.quickbuf.ProtoSink;
import us.hebi.quickbuf.ProtoSource;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * === Quickbuf (JDK8 unsafe)
 * Benchmark                                     Mode  Cnt     Score     Error   Units
 * SbeThroughputBenchmarkQuickbuf.testCarDecode     thrpt   10   2175.096 ±  39.897  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testCarEncode     thrpt   10   3940.677 ±  81.755  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketDecode  thrpt   10   7647.549 ± 260.050  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketEncode  thrpt   10  12695.215 ± 185.013  ops/ms
 *
 * === Quickbuf (JDK8 no unsafe)
 * SbeThroughputBenchmarkQuickbuf.testCarDecode     thrpt   10   1902.791 ± 101.543  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testCarEncode     thrpt   10   3608.368 ±  83.677  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketDecode  thrpt   10   8368.567 ± 205.855  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketEncode  thrpt   10  11791.775 ± 420.490  ops/ms
 *
 * == Quickbuf (JDK13 unsafe)
 * SbeThroughputBenchmarkQuickbuf.testCarDecode     thrpt   10   2160.508 ±  95.135  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testCarEncode     thrpt   10   3648.551 ±  57.977  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketDecode  thrpt   10   9361.098 ±  38.584  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketEncode  thrpt   10  12964.788 ± 175.684  ops/ms
 *
 * == Quickbuf (JDK13 no unsafe)
 * SbeThroughputBenchmarkQuickbuf.testCarDecode     thrpt   10   2329.465 ±  90.888  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testCarEncode     thrpt   10   3410.025 ±  59.886  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketDecode  thrpt   10   9805.340 ± 138.477  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketEncode  thrpt   10  12319.570 ± 222.686  ops/ms
 *
 * === JSON (JDK8)
 * Benchmark                                         Mode  Cnt     Score    Error   Units
 * SbeThroughputBenchmarkRobo.testCarEncodeJson     thrpt   10  1423.760 ± 31.877  ops/ms
 * SbeThroughputBenchmarkRobo.testMarketEncodeJson  thrpt   10  3284.856 ± 72.124  ops/ms
 *
 * === JSON (JDK13)
 * SbeThroughputBenchmarkQuickbuf.testCarEncodeJson     thrpt   10   1599.151 ±   18.887  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketEncodeJson  thrpt   10   3690.897 ±   46.368  ops/ms
 *
 * @author Florian Enner
 * @since 16 Oct 2019
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SbeThroughputBenchmarkQuickbuf {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + SbeThroughputBenchmarkQuickbuf.class.getSimpleName() + ".*Json.*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final MarketDataIncrementalRefreshTrades marketData = MarketDataIncrementalRefreshTrades.newInstance();
    final byte[] marketDecodeBuffer = buildMarketData(marketData).toByteArray();
    final Car car = Car.newInstance();
    final byte[] carDecodeBuffer = buildCarData(car).toByteArray();

    final byte[] encodeBuffer = new byte[Math.max(marketDecodeBuffer.length, carDecodeBuffer.length)];
    final ProtoSource source = ProtoSource.newInstance();
    final ProtoSink sink = ProtoSink.newInstance();

    final JsonSink jsonSink = JsonSink.newInstance().reserve(2048);
    final MarketDataIncrementalRefreshTrades marketDataFast = buildMarketData(MarketDataIncrementalRefreshTrades.newInstance());
    final Car carFast = buildCarData(Car.newInstance());

    {
        // pre-compute size so we can copy the cached size in copyFrom
        marketDataFast.getSerializedSize();
        carFast.getSerializedSize();
    }

    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    public int testMarketComputeSizeOnly() throws IOException {
        return marketDataFast.getSerializedSize();
    }

    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    public int testCarComputeSizeOnly() throws IOException {
        return carFast.getSerializedSize();
    }

    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    public int testMarketWriteOnly() throws IOException {
        sink.wrap(encodeBuffer);
        marketDataFast.writeTo(sink);
        return sink.position();
    }

    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    public Object testMarketBuildOnly() throws IOException {
        return buildMarketData(marketDataFast);
    }

    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    public Object testCarBuildOnly() throws IOException {
        return buildCarData(carFast);
    }

    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    public int testCarWriteOnly() throws IOException {
        sink.wrap(encodeBuffer);
        carFast.writeTo(sink);
        return sink.position();
    }

    @Benchmark
    public int testMarketEncodeJson() throws IOException {
        return jsonSink.clear()
                .writeMessage(buildMarketData(marketData))
                .getBuffer()
                .length();
    }

    @Benchmark
    public int testCarEncodeJson() throws IOException {
        return jsonSink.clear()
                .writeMessage(buildCarData(car))
                .getBuffer()
                .length();
    }

    @Benchmark
    public int testMarketEncodeFast() throws IOException { // no size computation
        sink.wrap(encodeBuffer);
        marketData.copyFrom(marketDataFast).writeTo(sink);
        return sink.position();
    }

    @Benchmark
    public int testCarEncodeFast() throws IOException { // no size computation
        sink.wrap(encodeBuffer);
        car.copyFrom(carFast).writeTo(sink);
        return sink.position();
    }

    @Benchmark
    public int testMarketEncode() throws IOException {
        sink.wrap(encodeBuffer);
        buildMarketData(marketData).writeTo(sink);
        return sink.position();
    }

    @Benchmark
    public Object testMarketDecode() throws IOException {
        source.wrap(marketDecodeBuffer);
        return marketData.clearQuick().mergeFrom(source);
    }

    @Benchmark
    public int testCarEncode() throws IOException {
        sink.wrap(encodeBuffer);
        buildCarData(car).writeTo(sink);
        return sink.position();
    }

    @Benchmark
    public Object testCarDecode() throws IOException {
        source.wrap(carDecodeBuffer);
        return car.clearQuick().mergeFrom(source);
    }

    static MarketDataIncrementalRefreshTrades buildMarketData(MarketDataIncrementalRefreshTrades marketData) {
        marketData.clearQuick()
                .setTransactTime(1234L)
                .setEventTimeDelta(987)
                .setMatchEventIndicatorValue(MarketDataIncrementalRefreshTrades.MatchEventIndicator.END_EVENT_VALUE);

        for (int i = 0; i < 2; i++) {

            Fix.MdIncGrp group = marketData.getMutableMdIncGroup().next()
                    .setTradeId(1234L)
                    .setSecurityId(56789L)
                    .setNumberOfOrders(1)
                    .setRepSeq(1)
                    .setMdUpdateActionValue(Fix.MdIncGrp.MdUpdateAction.NEW_VALUE)
                    .setAggressorSideValue(Fix.MdIncGrp.Side.BUY_VALUE)
                    .setMdEntryTypeValue(Fix.MdIncGrp.MdEntryType.BID_VALUE);
            group.getMutableMdEntryPx().setMantissa(50);
            group.getMutableMdEntrySize().setMantissa(10);

        }

        return marketData;
    }

    static Car buildCarData(Car car) {
        car.clearQuick()
                .setSerialNumber(12345)
                .setModelYear(2005)
                .setCodeValue(Car.Model.A_VALUE)
                .setAvailable(true);

        car.getMutableEngine()
                .setCapacity(4200)
                .setNumCylinders(8)
                .setManufacturerCode(ENG_MAN_CODE);

        car
                .setVehicleCode(VEHICLE_CODE)
                .setMake(MAKE)
                .setModel(MODEL);

        for (int i = 0; i < 5; i++) {
            car.addSomeNumbers(i);
        }

        car.getMutableOptionalExtras()
                .addValue(Car.Extras.SPORTS_PACK_VALUE)
                .addValue(Car.Extras.SUN_ROOF_VALUE);

        car.getMutableFuelFigures().next().setSpeed(30).setMpg(35.9F);
        car.getMutableFuelFigures().next().setSpeed(30).setMpg(49.0F);
        car.getMutableFuelFigures().next().setSpeed(30).setMpg(40.0F);

        Examples.PerformanceFigures perfFigures = car.getMutablePerformance().next().setOctaneRating(95);
        perfFigures.getMutableAcceleration().next().setMph(30).setSeconds(4.0f);
        perfFigures.getMutableAcceleration().next().setMph(60).setSeconds(7.5f);
        perfFigures.getMutableAcceleration().next().setMph(100).setSeconds(12.2f);

        perfFigures = car.getMutablePerformance().next().setOctaneRating(99);
        perfFigures.getMutableAcceleration().next().setMph(30).setSeconds(3.8f);
        perfFigures.getMutableAcceleration().next().setMph(60).setSeconds(7.1f);
        perfFigures.getMutableAcceleration().next().setMph(100).setSeconds(11.8f);

        return car;
    }

    static final String VEHICLE_CODE = "abcdef";
    static final String ENG_MAN_CODE = "abc";
    static final String MAKE = "AUDI";
    static final String MODEL = "R8";

}
