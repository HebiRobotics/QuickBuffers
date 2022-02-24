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

package us.hebi.quickbuf.benchmarks.encoding;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import us.hebi.quickbuf.ProtoSink;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * ===== JDK8 (Unsafe)
 * Benchmark                        Mode  Cnt     Score     Error  Units
 * TagEncodeBenchmark.writeArray_1  avgt   10  4743.763 ± 166.464  us/op
 * TagEncodeBenchmark.writeArray_2  avgt   10  4584.196 ± 144.663  us/op
 * TagEncodeBenchmark.writeArray_3  avgt   10  4626.061 ±  45.672  us/op
 * TagEncodeBenchmark.writeArray_4  avgt   10  4359.394 ± 377.503  us/op
 * TagEncodeBenchmark.writeArray_5  avgt   10  4727.530 ±  69.860  us/op
 *
 * TagEncodeBenchmark.writeBytes_1  avgt   10   547.952 ±  12.381  us/op
 * TagEncodeBenchmark.writeBytes_2  avgt   10  1115.578 ±  20.456  us/op
 * TagEncodeBenchmark.writeBytes_3  avgt   10  1678.910 ±  47.143  us/op
 * TagEncodeBenchmark.writeBytes_4  avgt   10  2179.561 ±  43.130  us/op
 * TagEncodeBenchmark.writeBytes_5  avgt   10  2730.458 ±  29.973  us/op
 *
 * TagEncodeBenchmark.writeFixed_1  avgt   10   541.634 ±  11.514  us/op
 * TagEncodeBenchmark.writeFixed_2  avgt   10   651.680 ±   9.010  us/op
 * TagEncodeBenchmark.writeFixed_3  avgt   10  1244.023 ±  37.511  us/op
 * TagEncodeBenchmark.writeFixed_4  avgt   10   668.437 ±  14.253  us/op
 * TagEncodeBenchmark.writeFixed_5  avgt   10  1250.233 ±  37.176  us/op
 *
 * ==== JDK8 (Safe) - array copied using array copy
 * Benchmark                        Mode  Cnt     Score     Error  Units
 * TagEncodeBenchmark.writeArray_1  avgt   10  4953.756 ± 319.273  us/op
 * TagEncodeBenchmark.writeArray_2  avgt   10  4870.247 ± 137.184  us/op
 * TagEncodeBenchmark.writeArray_3  avgt   10  5113.336 ± 135.793  us/op
 * TagEncodeBenchmark.writeArray_4  avgt   10  4845.112 ± 118.449  us/op
 * TagEncodeBenchmark.writeArray_5  avgt   10  4899.526 ±  94.384  us/op
 *
 * TagEncodeBenchmark.writeBytes_1  avgt   10   510.045 ±  19.308  us/op
 * TagEncodeBenchmark.writeBytes_2  avgt   10  1269.418 ±  34.826  us/op
 * TagEncodeBenchmark.writeBytes_3  avgt   10  1945.482 ±  60.687  us/op
 * TagEncodeBenchmark.writeBytes_4  avgt   10  2115.830 ±  92.935  us/op
 * TagEncodeBenchmark.writeBytes_5  avgt   10  2697.689 ± 105.998  us/op
 *
 * TagEncodeBenchmark.writeFixed_1  avgt   10   512.238 ±  24.827  us/op
 * TagEncodeBenchmark.writeFixed_2  avgt   10   994.209 ±  36.300  us/op
 * TagEncodeBenchmark.writeFixed_3  avgt   10  1824.046 ±  54.119  us/op
 * TagEncodeBenchmark.writeFixed_4  avgt   10  1336.181 ±  40.128  us/op
 * TagEncodeBenchmark.writeFixed_5  avgt   10  2046.004 ±  62.694  us/op
 *
 * ==== JDK8 (Safe) - array copied as for loop
 * Benchmark                        Mode  Cnt     Score     Error  Units
 * TagEncodeBenchmark.writeArray_1  avgt   10   897.741 ±  27.290  us/op
 * TagEncodeBenchmark.writeArray_2  avgt   10   705.380 ±  75.761  us/op
 * TagEncodeBenchmark.writeArray_3  avgt   10  1795.464 ± 143.586  us/op
 * TagEncodeBenchmark.writeArray_4  avgt   10  1845.852 ±  72.404  us/op
 * TagEncodeBenchmark.writeArray_5  avgt   10  2214.235 ±  95.335  us/op
 *
 * @author Florian Enner
 * @since 28 Nov 2019
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS, batchSize = 1000)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS, batchSize = 1000)
@State(Scope.Thread)
public class TagEncodeBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + TagEncodeBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    static final int n = 1000;
    final byte[] buffer = new byte[n * 5];
    final ProtoSink sink = ProtoSink.newSafeInstance();

    private static final byte[] TAG_1 = new byte[1];
    private static final byte[] TAG_2 = new byte[2];
    private static final byte[] TAG_3 = new byte[3];
    private static final byte[] TAG_4 = new byte[4];
    private static final byte[] TAG_5 = new byte[5];

    @Benchmark
    public int writeBytes_1() throws IOException {
        sink.wrap(buffer);
        for (int i = 0; i < n; i++) {
            sink.writeRawByte((byte) 0);
        }
        return sink.position();
    }

    @Benchmark
    public int writeBytes_2() throws IOException {
        sink.wrap(buffer);
        for (int i = 0; i < n; i++) {
            sink.writeRawByte((byte) 0);
            sink.writeRawByte((byte) 0);
        }
        return sink.position();
    }

    @Benchmark
    public int writeBytes_3() throws IOException {
        sink.wrap(buffer);
        for (int i = 0; i < n; i++) {
            sink.writeRawByte((byte) 0);
            sink.writeRawByte((byte) 0);
            sink.writeRawByte((byte) 0);
        }
        return sink.position();
    }

    @Benchmark
    public int writeBytes_4() throws IOException {
        sink.wrap(buffer);
        for (int i = 0; i < n; i++) {
            sink.writeRawByte((byte) 0);
            sink.writeRawByte((byte) 0);
            sink.writeRawByte((byte) 0);
            sink.writeRawByte((byte) 0);
        }
        return sink.position();
    }

    @Benchmark
    public int writeBytes_5() throws IOException {
        sink.wrap(buffer);
        for (int i = 0; i < n; i++) {
            sink.writeRawByte((byte) 0);
            sink.writeRawByte((byte) 0);
            sink.writeRawByte((byte) 0);
            sink.writeRawByte((byte) 0);
            sink.writeRawByte((byte) 0);
        }
        return sink.position();
    }

    @Benchmark
    public int writeFixed_1() throws IOException {
        sink.wrap(buffer);
        for (int i = 0; i < n; i++) {
            sink.writeRawByte((byte) 0);
        }
        return sink.position();
    }

    @Benchmark
    public int writeFixed_2() throws IOException {
        sink.wrap(buffer);
        for (int i = 0; i < n; i++) {
            sink.writeRawLittleEndian16((short) 0);
        }
        return sink.position();
    }

    @Benchmark
    public int writeFixed_3() throws IOException {
        sink.wrap(buffer);
        for (int i = 0; i < n; i++) {
            sink.writeRawLittleEndian16((short) 0);
            sink.writeRawByte((byte) 0);
        }
        return sink.position();
    }

    @Benchmark
    public int writeFixed_4() throws IOException {
        sink.wrap(buffer);
        for (int i = 0; i < n; i++) {
            sink.writeRawLittleEndian32((short) 0);
        }
        return sink.position();
    }

    @Benchmark
    public int writeFixed_5() throws IOException {
        sink.wrap(buffer);
        for (int i = 0; i < n; i++) {
            sink.writeRawLittleEndian32((short) 0);
            sink.writeRawByte((byte) 0);
        }
        return sink.position();
    }

    @Benchmark
    public int writeArray_1() throws IOException {
        sink.wrap(buffer);
        for (int i = 0; i < n; i++) {
            sink.writeRawBytes(TAG_1);
        }
        return sink.position();
    }

    @Benchmark
    public int writeArray_2() throws IOException {
        sink.wrap(buffer);
        for (int i = 0; i < n; i++) {
            sink.writeRawBytes(TAG_2);
        }
        return sink.position();
    }

    @Benchmark
    public int writeArray_3() throws IOException {
        sink.wrap(buffer);
        for (int i = 0; i < n; i++) {
            sink.writeRawBytes(TAG_3);
        }
        return sink.position();
    }

    @Benchmark
    public int writeArray_4() throws IOException {
        sink.wrap(buffer);
        for (int i = 0; i < n; i++) {
            sink.writeRawBytes(TAG_4);
        }
        return sink.position();
    }

    @Benchmark
    public int writeArray_5() throws IOException {
        sink.wrap(buffer);
        for (int i = 0; i < n; i++) {
            sink.writeRawBytes(TAG_5);
        }
        return sink.position();
    }

}
