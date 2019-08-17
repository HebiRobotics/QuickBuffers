package us.hebi.robobuf;

import java.io.IOException;
import java.nio.ByteOrder;

import static us.hebi.robobuf.UnsafeAccess.*;
import static us.hebi.robobuf.WireFormat.*;

/**
 * Sink that writes to an array using the sometimes
 * not-supported sun.misc.Unsafe intrinsics. Can be
 * used to write directly into a native buffer
 *
 * @author Florian Enner
 * @since 16 Aug 2019
 */
class UnsafeArraySink extends ArraySink {

    static boolean isAvailable() {
        return Platform.getJavaVersion() >= 8 && UnsafeAccess.isAvailable();
    }


    UnsafeArraySink(boolean enableDirect) {
        if (!isAvailable())
            throw new AssertionError("UnsafeArraySink requires Java >= 8 and access to sun.misc.Unsafe");
        this.enableDirect = enableDirect;
    }

    @Override
    public ProtoSink setOutput(byte[] buffer, long offset, int length) {
        if (!enableDirect || buffer != null) {
            return super.setOutput(buffer, offset, length);
        }
        if (offset == 0) {
            throw new NullPointerException("null reference with 0 direct offset");
        }
        this.baseOffset = offset;
        this.offset = 0;
        this.position = 0;
        this.limit = length;
        return this;
    }

    // offset to bytes in memory
    private final boolean enableDirect;
    private long baseOffset = BYTE_ARRAY_OFFSET;

    private void requireSpace(final int numBytes) throws OutOfSpaceException {
        if (spaceLeft() < numBytes)
            throw new OutOfSpaceException(position, limit);
    }

    @Override
    protected void writeRawFloats(final float[] values, final int length) throws IOException {
        final int numBytes = length * SIZEOF_FIXED_32;
        requireSpace(numBytes);
        if (IS_LITTLE_ENDIAN) {
            UNSAFE.copyMemory(values, FLOAT_ARRAY_OFFSET, buffer, baseOffset + position, numBytes);
            position += numBytes;
        } else {
            for (int i = 0; i < length; i++) {
                final int value = Integer.reverseBytes(Float.floatToIntBits(values[i]));
                UNSAFE.putLong(buffer, baseOffset + position, value);
                position += SIZEOF_FIXED_32;
            }
        }
    }

    @Override
    protected void writeRawFixed32s(final int[] values, final int length) throws IOException {
        final int numBytes = length * SIZEOF_FIXED_32;
        requireSpace(numBytes);
        if (IS_LITTLE_ENDIAN) {
            UNSAFE.copyMemory(values, INT_ARRAY_OFFSET, buffer, baseOffset + position, numBytes);
            position += numBytes;
        } else {
            for (int i = 0; i < length; i++) {
                final int value = Integer.reverseBytes(values[i]);
                UNSAFE.putLong(buffer, baseOffset + position, value);
                position += SIZEOF_FIXED_32;
            }
        }
    }

    @Override
    protected void writeRawFixed64s(final long[] values, final int length) throws IOException {
        final int numBytes = length * SIZEOF_FIXED_64;
        requireSpace(numBytes);
        if (IS_LITTLE_ENDIAN) {
            UNSAFE.copyMemory(values, LONG_ARRAY_OFFSET, buffer, baseOffset + position, numBytes);
            position += numBytes;
        } else {
            for (int i = 0; i < length; i++) {
                final long value = Long.reverseBytes(values[i]);
                UNSAFE.putLong(buffer, baseOffset + position, value);
                position += SIZEOF_FIXED_64;
            }
        }
    }

    @Override
    protected void writeRawDoubles(final double[] values, final int length) throws IOException {
        final int numBytes = length * SIZEOF_FIXED_64;
        requireSpace(numBytes);
        if (IS_LITTLE_ENDIAN) {
            UNSAFE.copyMemory(values, DOUBLE_ARRAY_OFFSET, buffer, baseOffset + position, numBytes);
            position += numBytes;
        } else {
            for (int i = 0; i < length; i++) {
                final long value = Long.reverseBytes(Double.doubleToLongBits(values[i]));
                UNSAFE.putLong(buffer, baseOffset + position, value);
                position += SIZEOF_FIXED_64;
            }
        }
    }

    @Override
    protected void writeRawBooleans(final boolean[] values, final int numBytes) throws IOException {
        requireSpace(numBytes);
        UNSAFE.copyMemory(values, BOOLEAN_ARRAY_OFFSET, buffer, baseOffset + position, numBytes);
        position += numBytes;
    }

    @Override
    public final void writeRawLittleEndian32(final int value) throws IOException {
        requireSpace(LITTLE_ENDIAN_32_SIZE);
        UNSAFE.putInt(buffer, baseOffset + position, IS_LITTLE_ENDIAN ? value : Integer.reverseBytes(value));
        position += LITTLE_ENDIAN_32_SIZE;
    }

