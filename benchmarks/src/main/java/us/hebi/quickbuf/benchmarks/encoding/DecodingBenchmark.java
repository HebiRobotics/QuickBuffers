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

package us.hebi.quickbuf.benchmarks.encoding;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import us.hebi.quickbuf.InvalidProtocolBufferException;
import us.hebi.quickbuf.ProtoSink;
import us.hebi.quickbuf.ProtoSource;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * (best case w/ positive integers below 128)
 * Benchmark                           Mode  Cnt    Score   Error   Units
 * DecodingBenchmark.readRawVarint32  thrpt   10  264,419 ± 0,821  ops/us
 * DecodingBenchmark.readRawVarint64  thrpt   10  247,520 ± 2,445  ops/us
 * DecodingBenchmark.skipRawVarint    thrpt   10  262,588 ± 2,578  ops/us
 *
 * (random input w/ 50% negative numbers)
 * Benchmark                           Mode  Cnt    Score    Error   Units
 * DecodingBenchmark.readRawVarint32  thrpt   10  264,442 ±  1,958  ops/us
 * DecodingBenchmark.readRawVarint64  thrpt   10  248,780 ±  0,960  ops/us
 * DecodingBenchmark.skipRawVarint    thrpt   10  256,273 ± 12,616  ops/us
 *
 * (random input w/ 25% negative numbers)
 * Benchmark                           Mode  Cnt    Score   Error   Units
 * DecodingBenchmark.readRawVarint32  thrpt   10  264,618 ± 1,463  ops/us
 * DecodingBenchmark.readRawVarint64  thrpt   10  247,329 ± 3,522  ops/us
 * DecodingBenchmark.skipRawVarint    thrpt   10  260,887 ± 3,749  ops/us
 *
 * @author Florian Enner
 * @since 07 Mär 2022
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class DecodingBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + DecodingBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    int[] values = new int[512];
    byte[] output = new byte[values.length * 10];
    ProtoSink sink = ProtoSink.newInstance();
    ProtoSource source = ProtoSource.newSafeInstance();

    @Setup(Level.Iteration)
    public void setup() throws IOException {
        Random random = new Random(System.nanoTime());
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.abs(random.nextInt()) % 128; // all 1 byte varint (best case)
//            values[i] = random.nextInt(); // 50% negative (worst case)
//            values[i] = random.nextDouble() < 0.50 ? Math.abs(random.nextInt()) : random.nextInt(); // 25% negative
        }

        sink.wrap(output);
        for (int i = 0; i < values.length; i++) {
            sink.writeRawVarint32(values[i]);
        }
        source.wrap(output, 0, sink.position());
    }

    @Benchmark
    public int readRawVarint32() throws IOException {
        int count = 0;
        while (!source.isAtEnd()) {
            source.readRawVarint32();
            count++;
        }
        return count;
    }

    @Benchmark
    public long readRawVarint64() throws IOException {
        int count = 0;
        while (!source.isAtEnd()) {
            source.readRawVarint64();
            count++;
        }
        return count;
    }

    @Benchmark
    public int skipRawVarint() throws IOException {
        int count = 0;
        while (!source.isAtEnd()) {
            skipRawVarint(source);
            count++;
        }
        return count;
    }

    // Same code as Protobuf's skipRawVarint. No benefit to separate method
    private static void skipRawVarint(ProtoSource source) throws IOException {
        for (int i = 0; i < 10; i++) {
            if (source.readRawByte() >= 0) {
                return;
            }
        }
        throw new RuntimeException();
    }

}
