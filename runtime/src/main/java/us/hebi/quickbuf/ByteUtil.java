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

    static int writeLittleEndian16(final byte[] buffer, final int offset, final short value) {
        buffer[offset/**/] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) (value >>> 8);
        return SIZEOF_FIXED_16;
    }

    static int writeLittleEndian32(final byte[] buffer, final int offset, final int value) {
        buffer[offset/**/] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) (value >>> 8);
        buffer[offset + 2] = (byte) (value >>> 16);
        buffer[offset + 3] = (byte) (value >>> 24);
        return SIZEOF_FIXED_32;
    }

    static int writeLittleEndian64(final byte[] buffer, final int offset, final long value) {
        buffer[offset/**/] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) (value >>> 8);
        buffer[offset + 2] = (byte) (value >>> 16);
        buffer[offset + 3] = (byte) (value >>> 24);
        buffer[offset + 4] = (byte) (value >>> 32);
        buffer[offset + 5] = (byte) (value >>> 40);
        buffer[offset + 6] = (byte) (value >>> 48);
        buffer[offset + 7] = (byte) (value >>> 56);
        return SIZEOF_FIXED_64;
    }

    static int writeFloat(final byte[] buffer, final int offset, final float value) {
        return writeLittleEndian32(buffer, offset, Float.floatToIntBits(value));
    }

    static int writeDouble(final byte[] buffer, final int offset, final double value) {
        return writeLittleEndian64(buffer, offset, Double.doubleToLongBits(value));
    }

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
            return (buffer[offset/* */] & 0xFF) |
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
            return (buffer[offset/* */] & 0xFFL) |
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

    static int writeUnsafeLittleEndian16(final byte[] buffer, final long offset, final short value) {
        UNSAFE.putByte(buffer, offset, (byte) (value & 0xFF));
        UNSAFE.putByte(buffer, offset + 1, (byte) (value >>> 8));
        return SIZEOF_FIXED_16;
    }

    static int writeUnsafeLittleEndian32(final byte[] buffer, final long offset, final int value) {
        UNSAFE.putByte(buffer, offset, (byte) (value & 0xFF));
        UNSAFE.putByte(buffer, offset + 1, (byte) (value >>> 8));
        UNSAFE.putByte(buffer, offset + 2, (byte) (value >>> 16));
        UNSAFE.putByte(buffer, offset + 3, (byte) (value >>> 24));
        return SIZEOF_FIXED_32;
    }

    static int writeUnsafeLittleEndian64(final byte[] buffer, final long offset, final long value) {
        UNSAFE.putByte(buffer, offset, (byte) (value & 0xFF));
        UNSAFE.putByte(buffer, offset + 1, (byte) (value >>> 8));
        UNSAFE.putByte(buffer, offset + 2, (byte) (value >>> 16));
        UNSAFE.putByte(buffer, offset + 3, (byte) (value >>> 24));
        UNSAFE.putByte(buffer, offset + 4, (byte) (value >>> 32));
        UNSAFE.putByte(buffer, offset + 5, (byte) (value >>> 40));
        UNSAFE.putByte(buffer, offset + 6, (byte) (value >>> 48));
        UNSAFE.putByte(buffer, offset + 7, (byte) (value >>> 56));
        return SIZEOF_FIXED_64;
    }

    static int writeUnsafeFloat(final byte[] buffer, final long offset, final float value) {
        return writeUnsafeLittleEndian32(buffer, offset, Float.floatToIntBits(value));
    }

    static int writeUnsafeDouble(final byte[] buffer, final long offset, final double value) {
        return writeUnsafeLittleEndian64(buffer, offset, Double.doubleToLongBits(value));
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
