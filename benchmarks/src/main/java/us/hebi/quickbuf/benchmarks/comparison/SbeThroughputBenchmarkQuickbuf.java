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
import us.hebi.quickbuf.JsonSink;
import us.hebi.quickbuf.ProtoSink;
import us.hebi.quickbuf.ProtoSource;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * === Quickbuf (JDK8 unsafe)
 * Benchmark                                     Mode  Cnt     Score     Error   Units
 * SbeThroughputBenchmarkQuickbuf.testCarDecode         thrpt   10  2059.416 ±  76.329  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testCarEncode         thrpt   10  2858.469 ±  40.052  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketDecode      thrpt   10  6196.707 ± 132.391  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketEncode      thrpt   10  8440.097 ± 132.906  ops/ms
 *
 * === Quickbuf (JDK8 no unsafe)
 * SbeThroughputBenchmarkQuickbuf.testCarDecode         thrpt   10  1857.434 ±  69.348  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testCarEncode         thrpt   10  2688.618 ± 119.564  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketDecode      thrpt   10  6611.062 ± 185.162  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketEncode      thrpt   10  8132.255 ±  79.923  ops/ms
 *
 * == Quickbuf (JDK13 unsafe)
 * SbeThroughputBenchmarkQuickbuf.testCarDecode         thrpt   10  2065.952 ±  72.137  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testCarEncode         thrpt   10  2669.079 ± 105.421  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketDecode      thrpt   10  7235.390 ± 234.589  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketEncode      thrpt   10  8036.102 ± 216.636  ops/ms
 *
 * == Quickbuf (JDK13 no unsafe)
 * SbeThroughputBenchmarkQuickbuf.testCarDecode         thrpt   10  2202.553 ± 116.821  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testCarEncode         thrpt   10  2538.979 ±  56.068  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketDecode      thrpt   10  7522.434 ± 119.982  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketEncode      thrpt   10  8088.595 ±  94.757  ops/ms
 *
 * === JSON (JDK8)
 * Benchmark                                         Mode  Cnt     Score    Error   Units
 * SbeThroughputBenchmarkRobo.testCarEncodeJson     thrpt   10  1423.760 ± 31.877  ops/ms
 * SbeThroughputBenchmarkRobo.testMarketEncodeJson  thrpt   10  3284.856 ± 72.124  ops/ms
 *
 * === JSON (JDK13)
 * SbeThroughputBenchmarkQuickbuf.testCarEncodeJson     thrpt   10  1502.202 ±  13.620  ops/ms
 * SbeThroughputBenchmarkQuickbuf.testMarketEncodeJson  thrpt   10  3208.274 ± 108.914  ops/ms
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
                .include(".*" + SbeThroughputBenchmarkQuickbuf.class.getSimpleName() + ".*MarketEncode")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final Fix.MarketDataIncrementalRefreshTrades marketData = Fix.MarketDataIncrementalRefreshTrades.newInstance();
    final byte[] marketDecodeBuffer = buildMarketData(marketData).toByteArray();
    final Car car = Car.newInstance();
    final byte[] carDecodeBuffer = buildCarData(car).toByteArray();

    final byte[] encodeBuffer = new byte[Math.max(marketDecodeBuffer.length, carDecodeBuffer.length)];
    final ProtoSource source = ProtoSource.newSafeInstance();
    final ProtoSink sink = ProtoSink.newInstance();

    final JsonSink jsonSink = JsonSink.newInstance().reserve(2048);

/*
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
*/

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

    static Fix.MarketDataIncrementalRefreshTrades buildMarketData(Fix.MarketDataIncrementalRefreshTrades marketData) {
        marketData.clearQuick()
                .setTransactTime(1234L)
                .setEventTimeDelta(987)
                .setMatchEventIndicator(Fix.MarketDataIncrementalRefreshTrades.MatchEventIndicator.END_EVENT_VALUE);

        for (int i = 0; i < 2; i++) {

            Fix.MdIncGrp group = marketData.getMutableMdIncGroup().next()
                    .setTradeId(1234L)
                    .setSecurityId(56789L)
                    .setNumberOfOrders(1)
                    .setRepSeq(1)
                    .setMdUpdateAction(Fix.MdIncGrp.MdUpdateAction.NEW_VALUE)
                    .setAggressorSide(Fix.MdIncGrp.Side.BUY_VALUE)
                    .setMdEntryType(Fix.MdIncGrp.MdEntryType.BID_VALUE);
            group.getMutableMdEntryPx().setMantissa(50);
            group.getMutableMdEntrySize().setMantissa(10);

        }

        return marketData;
    }

    static Car buildCarData(Car car) {
        car.clearQuick()
                .setSerialNumber(12345)
                .setModelYear(2005)
                .setCode(Car.Model.A_VALUE)
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
                .add(Car.Extras.SPORTS_PACK_VALUE)
                .add(Car.Extras.SUN_ROOF_VALUE);

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
