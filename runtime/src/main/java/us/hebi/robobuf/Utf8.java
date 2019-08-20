package us.hebi.robobuf;

import static us.hebi.robobuf.UnsafeAccess.*;

/**
 * Methods for dealing with UTF-8 copied from Guava and Protobuf,
 * and in some cases slightly modified to work with Unsafe.
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
    static int encode(CharSequence sequence, byte[] bytes, int offset, int length) {
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
                            final byte[] buffer,
                            final long offset,
                            final int position,
                            final int length) throws ProtoSink.OutOfSpaceException {
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
                throw new ProtoSink.OutOfSpaceException(position + j, position + length);
            }
        }
        return j;
    }
}
