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

import static java.lang.Character.*;
import static us.hebi.quickbuf.UnsafeAccess.*;

/**
 * Methods for dealing with UTF-8 copied from Guava and Protobuf,
 * and in some cases slightly modified to work with Unsafe and
 * StringBuilder.
 */
class Utf8 {

    /**
     * Returns the number of bytes in the UTF-8-encoded form of {@code sequence}. For a string,
     * this method is equivalent to {@code string.getBytes(UTF_8).length}, but is more efficient in
     * both time and space.
     *
     * @throws IllegalArgumentException if {@code sequence} contains ill-formed UTF-16 (unpaired
     *                                  surrogates)
     */
    static int encodedLength(CharSequence sequence) {
        // Warning to maintainers: this implementation is highly optimized.
        int utf16Length = sequence.length();
        int utf8Length = utf16Length;
        int i = 0;

        // This loop optimizes for pure ASCII.
        while (i < utf16Length && sequence.charAt(i) < 0x80) {
            i++;
        }

        // This loop optimizes for chars less than 0x800.
        for (; i < utf16Length; i++) {
            char c = sequence.charAt(i);
            if (c < 0x800) {
                utf8Length += ((0x7f - c) >>> 31);  // branch free!
            } else {
                utf8Length += encodedLengthGeneral(sequence, i);
                break;
            }
        }

        if (utf8Length < utf16Length) {
            // Necessary and sufficient condition for overflow because of maximum 3x expansion
            throw new IllegalArgumentException("UTF-8 length does not fit in int: "
                    + (utf8Length + (1L << 32)));
        }
        return utf8Length;
    }

    private static int encodedLengthGeneral(CharSequence sequence, int start) {
        int utf16Length = sequence.length();
        int utf8Length = 0;
        for (int i = start; i < utf16Length; i++) {
            char c = sequence.charAt(i);
            if (c < 0x800) {
                utf8Length += (0x7f - c) >>> 31; // branch free!
            } else {
                utf8Length += 2;
                // jdk7+: if (Character.isSurrogate(c)) {
                if (Character.MIN_SURROGATE <= c && c <= Character.MAX_SURROGATE) {
                    // Check that we have a well-formed surrogate pair.
                    int cp = Character.codePointAt(sequence, i);
                    if (cp < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                        throw new IllegalArgumentException("Unpaired surrogate at index " + i);
                    }
                    i++;
                }
            }
        }
        return utf8Length;
    }

    /**
     * Encodes {@code sequence} into UTF-8, in {@code bytes}. For a string, this method is
     * equivalent to {@code ByteBuffer.setOutput(buffer, offset, length).put(string.getBytes(UTF_8))},
     * but is more efficient in both time and space. Bytes are written starting at the offset.
     * This method requires paired surrogates, and therefore does not support chunking.
     *
     * <p>To ensure sufficient space in the output buffer, either call {@link Utf8#encodedLength} to
     * compute the exact amount needed, or leave room for {@code 3 * sequence.length()}, which is the
     * largest possible number of bytes that any input can be encoded to.
     *
     * @return buffer end position, i.e., offset + written byte length
     * @throws IllegalArgumentException       if {@code sequence} contains ill-formed UTF-16 (unpaired
     *                                        surrogates)
     * @throws ArrayIndexOutOfBoundsException if {@code sequence} encoded in UTF-8 does not fit in
     *                                        {@code bytes}' remaining space.
     */
    static int encodeArray(final CharSequence sequence, final byte[] bytes, final int offset, final int length) {
        final int utf16Length = sequence.length();
        int j = offset;
        int i = 0;
        final int limit = offset + length;
        // Designed to take advantage of
        // https://wikis.oracle.com/display/HotSpotInternals/RangeCheckElimination
        for (char c; i < utf16Length && i + j < limit && (c = sequence.charAt(i)) < 0x80; i++) {
            bytes[j + i] = (byte) c;
        }
        if (i == utf16Length) {
            return j + utf16Length;
        }
        j += i;
        for (char c; i < utf16Length; i++) {
            c = sequence.charAt(i);
            if (c < 0x80 && j < limit) {
                bytes[j++] = (byte) c;
            } else if (c < 0x800 && j <= limit - 2) { // 11 bits, two UTF-8 bytes
                bytes[j++] = (byte) ((0xF << 6) | (c >>> 6));
                bytes[j++] = (byte) (0x80 | (0x3F & c));
            } else if ((c < Character.MIN_SURROGATE || Character.MAX_SURROGATE < c) && j <= limit - 3) {
                // Maximum single-char code point is 0xFFFF, 16 bits, three UTF-8 bytes
                bytes[j++] = (byte) ((0xF << 5) | (c >>> 12));
                bytes[j++] = (byte) (0x80 | (0x3F & (c >>> 6)));
                bytes[j++] = (byte) (0x80 | (0x3F & c));
            } else if (j <= limit - 4) {
                // Minimum code point represented by a surrogate pair is 0x10000, 17 bits, four UTF-8 bytes
                final char low;
                if (i + 1 == sequence.length()
                        || !Character.isSurrogatePair(c, (low = sequence.charAt(++i)))) {
                    throw new IllegalArgumentException("Unpaired surrogate at index " + (i - 1));
                }
                int codePoint = Character.toCodePoint(c, low);
                bytes[j++] = (byte) ((0xF << 4) | (codePoint >>> 18));
                bytes[j++] = (byte) (0x80 | (0x3F & (codePoint >>> 12)));
                bytes[j++] = (byte) (0x80 | (0x3F & (codePoint >>> 6)));
                bytes[j++] = (byte) (0x80 | (0x3F & codePoint));
            } else {
                throw new ArrayIndexOutOfBoundsException("Failed writing " + c + " at index " + j);
            }
        }
        return j;
    }

