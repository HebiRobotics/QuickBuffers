/*-
 * #%L
 * quickbuf-runtime
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

package us.hebi.quickbuf;

import static us.hebi.quickbuf.UnsafeAccess.*;
import static us.hebi.quickbuf.WireFormat.*;

/**
 * Utility methods for reading and writing bytes. Callers are responsible
 * for making sure that the buffers are large enough. There are no bounds
 * checks internally.
 * <p>
 * Platforms without unaligned access automatically disable it entirely.
 * Protobuf has many 1 byte tags and mostly random alignment, so it's
 * not worth checking for alignment.
 *
 * @author Florian Enner
 * @since 07 Mär 2022
 */
class ByteUtil {

    public static short readLittleEndian16(byte[] buffer, int offset) {
        if (ENABLE_UNSAFE_UNALIGNED) {
            final short value = UNSAFE.getShort(buffer, BYTE_ARRAY_OFFSET + offset);
            return IS_LITTLE_ENDIAN ? value : Short.reverseBytes(value);
        } else {
            return (short) ((buffer[offset] & 0xFF) | (buffer[offset + 1] & 0xFF) << 8);
        }
    }

    public static int readLittleEndian32(byte[] buffer, int offset) {
        if (ENABLE_UNSAFE_UNALIGNED) {
            final int value = UNSAFE.getInt(buffer, BYTE_ARRAY_OFFSET + offset);
            return IS_LITTLE_ENDIAN ? value : Integer.reverseBytes(value);
        } else {
            return (buffer[offset] & 0xFF) |
                    (buffer[offset + 1] & 0xFF) << 8 |
                    (buffer[offset + 2] & 0xFF) << 16 |
                    (buffer[offset + 3] & 0xFF) << 24;
        }
    }

    public static long readLittleEndian64(byte[] buffer, int offset) {
        if (ENABLE_UNSAFE_UNALIGNED) {
            final long value = UNSAFE.getLong(buffer, BYTE_ARRAY_OFFSET + offset);
            return IS_LITTLE_ENDIAN ? value : Long.reverseBytes(value);
        } else {
            return (buffer[offset] & 0xFFL) |
                    (buffer[offset + 1] & 0xFFL) << 8 |
                    (buffer[offset + 2] & 0xFFL) << 16 |
                    (buffer[offset + 3] & 0xFFL) << 24 |
                    (buffer[offset + 4] & 0xFFL) << 32 |
                    (buffer[offset + 5] & 0xFFL) << 40 |
                    (buffer[offset + 6] & 0xFFL) << 48 |
                    (buffer[offset + 7] & 0xFFL) << 56;
        }
    }

    public static float readFloat(byte[] buffer, int offset) {
        if (ENABLE_UNSAFE_UNALIGNED && IS_LITTLE_ENDIAN) {
            return UNSAFE.getFloat(buffer, BYTE_ARRAY_OFFSET + offset);
        } else {
            return Float.intBitsToFloat(readLittleEndian32(buffer, offset));
        }
    }

    public static double readDouble(byte[] buffer, int offset) {
        if (ENABLE_UNSAFE_UNALIGNED && IS_LITTLE_ENDIAN) {
            return UNSAFE.getDouble(buffer, BYTE_ARRAY_OFFSET + offset);
        } else {
            return Double.longBitsToDouble(readLittleEndian64(buffer, offset));
        }
    }

    public static void readBytes(byte[] buffer, int offset, byte[] dst, int dstOffset, int dstLength) {
        System.arraycopy(buffer, offset, dst, dstOffset, dstLength);
    }

    public static void readLittleEndian32s(byte[] buffer, int offset, int[] dst, int dstOffset, int dstLength) {
        if (IS_LITTLE_ENDIAN && ENABLE_UNSAFE_COPY) {
            final int numBytes = dstLength * SIZEOF_FIXED_32;
            final long targetOffset = INT_ARRAY_OFFSET + (long) dstOffset * SIZEOF_FIXED_32;
            UNSAFE.copyMemory(buffer, BYTE_ARRAY_OFFSET + offset, dst, targetOffset, numBytes);
        } else {
            for (int i = 0; i < dstLength; i++) {
                dst[i] = readLittleEndian32(buffer, offset);
                offset += SIZEOF_FIXED_32;
            }
        }
    }

