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

import static us.hebi.quickbuf.UnsafeAccess.*;
import static us.hebi.quickbuf.WireFormat.*;

/**
 * Source that reads from a flat array
 *
 * @author Florian Enner
 * @since 06 Mär 2022
 */
class ArraySource extends ProtoSource{

    protected byte[] buffer;
    protected int offset;
    protected int limit;
    protected int position;
    private int bufferSizeAfterLimit;

    /** The absolute position of the end of the current message. */
    private int currentLimit = Integer.MAX_VALUE;

    protected ProtoSource resetInternalState() {
        super.resetInternalState();
        currentLimit = Integer.MAX_VALUE;
        bufferSizeAfterLimit = 0;
        return this;
    }

    @Override
    public int pushLimit(int byteLimit) throws InvalidProtocolBufferException {
        if (byteLimit < 0) {
            throw InvalidProtocolBufferException.negativeSize();
        }
        byteLimit += position;
        if (byteLimit > limit) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        final int oldLimit = currentLimit;
        currentLimit = byteLimit;
        recomputeBufferSizeAfterLimit();
        return oldLimit;
    }

    @Override
    public void popLimit(final int oldLimit) {
        currentLimit = oldLimit;
        recomputeBufferSizeAfterLimit();
    }

    @Override
    public int getBytesUntilLimit() {
        if (currentLimit == Integer.MAX_VALUE) {
            return -1;
        }
        return currentLimit - position;
    }

    @Override
    public boolean isAtEnd() {
        return position == limit;
    }

    @Override
    public int getTotalBytesRead() {
        return position - offset;
    }

    protected void rewindToPosition(int position) throws InvalidProtocolBufferException {
        this.position = offset + position;
    }

