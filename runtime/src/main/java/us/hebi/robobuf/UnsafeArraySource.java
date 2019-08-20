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
        return Platform.getJavaVersion() >= 8 && UnsafeAccess.isAvailable();
    }

    UnsafeArraySource(boolean enableDirect) {
        if (!isAvailable())
            throw new AssertionError("UnsafeArraySource requires Java >= 8 and access to sun.misc.Unsafe");
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
    public void readString(StringBuilder output) throws IOException {
        final int size = readRawVarint32();
        requireRemaining(size);
        Utf8.decodeUnsafe(buffer, baseOffset, bufferPos, size, output);
        bufferPos += size;
    }

    @Override
    public void readBytes(RepeatedByte store) throws IOException {
        final int numBytes = readRawVarint32();
        requireRemaining(numBytes);
        store.length = 0;
        store.requireCapacity(numBytes);
        UNSAFE.copyMemory(buffer, baseOffset + bufferPos, store.array, 0, numBytes);
        bufferPos += numBytes;
    }

    @Override
    public byte readRawByte() throws IOException {
        if (bufferPos == bufferSize) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        return UNSAFE.getByte(buffer, baseOffset + bufferPos++);
    }

    public int readRawLittleEndian32() throws IOException {
        requireRemaining(LITTLE_ENDIAN_32_SIZE);
        final int value = UNSAFE.getInt(buffer, baseOffset + bufferPos);
        bufferPos += LITTLE_ENDIAN_32_SIZE;
        return IS_LITTLE_ENDIAN ? value : Integer.reverseBytes(value);
    }

    public long readRawLittleEndian64() throws IOException {
        requireRemaining(LITTLE_ENDIAN_64_SIZE);
        final long value = UNSAFE.getLong(buffer, baseOffset + bufferPos);
        bufferPos += LITTLE_ENDIAN_64_SIZE;
        return IS_LITTLE_ENDIAN ? value : Long.reverseBytes(value);
    }

    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

}
