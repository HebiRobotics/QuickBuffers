package us.hebi.robobuf;

import java.io.IOException;
import java.nio.ByteOrder;

import static us.hebi.robobuf.UnsafeAccess.*;
import static us.hebi.robobuf.WireFormat.*;

/**
 * Sink that writes to an array using the sometimes
 * not-supported sun.misc.Unsafe intrinsics. Can be
 * used to write directly into a native buffer
 *
 * @author Florian Enner
 * @since 16 Aug 2019
 */
class UnsafeArraySink extends ArraySink {

    static boolean isAvailable() {
        return Platform.getJavaVersion() >= 7 && UnsafeAccess.isAvailable();
    }

    UnsafeArraySink(boolean enableDirect) {
        if (!isAvailable())
            throw new AssertionError("UnsafeArraySink requires Java >= 7 and access to sun.misc.Unsafe");
        this.enableDirect = enableDirect;
    }

    @Override
    public ProtoSink setOutput(byte[] buffer, long offset, int length) {
        if (!enableDirect || buffer != null) {
            baseOffset = BYTE_ARRAY_OFFSET;
            return super.setOutput(buffer, offset, length);
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
    private long baseOffset;

    private void requireSpace(final int numBytes) throws OutOfSpaceException {
        if (spaceLeft() < numBytes)
            throw new OutOfSpaceException(position, limit);
    }

    @Override
    protected void writeRawFloats(final float[] values, final int length) throws IOException {
        final int numBytes = length * SIZEOF_FIXED_32;
        requireSpace(numBytes);
        if (IS_LITTLE_ENDIAN) {
            UNSAFE.copyMemory(values, FLOAT_ARRAY_OFFSET, buffer, baseOffset + position, numBytes);
            position += numBytes;
        } else {
            for (int i = 0; i < length; i++) {
                final int value = Integer.reverseBytes(Float.floatToIntBits(values[i]));
                UNSAFE.putLong(buffer, baseOffset + position, value);
                position += SIZEOF_FIXED_32;
            }
        }
    }

    @Override
    protected void writeRawFixed32s(final int[] values, final int length) throws IOException {
        final int numBytes = length * SIZEOF_FIXED_32;
        requireSpace(numBytes);
        if (IS_LITTLE_ENDIAN) {
            UNSAFE.copyMemory(values, INT_ARRAY_OFFSET, buffer, baseOffset + position, numBytes);
            position += numBytes;
        } else {
            for (int i = 0; i < length; i++) {
                final int value = Integer.reverseBytes(values[i]);
                UNSAFE.putLong(buffer, baseOffset + position, value);
                position += SIZEOF_FIXED_32;
            }
        }
    }

    @Override
    protected void writeRawFixed64s(final long[] values, final int length) throws IOException {
        final int numBytes = length * SIZEOF_FIXED_64;
        requireSpace(numBytes);
        if (IS_LITTLE_ENDIAN) {
            UNSAFE.copyMemory(values, LONG_ARRAY_OFFSET, buffer, baseOffset + position, numBytes);
            position += numBytes;
        } else {
            for (int i = 0; i < length; i++) {
                final long value = Long.reverseBytes(values[i]);
                UNSAFE.putLong(buffer, baseOffset + position, value);
                position += SIZEOF_FIXED_64;
            }
        }
    }

    @Override
    protected void writeRawDoubles(final double[] values, final int length) throws IOException {
        final int numBytes = length * SIZEOF_FIXED_64;
        requireSpace(numBytes);
        if (IS_LITTLE_ENDIAN) {
            UNSAFE.copyMemory(values, DOUBLE_ARRAY_OFFSET, buffer, baseOffset + position, numBytes);
            position += numBytes;
        } else {
            for (int i = 0; i < length; i++) {
                final long value = Long.reverseBytes(Double.doubleToLongBits(values[i]));
                UNSAFE.putLong(buffer, baseOffset + position, value);
                position += SIZEOF_FIXED_64;
            }
        }
    }

    @Override
    protected void writeRawBooleans(final boolean[] values, final int numBytes) throws IOException {
        requireSpace(numBytes);
        UNSAFE.copyMemory(values, BOOLEAN_ARRAY_OFFSET, buffer, baseOffset + position, numBytes);
        position += numBytes;
    }

    @Override
    public void writeDoubleNoTag(double value) throws IOException {
        if (IS_LITTLE_ENDIAN) {
            requireSpace(LITTLE_ENDIAN_64_SIZE);
            UNSAFE.putDouble(buffer, baseOffset + position, value);
            position += LITTLE_ENDIAN_64_SIZE;
        } else {
            writeRawLittleEndian64(Double.doubleToLongBits(value));
        }
    }

    @Override
    public void writeFloatNoTag(float value) throws IOException {
        if (IS_LITTLE_ENDIAN) {
            requireSpace(LITTLE_ENDIAN_32_SIZE);
            UNSAFE.putFloat(buffer, baseOffset + position, value);
            position += LITTLE_ENDIAN_32_SIZE;
        } else {
            writeRawLittleEndian32(Float.floatToIntBits(value));
        }
    }

    @Override
    public final void writeRawLittleEndian32(final int value) throws IOException {
        requireSpace(LITTLE_ENDIAN_32_SIZE);
        UNSAFE.putInt(buffer, baseOffset + position, IS_LITTLE_ENDIAN ? value : Integer.reverseBytes(value));
        position += LITTLE_ENDIAN_32_SIZE;
    }

    @Override
    public final void writeRawLittleEndian64(final long value) throws IOException {
        requireSpace(LITTLE_ENDIAN_64_SIZE);
        UNSAFE.putLong(buffer, baseOffset + position, IS_LITTLE_ENDIAN ? value : Long.reverseBytes(value));
        position += LITTLE_ENDIAN_64_SIZE;
    }

    @Override
    public void writeRawByte(final byte value) throws IOException {
        if (position >= limit) {
            throw new OutOfSpaceException(position, limit);
        }
        UNSAFE.putByte(buffer, baseOffset + position++, value);
    }

    @Override
    public void writeRawBytes(final byte[] value, int offset, int length) throws IOException {
        if (spaceLeft() >= length) {
            UNSAFE.copyMemory(value, BYTE_ARRAY_OFFSET + offset, buffer, baseOffset + position, length);
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
                int endPosition = Utf8.encodeUnsafe(value, buffer, baseOffset, startPosition, spaceLeft() - minLengthVarIntSize);
                writeRawVarint32(endPosition - startPosition);
                position = endPosition;
            } else {
                writeRawVarint32(Utf8.encodedLength(value));
                position = Utf8.encodeUnsafe(value, buffer, baseOffset, position, spaceLeft());
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            final OutOfSpaceException outOfSpaceException = new OutOfSpaceException(position, limit);
            outOfSpaceException.initCause(e);
            throw outOfSpaceException;
        }
    }

    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

}
