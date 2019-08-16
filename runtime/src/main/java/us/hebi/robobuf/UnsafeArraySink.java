package us.hebi.robobuf;

import java.io.IOException;
import java.nio.ByteOrder;

import static us.hebi.robobuf.UnsafeAccess.*;
import static us.hebi.robobuf.WireFormat.*;

/**
 * @author Florian Enner
 * @since 16 Aug 2019
 */
class UnsafeArraySink extends ArraySink {

    UnsafeArraySink(final byte[] buffer,
                    final int offset,
                    final int length) {
        super(buffer, offset, length);
        UnsafeAccess.requireUnsafe();
    }

    private void requireSpace(int numBytes) throws OutOfSpaceException {
        if (spaceLeft() < numBytes)
            throw new OutOfSpaceException(position, limit);
    }

    @Override
    public final void writePackedDouble(final int fieldNumber, final RepeatedDouble values)
            throws IOException {
        final int numBytes = values.length * SIZEOF_FIXED_64;
        writeTag(fieldNumber, WIRETYPE_LENGTH_DELIMITED);
        writeRawVarint32(numBytes);

        if (IS_LITTLE_ENDIAN) {
            requireSpace(numBytes);
            UNSAFE.copyMemory(values.array, DOUBLE_ARRAY_OFFSET, buffer, BYTE_ARRAY_OFFSET + position, numBytes);
            position += numBytes;
        } else {
            for (int i = 0; i < values.array.length; i++) {
                writeDoubleNoTag(values.array[i]);
            }
        }
    }

    public void writePackedFloat(final int fieldNumber, final RepeatedFloat values)
            throws IOException {
        final int numBytes = values.length * SIZEOF_FIXED_32;
        writeTag(fieldNumber, WIRETYPE_LENGTH_DELIMITED);
        writeRawVarint32(numBytes);

        if (IS_LITTLE_ENDIAN) {
            requireSpace(numBytes);
            UNSAFE.copyMemory(values.array, FLOAT_ARRAY_OFFSET, buffer, BYTE_ARRAY_OFFSET + position, numBytes);
            position += numBytes;
        } else {
            for (int i = 0; i < values.array.length; i++) {
                writeFloatNoTag(values.array[i]);
            }
        }
    }

    /** Write a repeated (non-packed){@code fixed64} field, including tag, to the stream. */
    public void writePackedFixed64(final int fieldNumber, final RepeatedLong values)
            throws IOException {
        final int numBytes = values.length * SIZEOF_FIXED_64;
        writeTag(fieldNumber, WIRETYPE_LENGTH_DELIMITED);
        writeRawVarint32(numBytes);

        if (IS_LITTLE_ENDIAN) {
            requireSpace(numBytes);
            UNSAFE.copyMemory(values.array, LONG_ARRAY_OFFSET, buffer, BYTE_ARRAY_OFFSET + position, numBytes);
            position += numBytes;
        } else {
            for (int i = 0; i < values.array.length; i++) {
                writeFixed64NoTag(values.array[i]);
            }
        }
    }

    /** Write a repeated (non-packed){@code fixed32} field, including tag, to the stream. */
    public void writePackedFixed32(final int fieldNumber, final RepeatedInt values)
            throws IOException {
        final int numBytes = values.length * SIZEOF_FIXED_32;
        writeTag(fieldNumber, WIRETYPE_LENGTH_DELIMITED);
        writeRawVarint32(numBytes);

        if (IS_LITTLE_ENDIAN) {
            requireSpace(numBytes);
            UNSAFE.copyMemory(values.array, INT_ARRAY_OFFSET, buffer, BYTE_ARRAY_OFFSET + position, numBytes);
            position += numBytes;
        } else {
            for (int i = 0; i < values.array.length; i++) {
                writeFixed32NoTag(values.array[i]);
            }
        }
    }

    /** Write a repeated (non-packed) {@code sfixed32} field, including tag, to the stream. */
    public void writePackedSFixed32(final int fieldNumber, final RepeatedInt values)
            throws IOException {
        final int numBytes = values.length * SIZEOF_FIXED_32;
        writeTag(fieldNumber, WIRETYPE_LENGTH_DELIMITED);
        writeRawVarint32(numBytes);

        if (IS_LITTLE_ENDIAN) {
            requireSpace(numBytes);
            UNSAFE.copyMemory(values.array, INT_ARRAY_OFFSET, buffer, BYTE_ARRAY_OFFSET + position, numBytes);
            position += numBytes;
        } else {
            for (int i = 0; i < values.array.length; i++) {
                writeSFixed32NoTag(values.array[i]);
            }
        }
    }

    /** Write a repeated (non-packed) {@code sfixed64} field, including tag, to the stream. */
    public void writePackedSFixed64(final int fieldNumber, final RepeatedLong values)
            throws IOException {
        final int numBytes = values.length * SIZEOF_FIXED_64;
        writeTag(fieldNumber, WIRETYPE_LENGTH_DELIMITED);
        writeRawVarint32(numBytes);
        if (IS_LITTLE_ENDIAN) {
            requireSpace(numBytes);
            UNSAFE.copyMemory(values.array, LONG_ARRAY_OFFSET, buffer, BYTE_ARRAY_OFFSET + position, numBytes);
            position += numBytes;
        } else {
            for (int i = 0; i < values.array.length; i++) {
                writeSFixed64NoTag(values.array[i]);
            }
        }
    }

    /** Write a repeated (non-packed){@code bool} field, including tag, to the stream. */
    public void writePackedBool(final int fieldNumber, final RepeatedBoolean values)
            throws IOException {
        final int numBytes = values.length * SIZEOF_FIXED_BOOL;
        writeTag(fieldNumber, WIRETYPE_LENGTH_DELIMITED);
        writeRawVarint32(numBytes);
        requireSpace(numBytes);
        UNSAFE.copyMemory(values.array, BOOLEAN_ARRAY_OFFSET, buffer, BYTE_ARRAY_OFFSET + position, numBytes);
        position += numBytes;
    }

    @Override
    public final void writeRawLittleEndian32(final int value) throws IOException {
        requireSpace(LITTLE_ENDIAN_32_SIZE);
        UNSAFE.putInt(buffer, BYTE_ARRAY_OFFSET + position, IS_LITTLE_ENDIAN ? value : Integer.reverseBytes(value));
        position += LITTLE_ENDIAN_32_SIZE;
    }

    @Override
    public final void writeRawLittleEndian64(final long value) throws IOException {
        requireSpace(LITTLE_ENDIAN_64_SIZE);
        UNSAFE.putLong(buffer, BYTE_ARRAY_OFFSET + position, IS_LITTLE_ENDIAN ? value : Long.reverseBytes(value));
        position += LITTLE_ENDIAN_64_SIZE;
    }

    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;


}
