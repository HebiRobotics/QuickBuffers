/*-
 * #%L
 * quickbuf-runtime
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

package us.hebi.quickbuf;

import java.io.IOException;
import java.nio.ByteOrder;

import static us.hebi.quickbuf.UnsafeAccess.*;
import static us.hebi.quickbuf.WireFormat.*;

/**
 * Source that reads from an array using the potentially
 * unsupported (e.g. Android) sun.misc.Unsafe intrinsics.
 * Can be used to read directly from a native buffer.
 *
 * @author Florian Enner
 * @since 20 Aug 2019
 */
class UnsafeArraySource extends ProtoSource {

    static boolean isAvailable() {
        return UnsafeAccess.isAvailable() && UnsafeAccess.isCopyMemoryAvailable();
    }

    UnsafeArraySource(boolean enableDirect) {
        if (!isAvailable())
            throw new AssertionError("UnsafeArraySource requires Java >= 7 and access to sun.misc.Unsafe");
        this.enableDirect = enableDirect;
    }

    @Override
    public ProtoSource wrap(byte[] buffer, long offset, int length) {
        if (!enableDirect || buffer != null) {
            baseOffset = BYTE_ARRAY_OFFSET;
            return super.wrap(buffer, offset, length);
        }
        if (offset <= 0) {
            throw new NullPointerException("null reference with invalid address offset");
        }
        this.buffer = null;
        baseOffset = offset;
        bufferStart = 0;
        bufferSize = length;
        bufferPos = 0;
        return resetInternalState();
    }

    private final boolean enableDirect;
    long baseOffset;

    @Override
    public void copyBytesSinceMark(RepeatedByte store) {
        final int length = bufferPos - lastTagMark;
        final int bufferPos = store.addLength(length);
        UNSAFE.copyMemory(buffer, baseOffset + lastTagMark, store.array, BYTE_ARRAY_OFFSET + bufferPos, length);
    }

    @Override
    public void readString(StringBuilder output) throws IOException {
        final int size = readRawVarint32();
        requireRemaining(size);
        Utf8.decodeUnsafe(buffer, bufferSize, baseOffset, bufferPos, size, output);
        bufferPos += size;
    }

    public void readString(final Utf8String store) throws IOException {
        final int numBytes = readRawVarint32();
        requireRemaining(numBytes);
        store.setSerializedSize(numBytes);
        UNSAFE.copyMemory(buffer, baseOffset + bufferPos, store.getBytes(), BYTE_ARRAY_OFFSET, numBytes);
        bufferPos += numBytes;
    }

    @Override
    public void readBytes(RepeatedByte store) throws IOException {
        final int numBytes = readRawVarint32();
        requireRemaining(numBytes);
        store.reserve(numBytes);
        store.length = numBytes;
        UNSAFE.copyMemory(buffer, baseOffset + bufferPos, store.array, BYTE_ARRAY_OFFSET, numBytes);
        bufferPos += numBytes;
    }

    @Override
    public byte readRawByte() throws IOException {
        if (bufferPos == bufferSize) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        return UNSAFE.getByte(buffer, baseOffset + bufferPos++);
    }

    /** Read a 32-bit little-endian integer from the source. */
    public int readRawLittleEndian32() throws IOException {
        requireRemaining(LITTLE_ENDIAN_32_SIZE);
        final byte[] buffer = this.buffer;
        final long offset = baseOffset + bufferPos;
        bufferPos += LITTLE_ENDIAN_32_SIZE;
        return (UNSAFE.getByte(buffer, offset) & 0xFF) |
                (UNSAFE.getByte(buffer, offset + 1) & 0xFF) << 8 |
                (UNSAFE.getByte(buffer, offset + 2) & 0xFF) << 16 |
                (UNSAFE.getByte(buffer, offset + 3) & 0xFF) << 24;
    }

    /** Read a 64-bit little-endian integer from the source. */
    public long readRawLittleEndian64() throws IOException {
        requireRemaining(LITTLE_ENDIAN_64_SIZE);
        final byte[] buffer = this.buffer;
        final long offset = baseOffset + bufferPos;
        bufferPos += LITTLE_ENDIAN_64_SIZE;
        return (UNSAFE.getByte(buffer, offset) & 0xFFL) |
                (UNSAFE.getByte(buffer, offset + 1) & 0xFFL) << 8 |
                (UNSAFE.getByte(buffer, offset + 2) & 0xFFL) << 16 |
                (UNSAFE.getByte(buffer, offset + 3) & 0xFFL) << 24 |
                (UNSAFE.getByte(buffer, offset + 4) & 0xFFL) << 32 |
                (UNSAFE.getByte(buffer, offset + 5) & 0xFFL) << 40 |
                (UNSAFE.getByte(buffer, offset + 6) & 0xFFL) << 48 |
                (UNSAFE.getByte(buffer, offset + 7) & 0xFFL) << 56;
    }

    /**
     * Subclass that adds additional performance improvements for platforms
     * that support non-aligned access (e.g. reading an int from an address
     * that is not a multiple of 4). Due to the various 1 byte field tags, the
     * alignment is basically random and it's not worth checking for alignment.
     */
    static class Unaligned extends UnsafeArraySource {

        Unaligned(boolean enableDirect) {
            super(enableDirect);
        }

