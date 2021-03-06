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

import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Parser;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import protos.benchmarks.real_logic.protobuf.Examples;
import protos.benchmarks.real_logic.protobuf.Fix;
import protos.benchmarks.real_logic.quickbuf.Examples.Car;
import protos.benchmarks.real_logic.quickbuf.Fix.MarketDataIncrementalRefreshTrades;
import us.hebi.quickbuf.ProtoMessage;
import us.hebi.quickbuf.ProtoSink;
import us.hebi.quickbuf.ProtoSource;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Generates messages with the same contents as the old benchmarks in Simple Binary Encoding (SBE)
 *
 * NUC8i7BEH / Windows / JDK8
 *
 *
 * === QuickBuffers (Unsafe) ===
 * Benchmark                                Mode  Cnt   Score    Error  Units
 * SbeBenchmark.carUnsafeQuickRead           avgt   10  30.661 ± 2.892  ms/op
 * SbeBenchmark.carUnsafeQuickReadReadWrite  avgt   10  47.498 ± 2.198  ms/op
 * SbeBenchmark.marketUnsafeQuickRead        avgt   10  21.946 ± 0.997  ms/op
 * SbeBenchmark.marketUnsafeQuickReadWrite   avgt   10  35.220 ± 1.000  ms/op
 *
 * === QuickBuffers (Safe) ===
 * SbeBenchmark.carQuickRead          avgt   10  35.540 ± 2.008  ms/op
 * SbeBenchmark.carQuickReadWrite     avgt   10  55.914 ± 3.173  ms/op
 * SbeBenchmark.marketQuickRead       avgt   10  21.027 ± 0.871  ms/op
 * SbeBenchmark.marketQuickReadWrite  avgt   10  38.426 ± 0.743  ms/op
 *
 * === Protobuf-Java
 * Benchmark                          Mode  Cnt   Score    Error  Units
 * SbeBenchmark.carProtoRead          avgt   10  64.838 ±  6.028  ms/op -- 153 MB/s
 * SbeBenchmark.carProtoReadWrite     avgt   10  93.787 ±  2.824  ms/op -- 106 MB/s
 * SbeBenchmark.marketProtoRead       avgt   10  46.673 ±  6.769  ms/op -- 214 MB/s
 * SbeBenchmark.marketProtoReadWrite  avgt   10  88.855 ± 14.795  ms/op -- 112 MB/s
 *
 * === Protobuf-JavaLite
 * SbeBenchmark.carProtoRead                   avgt   10  147.068 ±  3.332  ms/op -- 68 MB/s
 * SbeBenchmark.carProtoReadWrite              avgt   10  247.656 ± 28.943  ms/op -- 40 MB/s
 * SbeBenchmark.marketProtoRead                avgt   10  154.648 ± 14.391  ms/op -- 64 MB/s
 * SbeBenchmark.marketProtoReadWrite           avgt   10  251.861 ±  7.509  ms/op -- 40 MB/s
 *
 * @author Florian Enner
 * @since 15 Oct 2019
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SbeBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + SbeBenchmark.class.getSimpleName() + ".*Unsafe.*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    private static byte[] multiplyToNumBytes(byte[] singleMessage, int maxNumBytes) {
        try {

            int sizePerMessage = ProtoSink.computeRawVarint32Size(singleMessage.length) + singleMessage.length;
            int numMessages = maxNumBytes / sizePerMessage;

            byte[] data = new byte[sizePerMessage * numMessages];
            ProtoSink sink = ProtoSink.newInstance().wrap(data);
            for (int i = 0; i < numMessages; i++) {
                sink.writeRawVarint32(singleMessage.length);
                sink.writeRawBytes(singleMessage);
            }

            return data;
        } catch (IOException e) {
            throw new AssertionError();
        }
    }

    // ===================== REUSABLE STATE =====================
    final MarketDataIncrementalRefreshTrades marketMsg = MarketDataIncrementalRefreshTrades.newInstance();
    final Car carMsg = Car.newInstance();
    final ProtoSource source = ProtoSource.newSafeInstance();
    final ProtoSink sink = ProtoSink.newSafeInstance();
    final ProtoSource unsafeSource = ProtoSource.newInstance();
    final ProtoSink unsafeSink = ProtoSink.newInstance();

    // ===================== DATASETS =====================
    final static int MAX_DATASET_SIZE = 10 * 1024 * 1024;
    final byte[] marketDataMessages = multiplyToNumBytes(SbeThroughputBenchmarkQuickbuf.buildMarketData(marketMsg).toByteArray(), MAX_DATASET_SIZE);
    final byte[] carDataMessages = multiplyToNumBytes(SbeThroughputBenchmarkQuickbuf.buildCarData(carMsg).toByteArray(), MAX_DATASET_SIZE);
    final byte[] output = new byte[MAX_DATASET_SIZE];

    // ===================== UNSAFE OPTION DISABLED (e.g. Android) =====================
    @Benchmark
    public int marketQuickRead() throws IOException {
        return readQuick(source.wrap(marketDataMessages), marketMsg);
    }

    @Benchmark
    public int marketQuickReadWrite() throws IOException {
        return readWriteQuick(source.wrap(marketDataMessages), marketMsg, sink.wrap(output));
    }

    @Benchmark
    public int carQuickRead() throws IOException {
        return readQuick(source.wrap(carDataMessages), carMsg);
    }

    @Benchmark
    public int carQuickReadWrite() throws IOException {
        return readWriteQuick(source.wrap(carDataMessages), carMsg, sink.wrap(output));
    }

    // ===================== UNSAFE OPTION ENABLED =====================
    @Benchmark
    public int marketUnsafeQuickRead() throws IOException {
        return readQuick(unsafeSource.wrap(marketDataMessages), marketMsg);
    }

    @Benchmark
    public int marketUnsafeQuickReadWrite() throws IOException {
        return readWriteQuick(unsafeSource.wrap(marketDataMessages), marketMsg, unsafeSink.wrap(output));
    }

    @Benchmark
    public int carUnsafeQuickRead() throws IOException {
        return readQuick(unsafeSource.wrap(carDataMessages), carMsg);
    }

    @Benchmark
    public int carUnsafeQuickReadReadWrite() throws IOException {
        return readWriteQuick(unsafeSource.wrap(carDataMessages), carMsg, unsafeSink.wrap(output));
    }

    // ===================== STOCK PROTOBUF =====================
    @Benchmark
    public int marketProtoRead() throws IOException {
        return readProto(marketDataMessages, Fix.MarketDataIncrementalRefreshTrades.parser());
    }

    @Benchmark
    public int marketProtoReadWrite() throws IOException {
        return readWriteProto(marketDataMessages, output, Fix.MarketDataIncrementalRefreshTrades.parser());
    }

    @Benchmark
    public int carProtoRead() throws IOException {
        return readProto(carDataMessages, Examples.Car.parser());
    }

    @Benchmark
    public int carProtoReadWrite() throws IOException {
        return readWriteProto(carDataMessages, output, Examples.Car.parser());
    }

    // ===================== UTIL METHODS =====================
    static int readQuick(final ProtoSource source, final ProtoMessage message) throws IOException {
        while (!source.isAtEnd()) {
            final int length = source.readRawVarint32();
            int limit = source.pushLimit(length);
            message.clearQuick().mergeFrom(source);
            source.popLimit(limit);
        }
        return source.getPosition();
    }

    static int readWriteQuick(final ProtoSource source, final ProtoMessage message, final ProtoSink sink) throws IOException {
        while (!source.isAtEnd()) {
            // read/write delimited
            source.readMessage(message.clearQuick());
            sink.writeMessageNoTag(message);
        }
        return sink.position();
    }

    static <MessageType extends AbstractMessageLite> int readProto(byte[] input, Parser<MessageType> parser) throws IOException {
        CodedInputStream source = CodedInputStream.newInstance(input);
        while (!source.isAtEnd()) {
            final int length = source.readRawVarint32();
            int limit = source.pushLimit(length);
            parser.parseFrom(source);
            source.popLimit(limit);
        }
        return source.getTotalBytesRead();
    }

    static <MessageType extends AbstractMessageLite> int readWriteProto(byte[] input, byte[] output, Parser<MessageType> parser) throws IOException {
        CodedInputStream source = CodedInputStream.newInstance(input);
        CodedOutputStream sink = CodedOutputStream.newInstance(output);
        while (!source.isAtEnd()) {
            // read delimited
            final int length = source.readRawVarint32();
            int limit = source.pushLimit(length);
            MessageType msg = parser.parseFrom(source);
            source.popLimit(limit);

            // write delimited
            sink.writeUInt32NoTag(msg.getSerializedSize());
            msg.writeTo(sink);
        }
        return sink.getTotalBytesWritten();
    }

}
