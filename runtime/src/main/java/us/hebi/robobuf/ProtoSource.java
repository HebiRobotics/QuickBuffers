/*-
 * #%L
 * robobuf-runtime
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

// Protocol Buffers - Google's data interchange format
// Copyright 2013 Google Inc.  All rights reserved.
// https://developers.google.com/protocol-buffers/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package us.hebi.robobuf;

import java.io.IOException;

import static us.hebi.robobuf.WireFormat.*;

/**
 * NOTE: the code was modified from Javanano's CodedInputByteBufferNano
 *
 * Reads and decodes protocol message fields.
 * <p>
 * This class contains two kinds of methods:  methods that read specific
 * protocol message constructs and field types (e.g. {@link #readTag()} and
 * {@link #readInt32()}) and methods that read low-level values (e.g.
 * {@link #readRawVarint32()} and {@link #readRawVarint64()}).  If you are reading
 * encoded protocol messages, you should use the former methods, but if you are
 * reading some other format of your own design, use the latter.
 *
 * @author kenton@google.com Kenton Varda
 * @author Florian Enner
 */
public class ProtoSource {

    /** Create a new ProtoSource wrapping the given byte array. */
    public static ProtoSource newInstance(final byte[] buf) {
        return newInstance(buf, 0, buf.length);
    }

    /** Create a new ProtoSource wrapping the given byte array slice. */
    public static ProtoSource newInstance(final byte[] buf,
                                          final int off,
                                          final int len) {
        return newSafeInstance().wrap(buf, off, len);
    }

    /**
     * Create a new {@code ProtoSource} that reads directly from a byte array.
     *
     * This method will return the fastest implementation available for the
     * current platform and may leverage features from sun.misc.Unsafe if
     * available.
     */
    public static ProtoSource newInstance() {
        return newInstance(true);
    }

    /**
     * Create a new {@code ProtoSource} that reads directly from a byte array.
     *
     * This method will return the fastest implementation available for the
     * current platform and may leverage features from sun.misc.Unsafe if
     * available.
     *
     * @param allowUnalignedAccess true if the platform supports non-aligned reads
     */
    public static ProtoSource newInstance(boolean allowUnalignedAccess) {
        if (UnsafeArraySource.isAvailable()) {
            if (allowUnalignedAccess) {
                return new UnsafeArraySource.Unaligned(false);
            } else {
                return new UnsafeArraySource(false);
            }
        }
        return newSafeInstance();
    }

    /**
     * Create a new {@code ProtoSource} that reads directly from a byte array.
     *
     * This method returns an implementation that does not leverage any
     * features from sun.misc.Unsafe, even if they are available on the
     * current platform.
     *
     * Unless you are doing comparisons you probably want to call
     * {@link #newInstance()} instead.
     */
    public static ProtoSource newSafeInstance() {
        return new ProtoSource();
    }

    /**
     * Create a new {@code ProtoSource} that reads directly from a byte array.
     *
     * This sink requires availability of sun.misc.Unsafe and Java 7 or higher.
     *
     * Additionally, this sink removes null-argument checks and allows users to
     * write to off-heap memory. Working with off-heap memory may cause segfaults
     * of the runtime, so only use if you know what you are doing.
     */
    public static ProtoSource newUnsafeInstance() {
        return newUnsafeInstance(true);
    }

    /**
     * Create a new {@code ProtoSource} that reads directly from a byte array.
     *
     * This sink requires availability of sun.misc.Unsafe and Java 7 or higher.
     *
     * Additionally, this sink removes null-argument checks and allows users to
     * write to off-heap memory. Working with off-heap memory may cause segfaults
     * of the runtime, so only use if you know what you are doing.
     *
     * @param allowUnalignedAccess true if the platform supports non-aligned reads
     */
    public static ProtoSource newUnsafeInstance(boolean allowUnalignedAccess) {
        if (allowUnalignedAccess) {
            return new UnsafeArraySource.Unaligned(true);
        } else {
            return new UnsafeArraySource(true);
        }
    }