    public static void readLittleEndian64s(byte[] buffer, int offset, long[] dst, int dstOffset, int dstLength) {
        if (IS_LITTLE_ENDIAN && ENABLE_UNSAFE_COPY) {
            final int numBytes = dstLength * SIZEOF_FIXED_64;
            final long targetOffset = LONG_ARRAY_OFFSET + (long) dstOffset * SIZEOF_FIXED_64;
            UNSAFE.copyMemory(buffer, BYTE_ARRAY_OFFSET + offset, dst, targetOffset, numBytes);
        } else {
            for (int i = 0; i < dstLength; i++) {
                dst[i] = readLittleEndian64(buffer, offset);
                offset += SIZEOF_FIXED_64;
            }
        }
    }

    public static void readFloats(byte[] buffer, int offset, float[] dst, int dstOffset, int dstLength) {
        if (IS_LITTLE_ENDIAN && ENABLE_UNSAFE_COPY) {
            final int numBytes = dstLength * SIZEOF_FIXED_32;
            final long targetOffset = FLOAT_ARRAY_OFFSET + (long) dstOffset * SIZEOF_FIXED_32;
            UNSAFE.copyMemory(buffer, BYTE_ARRAY_OFFSET + offset, dst, targetOffset, numBytes);
        } else {
            for (int i = 0; i < dstLength; i++) {
                dst[i] = readFloat(buffer, offset);
                offset += SIZEOF_FIXED_32;
            }
        }
    }

    public static void readDoubles(byte[] buffer, int offset, double[] dst, int dstOffset, int dstLength) {
        if (IS_LITTLE_ENDIAN && ENABLE_UNSAFE_COPY) {
            final int numBytes = dstLength * SIZEOF_FIXED_64;
            final long targetOffset = DOUBLE_ARRAY_OFFSET + (long) dstOffset * SIZEOF_FIXED_64;
            UNSAFE.copyMemory(buffer, BYTE_ARRAY_OFFSET + offset, dst, targetOffset, numBytes);
        } else {
            for (int i = 0; i < dstLength; i++) {
                dst[i] = readDouble(buffer, offset);
                offset += SIZEOF_FIXED_64;
            }
        }
    }

    public static short readUnsafeLittleEndian16(byte[] buffer, long offset) {
        if (ENABLE_UNSAFE_UNALIGNED) {
            final short value = UNSAFE.getShort(buffer, offset);
            return IS_LITTLE_ENDIAN ? value : Short.reverseBytes(value);
        } else {
            return (short) ((UNSAFE.getByte(buffer, offset) & 0xFF) | (UNSAFE.getByte(buffer, offset + 1) & 0xFF) << 8);
        }
    }

    public static int readUnsafeLittleEndian32(byte[] buffer, long offset) {
        if (ENABLE_UNSAFE_UNALIGNED) {
            final int value = UNSAFE.getInt(buffer, offset);
            return IS_LITTLE_ENDIAN ? value : Integer.reverseBytes(value);
        } else {
            return (UNSAFE.getByte(buffer, offset) & 0xFF) |
                    (UNSAFE.getByte(buffer, offset + 1) & 0xFF) << 8 |
                    (UNSAFE.getByte(buffer, offset + 2) & 0xFF) << 16 |
                    (UNSAFE.getByte(buffer, offset + 3) & 0xFF) << 24;
        }
    }

    public static long readUnsafeLittleEndian64(byte[] buffer, long offset) {
        if (ENABLE_UNSAFE_UNALIGNED) {
            final long value = UNSAFE.getLong(buffer, offset);
            return IS_LITTLE_ENDIAN ? value : Long.reverseBytes(value);
        } else {
            return (UNSAFE.getByte(buffer, offset) & 0xFFL) |
                    (UNSAFE.getByte(buffer, offset + 1) & 0xFFL) << 8 |
                    (UNSAFE.getByte(buffer, offset + 2) & 0xFFL) << 16 |
                    (UNSAFE.getByte(buffer, offset + 3) & 0xFFL) << 24 |
                    (UNSAFE.getByte(buffer, offset + 4) & 0xFFL) << 32 |
                    (UNSAFE.getByte(buffer, offset + 5) & 0xFFL) << 40 |
                    (UNSAFE.getByte(buffer, offset + 6) & 0xFFL) << 48 |
                    (UNSAFE.getByte(buffer, offset + 7) & 0xFFL) << 56;
        }
    }

