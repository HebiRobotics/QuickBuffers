/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2023 HEBI Robotics
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

package us.hebi.quickbuf;

import java.io.IOException;

/**
 * Utility class for Nov 2019 tests for writing varints in reverse. Separating
 * it out is easier for testing if we ever want to re-evaluate this in the future.
 * <p>
 * There is an argument for writing protobuf messages back to front as it
 * removes the need to pre-calculate the size of everything. However, it makes
 * varints more expensive and adds other complexities, so in the initial tests
 * it looked like a dead end.
 *
 * @author Florian Enner
 * @since 27 Nov 2019
 */
public class ReverseEncodingUtil {

    public static void writeUInt32NoTag(final ProtoSink sink, final int value) throws IOException {
        writeUInt32NoTag_5(sink, value); // [5] was empirically determined to be the fastest
    }

    public static void writeUInt64NoTag(final ProtoSink sink, final long value) throws IOException {
        writeUInt64NoTag_5(sink, value); // [5] needs more testing about whether it's good for longs
    }

    // ================== varint 32 ==================

    public static void writeUInt32NoTag_forward(final ProtoSink sink, int value) throws IOException {
        while (true) {
            if ((value & ~0x7F) == 0) {
                sink.writeRawByte((byte) value);
                return;
            } else {
                sink.writeRawByte((byte) (value | 0x80));
                value >>>= 7;
            }
        }
    }

    public static void writeUInt32NoTag_2(final ProtoSink sink, final int value) throws IOException {
        final int b1, b2, b3, b4;

        if ((b1 = value >>> 7) == 0) {
            sink.writeRawByte((byte) value);

        } else if ((b2 = value >>> 14) == 0) {
            sink.writeRawByte(((byte) (value & 0x7F) | 0x80));
            sink.writeRawByte(((byte) b1));

        } else if ((b3 = value >>> 21) == 0) {
            sink.writeRawByte(((byte) (value & 0x7F) | 0x80));
            sink.writeRawByte(((byte) (b1 & 0x7F) | 0x80));
            sink.writeRawByte(((byte) b2));

        } else if ((b4 = value >>> 28) == 0) {
            sink.writeRawByte(((byte) (value & 0x7F) | 0x80));
            sink.writeRawByte(((byte) (b1 & 0x7F) | 0x80));
            sink.writeRawByte(((byte) (b2 & 0x7F) | 0x80));
            sink.writeRawByte(((byte) b3));

        } else {
            sink.writeRawByte(((byte) (value & 0x7F) | 0x80));
            sink.writeRawByte(((byte) (b1 & 0x7F) | 0x80));
            sink.writeRawByte(((byte) (b2 & 0x7F) | 0x80));
            sink.writeRawByte(((byte) (b3 & 0x7F) | 0x80));
            sink.writeRawByte(((byte) b4));

        }
    }