    /**
     * Encodes {@code sequence} into UTF-8, in {@code bytes}. For a string, this method is
     * equivalent to {@code ByteBuffer.setOutput(buffer, offset, length).put(string.getBytes(UTF_8))},
     * but is more efficient in both time and space. Bytes are written starting at the offset.
     * This method requires paired surrogates, and therefore does not support chunking.
     *
     * <p>To ensure sufficient space in the output buffer, either call {@link Utf8#encodedLength} to
     * compute the exact amount needed, or leave room for {@code 3 * sequence.length()}, which is the
     * largest possible number of bytes that any input can be encoded to.
     *
     * @return buffer end position, i.e., offset + written byte length
     * @throws IllegalArgumentException       if {@code sequence} contains ill-formed UTF-16 (unpaired
     *                                        surrogates)
     * @throws ArrayIndexOutOfBoundsException if {@code sequence} encoded in UTF-8 does not fit in
     *                                        {@code bytes}' remaining space.
     */
    static int encodeUnsafe(final CharSequence sequence,
                            final byte[] bytes,
                            final long baseOffset,
                            final int offset,
                            final int length) throws ProtoSink.OutOfSpaceException {
        int utf16Length = sequence.length();
        long j = baseOffset + offset;
        int i = 0;
        long limit = baseOffset + offset + length;
        // Designed to take advantage of
        // https://wikis.oracle.com/display/HotSpotInternals/RangeCheckElimination
        for (char c; i < utf16Length && i + j < limit && (c = sequence.charAt(i)) < 0x80; i++) {
            UNSAFE.putByte(bytes, j + i, (byte) c);
        }
        if (i == utf16Length) {
            return offset + utf16Length;
        }
        j += i;
        for (char c; i < utf16Length; i++) {
            c = sequence.charAt(i);
            if (c < 0x80 && j < limit) {
                UNSAFE.putByte(bytes, j++, (byte) c);
            } else if (c < 0x800 && j <= limit - 2) { // 11 bits, two UTF-8 bytes
                UNSAFE.putByte(bytes, j++, (byte) ((0xF << 6) | (c >>> 6)));
                UNSAFE.putByte(bytes, j++, (byte) (0x80 | (0x3F & c)));
            } else if ((c < Character.MIN_SURROGATE || Character.MAX_SURROGATE < c) && j <= limit - 3) {
                // Maximum single-char code point is 0xFFFF, 16 bits, three UTF-8 bytes
                UNSAFE.putByte(bytes, j++, (byte) ((0xF << 5) | (c >>> 12)));
                UNSAFE.putByte(bytes, j++, (byte) (0x80 | (0x3F & (c >>> 6))));
                UNSAFE.putByte(bytes, j++, (byte) (0x80 | (0x3F & c)));
            } else if (j <= limit - 4) {
                // Minimum code point represented by a surrogate pair is 0x10000, 17 bits, four UTF-8 bytes
                final char low;
                if (i + 1 == sequence.length()
                        || !Character.isSurrogatePair(c, (low = sequence.charAt(++i)))) {
                    throw new IllegalArgumentException("Unpaired surrogate at index " + (i - 1));
                }
                int codePoint = Character.toCodePoint(c, low);
                UNSAFE.putByte(bytes, j++, (byte) ((0xF << 4) | (codePoint >>> 18)));
                UNSAFE.putByte(bytes, j++, (byte) (0x80 | (0x3F & (codePoint >>> 12))));
                UNSAFE.putByte(bytes, j++, (byte) (0x80 | (0x3F & (codePoint >>> 6))));
                UNSAFE.putByte(bytes, j++, (byte) (0x80 | (0x3F & codePoint)));
            } else {
                throw new ArrayIndexOutOfBoundsException("Failed writing " + c + " at index " + j);
            }
        }
        return (int) (j - baseOffset);
    }