    public static float readUnsafeFloat(byte[] buffer, long offset) {
        if (ENABLE_UNSAFE_UNALIGNED && IS_LITTLE_ENDIAN) {
            return UNSAFE.getFloat(buffer, offset);
        } else {
            return Float.intBitsToFloat(readUnsafeLittleEndian32(buffer, offset));
        }
    }

    public static double readUnsafeDouble(byte[] buffer, long offset) {
        if (ENABLE_UNSAFE_UNALIGNED && IS_LITTLE_ENDIAN) {
            return UNSAFE.getDouble(buffer, offset);
        } else {
            return Double.longBitsToDouble(readUnsafeLittleEndian64(buffer, offset));
        }
    }

    public static void readUnsafeBytes(byte[] buffer, long offset, byte[] dst, int dstOffset, int dstLength) {
        if (ENABLE_UNSAFE_COPY) {
            UNSAFE.copyMemory(buffer, offset, dst, BYTE_ARRAY_OFFSET + dstOffset, dstLength);
        } else {
            // fallback for old Java <7 versions
            for (int i = 0; i < dstLength; i++) {
                dst[dstOffset + i] = UNSAFE.getByte(buffer, offset + i);
            }
        }
    }

    public static void readUnsafeLittleEndian32s(byte[] buffer, long offset, int[] dst, int dstOffset, int dstLength) {
        if (IS_LITTLE_ENDIAN && ENABLE_UNSAFE_COPY) {
            final int numBytes = dstLength * SIZEOF_FIXED_32;
            final long targetOffset = INT_ARRAY_OFFSET + (long) dstOffset * SIZEOF_FIXED_32;
            UNSAFE.copyMemory(buffer, offset, dst, targetOffset, numBytes);
        } else {
            for (int i = 0; i < dstLength; i++) {
                dst[i] = readUnsafeLittleEndian32(buffer, offset);
                offset += SIZEOF_FIXED_32;
            }
        }
    }

    public static void readUnsafeLittleEndian64s(byte[] buffer, long offset, long[] dst, int dstOffset, int dstLength) {
        if (IS_LITTLE_ENDIAN && ENABLE_UNSAFE_COPY) {
            final int numBytes = dstLength * SIZEOF_FIXED_64;
            final long targetOffset = LONG_ARRAY_OFFSET + (long) dstOffset * SIZEOF_FIXED_64;
            UNSAFE.copyMemory(buffer, offset, dst, targetOffset, numBytes);
        } else {
            for (int i = 0; i < dstLength; i++) {
                dst[i] = readUnsafeLittleEndian64(buffer, offset);
                offset += SIZEOF_FIXED_64;
            }
        }
    }

    public static void readUnsafeFloats(byte[] buffer, long offset, float[] dst, int dstOffset, int dstLength) {
        if (IS_LITTLE_ENDIAN && ENABLE_UNSAFE_COPY) {
            final int numBytes = dstLength * SIZEOF_FIXED_32;
            final long targetOffset = FLOAT_ARRAY_OFFSET + (long) dstOffset * SIZEOF_FIXED_32;
            UNSAFE.copyMemory(buffer, offset, dst, targetOffset, numBytes);
        } else {
            for (int i = 0; i < dstLength; i++) {
                dst[i] = readUnsafeFloat(buffer, offset);
                offset += SIZEOF_FIXED_32;
            }
        }
    }

    public static void readUnsafeDoubles(byte[] buffer, long offset, double[] dst, int dstOffset, int dstLength) {
        if (IS_LITTLE_ENDIAN && ENABLE_UNSAFE_COPY) {
            final int numBytes = dstLength * SIZEOF_FIXED_64;
            final long targetOffset = DOUBLE_ARRAY_OFFSET + (long) dstOffset * SIZEOF_FIXED_64;
            UNSAFE.copyMemory(buffer, offset, dst, targetOffset, numBytes);
        } else {
            for (int i = 0; i < dstLength; i++) {
                dst[i] = readUnsafeDouble(buffer, offset);
                offset += SIZEOF_FIXED_64;
            }
        }
    }

    private ByteUtil() {
    }

}
