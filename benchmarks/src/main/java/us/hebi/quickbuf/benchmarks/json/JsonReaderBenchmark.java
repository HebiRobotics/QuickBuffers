/*-
 * #%L
 * quickbuf-benchmarks
 * %%
 * Copyright (C) 2019 - 2026 HEBI Robotics
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
import us.hebi.quickbuf.JsonReader;
import us.hebi.quickbuf.JsonSink;
import us.hebi.quickbuf.JsonSource;
import us.hebi.quickbuf.JsonWriter;
import us.hebi.quickbuf.Utf8String;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static us.hebi.quickbuf.benchmarks.comparison.SbeThroughputBenchmarkQuickbuf.*;

/**
 * Reads arrays of about 1000 values and 100 strings of 500 characters with the JsonReader,
 * and a message whose unknown fields dominate so that skipping gets measured.
 *
 * @author Florian Enner
 * @since 30 Sep 2026
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class JsonReaderBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + JsonReaderBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    public JsonReaderBenchmark() {
        Random rnd = new Random(0);
        JsonWriter doubles = JsonWriter.newInstance().beginArray();
        JsonWriter floats = JsonWriter.newInstance().beginArray();
        JsonWriter longs = JsonWriter.newInstance().beginArray();
        JsonWriter strings = JsonWriter.newInstance().beginArray();
        for (int i = 0; i < NUM_VALUES; i++) {
            // mostly 16-17 digit shortest round trip values, plus short human written ones such as 12.345
            final double value = i % 4 == 0
                    ? (rnd.nextInt(2000000) - 1000000) / 1000.0
                    : rnd.nextGaussian() * Math.pow(10, rnd.nextInt(20) - 10);
            doubles.writeDouble(value);
            floats.writeFloat((float) value);
            longs.writeLong(i % 2 == 0 ? rnd.nextInt() : rnd.nextLong() >> rnd.nextInt(64));
            strings.writeString(randomAscii(rnd, 5 + rnd.nextInt(40)));
        }
        doubleBytes = doubles.endArray().getBytes().toArray();
        floatBytes = floats.endArray().getBytes().toArray();
        longBytes = longs.endArray().getBytes().toArray();
        stringBytes = strings.endArray().getBytes().toArray();

        Random longRnd = new Random(1);
        JsonWriter longStrings = JsonWriter.newInstance().beginArray();
        for (int i = 0; i < NUM_VALUES / 10; i++) {
            longStrings.writeString(randomAscii(longRnd, 500));
        }
        longStringBytes = longStrings.endArray().getBytes().toArray();

        // known car fields after unknown ones with nested messages, long strings, and numbers
        String car = new String(JsonSink.newInstance()
                .writeMessageSilent(buildCarData(Car.newInstance()))
                .getBytes()
                .toArray(), StandardCharsets.UTF_8);
        String market = new String(JsonSink.newInstance()
                .writeMessageSilent(buildMarketData(MarketDataIncrementalRefreshTrades.newInstance()))
                .getBytes()
                .toArray(), StandardCharsets.UTF_8);
        String unknownFields = "\"unknownMarket\":" + market + "," +
                "\"unknownCar\":" + car + "," +
                "\"unknownAscii\":\"" + randomAscii(rnd, 2000) + "\"," +
                "\"unknownUtf8\":\"" + repeat("\u00e9t\u00e9 \u4e2d\u6587 \ud83d\ude00 ", 100) + "\"," +
                "\"unknownDoubles\":" + new String(doubleBytes, 0, 2000, StandardCharsets.UTF_8).replaceAll(",[^,]*$", "]") + "," +
                "\"unknownLongs\":" + new String(longBytes, 0, 1000, StandardCharsets.UTF_8).replaceAll(",[^,]*$", "]") + ",";
        skipBytes = ("{" + unknownFields + car.substring(1)).getBytes(StandardCharsets.UTF_8);
    }

    private static String randomAscii(Random rnd, int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ASCII.charAt(rnd.nextInt(ASCII.length())));
        }
        return builder.toString();
    }

    private static String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private static final int NUM_VALUES = 1000;
    private static final String ASCII = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .,;:-_";

    private final byte[] doubleBytes;
    private final byte[] floatBytes;
    private final byte[] longBytes;
    private final byte[] stringBytes;
    private final byte[] longStringBytes;
    private final byte[] skipBytes;

    private final Utf8String store = Utf8String.newEmptyInstance();
    private final Car carData = Car.newInstance();

    @Benchmark
    public double readDoubles() throws IOException {
        JsonReader reader = JsonReader.newInstance(doubleBytes).beginArray();
        double sum = 0;
        while (reader.hasNext()) {
            sum += reader.readDouble();
        }
        reader.endArray();
        return sum;
    }

    @Benchmark
    public float readFloats() throws IOException {
        JsonReader reader = JsonReader.newInstance(floatBytes).beginArray();
        float sum = 0;
        while (reader.hasNext()) {
            sum += reader.readFloat();
        }
        reader.endArray();
        return sum;
    }

    @Benchmark
    public long readLongs() throws IOException {
        JsonReader reader = JsonReader.newInstance(longBytes).beginArray();
        long sum = 0;
        while (reader.hasNext()) {
            sum += reader.readLong();
        }
        reader.endArray();
        return sum;
    }

    @Benchmark
    public int readStrings() throws IOException {
        JsonReader reader = JsonReader.newInstance(stringBytes).beginArray();
        int length = 0;
        while (reader.hasNext()) {
            reader.readString(store);
            length += store.size();
        }
        reader.endArray();
        return length;
    }

    @Benchmark
    public int readLongStrings() throws IOException {
        JsonReader reader = JsonReader.newInstance(longStringBytes).beginArray();
        int length = 0;
        while (reader.hasNext()) {
            reader.readString(store);
            length += store.size();
        }
        reader.endArray();
        return length;
    }

    @Benchmark
    public Object skipUnknownFields() throws IOException {
        return carData.clearQuick().mergeFrom(JsonSource.newInstance(skipBytes).setIgnoreUnknownFields(true));
    }

}
