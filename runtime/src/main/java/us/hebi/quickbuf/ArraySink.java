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
    public ArraySink reset() {
        position = offset;
        return this;
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
        position += ByteUtil.writeLittleEndian16(buffer, position, value);
    }

    @Override
    public void writeRawLittleEndian32(final int value) throws IOException {
        requireSpace(SIZEOF_FIXED_32);
        position += ByteUtil.writeLittleEndian32(buffer, position, value);
    }

    @Override
    public void writeRawLittleEndian64(final long value) throws IOException {
        requireSpace(SIZEOF_FIXED_64);
        position += ByteUtil.writeLittleEndian64(buffer, position, value);
    }

    @Override
    public void writeFloatNoTag(final float value) throws IOException {
        requireSpace(SIZEOF_FIXED_32);
        position += ByteUtil.writeFloat(buffer, position, value);
    }

    @Override
    public void writeDoubleNoTag(final double value) throws IOException {
        requireSpace(SIZEOF_FIXED_64);
        position += ByteUtil.writeDouble(buffer, position, value);
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