    static void decodeArray(byte[] bytes, int index, int size, StringBuilder result) {
        // Bitwise OR combines the sign bits so any negative value fails the check.
        if ((index | size | bytes.length - index - size) < 0) {
            throw new ArrayIndexOutOfBoundsException(
                    String.format("buffer length=%d, index=%d, size=%d", bytes.length, index, size));
        }

        int offset = index;
        final int limit = offset + size;

        // The longest possible resulting String is the same as the number of input bytes, when it is
        // all ASCII. For other cases, this over-allocates and we will truncate in the end.
        result.setLength(size);
        int resultPos = 0;

        // Optimize for 100% ASCII (Hotspot loves small simple top-level loops like this).
        // This simple loop stops when we encounter a byte >= 0x80 (i.e. non-ASCII).
        while (offset < limit) {
            byte b = bytes[offset];
            if (!DecodeUtil.isOneByte(b)) {
                break;
            }
            offset++;
            DecodeUtil.handleOneByte(b, result, resultPos++);
        }

        while (offset < limit) {
            byte byte1 = bytes[offset++];
            if (DecodeUtil.isOneByte(byte1)) {
                DecodeUtil.handleOneByte(byte1, result, resultPos++);
                // It's common for there to be multiple ASCII characters in a run mixed in, so add an
                // extra optimized loop to take care of these runs.
                while (offset < limit) {
                    byte b = bytes[offset];
                    if (!DecodeUtil.isOneByte(b)) {
                        break;
                    }
                    offset++;
                    DecodeUtil.handleOneByte(b, result, resultPos++);
                }
            } else if (DecodeUtil.isTwoBytes(byte1)) {
                if (offset >= limit) {
                    throw new IllegalArgumentException("Invalid UTF-8");
                }
                DecodeUtil.handleTwoBytes(byte1, /* byte2 */ bytes[offset++], result, resultPos++);
            } else if (DecodeUtil.isThreeBytes(byte1)) {
                if (offset >= limit - 1) {
                    throw new IllegalArgumentException("Invalid UTF-8");
                }
                DecodeUtil.handleThreeBytes(
                        byte1,
                        /* byte2 */ bytes[offset++],
                        /* byte3 */ bytes[offset++],
                        result,
                        resultPos++);
            } else {
                if (offset >= limit - 2) {
                    throw new IllegalArgumentException("Invalid UTF-8");
                }
                DecodeUtil.handleFourBytes(
                        byte1,
                        /* byte2 */ bytes[offset++],
                        /* byte3 */ bytes[offset++],
                        /* byte4 */ bytes[offset++],
                        result,
                        resultPos++);
                // 4-byte case requires two chars.
                resultPos++;
            }
        }

        result.setLength(resultPos);
    }

