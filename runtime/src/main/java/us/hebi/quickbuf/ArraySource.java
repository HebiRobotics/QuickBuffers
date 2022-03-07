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

    /** Reads one byte from the input. */
    @Override
    public byte readRawByte() throws IOException {
        if (pos == limit) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        return buffer[pos++];
    }

    /** Read a 16-bit little-endian integer from the source. */
    @Override
    public short readRawLittleEndian16() throws IOException {
        return ByteUtil.readLittleEndian16(buffer, require(SIZEOF_FIXED_16));
    }

    /** Read a 32-bit little-endian integer from the source. */
    @Override
    public int readRawLittleEndian32() throws IOException {
        return ByteUtil.readLittleEndian32(buffer, require(SIZEOF_FIXED_32));
    }

    /** Read a 64-bit little-endian integer from the source. */
    @Override
    public long readRawLittleEndian64() throws IOException {
        return ByteUtil.readLittleEndian64(buffer, require(SIZEOF_FIXED_64));
    }

    /** Read a {@code float} field value from the source. */
    public float readFloat() throws IOException {
        return ByteUtil.readFloat(buffer, require(SIZEOF_FIXED_32));
    }

    /** Read a {@code double} field value from the source. */
    public double readDouble() throws IOException {
        return ByteUtil.readDouble(buffer, require(SIZEOF_FIXED_64));
    }

    @Override
    public void readRawBytes(byte[] values, int offset, int length) throws IOException {
        ByteUtil.readBytes(buffer, require(length), values, offset, length);
    }

    @Override
    protected void readRawFixed32s(int[] values, int offset, int length) throws IOException {
        ByteUtil.readLittleEndian32s(buffer, require(length * SIZEOF_FIXED_32), values, offset, length);
    }

    @Override
    protected void readRawFixed64s(long[] values, int offset, int length) throws IOException {
        ByteUtil.readLittleEndian64s(buffer, require(length * SIZEOF_FIXED_64), values, offset, length);
    }

    @Override
    protected void readRawFloats(float[] values, int offset, int length) throws IOException {
        ByteUtil.readFloats(buffer, require(length * SIZEOF_FIXED_32), values, offset, length);
    }

    @Override
    protected void readRawDoubles(double[] values, int offset, int length) throws IOException {
        ByteUtil.readDoubles(buffer, require(length * SIZEOF_FIXED_64), values, offset, length);
    }

    @Override
    public void skipRawBytes(final int size) throws IOException {
        require(size);
    }

    @Override
    public int readTagMarked() throws IOException {
        lastTagMark = pos;
        return readTag();
    }

    @Override
    public void copyBytesSinceMark(RepeatedByte store) {
        store.addAll(buffer, lastTagMark, pos - lastTagMark);
    }

    @Override
    public ProtoSource wrap(byte[] buffer, long off, int len) {
        if (off < 0 || len < 0 || off > buffer.length || off + len > buffer.length)
            throw new ArrayIndexOutOfBoundsException();
        this.buffer = buffer;
        offset = (int) off;
        limit = offset + len;
        pos = offset;
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
        byteLimit += pos;
        if (byteLimit > limit) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        final int oldLimit = currentLimit;
        currentLimit = byteLimit;
        recomputeBufferSizeAfterLimit();
        return oldLimit;
    }

    public void popLimit(final int oldLimit) {
        currentLimit = oldLimit;
        recomputeBufferSizeAfterLimit();
    }

    public int getBytesUntilLimit() {
        if (currentLimit == Integer.MAX_VALUE) {
            return -1;
        }
        return currentLimit - pos;
    }

    public boolean isAtEnd() {
        return pos == limit;
    }

    public int getTotalBytesRead() {
        return pos - offset;
    }

    protected void rewindToPosition(int position) throws InvalidProtocolBufferException {
        pos = offset + position;
    }

    protected int remaining() {
        // limit is always the same as currentLimit
        // in cases where currentLimit != Integer.MAX_VALUE
        return limit - pos;
    }

    protected void requireRemaining(int numBytes) throws IOException {
        if (numBytes < 0) {
            throw InvalidProtocolBufferException.negativeSize();
        } else if (numBytes > remaining()) {
            // Read to the end of the current sub-message before failing
            if (currentLimit != Integer.MAX_VALUE) {
                pos = currentLimit;
            }
            throw InvalidProtocolBufferException.truncatedMessage();
        }
    }

    /**
     * Advances the current position by the specified bytes
     *
     * @param numBytes length to be added
     * @return current position before incrementing
     */
    private int require(int numBytes) throws IOException {
        requireRemaining(numBytes);
        try {
            return pos;
        } finally {
            pos += numBytes;
        }
    }

    private void recomputeBufferSizeAfterLimit() {
        limit += bufferSizeAfterLimit;
        final int bufferEnd = limit;
        if (bufferEnd > currentLimit) {
            // Limit is in current buffer.
            bufferSizeAfterLimit = bufferEnd - currentLimit;
            limit -= bufferSizeAfterLimit;
        } else {
            // Limit is beyond bounds or not set
            bufferSizeAfterLimit = 0;
        }
    }

    /** The absolute position of the end of the current message. */
    private int currentLimit = Integer.MAX_VALUE;

    protected byte[] buffer;
    protected int offset;
    protected int limit;
    private int bufferSizeAfterLimit;
    protected int pos;

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
        int startPos = getTotalBytesRead();
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
        final int position = getTotalBytesRead();
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
