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
 * Source that reads from an array using the potentially
 * unsupported (e.g. Android) sun.misc.Unsafe intrinsics.
 * Can be used to read directly from a native buffer.
 *
 * @author Florian Enner
 * @since 20 Aug 2019
 */
class UnsafeArraySource extends ArraySource {

    static boolean isAvailable() {
        return UnsafeAccess.isAvailable() && UnsafeAccess.isCopyMemoryAvailable();
    }

    UnsafeArraySource(boolean enableDirect) {
        if (!isAvailable())
            throw new AssertionError("UnsafeArraySource requires access to sun.misc.Unsafe");
        this.enableDirect = enableDirect;
    }

    @Override
    public ProtoSource wrap(byte[] buffer, long offset, int length) {
        if (!enableDirect || buffer != null) {
            baseOffset = BYTE_ARRAY_OFFSET;
            return super.wrap(buffer, offset, length);
        }
        if (offset <= 0) {
            throw new NullPointerException("null reference with invalid address offset");
        }
        this.buffer = null;
        baseOffset = offset;
        this.offset = 0;
        limit = length;
        pos = 0;
        return resetInternalState();
    }

    private final boolean enableDirect;
    long baseOffset;

    @Override
    public void copyBytesSinceMark(RepeatedByte store) {
        final int length = pos - lastTagMark;
        final int bufferPos = store.addLength(length);
        UNSAFE.copyMemory(buffer, baseOffset + lastTagMark, store.array, BYTE_ARRAY_OFFSET + bufferPos, length);
    }

    /** Reads one byte from the input. */
    @Override
    public byte readRawByte() throws IOException {
        if (pos == limit) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        return UNSAFE.getByte(buffer, baseOffset + pos++);
    }

    /** Read a 16-bit little-endian integer from the source. */
    @Override
    public short readRawLittleEndian16() throws IOException {
        return ByteUtil.readUnsafeLittleEndian16(buffer, require(SIZEOF_FIXED_16));
    }

    /** Read a 32-bit little-endian integer from the source. */
    @Override
    public int readRawLittleEndian32() throws IOException {
        return ByteUtil.readUnsafeLittleEndian32(buffer, require(SIZEOF_FIXED_32));
    }

    /** Read a 64-bit little-endian integer from the source. */
    @Override
    public long readRawLittleEndian64() throws IOException {
        return ByteUtil.readUnsafeLittleEndian64(buffer, require(SIZEOF_FIXED_64));
    }

    /** Read a {@code float} field value from the source. */
    public float readFloat() throws IOException {
        return ByteUtil.readUnsafeFloat(buffer, require(SIZEOF_FIXED_32));
    }

    /** Read a {@code double} field value from the source. */
    public double readDouble() throws IOException {
        return ByteUtil.readUnsafeDouble(buffer, require(SIZEOF_FIXED_64));
    }
    @Override
    public void readRawBytes(byte[] values, int offset, int length) throws IOException {
        ByteUtil.readUnsafeBytes(buffer, require(length), values, offset, length);
    }

    @Override
    protected void readRawFixed32s(int[] values, int offset, int length) throws IOException {
        ByteUtil.readUnsafeLittleEndian32s(buffer, require(length * SIZEOF_FIXED_32), values, offset, length);
    }

    @Override
    protected void readRawFixed64s(long[] values, int offset, int length) throws IOException {
        ByteUtil.readUnsafeLittleEndian64s(buffer, require(length * SIZEOF_FIXED_64), values, offset, length);
    }

    @Override
    protected void readRawFloats(float[] values, int offset, int length) throws IOException {
        ByteUtil.readUnsafeFloats(buffer, require(length * SIZEOF_FIXED_32), values, offset, length);
    }

    @Override
    protected void readRawDoubles(double[] values, int offset, int length) throws IOException {
        ByteUtil.readUnsafeDoubles(buffer, require(length * SIZEOF_FIXED_64), values, offset, length);
    }

    private long require(int numBytes) throws IOException {
        requireRemaining(numBytes);
        try {
            return baseOffset + pos;
        } finally {
            pos += numBytes;
        }
    }

}
