package us.hebi.robobuf;

import java.io.IOException;

/**
 * Sink that writes to a flat array
 *
 * @author Florian Enner
 * @since 16 Aug 2019
 */
class ArraySink extends ProtoSink {

    @Override
    public ProtoSink setOutput(byte[] buffer, long offset, int length) {
        if (offset < 0 || length < 0 || offset > buffer.length || offset + length > buffer.length)
            throw new ArrayIndexOutOfBoundsException();
        this.buffer = buffer;
        this.offset = (int) offset;
        this.limit = this.offset + length;
        this.position = this.offset;
        return this;
    }

    protected int position;
    protected byte[] buffer;
    protected int offset;
    protected int limit;

    public int position() {
        // This used to return ByteBuffer.position(), which is
        // the number of written bytes, and not the index within
        // the array.
        return position - offset;
    }

    public int spaceLeft() {
        return limit - position;
    }

    /**
     * Resets the position within the internal buffer to zero.
     *
     * @see #position
     * @see #spaceLeft
     */
    public void reset() {
        position = offset;
    }

    /** Write a single byte. */
    public void writeRawByte(final byte value) throws IOException {
        if (position >= limit) {
            throw new OutOfSpaceException(position, limit);
        }
        buffer[position++] = value;
    }

    /** Write part of an array of bytes. */
    public void writeRawBytes(final byte[] value, int offset, int length) throws IOException {
        if (spaceLeft() >= length) {
            // We have room in the current buffer.
            System.arraycopy(value, offset, buffer, position, length);
            position += length;
        } else {
            throw new OutOfSpaceException(position, limit);
        }
    }

    @Override
    public void writeStringNoTag(final CharSequence value) throws IOException {
        // UTF-8 byte length of the string is at least its UTF-16 code unit length (value.length()),
        // and at most 3 times of it. Optimize for the case where we know this length results in a
        // constant varint length - saves measuring length of the string.
        try {
            final int minLengthVarIntSize = computeRawVarint32Size(value.length());
            final int maxLengthVarIntSize = computeRawVarint32Size(value.length() * MAX_UTF8_EXPANSION);
            if (minLengthVarIntSize == maxLengthVarIntSize) {
                int startPosition = position + minLengthVarIntSize;
                int endPosition = encode(value, buffer, startPosition, spaceLeft() - minLengthVarIntSize);
                writeRawVarint32(endPosition - startPosition);
                position = endPosition;
            } else {
                writeRawVarint32(encodedLength(value));
                position = encode(value, buffer, position, spaceLeft());
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            final OutOfSpaceException outOfSpaceException = new OutOfSpaceException(position, limit);
            outOfSpaceException.initCause(e);
            throw outOfSpaceException;
        }
    }

    /**
     * (Copied from Guava's UTF-8)
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
    private static int encode(CharSequence sequence, byte[] bytes, int offset, int length) {
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

}
