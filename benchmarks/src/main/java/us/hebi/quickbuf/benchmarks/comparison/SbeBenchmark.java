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
 * 3900x / Windows 10
 *
 * === Quickbuf RC1 (JDK 17)
 * Benchmark                                 Mode  Cnt   Score   Error  Units
 * SbeBenchmark.carQuickRead                 avgt   20  23,323 ± 0,496  ms/op
 * SbeBenchmark.carQuickReadWrite            avgt   20  42,149 ± 1,107  ms/op
 * SbeBenchmark.marketQuickRead              avgt   20  18,001 ± 0,114  ms/op
 * SbeBenchmark.marketQuickReadWrite         avgt   20  32,464 ± 0,846  ms/op
 *
 * === Protobuf-Java 3.19.4 (JDK 17)
 * Benchmark                                 Mode  Cnt   Score   Error  Units
 * SbeBenchmark.carProtoRead                 avgt   20  54,041 ± 2,763  ms/op
 * SbeBenchmark.carProtoReadWrite            avgt   20  79,338 ± 2,707  ms/op
 * SbeBenchmark.marketProtoRead              avgt   20  42,773 ± 2,016  ms/op
 * SbeBenchmark.marketProtoReadWrite         avgt   20  73,791 ± 2,751  ms/op
 *
 * === Protobuf-Javalite 3.19.4 (JDK17)
 * Benchmark                                 Mode  Cnt    Score   Error  Units
 * SbeBenchmark.carProtoRead                 avgt   20  104,000 ± 3,800  ms/op
 * SbeBenchmark.carProtoReadWrite            avgt   20  172,748 ± 4,172  ms/op
 * SbeBenchmark.marketProtoRead              avgt   20  130,718 ± 2,707  ms/op
 * SbeBenchmark.marketProtoReadWrite         avgt   20  220,967 ± 2,370  ms/op
 *
 * @author Florian Enner
 * @since 15 Oct 2019
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SbeBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + SbeBenchmark.class.getSimpleName() + ".*.*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    private static byte[] multiplyToNumBytes(byte[] singleMessage, int maxNumBytes) {
        try {

            int sizePerMessage = ProtoSink.computeRawVarint32Size(singleMessage.length) + singleMessage.length;
            int numMessages = maxNumBytes / sizePerMessage;

            byte[] data = new byte[sizePerMessage * numMessages];
            ProtoSink sink = ProtoSink.newArraySink().setOutput(data);
            for (int i = 0; i < numMessages; i++) {
                sink.writeUInt32NoTag(singleMessage.length);
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
    final ProtoSource source = ProtoSource.newArraySource();
    final ProtoSink sink = ProtoSink.newArraySink();

    // ===================== DATASETS =====================
    final static int MAX_DATASET_SIZE = 10 * 1024 * 1024;
    final byte[] marketDataMessages = multiplyToNumBytes(SbeThroughputBenchmarkQuickbuf.buildMarketData(marketMsg).toByteArray(), MAX_DATASET_SIZE);
    final byte[] carDataMessages = multiplyToNumBytes(SbeThroughputBenchmarkQuickbuf.buildCarData(carMsg).toByteArray(), MAX_DATASET_SIZE);
    final byte[] output = new byte[MAX_DATASET_SIZE];

    // ===================== QUICKBUFFERS =====================
    @Benchmark
    public int marketQuickRead() throws IOException {
        return readQuick(source.setInput(marketDataMessages), marketMsg);
    }

    @Benchmark
    public int marketQuickReadWrite() throws IOException {
        return readWriteQuick(source.setInput(marketDataMessages), marketMsg, sink.setOutput(output));
    }

    @Benchmark
    public int carQuickRead() throws IOException {
        return readQuick(source.setInput(carDataMessages), carMsg);
    }

    @Benchmark
    public int carQuickReadWrite() throws IOException {
        return readWriteQuick(source.setInput(carDataMessages), carMsg, sink.setOutput(output));
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
        return source.getTotalBytesRead();
    }

    static int readWriteQuick(final ProtoSource source, final ProtoMessage message, final ProtoSink sink) throws IOException {
        while (!source.isAtEnd()) {
            // read/write delimited
            source.readMessage(message.clearQuick());
            sink.writeMessageNoTag(message);
        }
        return sink.getTotalBytesWritten();
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