    static void decodeUnsafe(byte[] bytes, int bufferSize, long baseOffset, int index, int size, StringBuilder result) {
        // Bitwise OR combines the sign bits so any negative value fails the check.
        if ((index | size | bufferSize - index - size) < 0) {
            throw new ArrayIndexOutOfBoundsException(
                    String.format("buffer length=%d, index=%d, size=%d", bufferSize, index, size));
        }

        // keep separate int/long counters so we don't have to convert types at every call
        int remaining = size;
        long offset = baseOffset + index;

        // The longest possible resulting String is the same as the number of input bytes, when it is
        // all ASCII. For other cases, this over-allocates and we will truncate in the end.
        result.setLength(size);
        int resultPos = 0;

        // Optimize for 100% ASCII (Hotspot loves small simple top-level loops like this).
        // This simple loop stops when we encounter a byte >= 0x80 (i.e. non-ASCII).
        while (remaining > 0) {
            byte b = UNSAFE.getByte(bytes, offset);
            if (!DecodeUtil.isOneByte(b)) {
                break;
            }
            offset++;
            remaining--;
            DecodeUtil.handleOneByte(b, result, resultPos++);
        }

        while (remaining > 0) {
            byte byte1 = UNSAFE.getByte(bytes, offset++);
            remaining--;
            if (DecodeUtil.isOneByte(byte1)) {
                DecodeUtil.handleOneByte(byte1, result, resultPos++);
                // It's common for there to be multiple ASCII characters in a run mixed in, so add an
                // extra optimized loop to take care of these runs.
                while (remaining > 0) {
                    byte b = UNSAFE.getByte(bytes, offset);
                    if (!DecodeUtil.isOneByte(b)) {
                        break;
                    }
                    offset++;
                    remaining--;
                    DecodeUtil.handleOneByte(b, result, resultPos++);
                }
            } else if (DecodeUtil.isTwoBytes(byte1)) {
                if (remaining < 1) {
                    throw new IllegalArgumentException("Invalid UTF-8");
                }
                byte byte2 = UNSAFE.getByte(bytes, offset++);
                remaining--;
                DecodeUtil.handleTwoBytes(byte1, byte2, result, resultPos++);
            } else if (DecodeUtil.isThreeBytes(byte1)) {
                if (remaining < 2) {
                    throw new IllegalArgumentException("Invalid UTF-8");
                }
                DecodeUtil.handleThreeBytes(
                        byte1,
                        /* byte2 */ UNSAFE.getByte(bytes, offset++),
                        /* byte3 */ UNSAFE.getByte(bytes, offset++),
                        result,
                        resultPos++);
                remaining -= 2;
            } else {
                if (remaining < 3) {
                    throw new IllegalArgumentException("Invalid UTF-8");
                }
                DecodeUtil.handleFourBytes(
                        byte1,
                        /* byte2 */ UNSAFE.getByte(bytes, offset++),
                        /* byte3 */ UNSAFE.getByte(bytes, offset++),
                        /* byte4 */ UNSAFE.getByte(bytes, offset++),
                        result,
                        resultPos++);
                remaining -= 3;
                // 4-byte case requires two chars.
                resultPos++;
            }
        }

        result.setLength(resultPos);
    }

    /**
     * Utility methods for decoding bytes into {@link String}. Callers are responsible for extracting
     * bytes (possibly using Unsafe methods), and checking remaining bytes. All other UTF-8 validity
     * checks and codepoint conversion happen in this class.
     */
    static class DecodeUtil {

        /**
         * Returns whether this is a single-byte codepoint (i.e., ASCII) with the form '0XXXXXXX'.
         */
        static boolean isOneByte(byte b) {
            return b >= 0;
        }

        /**
         * Returns whether this is a two-byte codepoint with the form '10XXXXXX'.
         */
        static boolean isTwoBytes(byte b) {
            return b < (byte) 0xE0;
        }

        /**
         * Returns whether this is a three-byte codepoint with the form '110XXXXX'.
         */
        static boolean isThreeBytes(byte b) {
            return b < (byte) 0xF0;
        }

        static void handleOneByte(byte byte1, StringBuilder result, int resultPos) {
            result.setCharAt(resultPos, (char) byte1);
        }