    public final ProtoSource wrap(byte[] buffer) {
        return wrap(buffer, 0, buffer.length);
    }

    /**
     * Clears internal state and removes any references to previous inputs.
     *
     * @return this
     */
    public ProtoSource clear() {
        return wrap(ProtoUtil.EMPTY_BYTE_ARRAY);
    }

    // -----------------------------------------------------------------

    /**
     * Attempt to read a field tag, returning zero if we have reached EOF.
     * Protocol message parsers use this to read tags, since a protocol message
     * may legally end wherever a tag occurs, and zero is not a valid tag number.
     */
    public int readTag() throws IOException {
        if (isAtEnd()) {
            lastTag = 0;
            return 0;
        }

        lastTag = readRawVarint32();
        if (lastTag == 0) {
            // If we actually read zero, that's not a valid tag.
            throw InvalidProtocolBufferException.invalidTag();
        }
        return lastTag;
    }

    /**
     * Verifies that the last call to readTag() returned the given tag value.
     * This is used to verify that a nested group ended with the correct
     * end tag.
     *
     * @throws InvalidProtocolBufferException {@code value} does not match the
     *                                        last tag.
     */
    public void checkLastTagWas(final int value)
            throws InvalidProtocolBufferException {
        if (lastTag != value) {
            throw InvalidProtocolBufferException.invalidEndTag();
        }
    }

    /**
     * Reads and discards a single field, given its tag value.
     *
     * @return {@code false} if the tag is an endgroup tag, in which case
     * nothing is skipped.  Otherwise, returns {@code true}.
     */
    public boolean skipField(final int tag) throws IOException {
        switch (WireFormat.getTagWireType(tag)) {
            case WireFormat.WIRETYPE_VARINT:
                readRawVarint32();
                return true;
            case WireFormat.WIRETYPE_FIXED64:
                skipRawBytes(SIZEOF_FIXED_64);
                return true;
            case WireFormat.WIRETYPE_LENGTH_DELIMITED:
                skipRawBytes(readRawVarint32());
                return true;
            case WireFormat.WIRETYPE_START_GROUP:
                skipMessage();
                checkLastTagWas(
                        WireFormat.makeTag(WireFormat.getTagFieldNumber(tag),
                                WireFormat.WIRETYPE_END_GROUP));
                return true;
            case WireFormat.WIRETYPE_END_GROUP:
                return false;
            case WireFormat.WIRETYPE_FIXED32:
                skipRawBytes(SIZEOF_FIXED_32);
                return true;
            default:
                throw InvalidProtocolBufferException.invalidWireType();
        }
    }

    /**
     * Reads and discards an entire message.  This will read either until EOF
     * or until an endgroup tag, whichever comes first.
     */
    public void skipMessage() throws IOException {
        while (true) {
            final int tag = readTag();
            if (tag == 0 || !skipField(tag)) {
                return;
            }
        }
    }

    // -----------------------------------------------------------------

    /** Read a repeated (packed) {@code double} field value from the source. */
    public void readPackedDouble(RepeatedDouble store) throws IOException {
        final int numEntries = readRawVarint32() / SIZEOF_FIXED_64;
        store.reserve(numEntries);
        readRawDoubles(store.array, store.length, numEntries);
        store.length += numEntries;
    }

    /** Read a repeated (packed) {@code fixed64} field value from the source. */
    public void readPackedFixed64(RepeatedLong store) throws IOException {
        final int numEntries = readRawVarint32() / SIZEOF_FIXED_64;
        store.reserve(numEntries);
        readRawFixed64s(store.array, store.length, numEntries);
        store.length += numEntries;
    }

    /** Read a repeated (packed) {@code sfixed64} field value from the source. */
    public void readPackedSFixed64(RepeatedLong store) throws IOException {
        final int numEntries = readRawVarint32() / SIZEOF_FIXED_64;
        store.reserve(numEntries);
        readRawFixed64s(store.array, store.length, numEntries);
        store.length += numEntries;
    }

