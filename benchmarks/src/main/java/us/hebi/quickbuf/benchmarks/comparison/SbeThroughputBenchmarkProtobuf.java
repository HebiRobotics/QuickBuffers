/*-
 * #%L
 * benchmarks
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

package us.hebi.quickbuf.benchmarks.comparison;

import com.google.protobuf.CodedOutputStream;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import protos.benchmarks.real_logic.protobuf.Examples;
import protos.benchmarks.real_logic.protobuf.Fix;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static us.hebi.quickbuf.benchmarks.comparison.SbeThroughputBenchmarkQuickbuf.*;

/**
 * Throughput test. Comparable to
 * https://mechanical-sympathy.blogspot.com/2014/05/simple-binary-encoding.html
 *
 * === Protobuf-Java === (OpenJDK13)
 * Benchmark                                      Mode  Cnt     Score     Error   Units
 * SbeThroughputBenchmarkProtobuf.testCarDecode     thrpt   10  1270.720 ± 170.138  ops/ms
 * SbeThroughputBenchmarkProtobuf.testCarEncode     thrpt   10   984.541 ±  12.779  ops/ms
 * SbeThroughputBenchmarkProtobuf.testMarketDecode  thrpt   10  3305.841 ± 119.879  ops/ms
 * SbeThroughputBenchmarkProtobuf.testMarketEncode  thrpt   10  3700.373 ±  99.843  ops/ms
 *
 * @author Florian Enner
 * @since 16 Oct 2019
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SbeThroughputBenchmarkProtobuf {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + SbeThroughputBenchmarkProtobuf.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final Fix.MarketDataIncrementalRefreshTrades.Builder marketData = Fix.MarketDataIncrementalRefreshTrades.newBuilder();
    final byte[] marketDecodeBuffer = buildMarketData(marketData).toByteArray();
    final Examples.Car.Builder car = Examples.Car.newBuilder();
    final byte[] carDecodeBuffer = buildCarData(car).toByteArray();
    final byte[] encodeBuffer = new byte[Math.max(marketDecodeBuffer.length, carDecodeBuffer.length)];

    @Benchmark
    public int testMarketEncode() throws IOException {
        final CodedOutputStream output = CodedOutputStream.newInstance(encodeBuffer);
        buildMarketData(marketData).writeTo(output);
        return output.getTotalBytesWritten();
    }

    @Benchmark
    public Object testMarketDecode() throws IOException {
        return Fix.MarketDataIncrementalRefreshTrades.parseFrom(marketDecodeBuffer);
    }

    @Benchmark
    public int testCarEncode() throws IOException {
        final CodedOutputStream output = CodedOutputStream.newInstance(encodeBuffer);
        buildCarData(car).writeTo(output);
        return output.getTotalBytesWritten();
    }

    @Benchmark
    public Object testCarDecode() throws IOException {
        return Examples.Car.parseFrom(carDecodeBuffer);
    }

    /**
     * Generates data identical to MarketDataBenchmark
     * https://github.com/real-logic/simple-binary-encoding/blob/26205593ca9ff6fefba5cfaf1db095397395c3a3/perf/java/uk/co/real_logic/protobuf/MarketDataBenchmark.java
     *
     * @return byte[] containing one message
     */
    private static Fix.MarketDataIncrementalRefreshTrades buildMarketData(final Fix.MarketDataIncrementalRefreshTrades.Builder marketData) {
        marketData.clear()
                .setTransactTime(1234L)
                .setEventTimeDelta(987)
                .setMatchEventIndicator(Fix.MarketDataIncrementalRefreshTrades.MatchEventIndicator.END_EVENT);

        for (int i = 0; i < 2; i++) {

            marketData.addMdIncGroup(Fix.MdIncGrp.newBuilder()
                    .setTradeId(1234L)
                    .setSecurityId(56789L)
                    .setMdEntryPx(Fix.Decimal64.newBuilder().setMantissa(50).build())
                    .setMdEntrySize(Fix.IntQty32.newBuilder().setMantissa(10).build())
                    .setNumberOfOrders(1)
                    .setMdUpdateAction(Fix.MdIncGrp.MdUpdateAction.NEW)
                    .setRepSeq(1)
                    .setAggressorSide(Fix.MdIncGrp.Side.BUY)
                    .setMdEntryType(Fix.MdIncGrp.MdEntryType.BID)
                    .build());

        }

        return marketData.build();
    }

    /**
     * Generates data identical to CarBenchmark
     * https://github.com/real-logic/simple-binary-encoding/blob/26205593ca9ff6fefba5cfaf1db095397395c3a3/perf/java/uk/co/real_logic/protobuf/CarBenchmark.java
     *
     * @return byte[] containing one message
     */
    static Examples.Car buildCarData(Examples.Car.Builder car) {
        car.clear()
                .setSerialNumber(12345)
                .setModelYear(2005)
                .setAvailable(true)
                .setCode(Examples.Car.Model.A)
                .setVehicleCode(VEHICLE_CODE)
                .setMake(MAKE)
                .setModel(MODEL)
                .addOptionalExtras(Examples.Car.Extras.SPORTS_PACK)
                .addOptionalExtras(Examples.Car.Extras.SUN_ROOF);

        for (int i = 0; i < 5; i++) {
            car.addSomeNumbers(i);
        }

        car.setEngine(Examples.Engine.newBuilder()
                .setCapacity(4200)
                .setNumCylinders(8)
                .setManufacturerCode(ENG_MAN_CODE)
                .build());

        car.addFuelFigures(Examples.FuelFigures.newBuilder().setSpeed(30).setMpg(35.9F).build());
        car.addFuelFigures(Examples.FuelFigures.newBuilder().setSpeed(30).setMpg(49.0F).build());
        car.addFuelFigures(Examples.FuelFigures.newBuilder().setSpeed(30).setMpg(40.0F).build());

        car.addPerformance(Examples.PerformanceFigures.newBuilder()
                .setOctaneRating(95)
                .addAcceleration(Examples.Acceleration.newBuilder().setMph(30).setSeconds(4.0f).build())
                .addAcceleration(Examples.Acceleration.newBuilder().setMph(60).setSeconds(7.5f).build())
                .addAcceleration(Examples.Acceleration.newBuilder().setMph(100).setSeconds(12.2f).build())
                .build());

        car.addPerformance(Examples.PerformanceFigures.newBuilder()
                .setOctaneRating(95)
                .addAcceleration(Examples.Acceleration.newBuilder().setMph(30).setSeconds(3.8f).build())
                .addAcceleration(Examples.Acceleration.newBuilder().setMph(60).setSeconds(7.1f).build())
                .addAcceleration(Examples.Acceleration.newBuilder().setMph(100).setSeconds(11.8f).build())
                .build());

        return car.build();
    }

}