        static void handleTwoBytes(
                byte byte1, byte byte2, StringBuilder result, int resultPos)
                throws IllegalArgumentException {
            // Simultaneously checks for illegal trailing-byte in leading position (<= '11000000') and
            // overlong 2-byte, '11000001'.
            if (byte1 < (byte) 0xC2) {
                throw new IllegalArgumentException("Invalid UTF-8: Illegal leading byte in 2 bytes utf");
            }
            if (isNotTrailingByte(byte2)) {
                throw new IllegalArgumentException("Invalid UTF-8: Illegal trailing byte in 2 bytes utf");
            }
            result.setCharAt(resultPos, (char) (((byte1 & 0x1F) << 6) | trailingByteValue(byte2)));
        }

        static void handleThreeBytes(
                byte byte1, byte byte2, byte byte3, StringBuilder result, int resultPos)
                throws IllegalArgumentException {
            if (isNotTrailingByte(byte2)
                    // overlong? 5 most significant bits must not all be zero
                    || (byte1 == (byte) 0xE0 && byte2 < (byte) 0xA0)
                    // check for illegal surrogate codepoints
                    || (byte1 == (byte) 0xED && byte2 >= (byte) 0xA0)
                    || isNotTrailingByte(byte3)) {
                throw new IllegalArgumentException("Invalid UTF-8");
            }
            result.setCharAt(resultPos, (char)
                    (((byte1 & 0x0F) << 12) | (trailingByteValue(byte2) << 6) | trailingByteValue(byte3)));
        }

        static void handleFourBytes(
                byte byte1, byte byte2, byte byte3, byte byte4, StringBuilder result, int resultPos)
                throws IllegalArgumentException {
            if (isNotTrailingByte(byte2)
                    // Check that 1 <= plane <= 16.  Tricky optimized form of:
                    //   valid 4-byte leading byte?
                    // if (byte1 > (byte) 0xF4 ||
                    //   overlong? 4 most significant bits must not all be zero
                    //     byte1 == (byte) 0xF0 && byte2 < (byte) 0x90 ||
                    //   codepoint larger than the highest code point (U+10FFFF)?
                    //     byte1 == (byte) 0xF4 && byte2 > (byte) 0x8F)
                    || (((byte1 << 28) + (byte2 - (byte) 0x90)) >> 30) != 0
                    || isNotTrailingByte(byte3)
                    || isNotTrailingByte(byte4)) {
                throw new IllegalArgumentException("Invalid UTF-8");
            }
            int codepoint = ((byte1 & 0x07) << 18)
                    | (trailingByteValue(byte2) << 12)
                    | (trailingByteValue(byte3) << 6)
                    | trailingByteValue(byte4);
            result.setCharAt(resultPos, DecodeUtil.highSurrogate(codepoint));
            result.setCharAt(resultPos + 1, DecodeUtil.lowSurrogate(codepoint));
        }

        /**
         * Returns whether the byte is not a valid continuation of the form '10XXXXXX'.
         */
        private static boolean isNotTrailingByte(byte b) {
            return b > (byte) 0xBF;
        }

        /**
         * Returns the actual value of the trailing byte (removes the prefix '10') for composition.
         */
        private static int trailingByteValue(byte b) {
            return b & 0x3F;
        }

        private static char highSurrogate(int codePoint) {
            return (char) ((MIN_HIGH_SURROGATE - (MIN_SUPPLEMENTARY_CODE_POINT >>> 10))
                    + (codePoint >>> 10));
        }

        private static char lowSurrogate(int codePoint) {
            return (char) (MIN_LOW_SURROGATE + (codePoint & 0x3ff));
        }
    }

    // These UTF-8 handling methods are copied from Guava's Utf8Unsafe class with a modification to throw
    // a protocol buffer local exception. This exception is then caught in CodedOutputStream so it can
    // fallback to more lenient behavior.
    static class UnpairedSurrogateException extends IllegalArgumentException {
        UnpairedSurrogateException(int index, int length) {
            super("Unpaired surrogate at index " + index + " of " + length);
        }
    }
}
