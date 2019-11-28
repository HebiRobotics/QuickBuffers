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

import static us.hebi.quickbuf.WireFormat.*;

/**
 * Sink that writes to a flat array
 *
 * @author Florian Enner
 * @since 16 Aug 2019
 */
class ArraySink extends ProtoSink {

    @Override
    public ProtoSink wrap(byte[] buffer, long offset, int length) {
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

    protected OutOfSpaceException outOfSpace() {
        return new OutOfSpaceException(position, limit);
    }

    protected void requireSpace(final int numBytes) throws OutOfSpaceException {
        if (spaceLeft() < numBytes)
            throw outOfSpace();
    }

    /** Write a single byte. */
    @Override
    public void writeRawByte(final byte value) throws IOException {
        if (position == limit) {
            throw outOfSpace();
        }
        buffer[position++] = value;
    }

    @Override
    public void writeRawLittleEndian16(final short value) throws IOException {
        requireSpace(SIZEOF_FIXED_16);
        position += writeRawLittleEndian16(buffer, position, value);
    }

    @Override
    public void writeRawLittleEndian32(final int value) throws IOException {
        requireSpace(SIZEOF_FIXED_32);
        position += writeRawLittleEndian32(buffer, position, value);
    }

    @Override
    public void writeRawLittleEndian64(final long value) throws IOException {
        requireSpace(SIZEOF_FIXED_64);
        position += writeRawLittleEndian64(buffer, position, value);
    }

    private static int writeRawLittleEndian64(final byte[] buffer, final int offset, final long value) {
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

    private static int writeRawLittleEndian32(final byte[] buffer, final int offset, final int value) {
        buffer[offset/**/] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) (value >>> 8);
        buffer[offset + 2] = (byte) (value >>> 16);
        buffer[offset + 3] = (byte) (value >>> 24);
        return SIZEOF_FIXED_32;
    }

    private static int writeRawLittleEndian16(final byte[] buffer, final int offset, final short value) {
        buffer[offset/**/] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) (value >>> 8);
        return SIZEOF_FIXED_16;
    }

    /** Write part of an array of bytes. */
    @Override
    public void writeRawBytes(final byte[] value, int offset, int length) throws IOException {
        requireSpace(length);
        System.arraycopy(value, offset, buffer, position, length);
        position += length;
    }

    @Override
    public final void writeStringNoTag(final CharSequence value) throws IOException {
        // UTF-8 byte length of the string is at least its UTF-16 code unit length (value.length()),
        // and at most 3 times of it. Optimize for the case where we know this length results in a
        // constant varint length - saves measuring length of the string.
        try {
            final int minLengthVarIntSize = computeRawVarint32Size(value.length());
            final int maxLengthVarIntSize = computeRawVarint32Size(value.length() * Utf8.MAX_UTF8_EXPANSION);
            if (minLengthVarIntSize == maxLengthVarIntSize) {
                int startPosition = position + minLengthVarIntSize;
                int endPosition = writeUtf8Encoded(value, buffer, startPosition, spaceLeft() - minLengthVarIntSize);
                writeRawVarint32(endPosition - startPosition);
                position = endPosition;
            } else {
                writeRawVarint32(Utf8.encodedLength(value));
                position = writeUtf8Encoded(value, buffer, position, spaceLeft());
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            final OutOfSpaceException outOfSpaceException = outOfSpace();
            outOfSpaceException.initCause(e);
            throw outOfSpaceException;
        }
    }

    protected int writeUtf8Encoded(final CharSequence value, final byte[] buffer, final int position, final int maxSize) {
        return Utf8.encodeArray(value, buffer, position, maxSize);
    }

}
