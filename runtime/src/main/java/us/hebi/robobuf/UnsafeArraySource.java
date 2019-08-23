package us.hebi.robobuf;

import java.io.IOException;
import java.nio.ByteOrder;

import static us.hebi.robobuf.UnsafeAccess.*;
import static us.hebi.robobuf.WireFormat.*;

/**
 * Source that reads from an array using the sometimes
 * not-supported sun.misc.Unsafe intrinsics. Can be
 * used to read directly from a native buffer
 *
 * @author Florian Enner
 * @since 20 Aug 2019
 */
class UnsafeArraySource extends ProtoSource {

    static boolean isAvailable() {
        return Platform.getJavaVersion() >= 7 && UnsafeAccess.isAvailable();
    }

    UnsafeArraySource(boolean enableDirect) {
        if (!isAvailable())
            throw new AssertionError("UnsafeArraySource requires Java >= 7 and access to sun.misc.Unsafe");
        this.enableDirect = enableDirect;
    }

    @Override
    public ProtoSource setInput(byte[] buffer, long offset, int length) {
        if (!enableDirect || buffer != null) {
            baseOffset = BYTE_ARRAY_OFFSET;
            return super.setInput(buffer, offset, length);
        }
        if (offset <= 0) {
            throw new NullPointerException("null reference with invalid address offset");
        }
        this.buffer = null;
        baseOffset = offset;
        bufferStart = 0;
        bufferSize = length;
        bufferPos = 0;
        return resetInternalState();
    }

    private final boolean enableDirect;
    private long baseOffset;

    @Override
    protected void readRawDoubles(double[] values, int offset, int length) throws IOException {
        final int numBytes = length * SIZEOF_FIXED_64;
        requireRemaining(numBytes);
        if (IS_LITTLE_ENDIAN) {
            final long targetOffset = DOUBLE_ARRAY_OFFSET + SIZEOF_FIXED_64 * offset;
            UNSAFE.copyMemory(buffer, baseOffset + bufferPos, values, targetOffset, numBytes);
            bufferPos += numBytes;
        } else {
            for (int i = 0; i < length; i++) {
                final long bits = UNSAFE.getLong(buffer, baseOffset + bufferPos);
                values[offset + i] = Double.longBitsToDouble(Long.reverseBytes(bits));
                bufferPos += SIZEOF_FIXED_64;
            }
        }
    }

    @Override
    protected void readRawFloats(float[] values, int offset, int length) throws IOException {
        final int numBytes = length * SIZEOF_FIXED_32;
        requireRemaining(numBytes);
        if (IS_LITTLE_ENDIAN) {
            final long targetOffset = FLOAT_ARRAY_OFFSET + SIZEOF_FIXED_32 * offset;
            UNSAFE.copyMemory(buffer, baseOffset + bufferPos, values, targetOffset, numBytes);
            bufferPos += numBytes;
        } else {
            for (int i = 0; i < length; i++) {
                final int bits = UNSAFE.getInt(buffer, baseOffset + bufferPos);
                values[offset + i] = Float.intBitsToFloat(Integer.reverseBytes(bits));
                bufferPos += SIZEOF_FIXED_32;
            }
        }
    }

    @Override
    protected void readRawFixed64s(long[] values, int offset, int length) throws IOException {
        final int numBytes = length * SIZEOF_FIXED_64;
        requireRemaining(numBytes);
        if (IS_LITTLE_ENDIAN) {
            final long targetOffset = INT_ARRAY_OFFSET + SIZEOF_FIXED_64 * offset;
            UNSAFE.copyMemory(buffer, baseOffset + bufferPos, values, targetOffset, numBytes);
            bufferPos += numBytes;
        } else {
            for (int i = 0; i < length; i++) {
                final long bits = UNSAFE.getLong(buffer, baseOffset + bufferPos);
                values[offset + i] = Long.reverseBytes(bits);
                bufferPos += SIZEOF_FIXED_64;
            }
        }
    }

    @Override
    protected void readRawFixed32s(int[] values, int offset, int length) throws IOException {
        final int numBytes = length * SIZEOF_FIXED_32;
        requireRemaining(numBytes);
        if (IS_LITTLE_ENDIAN) {
            final long targetOffset = INT_ARRAY_OFFSET + SIZEOF_FIXED_32 * offset;
            UNSAFE.copyMemory(buffer, baseOffset + bufferPos, values, targetOffset, numBytes);
            bufferPos += numBytes;
        } else {
            for (int i = 0; i < length; i++) {
                final int bits = UNSAFE.getInt(buffer, baseOffset + bufferPos);
                values[offset + i] = Integer.reverseBytes(bits);
                bufferPos += SIZEOF_FIXED_32;
            }
        }
    }

    @Override
    public void readString(StringBuilder output) throws IOException {
        final int size = readRawVarint32();
        requireRemaining(size);
        Utf8.decodeUnsafe(buffer, bufferSize, baseOffset, bufferPos, size, output);
        bufferPos += size;
    }

    @Override
    public void readBytes(RepeatedByte store) throws IOException {
        final int numBytes = readRawVarint32();
        requireRemaining(numBytes);
        store.requireCapacity(numBytes);
        store.length = numBytes;
        UNSAFE.copyMemory(buffer, baseOffset + bufferPos, store.array, BYTE_ARRAY_OFFSET, numBytes);
        bufferPos += numBytes;
    }

    @Override
    public byte readRawByte() throws IOException {
        if (bufferPos == bufferSize) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        return UNSAFE.getByte(buffer, baseOffset + bufferPos++);
    }

    @Override
    public double readDouble() throws IOException {
        if (IS_LITTLE_ENDIAN) {
            requireRemaining(LITTLE_ENDIAN_64_SIZE);
            final double value = UNSAFE.getDouble(buffer, baseOffset + bufferPos);
            bufferPos += LITTLE_ENDIAN_64_SIZE;
            return value;
        } else {
            return Double.longBitsToDouble(readRawLittleEndian64());
        }
    }

    @Override
    public float readFloat() throws IOException {
        if (IS_LITTLE_ENDIAN) {
            requireRemaining(LITTLE_ENDIAN_32_SIZE);
            final float value = UNSAFE.getFloat(buffer, baseOffset + bufferPos);
            bufferPos += LITTLE_ENDIAN_32_SIZE;
            return value;
        } else {
            return Float.intBitsToFloat(readRawLittleEndian32());
        }
    }

    @Override
    public int readRawLittleEndian32() throws IOException {
        requireRemaining(LITTLE_ENDIAN_32_SIZE);
        final int value = UNSAFE.getInt(buffer, baseOffset + bufferPos);
        bufferPos += LITTLE_ENDIAN_32_SIZE;
        return IS_LITTLE_ENDIAN ? value : Integer.reverseBytes(value);
    }

    @Override
    public long readRawLittleEndian64() throws IOException {
        requireRemaining(LITTLE_ENDIAN_64_SIZE);
        final long value = UNSAFE.getLong(buffer, baseOffset + bufferPos);
        bufferPos += LITTLE_ENDIAN_64_SIZE;
        return IS_LITTLE_ENDIAN ? value : Long.reverseBytes(value);
    }

    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

}
