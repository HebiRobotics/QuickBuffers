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

package us.hebi.quickbuf.benchmarks.encoding;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import us.hebi.quickbuf.ProtoSink;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * This benchmark measures the time it takes to write a varint64 to a sink
 * <p>
 * (best case w/ positive integers below 128)
 * Benchmark                           Mode  Cnt     Score    Error   Units
 * Varint64Benchmark.writeInt32       thrpt   10  1932,782 ± 45,614  ops/ms
 * Varint64Benchmark.writeInt64       thrpt   10  1950,888 ± 21,413  ops/ms
 * Varint64Benchmark.writeUInt32      thrpt   10  1917,170 ± 46,138  ops/ms
 * Varint64Benchmark.writeUInt64      thrpt   10  1892,830 ± 18,281  ops/ms
 * Varint64Benchmark.writeUInt64As32  thrpt   10  1870,513 ± 83,114  ops/ms
 * <p>
 * (random 1-3 byte varints)
 * Benchmark                           Mode  Cnt    Score    Error   Units
 * Varint64Benchmark.writeInt32       thrpt   10  478,970 ± 11,709  ops/ms
 * Varint64Benchmark.writeInt64       thrpt   10  435,819 ± 19,813  ops/ms
 * Varint64Benchmark.writeUInt32      thrpt   10  466,737 ± 19,769  ops/ms
 * Varint64Benchmark.writeUInt64      thrpt   10  467,222 ±  9,132  ops/ms
 * Varint64Benchmark.writeUInt64As32  thrpt   10  455,883 ± 11,442  ops/ms
 * <p>
 * (random input w/ 50% negative numbers)
 * Benchmark                           Mode  Cnt    Score    Error   Units
 * Varint64Benchmark.writeInt32       thrpt   10  398,720 ± 18,320  ops/ms
 * Varint64Benchmark.writeInt64       thrpt   10  393,352 ± 22,100  ops/ms
 * Varint64Benchmark.writeUInt32      thrpt   10  309,191 ±  5,012  ops/ms
 * Varint64Benchmark.writeUInt64      thrpt   10  225,217 ±  7,217  ops/ms
 * Varint64Benchmark.writeUInt64As32  thrpt   10  279,257 ±  8,177  ops/ms
 * <p>
 * (random input w/ 25% negative numbers)
 * Benchmark                           Mode  Cnt    Score    Error   Units
 * Varint64Benchmark.writeInt32       thrpt   10  365,734 ± 17,975  ops/ms
 * Varint64Benchmark.writeInt64       thrpt   10  321,945 ± 10,764  ops/ms
 * Varint64Benchmark.writeUInt32      thrpt   10  299,056 ±  5,079  ops/ms
 * Varint64Benchmark.writeUInt64      thrpt   10  260,672 ±  4,122  ops/ms
 * Varint64Benchmark.writeUInt64As32  thrpt   10  326,815 ±  8,257  ops/ms
 * <p>
 * (random bit distribution with aligned writes)
 * Benchmark                           Mode  Cnt    Score    Error   Units
 * Varint64Benchmark.writeInt32       thrpt   20  484,247 ±  7,261  ops/ms
 * Varint64Benchmark.writeInt64       thrpt   20  464,714 ±  4,411  ops/ms
 * Varint64Benchmark.writeUInt32      thrpt   20  485,122 ±  5,525  ops/ms
 * Varint64Benchmark.writeUInt64      thrpt   20  472,033 ± 10,698  ops/ms
 * Varint64Benchmark.writeUInt64As32  thrpt   20  444,624 ± 35,104  ops/ms
 * <p>
 * (random bit distribution w/o aligned writes)
 * Benchmark                           Mode  Cnt    Score   Error   Units
 * Varint64Benchmark.writeInt32       thrpt   20  481,644 ± 7,317  ops/ms
 * Varint64Benchmark.writeInt64       thrpt   20  470,096 ± 4,354  ops/ms
 * Varint64Benchmark.writeUInt32      thrpt   20  474,296 ± 4,893  ops/ms
 * Varint64Benchmark.writeUInt64      thrpt   20  479,837 ± 5,909  ops/ms
 * Varint64Benchmark.writeUInt64As32  thrpt   20  461,024 ± 5,515  ops/ms
 *
 * (random long bit range)
 * Benchmark                              Mode  Cnt    Score    Error   Units
 * Varint64Benchmark.writeExtendedInt32  thrpt   20  795,352 ± 38,311  ops/ms
 * Varint64Benchmark.writeExtendedInt64  thrpt   20  638,104 ±  7,639  ops/ms
 * Varint64Benchmark.writeInt32          thrpt   20  554,267 ±  8,925  ops/ms
 * Varint64Benchmark.writeInt64          thrpt   20  295,103 ±  7,313  ops/ms
 * Varint64Benchmark.writeUInt64         thrpt   20  280,490 ± 11,318  ops/ms
 * Varint64Benchmark.writeUInt64As32     thrpt   20  284,050 ±  4,707  ops/ms
 *
 * (production distribution)
 * Benchmark                                           Mode  Cnt     Score     Error   Units
 * Varint64Benchmark.computeRawVarInt64Size_branches  thrpt   20  1323,027 ±  32,910  ops/ms
 * Varint64Benchmark.computeRawVarInt64Size_clz       thrpt   20  5164,235 ± 173,749  ops/ms
 * Varint64Benchmark.writeExtendedInt64               thrpt   20   201,497 ±  0,658  ops/ms
 * Varint64Benchmark.writeInt64                       thrpt   20   357,049 ±  9,361  ops/ms
 * Varint64Benchmark.writeInt64_counted               thrpt   20   293,787 ±  6,596  ops/ms
 * Varint64Benchmark.writeUInt64                      thrpt   20   395,856 ± 11,377  ops/ms
 * Varint64Benchmark.writeUInt64As32                  thrpt   20   450,396 ±  5,713  ops/ms
 *
 * @author Florian Enner
 * @since 12 Sep 2014
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class Varint64Benchmark {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + Varint64Benchmark.class.getSimpleName() + ".*UInt64.*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    long[] values = new long[512];
    byte[] output = new byte[values.length * 10];
    ProtoSink sink = ProtoSink.newInstance(output);
    private static final boolean ENABLE_ALIGNED_ACCESS = false;

    @Setup(Level.Iteration)
    public void setup() {
        Random random = new Random(System.nanoTime());
        for (int i = 0; i < values.length; i++) {
//            values[i] = Math.abs(random.nextInt()) % 128; // all 1 byte varint (best case)
//            values[i] = random.nextInt(); // 50% negative (worst case)
//            values[i] = random.nextDouble() < 0.50 ? Math.abs(random.nextInt()) : random.nextInt(); // 25% negative
//            values[i] = 1 << random.nextInt(32); // random int bit distribution
//            values[i] = 1 << random.nextInt(21); // random 1-3 byte varints
//            values[i] = 1L << random.nextInt(64); // random long bit distribution
            values[i] = withProductionDistribution(random);
        }
    }

    private long withProductionDistribution(Random random) {
        float rnd = random.nextFloat() * 100f;
        if(rnd < 43.88) return 1L; // 1 byte
        if(rnd < 52.04) return 1L << 7; // 2 bytes
        //if(rnd < 52.04) return 1L << 14; // 3 bytes
        //if(rnd < 52.04) return 1L << 21; // 4 bytes
        if(rnd < 64.29) return 1L << 28; // 5 byte
        if(rnd < 78.57) return 1L << 35; // 6 byte
        if(rnd < 92.88) return 1L << 42; // 7 byte
        /*if(rnd < 100)*/ return 1L << 49; // 8 byte
        //if(rnd < 100) return 1L << 56; // 9 byte
        //if(rnd < 100) return 1L << 63; // 10 byte
    }

    private void writeUInt32NoTag(int value) throws IOException {
        while (true) {
            if ((value & ~0x7F) == 0) {
                writeRawByte((byte) value);
                return;
            } else {
                writeRawByte((byte) (value | 0x80));
                value >>>= 7;
            }
        }
    }

    private void writeInt32NoTag(int value) throws IOException {
        if (value >= 0) {
            writeUInt32NoTag(value);
        } else {
            writeRawExtendedVarint64(value);
        }
    }

    protected void writeRawExtendedVarint32(int value) throws IOException {
        if (ENABLE_ALIGNED_ACCESS) {
            long first8 = 0x8080808080808080L
                    | (((long) value & 0xFF))
                    | (((long) value << 1) & ((0xFFL << 8)))
                    | (((long) value << 2) & ((0xFFL << 16)))
                    | (((long) value << 3) & ((0xFFL << 24)))
                    | (((long) value << 4) & ((0xFFL << 32)))
                    | (~0L << 37);
            sink.writeRawLittleEndian64(first8);
            sink.writeRawLittleEndian16((short) 0x1FF);
        } else {
            writeRawByte((byte) (value | 0x80));
            writeRawByte((byte) (((value >>> 7)) | 0x80));
            writeRawByte((byte) (((value >>> 14)) | 0x80));
            writeRawByte((byte) (((value >>> 21)) | 0x80));
            writeRawByte((byte) (((value >>> 28)) | 0x80));
            writeRawByte((byte) -1);
            writeRawByte((byte) -1);
            writeRawByte((byte) -1);
            writeRawByte((byte) -1);
            writeRawByte((byte) 1);
        }
    }

    private void writeUInt64NoTag(long value) throws IOException {
        while (true) {
            if ((value & ~0x7FL) == 0) {
                writeRawByte((byte) value);
                return;
            } else {
                writeRawByte((byte) (((int) value) | 0x80));
                value >>>= 7;
            }
        }
    }

    private void writeInt64NoTag(long value) throws IOException {
        if (value >= 0) {
            writeUInt64NoTag(value);
        } else {
            writeRawExtendedVarint64(value);
        }
    }

    private void writeInt64NoTag_counted(long value) throws IOException {
        final int numBytes = Varint64.sizeOf(value);
        for (int i = 1; i < numBytes; i++) {
            writeRawByte((byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        writeRawByte((byte) value);
    }

    private void writeRawExtendedVarint64(long value) throws IOException {
        if (ENABLE_ALIGNED_ACCESS) {
            long first8 = 0x8080808080808080L
                    | ((value & 0xFF))
                    | ((value << 1) & ((0xFFL << 8)))
                    | ((value << 2) & ((0xFFL << 16)))
                    | ((value << 3) & ((0xFFL << 24)))
                    | ((value << 4) & ((0xFFL << 32)))
                    | ((value << 5) & ((0xFFL << 40)))
                    | ((value << 6) & ((0xFFL << 48)))
                    | ((value << 7) & ((0xFFL << 56)));
            long last2 = 0x80L
                    | ((value >>> 56) & 0xFF)
                    | ((value >>> 55) & (0xFF << 8));
            sink.writeRawLittleEndian64(first8);
            sink.writeRawLittleEndian16((short) last2);
        } else {
            writeRawByte((byte) (((int) value) | 0x80));
            writeRawByte((byte) (((int) (value >>> 7)) | 0x80));
            writeRawByte((byte) (((int) (value >>> 14)) | 0x80));
            writeRawByte((byte) (((int) (value >>> 21)) | 0x80));
            writeRawByte((byte) (((int) (value >>> 28)) | 0x80));
            writeRawByte((byte) (((int) (value >>> 35)) | 0x80));
            writeRawByte((byte) (((int) (value >>> 42)) | 0x80));
            writeRawByte((byte) (((int) (value >>> 49)) | 0x80));
            writeRawByte((byte) (((int) (value >>> 56)) | 0x80));
            writeRawByte((byte) (value >>> 63));
        }
    }

    private void writeUInt64NoTagAs32(long value) throws IOException {
        if ((value & (~0L << 28)) == 0) {
            // write first 3 bytes as UInt32
            writeUInt32NoTag((int) value);
            return;
        }
        writeVarintFirst4Bytes((int) value);
        value >>>= 28;
        while (true) {
            if ((value & ~0x7FL) == 0) {
                writeRawByte((byte) value);
                return;
            } else {
                writeRawByte((byte) (value | 0x80));
                value >>>= 7;
            }
        }
    }

    private void writeVarintFirst4Bytes(int value) throws IOException {
        if (ENABLE_ALIGNED_ACCESS) {
            final int bits = 0x80808080
                    | ((value & 0xFF))
                    | ((value << 1) & ((0xFF << 8)))
                    | ((value << 2) & ((0xFF << 16)))
                    | ((value << 3) & ((0xFF << 24)));
            sink.writeRawLittleEndian32(bits);
        } else {
            writeRawByte((byte) (value | 0x80));
            writeRawByte((byte) (((value >>> 7)) | 0x80));
            writeRawByte((byte) (((value >>> 14)) | 0x80));
            writeRawByte((byte) (((value >>> 21)) | 0x80));
        }
    }

    private void writeRawByte(byte value) throws IOException {
        sink.writeRawByte(value);
    }

    @Benchmark
    public int writeUInt32() throws IOException {
        sink.reset();
        for (int i = 0; i < values.length; i++) {
            writeUInt32NoTag((int) values[i]);
        }
        return sink.getTotalBytesWritten();
    }

    @Benchmark
    public int writeInt32() throws IOException {
        sink.reset();
        for (int i = 0; i < values.length; i++) {
            writeInt32NoTag((int) values[i]);
        }
        return sink.getTotalBytesWritten();
    }

    @Benchmark
    public int writeExtendedInt32() throws IOException {
        sink.reset();
        for (int i = 0; i < values.length; i++) {
            writeRawExtendedVarint32((int) values[i]);
        }
        return sink.getTotalBytesWritten();
    }

    @Benchmark
    public int writeUInt64() throws IOException {
        sink.reset();
        for (int i = 0; i < values.length; i++) {
            writeUInt64NoTag(values[i]);
        }
        return sink.getTotalBytesWritten();
    }

    @Benchmark
    public int writeInt64() throws IOException {
        sink.reset();
        for (int i = 0; i < values.length; i++) {
            writeInt64NoTag(values[i]);
        }
        return sink.getTotalBytesWritten();
    }

    @Benchmark
    public int writeInt64_counted() throws IOException {
        sink.reset();
        for (int i = 0; i < values.length; i++) {
            writeInt64NoTag_counted(values[i]);
        }
        return sink.getTotalBytesWritten();
    }

    @Benchmark
    public int writeUInt64As32() throws IOException {
        sink.reset();
        for (int i = 0; i < values.length; i++) {
            writeUInt64NoTagAs32(values[i]);
        }
        return sink.getTotalBytesWritten();
    }

    @Benchmark
    public int writeExtendedInt64() throws IOException {
        sink.reset();
        for (int i = 0; i < values.length; i++) {
            writeRawExtendedVarint64(values[i]);
        }
        return sink.getTotalBytesWritten();
    }

    @Benchmark
    public int computeRawVarInt64Size_branches() {
        int sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += computeRawVarint64Size(values[i]);
        }
        return sum;
    }

    @Benchmark
    public int computeRawVarInt64Size_clz() {
        int sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += Varint64.sizeOf(values[i]);
        }
        return sum;
    }

    // Current implementation in Protobuf
    public static int computeRawVarint64Size(long value) {
        // handle two popular special cases up front ...
        if ((value & (~0L << 7)) == 0L) {
            return 1;
        }
        if (value < 0L) {
            return 10;
        }
        // ... leaving us with 8 remaining, which we can divide and conquer
        int n = 2;
        if ((value & (~0L << 35)) != 0L) {
            n += 4;
            value >>>= 28;
        }
        if ((value & (~0L << 21)) != 0L) {
            n += 2;
            value >>>= 14;
        }
        if ((value & (~0L << 14)) != 0L) {
            n += 1;
        }
        return n;
    }

    // Faster in all cases except when the data is entirely 1 byte. For random distribution this is 5x faster.
    static class Varint64 {
        static int sizeOf(long value) {
            return SIZE[Long.numberOfLeadingZeros(value)];
        }
        static final int[] SIZE = new int[65];
        static {
            for (int i = 0; i <= 64; i++) {
                SIZE[i] = 1 + (63 - i) / 7;
            }
        }
    }

}
