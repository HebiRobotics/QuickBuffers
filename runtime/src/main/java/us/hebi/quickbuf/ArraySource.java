/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2022 HEBI Robotics
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
 * Source that reads from a flat array
 *
 * @author Florian Enner
 * @since 06 Mär 2022
 */
class ArraySource extends ProtoSource{

    /** Read a 16-bit little-endian integer from the source. */
    @Override
    public short readRawLittleEndian16() throws IOException {
        requireRemaining(SIZEOF_FIXED_16);
        final byte[] buffer = this.buffer;
        final int offset = bufferPos;
        bufferPos += SIZEOF_FIXED_16;
        return (short) ((buffer[offset] & 0xFF) | (buffer[offset + 1] & 0xFF) << 8);
    }

    /** Read a 32-bit little-endian integer from the source. */
    @Override
    public int readRawLittleEndian32() throws IOException {
        requireRemaining(SIZEOF_FIXED_32);
        final byte[] buffer = this.buffer;
        final int offset = bufferPos;
        bufferPos += SIZEOF_FIXED_32;
        return (buffer[offset] & 0xFF) |
                (buffer[offset + 1] & 0xFF) << 8 |
                (buffer[offset + 2] & 0xFF) << 16 |
                (buffer[offset + 3] & 0xFF) << 24;
    }

    /** Read a 64-bit little-endian integer from the source. */
    @Override
    public long readRawLittleEndian64() throws IOException {
        requireRemaining(SIZEOF_FIXED_64);
        final byte[] buffer = this.buffer;
        final int offset = bufferPos;
        bufferPos += SIZEOF_FIXED_64;
        return (buffer[offset] & 0xFFL) |
                (buffer[offset + 1] & 0xFFL) << 8 |
                (buffer[offset + 2] & 0xFFL) << 16 |
                (buffer[offset + 3] & 0xFFL) << 24 |
                (buffer[offset + 4] & 0xFFL) << 32 |
                (buffer[offset + 5] & 0xFFL) << 40 |
                (buffer[offset + 6] & 0xFFL) << 48 |
                (buffer[offset + 7] & 0xFFL) << 56;
    }

    /** Read a {@code bytes} field value from the source. */
    @Override
    public void readBytes(RepeatedByte store) throws IOException {
        final int numBytes = readRawVarint32();
        requireRemaining(numBytes);
        store.copyFrom(buffer, bufferPos, numBytes);
        bufferPos += numBytes;
    }

    @Override
    public void readString(final Utf8String store) throws IOException {
        final int numBytes = readRawVarint32();
        requireRemaining(numBytes);
        store.setSize(numBytes);
        System.arraycopy(buffer, bufferPos, store.bytes(), 0, numBytes);
        bufferPos += numBytes;
    }

    /** Read a {@code string} field value from the source. */
    @Override
    public void readString(StringBuilder output) throws IOException {
        final int size = readRawVarint32();
        requireRemaining(size);
        Utf8.decodeArray(buffer, bufferPos, size, output);
        bufferPos += size;
    }

    @Override
    public int readTagMarked() throws IOException {
        lastTagMark = bufferPos;
        return readTag();
    }

    @Override
    public void copyBytesSinceMark(RepeatedByte store) {
        store.addAll(buffer, lastTagMark, bufferPos - lastTagMark);
    }

    @Override
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
        super.resetInternalState();
        currentLimit = Integer.MAX_VALUE;
        bufferSizeAfterLimit = 0;
        lastTagMark = 0;
        return this;
    }

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

    public void popLimit(final int oldLimit) {
        currentLimit = oldLimit;
        recomputeBufferSizeAfterLimit();
    }

    public int getBytesUntilLimit() { // replace with isAtEnd in generated messages?
        if (currentLimit == Integer.MAX_VALUE) {
            return -1;
        }

        final int currentAbsolutePosition = bufferPos;
        return currentLimit - currentAbsolutePosition;
    }

    public boolean isAtEnd() {
        return bufferPos == bufferSize;
    }

    public int getPosition() {
        return bufferPos - bufferStart;
    }

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

    public byte readRawByte() throws IOException {
        if (bufferPos == bufferSize) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        return buffer[bufferPos++];
    }

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

    /** The absolute position of the end of the current message. */
    private int currentLimit = Integer.MAX_VALUE;

    protected byte[] buffer;
    protected int bufferStart;
    protected int bufferSize;
    private int bufferSizeAfterLimit;
    protected int bufferPos;

    protected int lastTagMark;

    /**
     * Computes the remaining array length of a repeated field. We assume that in the common case
     * repeated fields are contiguously serialized, but we still correctly handle interspersed values
     * of a repeated field (although with extra allocations).
     * <p>
     * Rewinds the current input position before returning. Sources that do not support
     * lookahead may return zero.
     *
     * @param tag   repeated field tag just read
     * @return length of array or zero if not available
     * @throws IOException
     */
    protected int getRemainingRepeatedFieldCount(int tag) throws IOException {
        int arrayLength = 1;
        int startPos = getPosition();
        skipField(tag);
        while (readTag() == tag) {
            skipField(tag);
            arrayLength++;
        }
        rewindToPosition(startPos);
        return arrayLength;
    }

    /**
     * Computes the array length of a packed repeated field of varint values. Packed fields
     * know the total delimited byte size, but the number of elements is unknown for variable
     * width fields. This method looks ahead to see how many varints are left until the limit
     * is reached.
     * <p>
     * Rewinds the current input position before returning. Sources that do not support
     * lookahead may return zero.
     *
     * @return length of array or zero if not available
     * @throws IOException
     */
    protected int getRemainingVarintCount() throws IOException {
        final int position = getPosition();
        int count = 0;
        while (!isAtEnd()) {
            readRawVarint32();
            count++;
        }
        rewindToPosition(position);
        return count;
    }

    @Override
    protected void reserveRepeatedFieldCapacity(RepeatedField<?, ?> store, int tag) throws IOException {
        // resize on demand and look ahead until end
        if (store.remainingCapacity() == 0) {
            store.reserve(getRemainingRepeatedFieldCount(tag));
        }
    }

    @Override
    protected void reservePackedVarintCapacity(RepeatedField<?, ?> store) throws IOException {
        // resize on demand and look ahead until end
        if (store.remainingCapacity() == 0) {
            store.reserve(getRemainingVarintCount());
        }
    }

}
