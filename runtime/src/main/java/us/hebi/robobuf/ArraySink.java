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
                int endPosition = Utf8.encode(value, buffer, startPosition, spaceLeft() - minLengthVarIntSize);
                writeRawVarint32(endPosition - startPosition);
                position = endPosition;
            } else {
                writeRawVarint32(Utf8.encodedLength(value));
                position = Utf8.encode(value, buffer, position, spaceLeft());
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            final OutOfSpaceException outOfSpaceException = new OutOfSpaceException(position, limit);
            outOfSpaceException.initCause(e);
            throw outOfSpaceException;
        }
    }

}
