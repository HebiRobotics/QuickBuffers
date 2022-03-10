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

import static us.hebi.quickbuf.UnsafeAccess.*;
import static us.hebi.quickbuf.WireFormat.*;

/**
 * Sink that writes to a flat array
 *
 * @author Florian Enner
 * @since 16 Aug 2019
 */
class ArraySink extends ProtoSink {

    protected byte[] buffer;
    protected int offset;
    protected int limit;
    protected int position;

    @Override
    public int getTotalBytesWritten() {
        return position - offset;
    }

    @Override
    public int spaceLeft() {
        return limit - position;
    }

    /**
     * Resets the position within the internal buffer to zero.
     *
     * @see #position
     * @see #spaceLeft
     */
    @Override
    public ArraySink reset() {
        position = offset;
        return this;
    }

    protected OutOfSpaceException outOfSpace() {
        return new OutOfSpaceException(position, limit);
    }

    @Override
    public final void writeStringNoTag(final CharSequence value) throws IOException {
        // UTF-8 byte length of the string is at least its UTF-16 code unit length (value.length()),
        // and at most 3 times of it. Optimize for the case where we know this length results in a
        // constant varint length - saves measuring length of the string.
        try {
            final int maxLengthVarIntSize = computeRawVarint32Size(value.length() * Utf8.MAX_UTF8_EXPANSION);
            if (maxLengthVarIntSize == 1 || maxLengthVarIntSize == computeRawVarint32Size(value.length())) {
                int startPosition = position + maxLengthVarIntSize;
                int endPosition = writeUtf8Encoded(value, buffer, startPosition, spaceLeft() - maxLengthVarIntSize);
                writeLength(endPosition - startPosition);
                position = endPosition;
            } else {
                writeLength(Utf8.encodedLength(value));
                position = writeUtf8Encoded(value, buffer, position, spaceLeft());
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            final OutOfSpaceException outOfSpaceException = outOfSpace();
            outOfSpaceException.initCause(e);
            throw outOfSpaceException;
        }
    }

    @Override
    public void writeRawByte(final byte value) throws IOException {
        if (position == limit) {
            throw outOfSpace();
        }
        buffer[position++] = value;
    }

    // ----------------- OVERRIDE METHODS -----------------

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

    @Override
    public ProtoSink clear() {
        return setOutput(ProtoUtil.EMPTY_BYTE_ARRAY);
    }

    @Override
    public void writeRawLittleEndian16(final short value) throws IOException {
        ByteUtil.writeLittleEndian16(buffer, require(FIXED_16_SIZE), value);
    }

    @Override
    public void writeRawLittleEndian32(final int value) throws IOException {
        ByteUtil.writeLittleEndian32(buffer, require(FIXED_32_SIZE), value);
    }

    @Override
    public void writeRawLittleEndian64(final long value) throws IOException {
        ByteUtil.writeLittleEndian64(buffer, require(FIXED_64_SIZE), value);
    }

    @Override
    public void writeFloatNoTag(final float value) throws IOException {
        ByteUtil.writeFloat(buffer, require(FIXED_32_SIZE), value);
    }

    @Override
    public void writeDoubleNoTag(final double value) throws IOException {
        ByteUtil.writeDouble(buffer, require(FIXED_64_SIZE), value);
    }

    @Override
    public void writeRawBytes(final byte[] value, int offset, int length) throws IOException {
        ByteUtil.writeBytes(buffer, require(length), value, offset, length);
    }

    @Override
    protected void writeRawBooleans(final boolean[] values, final int length) throws IOException {
        ByteUtil.writeBooleans(buffer, require(length), values, length);
    }

    @Override
    protected void writeRawFixed32s(final int[] values, final int length) throws IOException {
        ByteUtil.writeLittleEndian32s(buffer, require(length * FIXED_32_SIZE), values, length);
    }

    @Override
    protected void writeRawFixed64s(final long[] values, final int length) throws IOException {
        ByteUtil.writeLittleEndian64s(buffer, require(length * FIXED_64_SIZE), values, length);
    }

    @Override
    protected void writeRawFloats(final float[] values, final int length) throws IOException {
        ByteUtil.writeFloats(buffer, require(length * FIXED_32_SIZE), values, length);
    }

    @Override
    protected void writeRawDoubles(final double[] values, final int length) throws IOException {
        ByteUtil.writeDoubles(buffer, require(length * FIXED_64_SIZE), values, length);
    }

    protected int writeUtf8Encoded(final CharSequence value, final byte[] buffer, final int position, final int maxSize) {
        return Utf8.encodeArray(value, buffer, position, maxSize);
    }

    private int require(final int numBytes) throws OutOfSpaceException {
        if (spaceLeft() < numBytes)
            throw outOfSpace();
        try {
            return position;
        } finally {
            position += numBytes;
        }
    }

    /**
     * Sink that writes to an array using the potentially
     * unsupported (e.g. Android) sun.misc.Unsafe intrinsics.
     * Can be used to write directly into a native buffer.
     *
     * @author Florian Enner
     * @since 16 Aug 2019
     */
    static class DirectArraySink extends ArraySink {

        private long baseOffset;
        Object gcRef;

        DirectArraySink() {
            if (!UnsafeAccess.isAvailable())
                throw new AssertionError("DirectArraySink requires access to sun.misc.Unsafe");
        }

        @Override
        public ProtoSink setOutput(byte[] buffer, long offset, int length) {
            gcRef = null;
            if (buffer != null) {
                // Wrapping heap buffer
                baseOffset = BYTE_ARRAY_OFFSET;
                return super.setOutput(buffer, offset, length);
            } else {
                // Wrapping direct memory
                if (offset <= 0) {
                    throw new NullPointerException("null reference with invalid address offset");
                }
                this.buffer = null;
                this.baseOffset = offset;
                this.offset = 0;
                this.position = 0;
                this.limit = length;
                return this;
            }
        }

        @Override
        public void writeRawByte(final byte value) throws IOException {
            if (position == limit) {
                throw outOfSpace();
            }
            UNSAFE.putByte(buffer, baseOffset + position++, value);
        }

        @Override
        public void writeRawLittleEndian16(final short value) throws IOException {
            ByteUtil.writeUnsafeLittleEndian16(buffer, require(FIXED_16_SIZE), value);
        }

        @Override
        public void writeRawLittleEndian32(final int value) throws IOException {
            ByteUtil.writeUnsafeLittleEndian32(buffer, require(FIXED_32_SIZE), value);
        }

        @Override
        public void writeRawLittleEndian64(final long value) throws IOException {
            ByteUtil.writeUnsafeLittleEndian64(buffer, require(FIXED_64_SIZE), value);
        }

        @Override
        public void writeFloatNoTag(final float value) throws IOException {
            ByteUtil.writeUnsafeFloat(buffer, require(FIXED_32_SIZE), value);
        }

        @Override
        public void writeDoubleNoTag(final double value) throws IOException {
            ByteUtil.writeUnsafeDouble(buffer, require(FIXED_64_SIZE), value);
        }

        @Override
        public void writeRawBytes(final byte[] values, int offset, int length) throws IOException {
            ProtoUtil.checkBounds(values,offset, length);
            ByteUtil.writeUnsafeBytes(buffer, require(length), values, offset, length);
        }

        @Override
        protected void writeRawBooleans(final boolean[] values, final int length) throws IOException {
            ByteUtil.writeUnsafeBooleans(buffer, require(length), values, length);
        }

        @Override
        protected void writeRawFixed32s(final int[] values, final int length) throws IOException {
            ByteUtil.writeUnsafeLittleEndian32s(buffer, require(length * FIXED_32_SIZE), values, length);
        }

        @Override
        protected void writeRawFixed64s(final long[] values, final int length) throws IOException {
            ByteUtil.writeUnsafeLittleEndian64s(buffer, require(length * FIXED_64_SIZE), values, length);
        }

        @Override
        protected void writeRawFloats(final float[] values, final int length) throws IOException {
            ByteUtil.writeUnsafeFloats(buffer, require(length * FIXED_32_SIZE), values, length);
        }

        @Override
        protected void writeRawDoubles(final double[] values, final int length) throws IOException {
            ByteUtil.writeUnsafeDoubles(buffer, require(length * FIXED_64_SIZE), values, length);
        }

        @Override
        protected int writeUtf8Encoded(final CharSequence value, final byte[] buffer, final int position, final int maxSize) {
            return Utf8.encodeUnsafe(value, buffer, baseOffset, position, maxSize);
        }

        private long require(final int numBytes) throws OutOfSpaceException {
            if (spaceLeft() < numBytes)
                throw outOfSpace();
            try {
                return baseOffset + position;
            } finally {
                position += numBytes;
            }
        }

    }

    static class RepeatedByteSink extends  ProtoSink {

        public ProtoSink setOutput(RepeatedByte bytes) {
            initialPosition = bytes.length;
            output = bytes;
            return this;
        }

        @Override
        public ProtoSink clear() {
            initialPosition = 0;
            output = null;
            return this;
        }

        @Override
        public int spaceLeft() {
            throw new UnsupportedOperationException("Output grows automatically and has no size limit");
        }

        @Override
        public int getTotalBytesWritten() {
            return output.length() - initialPosition;
        }

        @Override
        public ProtoSink reset() {
            output.setLength(initialPosition);
            return this;
        }

        @Override
        public void writeRawByte(final byte value) throws IOException {
            output.add(value);
        }

        @Override
        public void writeLength(int length) throws IOException {
            // Length bytes are always followed by that amount of
            // content, so we can eagerly reserve the size.
            output.reserve(MAX_VARINT32_SIZE + length);
            super.writeLength(length);
        }

        @Override
        public void writeRawLittleEndian16(final short value) throws IOException {
            final int position = output.addLength(FIXED_16_SIZE);
            ByteUtil.writeLittleEndian16(output.array(), position, value);
        }

        @Override
        public void writeRawLittleEndian32(final int value) throws IOException {
            final int position = output.addLength(FIXED_32_SIZE);
            ByteUtil.writeLittleEndian32(output.array(), position, value);
        }

        @Override
        public void writeRawLittleEndian64(final long value) throws IOException {
            final int position = output.addLength(FIXED_64_SIZE);
            ByteUtil.writeLittleEndian64(output.array(), position, value);
        }

        @Override
        public void writeFloatNoTag(final float value) throws IOException {
            final int position = output.addLength(FIXED_32_SIZE);
            ByteUtil.writeFloat(output.array(), position, value);
        }

        @Override
        public void writeDoubleNoTag(final double value) throws IOException {
            final int position = output.addLength(FIXED_64_SIZE);
            ByteUtil.writeDouble(output.array(), position, value);
        }

        @Override
        public void writeRawBytes(final byte[] value, int offset, int length) throws IOException {
            output.addAll(value, offset, length);
        }

        @Override
        protected void writeRawBooleans(final boolean[] values, final int length) throws IOException {
            final int position = output.addLength(length);
            ByteUtil.writeBooleans(output.array(), position, values, length);
        }

        @Override
        protected void writeRawFixed32s(final int[] values, final int length) throws IOException {
            final int position = output.addLength(length * FIXED_32_SIZE);
            ByteUtil.writeLittleEndian32s(output.array(), position, values, length);
        }

        @Override
        protected void writeRawFixed64s(final long[] values, final int length) throws IOException {
            final int position = output.addLength(length * FIXED_64_SIZE);
            ByteUtil.writeLittleEndian64s(output.array(), position, values, length);
        }

        @Override
        protected void writeRawFloats(final float[] values, final int length) throws IOException {
            final int position = output.addLength(length * FIXED_32_SIZE);
            ByteUtil.writeFloats(output.array(), position, values, length);
        }

        @Override
        protected void writeRawDoubles(final double[] values, final int length) throws IOException {
            final int position = output.addLength(length * FIXED_64_SIZE);
            ByteUtil.writeDoubles(output.array(), position, values, length);
        }

        int initialPosition = 0;
        RepeatedByte output;

    }

}
