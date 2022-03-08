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
 * Sink that writes to an array using the potentially
 * unsupported (e.g. Android) sun.misc.Unsafe intrinsics.
 * Can be used to write directly into a native buffer.
 *
 * @author Florian Enner
 * @since 16 Aug 2019
 */
class UnsafeArraySink extends ArraySink {

    UnsafeArraySink(boolean enableDirect) {
        if (!UnsafeAccess.isAvailable())
            throw new AssertionError("UnsafeArraySink requires access to sun.misc.Unsafe");
        this.enableDirect = enableDirect;
    }

    @Override
    public ProtoSink wrap(byte[] buffer, long offset, int length) {
        if (!enableDirect || buffer != null) {
            baseOffset = BYTE_ARRAY_OFFSET;
            return super.wrap(buffer, offset, length);
        }
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

    private final boolean enableDirect;
    long baseOffset;

    @Override
    public void writeRawByte(final byte value) throws IOException {
        if (position == limit) {
            throw outOfSpace();
        }
        UNSAFE.putByte(buffer, baseOffset + position++, value);
    }

    @Override
    public void writeRawLittleEndian16(final short value) throws IOException {
        ByteUtil.writeUnsafeLittleEndian16(buffer, require(SIZEOF_FIXED_16), value);
    }

    @Override
    public void writeRawLittleEndian32(final int value) throws IOException {
        ByteUtil.writeUnsafeLittleEndian32(buffer, require(SIZEOF_FIXED_32), value);
    }

    @Override
    public void writeRawLittleEndian64(final long value) throws IOException {
        ByteUtil.writeUnsafeLittleEndian64(buffer, require(SIZEOF_FIXED_64), value);
    }

    @Override
    public void writeFloatNoTag(final float value) throws IOException {
        ByteUtil.writeUnsafeFloat(buffer, require(SIZEOF_FIXED_32), value);
    }

    @Override
    public void writeDoubleNoTag(final double value) throws IOException {
        ByteUtil.writeUnsafeDouble(buffer, require(SIZEOF_FIXED_64), value);
    }

    @Override
    public void writeRawBytes(final byte[] values, int offset, int length) throws IOException {
        ByteUtil.writeUnsafeBytes(buffer, require(length), values, offset, length);
    }

    @Override
    protected void writeRawBooleans(final boolean[] values, final int length) throws IOException {
        ByteUtil.writeUnsafeBooleans(buffer, require(length), values, length);
    }

    @Override
    protected void writeRawFixed32s(final int[] values, final int length) throws IOException {
        ByteUtil.writeUnsafeLittleEndian32s(buffer, require(length * SIZEOF_FIXED_32), values, length);
    }

    @Override
    protected void writeRawFixed64s(final long[] values, final int length) throws IOException {
        ByteUtil.writeUnsafeLittleEndian64s(buffer, require(length * SIZEOF_FIXED_64), values, length);
    }

    @Override
    protected void writeRawFloats(final float[] values, final int length) throws IOException {
        ByteUtil.writeUnsafeFloats(buffer, require(length * SIZEOF_FIXED_32), values, length);
    }

    @Override
    protected void writeRawDoubles(final double[] values, final int length) throws IOException {
        ByteUtil.writeUnsafeDoubles(buffer, require(length * SIZEOF_FIXED_64), values, length);
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