    protected void requireRemaining(final int numBytes) throws IOException {
        if (numBytes < 0) {
            throw InvalidProtocolBufferException.negativeSize();
        } else if (numBytes > limit - position) {
            // Read to the end of the current sub-message before failing
            if (currentLimit != Integer.MAX_VALUE) {
                position = currentLimit;
            }
            throw InvalidProtocolBufferException.truncatedMessage();
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

    @Override
    public boolean skipField(int tag, RepeatedByte store) throws IOException {
        // Skip field
        final int mark = position;
        if (!skipField(tag)) {
            return false;

        } else if (!discardUnknownFields) {
            // Reserve sufficient size
            final int length = position - mark;
            int position = store.addLength(MAX_VARINT32_SIZE + length);

            // Write field tag
            position += ByteUtil.writeUInt32(store.array(), position, store.capacity(), tag);

            // Copy field content
            rewindToPosition(mark);
            readRawBytes(store.array(), position, length);
            store.length = position + length;
        }
        return true;
    }

    public void skipEnum(final int tag, final int value, final RepeatedByte store) throws IOException {
        if (!discardUnknownFields) {
            int position = store.addLength(2 * MAX_VARINT32_SIZE);
            position += ByteUtil.writeUInt32(store.array(), position, store.capacity(), tag);
            position += ByteUtil.writeUInt32(store.array(), position, store.capacity(), value);
            store.length = position;
        }
    }

    // ----------------- OVERRIDE METHODS -----------------

    @Override
    public ProtoSource wrap(byte[] buffer, long off, int len) {
        if (off < 0 || len < 0 || off > buffer.length || off + len > buffer.length)
            throw new ArrayIndexOutOfBoundsException();
        this.buffer = buffer;
        this.position = this.offset = (int) off;
        this.limit = offset + len;
        return resetInternalState();
    }

    @Override
    public void skipRawBytes(final int size) throws IOException {
        require(size);
    }

    @Override
    public byte readRawByte() throws IOException {
        if (position == limit) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        return buffer[position++];
    }

    @Override
    public short readRawLittleEndian16() throws IOException {
        return ByteUtil.readLittleEndian16(buffer, require(FIXED_16_SIZE));
    }

    @Override
    public int readRawLittleEndian32() throws IOException {
        return ByteUtil.readLittleEndian32(buffer, require(FIXED_32_SIZE));
    }

    @Override
    public long readRawLittleEndian64() throws IOException {
        return ByteUtil.readLittleEndian64(buffer, require(FIXED_64_SIZE));
    }

    @Override
    public float readFloat() throws IOException {
        return ByteUtil.readFloat(buffer, require(FIXED_32_SIZE));
    }

    @Override
    public double readDouble() throws IOException {
        return ByteUtil.readDouble(buffer, require(FIXED_64_SIZE));
    }

    @Override
    public void readRawBytes(byte[] values, int offset, int length) throws IOException {
        ByteUtil.readBytes(buffer, require(length), values, offset, length);
    }

    @Override
    protected void readRawFixed32s(int[] values, int offset, int length) throws IOException {
        ByteUtil.readLittleEndian32s(buffer, require(length * FIXED_32_SIZE), values, offset, length);
    }

    @Override
    protected void readRawFixed64s(long[] values, int offset, int length) throws IOException {
        ByteUtil.readLittleEndian64s(buffer, require(length * FIXED_64_SIZE), values, offset, length);
    }

    @Override
    protected void readRawFloats(float[] values, int offset, int length) throws IOException {
        ByteUtil.readFloats(buffer, require(length * FIXED_32_SIZE), values, offset, length);
    }

    @Override
    protected void readRawDoubles(double[] values, int offset, int length) throws IOException {
        ByteUtil.readDoubles(buffer, require(length * FIXED_64_SIZE), values, offset, length);
    }

    /** moves forward by numBytes and returns the current position */
    private int require(int numBytes) throws IOException {
        requireRemaining(numBytes);
        try {
            return position;
        } finally {
            position += numBytes;
        }
    }

    /**
     * Source that reads from an array using the potentially
     * unsupported (e.g. Android) sun.misc.Unsafe intrinsics.
     * Can be used to read directly from a native buffer.
     *
     * @author Florian Enner
     * @since 20 Aug 2019
     */
    static class DirectArraySource extends ArraySource {

        private long baseOffset;

        DirectArraySource() {
            if (!UnsafeAccess.isAvailable())
                throw new AssertionError("DirectArraySource requires access to sun.misc.Unsafe");
        }

        @Override
        public ProtoSource wrap(byte[] buffer, long offset, int length) {
            if (buffer != null) {
                // Wrapping heap buffer
                baseOffset = BYTE_ARRAY_OFFSET;
                return super.wrap(buffer, offset, length);
            } else {
                // Wrapping direct memory
                if (offset <= 0) {
                    throw new NullPointerException("null reference with invalid address offset");
                }
                this.buffer = null;
                this.baseOffset = offset;
                this.position = this.offset = 0;
                this.limit = length;
                return resetInternalState();
            }
        }

        @Override
        public void skipRawBytes(final int size) throws IOException {
            require(size);
        }

        @Override
        public byte readRawByte() throws IOException {
            if (position == limit) {
                throw InvalidProtocolBufferException.truncatedMessage();
            }
            return UNSAFE.getByte(buffer, baseOffset + position++);
        }

        @Override
        public short readRawLittleEndian16() throws IOException {
            return ByteUtil.readUnsafeLittleEndian16(buffer, require(FIXED_16_SIZE));
        }

        @Override
        public int readRawLittleEndian32() throws IOException {
            return ByteUtil.readUnsafeLittleEndian32(buffer, require(FIXED_32_SIZE));
        }

        @Override
        public long readRawLittleEndian64() throws IOException {
            return ByteUtil.readUnsafeLittleEndian64(buffer, require(FIXED_64_SIZE));
        }

        public float readFloat() throws IOException {
            return ByteUtil.readUnsafeFloat(buffer, require(FIXED_32_SIZE));
        }

        public double readDouble() throws IOException {
            return ByteUtil.readUnsafeDouble(buffer, require(FIXED_64_SIZE));
        }
        @Override
        public void readRawBytes(byte[] values, int offset, int length) throws IOException {
            ByteUtil.readUnsafeBytes(buffer, require(length), values, offset, length);
        }

        @Override
        protected void readRawFixed32s(int[] values, int offset, int length) throws IOException {
            ByteUtil.readUnsafeLittleEndian32s(buffer, require(length * FIXED_32_SIZE), values, offset, length);
        }

        @Override
        protected void readRawFixed64s(long[] values, int offset, int length) throws IOException {
            ByteUtil.readUnsafeLittleEndian64s(buffer, require(length * FIXED_64_SIZE), values, offset, length);
        }

        @Override
        protected void readRawFloats(float[] values, int offset, int length) throws IOException {
            ByteUtil.readUnsafeFloats(buffer, require(length * FIXED_32_SIZE), values, offset, length);
        }

        @Override
        protected void readRawDoubles(double[] values, int offset, int length) throws IOException {
            ByteUtil.readUnsafeDoubles(buffer, require(length * FIXED_64_SIZE), values, offset, length);
        }

        /** moves forward by numBytes and returns the current address */
        private long require(int numBytes) throws IOException {
            requireRemaining(numBytes);
            try {
                return baseOffset + position;
            } finally {
                position += numBytes;
            }
        }

    }

}