    public static void writeUInt32NoTag_3(final ProtoSink sink, final int value) throws IOException {
        final int b1;
        if ((b1 = value >>> 7) == 0) {
            sink.writeRawByte((byte) value);
            return;
        }

        final int b2, b3, b4;
        if ((b2 = value >>> 14) == 0) {
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) b1);

        } else if ((b3 = value >>> 21) == 0) {
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) (b1 | 0x80));
            sink.writeRawByte((byte) b2);

        } else if ((b4 = value >>> 28) == 0) {
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) (b1 | 0x80));
            sink.writeRawByte((byte) (b2 | 0x80));
            sink.writeRawByte((byte) b3);

        } else {
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) (b1 | 0x80));
            sink.writeRawByte((byte) (b2 | 0x80));
            sink.writeRawByte((byte) (b3 | 0x80));
            sink.writeRawByte((byte) b4);

        }
    }

    public static void writeUInt32NoTag_4(final ProtoSink sink, final int value) throws IOException {
        if ((value & shift7) == 0) {
            sink.writeRawByte((byte) value);
            return;
        }

        final int b1 = value >>> 7;
        if (b1 < 128) {
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) (b1));
            return;
        }

        final int b2 = value >>> 14;
        if (b2 < 128) {
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) (b1 | 0x80));
            sink.writeRawByte((byte) (b2));
            return;
        }

        final int b3 = value >>> 21;
        if (b3 < 128) {
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) (b1 | 0x80));
            sink.writeRawByte((byte) (b2 | 0x80));
            sink.writeRawByte((byte) (b3));
            return;
        }

        final int b4 = value >>> 28;
        sink.writeRawByte((byte) (value | 0x80));
        sink.writeRawByte((byte) (b1 | 0x80));
        sink.writeRawByte((byte) (b2 | 0x80));
        sink.writeRawByte((byte) (b3 | 0x80));
        sink.writeRawByte((byte) (b4));

    }

    public static void writeUInt32NoTag_5(final ProtoSink sink, final int value) throws IOException {
        if ((value & shift7) == 0) {
            sink.writeRawByte((byte) value);

        } else if ((value & shift14) == 0) {
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) (value >>> 7));

        } else if ((value & shift21) == 0) {
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) ((value >>> 7) | 0x80));
            sink.writeRawByte((byte) (value >>> 14));

        } else if ((value & shift28) == 0) {
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) ((value >>> 7) | 0x80));
            sink.writeRawByte((byte) ((value >>> 14) | 0x80));
            sink.writeRawByte((byte) (value >>> 21));

        } else {
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) ((value >>> 7) | 0x80));
            sink.writeRawByte((byte) ((value >>> 14) | 0x80));
            sink.writeRawByte((byte) ((value >>> 21) | 0x80));
            sink.writeRawByte((byte) (value >>> 28));
        }

    }

    public static void writeUInt32NoTag_6(final ProtoSink sink, final int value) throws IOException {
        if ((value & (~0 << 7)) == 0) {
            sink.writeRawByte((byte) value);
            return;
        }

        final int b1, b2, b3;
        if ((b1 = value >>> 7) < 128) {
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) (b1));

        } else if ((b2 = value >>> 14) < 128) {
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) (b1 | 0x80));
            sink.writeRawByte((byte) (b2));

        } else if ((b3 = value >>> 21) < 128) {
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) (b1 | 0x80));
            sink.writeRawByte((byte) (b2 | 0x80));
            sink.writeRawByte((byte) (b3));

        } else {
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) (b1 | 0x80));
            sink.writeRawByte((byte) (b2 | 0x80));
            sink.writeRawByte((byte) (b3 | 0x80));
            sink.writeRawByte((byte) (value >>> 28));

        }

    }

    private static final int shift7 = ~0 << 7;
    private static final int shift14 = ~0 << 14;
    private static final int shift21 = ~0 << 21;
    private static final int shift28 = ~0 << 28;


    // ================== varint 64 ==================

    public static void writeUInt64NoTag_forward(final ProtoSink sink, long value) throws IOException {
        while (true) {
            if ((value & ~0x7FL) == 0) {
                sink.writeRawByte((byte) value);
                return;
            } else {
                sink.writeRawByte((byte) (((int) value) | 0x80));
                value >>>= 7;
            }
        }
    }

    public static void writeUInt64NoTag_2(final ProtoSink sink, long value) throws IOException {
        if ((value & (~0L << 28)) == 0L) {
            // 4 byte -> write as int
            writeUInt32NoTag(sink, (int) value);
        } else {
            // 5 byte and higher -> write as long
            sink.writeRawByte((byte) (value | 0x80));
            sink.writeRawByte((byte) ((value >>> 7) | 0x80));
            sink.writeRawByte((byte) ((value >>> 14) | 0x80));
            sink.writeRawByte((byte) ((value >>> 21) | 0x80));
            value >>>= 28;
            while (true) {
                if ((value & ~0x7FL) == 0) {
                    sink.writeRawByte((byte) value);
                    return;
                } else {
                    sink.writeRawByte((((int) value) | 0x80));
                    value >>>= 7;
                }
            }
        }
    }

    public static void writeUInt64NoTag_3(final ProtoSink sink, final long value) throws IOException {
        switch (ProtoSink.computeRawVarint64Size(value)) {
            case 1:
                writeUInt64NoTagOneByte(sink, value);
                break;
            case 2:
                writeUInt64NoTagTwoBytes(sink, value);
                break;
            case 3:
                writeUInt64NoTagThreeBytes(sink, value);
                break;
            case 4:
                writeUInt64NoTagFourBytes(sink, value);
                break;
            case 5:
                writeUInt64NoTagFiveBytes(sink, value);
                break;
            case 6:
                writeUInt64NoTagSixBytes(sink, value);
                break;
            case 7:
                writeUInt64NoTagSevenBytes(sink, value);
                break;
            case 8:
                writeUInt64NoTagEightBytes(sink, value);
                break;
            case 9:
                writeUInt64NoTagNineBytes(sink, value);
                break;
            case 10:
                writeUInt64NoTagTenBytes(sink, value);
                break;
        }
    }

    private static void writeUInt64NoTagOneByte(final ProtoSink sink, final long value) throws IOException {
        sink.writeRawByte((byte) value);
    }

    private static void writeUInt64NoTagTwoBytes(final ProtoSink sink, final long value) throws IOException {
        sink.writeRawByte((byte) (value | 0x80));
        sink.writeRawByte((byte) (value >>> 7));
    }

    private static void writeUInt64NoTagThreeBytes(final ProtoSink sink, final long value) throws IOException {
        sink.writeRawByte((byte) (value | 0x80));
        sink.writeRawByte((byte) ((value >>> 7) | 0x80));
        sink.writeRawByte((byte) (value >>> 14));
    }

    private static void writeUInt64NoTagFourBytes(final ProtoSink sink, final long value) throws IOException {
        sink.writeRawByte((byte) (value | 0x80));
        sink.writeRawByte((byte) ((value >>> 7) | 0x80));
        sink.writeRawByte((byte) ((value >>> 14) | 0x80));
        sink.writeRawByte((byte) (value >>> 21));
    }

    private static void writeUInt64NoTagFiveBytes(final ProtoSink sink, final long value) throws IOException {
        sink.writeRawByte((byte) (value | 0x80));
        sink.writeRawByte((byte) (((value >>> 7)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 14)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 21)) | 0x80));
        sink.writeRawByte((byte) (value >>> 28));
    }

    private static void writeUInt64NoTagSixBytes(final ProtoSink sink, final long value) throws IOException {
        sink.writeRawByte((byte) (value | 0x80));
        sink.writeRawByte((byte) (((value >>> 7)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 14)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 21)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 28)) | 0x80));
        sink.writeRawByte((byte) (value >>> 35));
    }

    private static void writeUInt64NoTagSevenBytes(final ProtoSink sink, final long value) throws IOException {
        sink.writeRawByte((byte) (value | 0x80));
        sink.writeRawByte((byte) (((value >>> 7)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 14)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 21)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 28)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 35)) | 0x80));
        sink.writeRawByte((byte) (value >>> 42));
    }

    private static void writeUInt64NoTagEightBytes(final ProtoSink sink, final long value) throws IOException {
        sink.writeRawByte((byte) (value | 0x80));
        sink.writeRawByte((byte) (((value >>> 7)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 14)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 21)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 28)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 35)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 42)) | 0x80));
        sink.writeRawByte((byte) (value >>> 49));
    }

    private static void writeUInt64NoTagNineBytes(final ProtoSink sink, final long value) throws IOException {
        sink.writeRawByte((byte) (value | 0x80));
        sink.writeRawByte((byte) (((value >>> 7)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 14)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 21)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 28)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 35)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 42)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 49)) | 0x80));
        sink.writeRawByte((byte) (value >>> 56));
    }

    private static void writeUInt64NoTagTenBytes(final ProtoSink sink, final long value) throws IOException {
        sink.writeRawByte((byte) (value | 0x80));
        sink.writeRawByte((byte) (((value >>> 7)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 14)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 21)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 28)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 35)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 42)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 49)) | 0x80));
        sink.writeRawByte((byte) (((value >>> 56)) | 0x80));
        sink.writeRawByte((byte) (value >>> 63));
    }

    /*
   if ((value & (0xffffffff << 14)) == 0) return 2;
   if ((value & (0xffffffff << 21)) == 0) return 3;
   if ((value & (0xffffffff << 28)) == 0) return 4;
   if ((value & (0xffffffff << 35)) == 0) return 5;

   if ((value & (0xffffffff << 42)) == 0) return 6;
   if ((value & (0xffffffff << 49)) == 0) return 7;
   if ((value & (0xffffffff << 56)) == 0) return 8;
   if ((value & (0xffffffff << 63)) == 0) return 9;
    */
    public static void writeUInt64NoTag_4(final ProtoSink sink, final long value) throws IOException {
        if ((value & (~0L << 7)) == 0L) { // 1 byte
            writeUInt64NoTagOneByte(sink, value);
        } else if (value < 0L) { // 10 bytes
            writeUInt64NoTagTenBytes(sink, value);
        } else if ((value & (~0L << 35)) == 0) { // 2-5 bytes
            if ((value & (~0L << 21)) == 0) { // 2-3 bytes
                if ((value & (~0L << 14)) == 0) {
                    writeUInt64NoTagTwoBytes(sink, value);
                } else {
                    writeUInt64NoTagThreeBytes(sink, value);
                }
            } else { // 4-5 bytes
                if ((value & (~0L << 28)) == 0) {
                    writeUInt64NoTagFourBytes(sink, value);
                } else {
                    writeUInt64NoTagFiveBytes(sink, value);
                }
            }
        } else { // 6-9 bytes
            if ((value & (~0L << 49)) == 0) { // 6-7 bytes
                if ((value & (~0L << 42)) == 0) {
                    writeUInt64NoTagSixBytes(sink, value);
                } else {
                    writeUInt64NoTagSevenBytes(sink, value);
                }
            } else { // 8-9 bytes
                if ((value & (~0L << 56)) == 0) {
                    writeUInt64NoTagEightBytes(sink, value);
                } else {
                    writeUInt64NoTagNineBytes(sink, value);
                }
            }
        }
    }

    public static void writeUInt64NoTag_5(final ProtoSink sink, final long value) throws IOException {
        if ((value & upper57) == 0) {
            sink.writeRawByte((byte) value);

        } else if ((value & upper50) == 0) {
            sink.writeRawByte((byte) (value | 0x80L));
            sink.writeRawByte((byte) (value >>> 7));

        } else if ((value & upper43) == 0) {
            sink.writeRawByte((byte) (value | 0x80L));
            sink.writeRawByte((byte) ((value >>> 7) | 0x80L));
            sink.writeRawByte((byte) (value >>> 14));

        } else if ((value & upper36) == 0) {
            sink.writeRawByte((byte) (value | 0x80L));
            sink.writeRawByte((byte) ((value >>> 7) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 14) | 0x80L));
            sink.writeRawByte((byte) (value >>> 21));

        } else if ((value & upper29) == 0) {
            sink.writeRawByte((byte) (value | 0x80L));
            sink.writeRawByte((byte) ((value >>> 7) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 14) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 21) | 0x80L));
            sink.writeRawByte((byte) (value >>> 28));

        } else if ((value & upper22) == 0) {
            sink.writeRawByte((byte) (value | 0x80L));
            sink.writeRawByte((byte) ((value >>> 7) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 14) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 21) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 28) | 0x80L));
            sink.writeRawByte((byte) (value >>> 35));

        } else if ((value & upper15) == 0) {
            sink.writeRawByte((byte) (value | 0x80L));
            sink.writeRawByte((byte) ((value >>> 7) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 14) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 21) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 28) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 35) | 0x80L));
            sink.writeRawByte((byte) (value >>> 42));

        } else if ((value & upper8) == 0) {
            sink.writeRawByte((byte) (value | 0x80L));
            sink.writeRawByte((byte) ((value >>> 7) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 14) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 21) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 28) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 35) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 42) | 0x80L));
            sink.writeRawByte((byte) (value >>> 49));

        } else if ((value & upper1) == 0) {
            sink.writeRawByte((byte) (value | 0x80L));
            sink.writeRawByte((byte) ((value >>> 7) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 14) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 21) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 28) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 35) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 42) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 49) | 0x80L));
            sink.writeRawByte((byte) (value >>> 56));

        } else {
            sink.writeRawByte((byte) (value | 0x80L));
            sink.writeRawByte((byte) ((value >>> 7) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 14) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 21) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 28) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 35) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 42) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 49) | 0x80L));
            sink.writeRawByte((byte) ((value >>> 56) | 0x80L));
            sink.writeRawByte((byte) (value >>> 63));

        }
    }

    private static final long upper57 = ~0L << 7;
    private static final long upper50 = ~0L << 14;
    private static final long upper43 = ~0L << 21;
    private static final long upper36 = ~0L << 28;
    private static final long upper29 = ~0L << 35;
    private static final long upper22 = ~0L << 42;
    private static final long upper15 = ~0L << 49;
    private static final long upper8 = ~0L << 56;
    private static final long upper1 = ~0L << 63;

}