        @Override
        protected void readRawDoubles(double[] values, int offset, int length) throws IOException {
            final int numBytes = length * SIZEOF_FIXED_64;
            requireRemaining(numBytes);
            if (IS_LITTLE_ENDIAN) {
                final long targetOffset = DOUBLE_ARRAY_OFFSET + SIZEOF_FIXED_64 * offset;
                UNSAFE.copyMemory(buffer, baseOffset + bufferPos, values, targetOffset, numBytes);
                bufferPos += numBytes;
            } else {
                for (int i = 0; i < length; i++) {
                    final long bits = UNSAFE.getLong(buffer, baseOffset + bufferPos);
                    values[offset + i] = Double.longBitsToDouble(Long.reverseBytes(bits));
                    bufferPos += SIZEOF_FIXED_64;
                }
            }
        }

        @Override
        protected void readRawFloats(float[] values, int offset, int length) throws IOException {
            final int numBytes = length * SIZEOF_FIXED_32;
            requireRemaining(numBytes);
            if (IS_LITTLE_ENDIAN) {
                final long targetOffset = FLOAT_ARRAY_OFFSET + SIZEOF_FIXED_32 * offset;
                UNSAFE.copyMemory(buffer, baseOffset + bufferPos, values, targetOffset, numBytes);
                bufferPos += numBytes;
            } else {
                for (int i = 0; i < length; i++) {
                    final int bits = UNSAFE.getInt(buffer, baseOffset + bufferPos);
                    values[offset + i] = Float.intBitsToFloat(Integer.reverseBytes(bits));
                    bufferPos += SIZEOF_FIXED_32;
                }
            }
        }

        @Override
        protected void readRawFixed64s(long[] values, int offset, int length) throws IOException {
            final int numBytes = length * SIZEOF_FIXED_64;
            requireRemaining(numBytes);
            if (IS_LITTLE_ENDIAN) {
                final long targetOffset = INT_ARRAY_OFFSET + SIZEOF_FIXED_64 * offset;
                UNSAFE.copyMemory(buffer, baseOffset + bufferPos, values, targetOffset, numBytes);
                bufferPos += numBytes;
            } else {
                for (int i = 0; i < length; i++) {
                    final long bits = UNSAFE.getLong(buffer, baseOffset + bufferPos);
                    values[offset + i] = Long.reverseBytes(bits);
                    bufferPos += SIZEOF_FIXED_64;
                }
            }
        }

        @Override
        protected void readRawFixed32s(int[] values, int offset, int length) throws IOException {
            final int numBytes = length * SIZEOF_FIXED_32;
            requireRemaining(numBytes);
            if (IS_LITTLE_ENDIAN) {
                final long targetOffset = INT_ARRAY_OFFSET + SIZEOF_FIXED_32 * offset;
                UNSAFE.copyMemory(buffer, baseOffset + bufferPos, values, targetOffset, numBytes);
                bufferPos += numBytes;
            } else {
                for (int i = 0; i < length; i++) {
                    final int bits = UNSAFE.getInt(buffer, baseOffset + bufferPos);
                    values[offset + i] = Integer.reverseBytes(bits);
                    bufferPos += SIZEOF_FIXED_32;
                }
            }
        }

        @Override
        public double readDouble() throws IOException {
            if (IS_LITTLE_ENDIAN) {
                requireRemaining(LITTLE_ENDIAN_64_SIZE);
                final double value = UNSAFE.getDouble(buffer, baseOffset + bufferPos);
                bufferPos += LITTLE_ENDIAN_64_SIZE;
                return value;
            } else {
                return Double.longBitsToDouble(readRawLittleEndian64());
            }
        }

        @Override
        public float readFloat() throws IOException {
            if (IS_LITTLE_ENDIAN) {
                requireRemaining(LITTLE_ENDIAN_32_SIZE);
                final float value = UNSAFE.getFloat(buffer, baseOffset + bufferPos);
                bufferPos += LITTLE_ENDIAN_32_SIZE;
                return value;
            } else {
                return Float.intBitsToFloat(readRawLittleEndian32());
            }
        }

        @Override
        public short readRawLittleEndian16() throws IOException {
            requireRemaining(SIZEOF_FIXED_16);
            final short value = UNSAFE.getShort(buffer, baseOffset + bufferPos);
            bufferPos += SIZEOF_FIXED_16;
            return IS_LITTLE_ENDIAN ? value : Short.reverseBytes(value);
        }

        @Override
        public int readRawLittleEndian32() throws IOException {
            requireRemaining(LITTLE_ENDIAN_32_SIZE);
            final int value = UNSAFE.getInt(buffer, baseOffset + bufferPos);
            bufferPos += LITTLE_ENDIAN_32_SIZE;
            return IS_LITTLE_ENDIAN ? value : Integer.reverseBytes(value);
        }

        @Override
        public long readRawLittleEndian64() throws IOException {
            requireRemaining(LITTLE_ENDIAN_64_SIZE);
            final long value = UNSAFE.getLong(buffer, baseOffset + bufferPos);
            bufferPos += LITTLE_ENDIAN_64_SIZE;
            return IS_LITTLE_ENDIAN ? value : Long.reverseBytes(value);
        }

        private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    }

}