    /** Read a repeated (packed) {@code float} field value from the source. */
    public void readPackedFloat(RepeatedFloat store) throws IOException {
        final int numEntries = readRawVarint32() / SIZEOF_FIXED_32;
        store.reserve(numEntries);
        readRawFloats(store.array, store.length, numEntries);
        store.length += numEntries;
    }

    /** Read a repeated (packed) {@code fixed32} field value from the source. */
    public void readPackedFixed32(RepeatedInt store) throws IOException {
        final int numEntries = readRawVarint32() / SIZEOF_FIXED_32;
        store.reserve(numEntries);
        readRawFixed32s(store.array, store.length, numEntries);
        store.length += numEntries;
    }

    /** Read a repeated (packed) {@code sfixed32} field value from the source. */
    public void readPackedSFixed32(RepeatedInt store) throws IOException {
        final int numEntries = readRawVarint32() / SIZEOF_FIXED_32;
        store.reserve(numEntries);
        readRawFixed32s(store.array, store.length, numEntries);
        store.length += numEntries;
    }

    /** Read a repeated (packed) {@code bool} field value from the source. */
    public void readPackedBool(RepeatedBoolean store) throws IOException {
        final int numEntries = readRawVarint32() / SIZEOF_FIXED_BOOL;
        store.reserve(numEntries);
        for (int i = 0; i < numEntries; i++) {
            store.add(readBool());
        }
    }

    protected void readRawDoubles(double[] values, int offset, int length) throws IOException {
        final int limit = offset + length;
        for (int i = offset; i < limit; i++) {
            values[i] = readDouble();
        }
    }

    protected void readRawFloats(float[] values, int offset, int length) throws IOException {
        final int limit = offset + length;
        for (int i = offset; i < limit; i++) {
            values[i] = readFloat();
        }
    }

    protected void readRawFixed64s(long[] values, int offset, int length) throws IOException {
        final int limit = offset + length;
        for (int i = offset; i < limit; i++) {
            values[i] = readRawLittleEndian64();
        }
    }

    protected void readRawFixed32s(int[] values, int offset, int length) throws IOException {
        final int limit = offset + length;
        for (int i = offset; i < limit; i++) {
            values[i] = readRawLittleEndian32();
        }
    }

    // -----------------------------------------------------------------

