/*-
 * #%L
 * quickbuf-runtime
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

package us.hebi.quickbuf;

import java.io.IOException;

import static us.hebi.quickbuf.UnsafeAccess.*;
import static us.hebi.quickbuf.WireFormat.*;

/**
 * Sink that writes to an array using the potentially
 * unsupported (e.g. Android) sun.misc.Unsafe intrinsics.
 * Can be used to write directly into a native buffer.
 *
 * @author Florian Enner
 * @since 16 Aug 2019
 */
class UnsafeArraySink extends ArraySink {

    static boolean isAvailable() {
        return UnsafeAccess.isAvailable() && UnsafeAccess.isCopyMemoryAvailable();
    }

    UnsafeArraySink(boolean enableDirect) {
        if (!isAvailable())
            throw new AssertionError("UnsafeArraySink requires Java >= 7 and access to sun.misc.Unsafe");
        this.enableDirect = enableDirect;
    }

    @Override
    public ProtoSink wrap(byte[] buffer, long offset, int length) {
        if (!enableDirect || buffer != null) {
            baseOffset = BYTE_ARRAY_OFFSET;
            return super.wrap(buffer, offset, length);
        }
        if (offset <= 0) {
            throw new NullPointerException("null reference with invalid address offset");
        }
        this.buffer = null;
        this.baseOffset = offset;
        this.offset = 0;
        this.position = 0;
        this.limit = length;
        return this;
    }

    private final boolean enableDirect;
    long baseOffset;

    @Override
    protected void writeRawBooleans(final boolean[] values, final int length) throws IOException {
        requireSpace(length);
        UNSAFE.copyMemory(values, BOOLEAN_ARRAY_OFFSET, buffer, baseOffset + position, length);
        position += length;
    }

    @Override
    public void writeRawByte(final byte value) throws IOException {
        if (position == limit) {
            throw outOfSpace();
        }
        UNSAFE.putByte(buffer, baseOffset + position++, value);
    }

    @Override
    public void writeRawBytes(final byte[] value, int offset, int length) throws IOException {
        requireSpace(length);
        UNSAFE.copyMemory(value, BYTE_ARRAY_OFFSET + offset, buffer, baseOffset + position, length);
        position += length;
    }

    @Override
    public void writeRawLittleEndian16(final short value) throws IOException {
        requireSpace(SIZEOF_FIXED_16);
        position += writeRawLittleEndian16(buffer, baseOffset + position, value);
    }

    @Override
    public void writeRawLittleEndian32(final int value) throws IOException {
        requireSpace(SIZEOF_FIXED_32);
        position += writeRawLittleEndian32(buffer, baseOffset + position, value);
    }

    @Override
    public void writeRawLittleEndian64(final long value) throws IOException {
        requireSpace(SIZEOF_FIXED_64);
        position += writeRawLittleEndian64(buffer, baseOffset + position, value);
    }

    @Override
    protected int writeUtf8Encoded(final CharSequence value, final byte[] buffer, final int position, final int maxSize) {
        return Utf8.encodeUnsafe(value, buffer, baseOffset, position, maxSize);
    }