    @Override
    public final void writeRawLittleEndian64(final long value) throws IOException {
        requireSpace(LITTLE_ENDIAN_64_SIZE);
        UNSAFE.putLong(buffer, baseOffset + position, IS_LITTLE_ENDIAN ? value : Long.reverseBytes(value));
        position += LITTLE_ENDIAN_64_SIZE;
    }

    @Override
    public void writeStringNoTag(final CharSequence value) throws IOException {
        // UTF-8 byte length of the string is at least its UTF-16 code unit length (value.length()),
        // and at most 3 times of it. Optimize for the case where we know this length results in a
        // constant varint length - saves measuring length of the string.
        final int minLengthVarIntSize = computeRawVarint32Size(value.length());
        final int maxLengthVarIntSize = computeRawVarint32Size(value.length() * MAX_UTF8_EXPANSION);
        if (minLengthVarIntSize == maxLengthVarIntSize) {
            int startPosition = position + minLengthVarIntSize;
            int endPosition = encode(value, buffer, baseOffset, startPosition, spaceLeft() - minLengthVarIntSize);
            writeRawVarint32(endPosition - startPosition);
            position = endPosition;
        } else {
            writeRawVarint32(encodedLength(value));
            position = encode(value, buffer, baseOffset, position, spaceLeft());
        }
    }

    /**
     * (Modified from Guava's UTF-8)
     *
     * Encodes {@code sequence} into UTF-8, in {@code bytes}. For a string, this method is
     * equivalent to {@code ByteBuffer.setOutput(buffer, offset, length).put(string.getBytes(UTF_8))},
     * but is more efficient in both time and space. Bytes are written starting at the offset.
     * This method requires paired surrogates, and therefore does not support chunking.
     *
     * <p>To ensure sufficient space in the output buffer, either call {@link #encodedLength} to
     * compute the exact amount needed, or leave room for {@code 3 * sequence.length()}, which is the
     * largest possible number of bytes that any input can be encoded to.
     *
     * @return buffer end position, i.e., offset + written byte length
     * @throws IllegalArgumentException       if {@code sequence} contains ill-formed UTF-16 (unpaired
     *                                        surrogates)
     * @throws ArrayIndexOutOfBoundsException if {@code sequence} encoded in UTF-8 does not fit in
     *                                        {@code bytes}' remaining space.
     */
    private static int encode(final CharSequence sequence,
                              final byte[] buffer,
                              final long offset,
                              final int position, final
                              int length) throws OutOfSpaceException {
        int utf16Length = sequence.length();
        int j = position;
        int i = 0;
        int limit = position + length;
        // Designed to take advantage of
        // https://wikis.oracle.com/display/HotSpotInternals/RangeCheckElimination
        for (char c; i < utf16Length && i + j < limit && (c = sequence.charAt(i)) < 0x80; i++) {
            UNSAFE.putByte(buffer, offset + j + i, (byte) c);
        }
        if (i == utf16Length) {
            return j + utf16Length;
        }
        j += i;
        for (char c; i < utf16Length; i++) {
            c = sequence.charAt(i);
            if (c < 0x80 && j < limit) {
                UNSAFE.putByte(buffer, offset + j++, (byte) c);
            } else if (c < 0x800 && j <= limit - 2) { // 11 bits, two UTF-8 bytes
                UNSAFE.putByte(buffer, offset + j++, (byte) ((0xF << 6) | (c >>> 6)));
                UNSAFE.putByte(buffer, offset + j++, (byte) (0x80 | (0x3F & c)));
            } else if ((c < Character.MIN_SURROGATE || Character.MAX_SURROGATE < c) && j <= limit - 3) {
                // Maximum single-char code point is 0xFFFF, 16 bits, three UTF-8 bytes
                UNSAFE.putByte(buffer, offset + j++, (byte) ((0xF << 5) | (c >>> 12)));
                UNSAFE.putByte(buffer, offset + j++, (byte) (0x80 | (0x3F & (c >>> 6))));
                UNSAFE.putByte(buffer, offset + j++, (byte) (0x80 | (0x3F & c)));
            } else if (j <= limit - 4) {
                // Minimum code point represented by a surrogate pair is 0x10000, 17 bits, four UTF-8 bytes
                final char low;
                if (i + 1 == sequence.length()
                        || !Character.isSurrogatePair(c, (low = sequence.charAt(++i)))) {
                    throw new IllegalArgumentException("Unpaired surrogate at index " + (i - 1));
                }
                int codePoint = Character.toCodePoint(c, low);
                UNSAFE.putByte(buffer, offset + j++, (byte) ((0xF << 4) | (codePoint >>> 18)));
                UNSAFE.putByte(buffer, offset + j++, (byte) (0x80 | (0x3F & (codePoint >>> 12))));
                UNSAFE.putByte(buffer, offset + j++, (byte) (0x80 | (0x3F & (codePoint >>> 6))));
                UNSAFE.putByte(buffer, offset + j++, (byte) (0x80 | (0x3F & codePoint)));
            } else {
                throw new OutOfSpaceException(position + j, position + length);
            }
        }
        return j;
    }

    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;


}