    /** Read a {@code double} field value from the source. */
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readRawLittleEndian64());
    }

    /** Read a {@code float} field value from the source. */
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readRawLittleEndian32());
    }

    /** Read a {@code uint64} field value from the source. */
    public long readUInt64() throws IOException {
        return readRawVarint64();
    }

    /** Read an {@code int64} field value from the source. */
    public long readInt64() throws IOException {
        return readRawVarint64();
    }

    /** Read an {@code int32} field value from the source. */
    public int readInt32() throws IOException {
        return readRawVarint32();
    }

    /** Read a {@code fixed64} field value from the source. */
    public long readFixed64() throws IOException {
        return readRawLittleEndian64();
    }

    /** Read a {@code fixed32} field value from the source. */
    public int readFixed32() throws IOException {
        return readRawLittleEndian32();
    }

    /** Read a {@code bool} field value from the source. */
    public boolean readBool() throws IOException {
        return readRawVarint32() != 0;
    }

    /** Read a {@code string} field value from the source. */
    public void readString(StringBuilder output) throws IOException {
        final int size = readRawVarint32();
        requireRemaining(size);
        Utf8.decodeArray(buffer, bufferPos, size, output);
        bufferPos += size;
    }

    /** Read a {@code group} field value from the source. */
    public void readGroup(final ProtoMessage msg, final int fieldNumber)
            throws IOException {
        if (recursionDepth >= recursionLimit) {
            throw InvalidProtocolBufferException.recursionLimitExceeded();
        }
        ++recursionDepth;
        msg.mergeFrom(this);
        checkLastTagWas(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_END_GROUP));
        --recursionDepth;
    }

    public void readMessage(final ProtoMessage msg)
            throws IOException {
        final int length = readRawVarint32();
        if (recursionDepth >= recursionLimit) {
            throw InvalidProtocolBufferException.recursionLimitExceeded();
        }
        final int oldLimit = pushLimit(length);
        ++recursionDepth;
        msg.mergeFrom(this);
        checkLastTagWas(0);
        --recursionDepth;
        popLimit(oldLimit);
    }

    /** Read a {@code bytes} field value from the source. */
    public void readBytes(RepeatedByte store) throws IOException {
        final int numBytes = readRawVarint32();
        requireRemaining(numBytes);
        store.copyFrom(buffer, bufferPos, numBytes);
        bufferPos += numBytes;
    }

    /** Read a {@code uint32} field value from the source. */
    public int readUInt32() throws IOException {
        return readRawVarint32();
    }

    /**
     * Read an enum field value from the source.  Caller is responsible
     * for converting the numeric value to an actual enum.
     */
    public int readEnum() throws IOException {
        return readRawVarint32();
    }

    /** Read an {@code sfixed32} field value from the source. */
    public int readSFixed32() throws IOException {
        return readRawLittleEndian32();
    }

    /** Read an {@code sfixed64} field value from the source. */
    public long readSFixed64() throws IOException {
        return readRawLittleEndian64();
    }

    /** Read an {@code sint32} field value from the source. */
    public int readSInt32() throws IOException {
        return decodeZigZag32(readRawVarint32());
    }

    /** Read an {@code sint64} field value from the source. */
    public long readSInt64() throws IOException {
        return decodeZigZag64(readRawVarint64());
    }

    // =================================================================

    /**
     * Read a raw Varint from the source.  If larger than 32 bits, discard the
     * upper bits.
     */
    public int readRawVarint32() throws IOException {
        byte tmp = readRawByte();
        if (tmp >= 0) {
            return tmp;
        }
        int result = tmp & 0x7f;
        if ((tmp = readRawByte()) >= 0) {
            result |= tmp << 7;
        } else {
            result |= (tmp & 0x7f) << 7;
            if ((tmp = readRawByte()) >= 0) {
                result |= tmp << 14;
            } else {
                result |= (tmp & 0x7f) << 14;
                if ((tmp = readRawByte()) >= 0) {
                    result |= tmp << 21;
                } else {
                    result |= (tmp & 0x7f) << 21;
                    result |= (tmp = readRawByte()) << 28;
                    if (tmp < 0) {
                        // Discard upper 32 bits.
                        for (int i = 0; i < 5; i++) {
                            if (readRawByte() >= 0) {
                                return result;
                            }
                        }
                        throw InvalidProtocolBufferException.malformedVarint();
                    }
                }
            }
        }
        return result;
    }

    /** Read a raw Varint from the source. */
    public long readRawVarint64() throws IOException {
        int shift = 0;
        long result = 0;
        while (shift < 64) {
            final byte b = readRawByte();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
        throw InvalidProtocolBufferException.malformedVarint();
    }

    /** Read a 16-bit little-endian integer from the source. */
    public short readRawLittleEndian16() throws IOException {
        final byte b1 = readRawByte();
        final byte b2 = readRawByte();
        return (short) (
                ((b1 & 0xff)) |
                ((b2 & 0xff) << 8));
    }

    /** Read a 32-bit little-endian integer from the source. */
    public int readRawLittleEndian32() throws IOException {
        final byte b1 = readRawByte();
        final byte b2 = readRawByte();
        final byte b3 = readRawByte();
        final byte b4 = readRawByte();
        return ((b1 & 0xff)) |
                ((b2 & 0xff) << 8) |
                ((b3 & 0xff) << 16) |
                ((b4 & 0xff) << 24);
    }

    /** Read a 64-bit little-endian integer from the source. */
    public long readRawLittleEndian64() throws IOException {
        final byte b1 = readRawByte();
        final byte b2 = readRawByte();
        final byte b3 = readRawByte();
        final byte b4 = readRawByte();
        final byte b5 = readRawByte();
        final byte b6 = readRawByte();
        final byte b7 = readRawByte();
        final byte b8 = readRawByte();
        return (((long) b1 & 0xff)) |
                (((long) b2 & 0xff) << 8) |
                (((long) b3 & 0xff) << 16) |
                (((long) b4 & 0xff) << 24) |
                (((long) b5 & 0xff) << 32) |
                (((long) b6 & 0xff) << 40) |
                (((long) b7 & 0xff) << 48) |
                (((long) b8 & 0xff) << 56);
    }

    /**
     * Decode a ZigZag-encoded 32-bit value.  ZigZag encodes signed integers
     * into values that can be efficiently encoded with varint.  (Otherwise,
     * negative values must be sign-extended to 64 bits to be varint encoded,
     * thus always taking 10 bytes on the wire.)
     *
     * @param n An unsigned 32-bit integer, stored in a signed int because
     *          Java has no explicit unsigned support.
     * @return A signed 32-bit integer.
     */
    public static int decodeZigZag32(final int n) {
        return (n >>> 1) ^ -(n & 1);
    }

    /**
     * Decode a ZigZag-encoded 64-bit value.  ZigZag encodes signed integers
     * into values that can be efficiently encoded with varint.  (Otherwise,
     * negative values must be sign-extended to 64 bits to be varint encoded,
     * thus always taking 10 bytes on the wire.)
     *
     * @param n An unsigned 64-bit integer, stored in a signed int because
     *          Java has no explicit unsigned support.
     * @return A signed 64-bit integer.
     */
    public static long decodeZigZag64(final long n) {
        return (n >>> 1) ^ -(n & 1);
    }

    // -----------------------------------------------------------------

    protected byte[] buffer;
    protected int bufferStart;
    protected int bufferSize;
    private int bufferSizeAfterLimit;
    protected int bufferPos;
    private int lastTag;

    /** The absolute position of the end of the current message. */
    private int currentLimit = Integer.MAX_VALUE;

    /** See setRecursionLimit() */
    private int recursionDepth;
    private int recursionLimit = DEFAULT_RECURSION_LIMIT;
    private static final int DEFAULT_RECURSION_LIMIT = 64;

    /**
     * Changes the input to the given array. This resets any existing
     * internal state such as position and is equivalent to creating
     * a new instance.
     *
     * @param buffer
     * @param off
     * @param len
     * @return this
     */
    public ProtoSource wrap(byte[] buffer, long off, int len) {
        if (off < 0 || len < 0 || off > buffer.length || off + len > buffer.length)
            throw new ArrayIndexOutOfBoundsException();
        this.buffer = buffer;
        bufferStart = (int) off;
        bufferSize = bufferStart + len;
        bufferPos = bufferStart;
        return resetInternalState();
    }

    protected ProtoSource resetInternalState() {
        currentLimit = Integer.MAX_VALUE;
        bufferSizeAfterLimit = 0;
        lastTag = 0;
        recursionDepth = 0;
        return this;
    }

    protected ProtoSource() {
    }

    /**
     * Set the maximum message recursion depth.  In order to prevent malicious
     * messages from causing stack overflows, {@code ProtoSource} limits
     * how deeply messages may be nested.  The default limit is 64.
     *
     * @return the old limit.
     */
    public int setRecursionLimit(final int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException(
                    "Recursion limit cannot be negative: " + limit);
        }
        final int oldLimit = recursionLimit;
        recursionLimit = limit;
        return oldLimit;
    }

    /**
     * Sets {@code currentLimit} to (current position) + {@code byteLimit}.  This
     * is called when descending into a length-delimited embedded message.
     *
     * @return the old limit.
     */
    public int pushLimit(int byteLimit) throws InvalidProtocolBufferException {
        if (byteLimit < 0) {
            throw InvalidProtocolBufferException.negativeSize();
        }
        byteLimit += bufferPos;
        final int oldLimit = currentLimit;
        if (byteLimit > oldLimit) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        currentLimit = byteLimit;

        recomputeBufferSizeAfterLimit();

        return oldLimit;
    }

    private void recomputeBufferSizeAfterLimit() {
        bufferSize += bufferSizeAfterLimit;
        final int bufferEnd = bufferSize;
        if (bufferEnd > currentLimit) {
            // Limit is in current buffer.
            bufferSizeAfterLimit = bufferEnd - currentLimit;
            bufferSize -= bufferSizeAfterLimit;
        } else {
            bufferSizeAfterLimit = 0;
        }
    }

    /**
     * Discards the current limit, returning to the previous limit.
     *
     * @param oldLimit The old limit, as returned by {@code pushLimit}.
     */
    public void popLimit(final int oldLimit) {
        currentLimit = oldLimit;
        recomputeBufferSizeAfterLimit();
    }

    /**
     * Returns the number of bytes to be read before the current limit.
     * If no limit is set, returns -1.
     */
    public int getBytesUntilLimit() {
        if (currentLimit == Integer.MAX_VALUE) {
            return -1;
        }

        final int currentAbsolutePosition = bufferPos;
        return currentLimit - currentAbsolutePosition;
    }

    /**
     * Returns true if the source has reached the end of the input.  This is the
     * case if either the end of the underlying input source has been reached or
     * if the source has reached a limit created using {@link #pushLimit(int)}.
     */
    public boolean isAtEnd() {
        return bufferPos == bufferSize;
    }

    /** Get current position in buffer relative to beginning offset. */
    public int getPosition() {
        return bufferPos - bufferStart;
    }

    /** Rewind to previous position. Cannot go forward. */
    public void rewindToPosition(int position) {
        if (position > bufferPos - bufferStart) {
            throw new IllegalArgumentException(
                    "Position " + position + " is beyond current " + (bufferPos - bufferStart));
        }
        if (position < 0) {
            throw new IllegalArgumentException("Bad position " + position);
        }
        bufferPos = bufferStart + position;
    }

    /**
     * Read one byte from the input.
     *
     * @throws InvalidProtocolBufferException The end of the source or the current
     *                                        limit was reached.
     */
    public byte readRawByte() throws IOException {
        if (bufferPos == bufferSize) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        return buffer[bufferPos++];
    }

    /**
     * Reads and discards {@code size} bytes.
     *
     * @throws InvalidProtocolBufferException The end of the source or the current
     *                                        limit was reached.
     */
    public void skipRawBytes(final int size) throws IOException {
        requireRemaining(size);
        bufferPos += size;
    }

    protected int remaining() {
        // bufferSize is always the same as currentLimit
        // in cases where currentLimit != Integer.MAX_VALUE
        return bufferSize - bufferPos;
    }

    protected void requireRemaining(int numBytes) throws IOException {
        if (numBytes < 0) {
            throw InvalidProtocolBufferException.negativeSize();

        } else if (numBytes > remaining()) {
            // Read to the end of the current sub-message before failing
            if (numBytes > currentLimit - bufferPos) {
                bufferPos = currentLimit;
            }
            throw InvalidProtocolBufferException.truncatedMessage();
        }
    }

    /**
     * Computes the array length of a repeated field. We assume that in the common case repeated
     * fields are contiguously serialized but we still correctly handle interspersed values of a
     * repeated field (but with extra allocations).
     * <p>
     * Rewinds to current input position before returning.
     *
     * @param input source input, pointing to the byte after the first tag
     * @param tag   repeated field tag just read
     * @return length of array
     * @throws IOException
     */
    public static int getRepeatedFieldArrayLength(final ProtoSource input, final int tag) throws IOException {
        int arrayLength = 1;
        int startPos = input.getPosition();
        input.skipField(tag);
        while (input.readTag() == tag) {
            input.skipField(tag);
            arrayLength++;
        }
        input.rewindToPosition(startPos);
        return arrayLength;
    }

}