    private static int writeRawLittleEndian64(final byte[] buffer, final long offset, final long value) {
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

    private static int writeRawLittleEndian32(final byte[] buffer, final long offset, final int value) {
        UNSAFE.putByte(buffer, offset, (byte) (value & 0xFF));
        UNSAFE.putByte(buffer, offset + 1, (byte) (value >>> 8));
        UNSAFE.putByte(buffer, offset + 2, (byte) (value >>> 16));
        UNSAFE.putByte(buffer, offset + 3, (byte) (value >>> 24));
        return SIZEOF_FIXED_32;
    }

    private static int writeRawLittleEndian16(final byte[] buffer, final long offset, final short value) {
        UNSAFE.putByte(buffer, offset, (byte) (value & 0xFF));
        UNSAFE.putByte(buffer, offset + 1, (byte) (value >>> 8));
        return SIZEOF_FIXED_16;
    }

    /**
     * Subclass that adds additional performance improvements for platforms
     * that support non-aligned access (e.g. writing an int to an address
     * that is not a multiple of 4). Due to the various 1 byte field tags, the
     * alignment is basically random and it's not worth checking for alignment.
     */
    static class Unaligned extends UnsafeArraySink {

        Unaligned(boolean enableDirect) {
            super(enableDirect);
        }

        @Override
        protected void writeRawFloats(final float[] values, final int length) throws IOException {
            final int numBytes = length * SIZEOF_FIXED_32;
            requireSpace(numBytes);
            if (IS_LITTLE_ENDIAN) {
                UNSAFE.copyMemory(values, FLOAT_ARRAY_OFFSET, buffer, baseOffset + position, numBytes);
            } else {
                long offset = baseOffset + position;
                for (int i = 0; i < length; i++, offset += SIZEOF_FIXED_32) {
                    final int bits = Integer.reverse(Float.floatToIntBits(values[i]));
                    UNSAFE.putInt(buffer, offset, bits);
                }
            }
            position += numBytes;
        }

        @Override
        protected void writeRawFixed32s(final int[] values, final int length) throws IOException {
            final int numBytes = length * SIZEOF_FIXED_32;
            requireSpace(numBytes);
            if (IS_LITTLE_ENDIAN) {
                UNSAFE.copyMemory(values, INT_ARRAY_OFFSET, buffer, baseOffset + position, numBytes);
            } else {
                long offset = baseOffset + position;
                for (int i = 0; i < length; i++, offset += SIZEOF_FIXED_32) {
                    final int bits = Integer.reverse(values[i]);
                    UNSAFE.putInt(buffer, offset, bits);
                }
            }
            position += numBytes;
        }

        @Override
        protected void writeRawDoubles(final double[] values, final int length) throws IOException {
            final int numBytes = length * SIZEOF_FIXED_64;
            requireSpace(numBytes);
            if (IS_LITTLE_ENDIAN) {
                UNSAFE.copyMemory(values, DOUBLE_ARRAY_OFFSET, buffer, baseOffset + position, numBytes);
            } else {
                long offset = baseOffset + position;
                for (int i = 0; i < length; i++, offset += SIZEOF_FIXED_64) {
                    final long bits = Long.reverse(Double.doubleToLongBits(values[i]));
                    UNSAFE.putLong(buffer, offset, bits);
                }
            }
            position += numBytes;
        }

        @Override
        protected void writeRawFixed64s(final long[] values, final int length) throws IOException {
            final int numBytes = length * SIZEOF_FIXED_64;
            requireSpace(numBytes);
            if (IS_LITTLE_ENDIAN) {
                UNSAFE.copyMemory(values, LONG_ARRAY_OFFSET, buffer, baseOffset + position, numBytes);
            } else {
                long offset = baseOffset + position;
                for (int i = 0; i < length; i++, offset += SIZEOF_FIXED_64) {
                    final long bits = Long.reverse(values[i]);
                    UNSAFE.putLong(buffer, offset, bits);
                }
            }
            position += numBytes;
        }

        @Override
        public void writeDoubleNoTag(double value) throws IOException {
            if (IS_LITTLE_ENDIAN) {
                requireSpace(SIZEOF_FIXED_64);
                UNSAFE.putDouble(buffer, baseOffset + position, value);
                position += SIZEOF_FIXED_64;
            } else {
                writeRawLittleEndian64(Double.doubleToLongBits(value));
            }
        }

        @Override
        public void writeFloatNoTag(float value) throws IOException {
            if (IS_LITTLE_ENDIAN) {
                requireSpace(SIZEOF_FIXED_32);
                UNSAFE.putFloat(buffer, baseOffset + position, value);
                position += SIZEOF_FIXED_32;
            } else {
                writeRawLittleEndian32(Float.floatToIntBits(value));
            }
        }

        @Override
        public final void writeRawLittleEndian16(final short value) throws IOException {
            requireSpace(SIZEOF_FIXED_16);
            UNSAFE.putShort(buffer, baseOffset + position, IS_LITTLE_ENDIAN ? value : Short.reverseBytes(value));
            position += SIZEOF_FIXED_16;
        }

        @Override
        public final void writeRawLittleEndian32(final int value) throws IOException {
            requireSpace(SIZEOF_FIXED_32);
            UNSAFE.putInt(buffer, baseOffset + position, IS_LITTLE_ENDIAN ? value : Integer.reverseBytes(value));
            position += SIZEOF_FIXED_32;
        }

        @Override
        public final void writeRawLittleEndian64(final long value) throws IOException {
            requireSpace(SIZEOF_FIXED_64);
            UNSAFE.putLong(buffer, baseOffset + position, IS_LITTLE_ENDIAN ? value : Long.reverseBytes(value));
            position += SIZEOF_FIXED_64;
        }

    }

}
