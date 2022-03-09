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

// Protocol Buffers - Google's data interchange format
// Copyright 2013 Google Inc.  All rights reserved.
// https://developers.google.com/protocol-buffers/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package us.hebi.quickbuf;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static us.hebi.quickbuf.WireFormat.*;

/**
 * NOTE: the code was modified from Javanano's CodedOutputByteBufferNano
 *
 * Encodes and writes protocol message fields.
 *
 * <p>This class contains two kinds of methods:  methods that write specific
 * protocol message constructs and field types (e.g. {@link #writeTag} and
 * {@link #writeInt32}) and methods that write low-level values (e.g.
 * {@link #writeRawVarint32} and {@link #writeRawBytes}).  If you are
 * writing encoded protocol messages, you should use the former methods, but if
 * you are writing some other format of your own design, use the latter.
 *
 * <p>This class is totally unsynchronized.
 *
 * @author kneton@google.com Kenton Varda
 * @author Florian Enner
 */
public abstract class ProtoSink {

    protected ProtoSink() {
    }

    /**
     * Create a new {@code ProtoSink} that writes directly to the given
     * byte array. If more bytes are written than fit in the array,
     * {@link OutOfSpaceException} will be thrown.
     */
    public static ProtoSink newInstance(final byte[] flatArray) {
        return newInstance().wrap(flatArray);
    }

    /**
     * Create a new {@code ProtoSink} that writes directly to the given
     * byte array slice. If more bytes are written than fit in the slice,
     * {@link OutOfSpaceException} will be thrown.
     */
    public static ProtoSink newInstance(final byte[] flatArray,
                                        final int offset,
                                        final int length) {
        return newInstance().wrap(flatArray, offset, length);
    }

    /**
     * Create a new {@code ProtoSink} that writes directly to a byte array.
     * If more bytes are written than fit in the array, {@link OutOfSpaceException} will
     * be thrown.
     *
     * This method will return the fastest implementation available for the
     * current platform and may leverage features from sun.misc.Unsafe if
     * available.
     */
    public static ProtoSink newInstance() {
        return new ArraySink();
    }

    /**
     * Create a new {@code ProtoSink} that writes directly to a byte array or
     * direct memory. If more bytes are written than fit in the array, an
     * {@link OutOfSpaceException} will be thrown.
     *
     * This sink requires availability of sun.misc.Unsafe.
     *
     * Additionally, this sink removes null-argument checks and allows users to
     * write to off-heap memory. Working with off-heap memory may cause segfaults
     * of the runtime, so only use if you know what you are doing.
     */
    public static ProtoSink newUnsafeInstance() {
        return new ArraySink.DirectArraySink();
    }

    /**
     * Creates a lightweight wrapper to write protobuf messages
     * to an {@link OutputStream}). This is slower than writing
     * to an array, but it does not require extra memory.
     *
     * @param outputStream target output
     * @return wrapper
     */
    public static ProtoSink wrap(OutputStream outputStream) {
        return new StreamSink(outputStream);
    }

    /**
     * Creates a lightweight wrapper to write protobuf messages to
     * a {@link ByteBuffer}). This is slower than  writing to an
     * array, but it does not require extra memory.
     *
     * @param buffer target output
     * @return wrapper
     */
    public static ProtoSink wrap(ByteBuffer buffer) {
        return new BufferSink(buffer);
    }

    /**
     * Changes the output to the given array. This resets any
     * existing internal state such as position and is
     * equivalent to creating a new instance.
     *
     * @param buffer target buffer
     * @param offset start index
     * @param length buffer size
     * @return this
     */
    public abstract ProtoSink wrap(byte[] buffer, long offset, int length);

    public final ProtoSink wrap(byte[] buffer) {
        return wrap(buffer, 0, buffer.length);
    }

    /**
     * Clears internal state and removes any references to previous outputs.
     *
     * @return this
     */
    public ProtoSink clear() {
        return wrap(ProtoUtil.EMPTY_BYTE_ARRAY);
    }

    // ---------------------- WRITES WITH TAG (NOT USED) ----------------------

    /** Encode and write a tag. */
    public void writeTag(final int fieldNumber, final int wireType) throws IOException {
        writeUInt32NoTag(WireFormat.makeTag(fieldNumber, wireType));
    }

    /** Encode and write a packed tag for the given field number. */
    public void writePackedTag(final int fieldNumber) throws IOException {
        writeUInt32NoTag(WireFormat.makeTag(fieldNumber, WIRETYPE_LENGTH_DELIMITED));
    }

    /** Write a {@code double} field, including tag, to the sink. */
    public void writeDouble(final int fieldNumber, final double value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_FIXED64);
        writeDoubleNoTag(value);
    }

    /** Write a {@code float} field, including tag, to the sink. */
    public void writeFloat(final int fieldNumber, final float value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_FIXED32);
        writeFloatNoTag(value);
    }

    /** Write a {@code uint64} field, including tag, to the sink. */
    public void writeUInt64(final int fieldNumber, final long value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_VARINT);
        writeUInt64NoTag(value);
    }

    /** Write an {@code int64} field, including tag, to the sink. */
    public void writeInt64(final int fieldNumber, final long value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_VARINT);
        writeInt64NoTag(value);
    }

    /** Write an {@code int32} field, including tag, to the sink. */
    public void writeInt32(final int fieldNumber, final int value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_VARINT);
        writeInt32NoTag(value);
    }

    /** Write a {@code fixed64} field, including tag, to the sink. */
    public void writeFixed64(final int fieldNumber, final long value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_FIXED64);
        writeFixed64NoTag(value);
    }

    /** Write a {@code fixed32} field, including tag, to the sink. */
    public void writeFixed32(final int fieldNumber, final int value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_FIXED32);
        writeFixed32NoTag(value);
    }

    /** Write a {@code bool} field, including tag, to the sink. */
    public void writeBool(final int fieldNumber, final boolean value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_VARINT);
        writeBoolNoTag(value);
    }

    /** Write a {@code string} field, including tag, to the sink. */
    public void writeString(final int fieldNumber, final CharSequence value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_LENGTH_DELIMITED);
        writeStringNoTag(value);
    }

    /** Write a {@code group} field, including tag, to the sink. */
    public void writeGroup(final int fieldNumber, final ProtoMessage value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_START_GROUP);
        writeGroupNoTag(value);
        writeTag(fieldNumber, WireFormat.WIRETYPE_END_GROUP);
    }

    /** Write an embedded message field, including tag, to the sink. */
    public void writeMessage(final int fieldNumber, final ProtoMessage value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_LENGTH_DELIMITED);
        writeMessageNoTag(value);
    }

    /** Write a {@code bytes} field, including tag, to the sink. */
    public void writeBytes(final int fieldNumber, final RepeatedByte value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_LENGTH_DELIMITED);
        writeBytesNoTag(value);
    }

    /** Write a {@code uint32} field, including tag, to the sink. */
    public void writeUInt32(final int fieldNumber, final int value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_VARINT);
        writeUInt32NoTag(value);
    }

    /**
     * Write an enum field, including tag, to the sink.  Caller is responsible
     * for converting the enum value to its numeric value.
     */
    public void writeEnum(final int fieldNumber, final int value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_VARINT);
        writeEnumNoTag(value);
    }

    /** Write an {@code sfixed32} field, including tag, to the sink. */
    public void writeSFixed32(final int fieldNumber, final int value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_FIXED32);
        writeSFixed32NoTag(value);
    }

    /** Write an {@code sfixed64} field, including tag, to the sink. */
    public void writeSFixed64(final int fieldNumber, final long value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_FIXED64);
        writeSFixed64NoTag(value);
    }

    /** Write an {@code sint32} field, including tag, to the sink. */
    public void writeSInt32(final int fieldNumber, final int value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_VARINT);
        writeSInt32NoTag(value);
    }

    /** Write an {@code sint64} field, including tag, to the sink. */
    public void writeSInt64(final int fieldNumber, final long value)
            throws IOException {
        writeTag(fieldNumber, WireFormat.WIRETYPE_VARINT);
        writeSInt64NoTag(value);
    }

    // -------------------------- PACKED FIXED WIDTH TYPES --------------------------

    /** Write a repeated (packed) {@code double} field, excluding tag, to the sink. */
    public void writePackedDoubleNoTag(final RepeatedDouble values)
            throws IOException {
        writeLength(values.length * FIXED_64_SIZE);
        writeRawDoubles(values.array, values.length);
    }

    /** Write a repeated (packed) {@code float} field, excluding tag, to the sink. */
    public void writePackedFloatNoTag(final RepeatedFloat values)
            throws IOException {
        writeLength(values.length * FIXED_32_SIZE);
        writeRawFloats(values.array, values.length);
    }

    /** Write a repeated (packed){@code fixed64} field, excluding tag, to the sink. */
    public void writePackedFixed64NoTag(final RepeatedLong values)
            throws IOException {
        writeLength(values.length * FIXED_64_SIZE);
        writeRawFixed64s(values.array, values.length);
    }

    /** Write a repeated (packed){@code fixed32} field, excluding tag, to the sink. */
    public void writePackedFixed32NoTag(final RepeatedInt values)
            throws IOException {
        writeLength(values.length * FIXED_32_SIZE);
        writeRawFixed32s(values.array, values.length);
    }

    /** Write a repeated (packed) {@code sfixed32} field, excluding tag, to the sink. */
    public void writePackedSFixed32NoTag(final RepeatedInt values)
            throws IOException {
        writeLength(values.length * FIXED_32_SIZE);
        writeRawFixed32s(values.array, values.length);
    }

    /** Write a repeated (packed) {@code sfixed64} field, excluding tag, to the sink. */
    public void writePackedSFixed64NoTag(final RepeatedLong values)
            throws IOException {
        writeLength(values.length * FIXED_64_SIZE);
        writeRawFixed64s(values.array, values.length);
    }

    /** Write a repeated (non-packed){@code bool} field, excluding tag, to the sink. */
    public void writePackedBoolNoTag(final RepeatedBoolean values)
            throws IOException {
        writeLength(values.length * MIN_BOOL_SIZE);
        writeRawBooleans(values.array, values.length);
    }

    protected void writeRawDoubles(final double[] values, int length) throws IOException {
        for (int i = 0; i < length; i++) {
            writeRawLittleEndian64(Double.doubleToLongBits(values[i]));
        }
    }

    protected void writeRawFloats(final float[] values, int length) throws IOException {
        for (int i = 0; i < length; i++) {
            writeRawLittleEndian32(Float.floatToIntBits(values[i]));
        }
    }

    protected void writeRawFixed64s(final long[] values, int length) throws IOException {
        for (int i = 0; i < length; i++) {
            writeRawLittleEndian64(values[i]);
        }
    }

    protected void writeRawFixed32s(final int[] values, int length) throws IOException {
        for (int i = 0; i < length; i++) {
            writeRawLittleEndian32(values[i]);
        }
    }

    protected void writeRawBooleans(final boolean[] values, int length) throws IOException {
        for (int i = 0; i < length; i++) {
            writeRawByte(values[i] ? 1 : 0);
        }
    }

    // -------------------------- PACKED VARINT TYPES --------------------------

    /** Compute the number of bytes needed to encode all contained {@code enum} values */
    public static int computeRepeatedEnumSizeNoTag(final RepeatedEnum<?> values) {
        int dataSize = 0;
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            dataSize += ProtoSink.computeEnumSizeNoTag(values.array()[i]);
        }
        return dataSize;
    }

    /** Write a repeated (packed) {@code enum} field to the sink. */
    public void writePackedEnumNoTag(final RepeatedEnum<?> values) throws IOException {
        writeLength(computeRepeatedEnumSizeNoTag(values));
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            writeEnumNoTag(values.array()[i]);
        }
    }

    /** Compute the number of bytes needed to encode all contained {@code int32} values */
    public static int computeRepeatedInt32SizeNoTag(final RepeatedInt values) {
        int dataSize = 0;
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            dataSize += ProtoSink.computeInt32SizeNoTag(values.array()[i]);
        }
        return dataSize;
    }

    /** Write a repeated (packed) {@code int32} field to the sink. */
    public void writePackedInt32NoTag(final RepeatedInt values) throws IOException {
        writeLength(computeRepeatedInt32SizeNoTag(values));
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            writeInt32NoTag(values.array()[i]);
        }
    }

    /** Compute the number of bytes needed to encode all contained {@code uint32} values */
    public static int computeRepeatedUInt32SizeNoTag(final RepeatedInt values) {
        return computeRepeatedInt32SizeNoTag(values);
    }

    /** Write a repeated (packed) {@code uint32} field to the sink. */
    public void writePackedUInt32NoTag(final RepeatedInt values) throws IOException {
        writePackedInt32NoTag(values);
    }

    /** Compute the number of bytes needed to encode all contained {@code sint32} values */
    public static int computeRepeatedSInt32SizeNoTag(final RepeatedInt values) {
        int dataSize = 0;
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            dataSize += ProtoSink.computeSInt32SizeNoTag(values.array()[i]);
        }
        return dataSize;
    }

    /** Write a repeated (packed) {@code sint32} field to the sink. */
    public void writePackedSInt32NoTag(final RepeatedInt values) throws IOException {
        writeLength(computeRepeatedSInt32SizeNoTag(values));
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            writeSInt32NoTag(values.array()[i]);
        }
    }

    /** Compute the number of bytes needed to encode all contained {@code int32} values */
    public static int computeRepeatedInt64SizeNoTag(final RepeatedLong values) {
        int dataSize = 0;
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            dataSize += ProtoSink.computeInt64SizeNoTag(values.array()[i]);
        }
        return dataSize;
    }

    /** Write a repeated (packed) {@code int32} field to the sink. */
    public void writePackedInt64NoTag(final RepeatedLong values) throws IOException {
        writeLength(computeRepeatedInt64SizeNoTag(values));
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            writeInt64NoTag(values.array()[i]);
        }
    }

    /** Compute the number of bytes needed to encode all contained {@code uint64} values */
    public static int computeRepeatedUInt64SizeNoTag(final RepeatedLong values) {
        return computeRepeatedInt64SizeNoTag(values);
    }

    /** Write a repeated (packed) {@code uint32} field to the sink. */
    public void writePackedUInt64NoTag(final RepeatedLong values) throws IOException {
        writePackedInt64NoTag(values);
    }

    /** Compute the number of bytes needed to encode all contained {@code sint64} values */
    public static int computeRepeatedSInt64SizeNoTag(final RepeatedLong values) {
        int dataSize = 0;
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            dataSize += ProtoSink.computeSInt64SizeNoTag(values.array()[i]);
        }
        return dataSize;
    }

    /** Write a repeated (packed) {@code sint64} field to the sink. */
    public void writePackedSInt64NoTag(final RepeatedLong values) throws IOException {
        writeLength(computeRepeatedSInt64SizeNoTag(values));
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            writeSInt64NoTag(values.array()[i]);
        }
    }

    /** Compute the number of bytes needed to encode all contained {@code message} values */
    public static int computeRepeatedMessageSizeNoTag(final RepeatedMessage<?> values) {
        int dataSize = 0;
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            dataSize += ProtoSink.computeMessageSizeNoTag(values.array[i]);
        }
        return dataSize;
    }

    /** Compute the number of bytes needed to encode all contained {@code message} values */
    public static int computeRepeatedGroupSizeNoTag(final RepeatedMessage<?> values) {
        int dataSize = 0;
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            dataSize += ProtoSink.computeGroupSizeNoTag(values.array[i]);
        }
        return dataSize;
    }

    /** Compute the number of bytes needed to encode all contained {@code string} values */
    public static int computeRepeatedStringSizeNoTag(final RepeatedString values) {
        int dataSize = 0;
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            dataSize += ProtoSink.computeStringSizeNoTag(values.array[i]);
        }
        return dataSize;
    }

    /** Compute the number of bytes needed to encode all contained {@code bytes} values */
    public static int computeRepeatedBytesSizeNoTag(final RepeatedBytes values) {
        int dataSize = 0;
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            dataSize += ProtoSink.computeBytesSizeNoTag(values.array[i]);
        }
        return dataSize;
    }

    // -----------------------------------------------------------------

    /** Write a length delimiter to the sink */
    public void writeLength(final int length) throws IOException {
        writeUInt32NoTag(length);
    }

    /** Write a {@code double} field to the sink. */
    public void writeDoubleNoTag(final double value) throws IOException {
        writeRawLittleEndian64(Double.doubleToLongBits(value));
    }

    /** Write a {@code float} field to the sink. */
    public void writeFloatNoTag(final float value) throws IOException {
        writeRawLittleEndian32(Float.floatToIntBits(value));
    }

    /** Write a {@code uint64} field to the sink. */
    public void writeUInt64NoTag(final long value) throws IOException {
        writeRawVarint64(value);
    }

    /** Write an {@code int64} field to the sink. */
    public void writeInt64NoTag(final long value) throws IOException {
        writeRawVarint64(value);
    }

    /** Write an {@code int32} field to the sink. */
    public void writeInt32NoTag(final int value) throws IOException {
        if (value >= 0) {
            writeUInt32NoTag(value);
        } else {
            // Must sign-extend.
            writeRawVarint64(value);
        }
    }

    /** Write a {@code fixed64} field to the sink. */
    public void writeFixed64NoTag(final long value) throws IOException {
        writeRawLittleEndian64(value);
    }

    /** Write a {@code fixed32} field to the sink. */
    public void writeFixed32NoTag(final int value) throws IOException {
        writeRawLittleEndian32(value);
    }

    /** Write a {@code bool} field to the sink. */
    public void writeBoolNoTag(final boolean value) throws IOException {
        writeRawByte(value ? 1 : 0);
    }

    /** Write a {@code string} field to the sink. */
    public void writeStringNoTag(final Utf8String value) throws IOException {
        final int length = value.size();
        writeLength(length);
        writeRawBytes(value.bytes(), 0, length);
    }

    /** Write a {@code string} field to the sink. */
    public void writeStringNoTag(final CharSequence value) throws IOException {
        writeLength(Utf8.encodedLength(value));
        Utf8.encodeSink(value, this);
    }

    /** Write a {@code group} field to the sink. */
    public void writeGroupNoTag(final ProtoMessage<?> value) throws IOException {
        value.writeTo(this);
    }

    /** Write an embedded message field to the sink. */
    public void writeMessageNoTag(final ProtoMessage<?> value) throws IOException {
        writeLength(value.getCachedSize());
        value.writeTo(this);
    }

    /** Write a {@code bytes} field to the sink. */
    public void writeBytesNoTag(final RepeatedByte value) throws IOException {
        writeLength(value.length);
        writeRawBytes(value.array, 0, value.length);
    }

    /**
     * Write an enum field to the sink.  Caller is responsible
     * for converting the enum value to its numeric value.
     */
    public void writeEnumNoTag(final int value) throws IOException {
        writeUInt32NoTag(value);
    }

    /** Write an {@code sfixed32} field to the sink. */
    public void writeSFixed32NoTag(final int value) throws IOException {
        writeRawLittleEndian32(value);
    }

    /** Write an {@code sfixed64} field to the sink. */
    public void writeSFixed64NoTag(final long value) throws IOException {
        writeRawLittleEndian64(value);
    }

    /** Write an {@code sint32} field to the sink. */
    public void writeSInt32NoTag(final int value) throws IOException {
        writeUInt32NoTag(encodeZigZag32(value));
    }

    /** Write an {@code sint64} field to the sink. */
    public void writeSInt64NoTag(final long value) throws IOException {
        writeRawVarint64(encodeZigZag64(value));
    }

    // =================================================================

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code double} field, including tag.
     */
    public static int computeDoubleSize(final int fieldNumber,
                                        final double value) {
        return computeTagSize(fieldNumber) + computeDoubleSizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code float} field, including tag.
     */
    public static int computeFloatSize(final int fieldNumber, final float value) {
        return computeTagSize(fieldNumber) + computeFloatSizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code uint64} field, including tag.
     */
    public static int computeUInt64Size(final int fieldNumber, final long value) {
        return computeTagSize(fieldNumber) + computeUInt64SizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode an
     * {@code int64} field, including tag.
     */
    public static int computeInt64Size(final int fieldNumber, final long value) {
        return computeTagSize(fieldNumber) + computeInt64SizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode an
     * {@code int32} field, including tag.
     */
    public static int computeInt32Size(final int fieldNumber, final int value) {
        return computeTagSize(fieldNumber) + computeInt32SizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code fixed64} field, including tag.
     */
    public static int computeFixed64Size(final int fieldNumber,
                                         final long value) {
        return computeTagSize(fieldNumber) + computeFixed64SizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code fixed32} field, including tag.
     */
    public static int computeFixed32Size(final int fieldNumber,
                                         final int value) {
        return computeTagSize(fieldNumber) + computeFixed32SizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code bool} field, including tag.
     */
    public static int computeBoolSize(final int fieldNumber,
                                      final boolean value) {
        return computeTagSize(fieldNumber) + computeBoolSizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code string} field, including tag.
     */
    public static int computeStringSize(final int fieldNumber,
                                        final CharSequence value) {
        return computeTagSize(fieldNumber) + computeStringSizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code group} field, including tag.
     */
    public static int computeGroupSize(final int fieldNumber,
                                       final ProtoMessage<?> value) {
        return computeTagSize(fieldNumber) * 2 + computeGroupSizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode an
     * embedded message field, including tag.
     */
    public static int computeMessageSize(final int fieldNumber,
                                         final ProtoMessage<?> value) {
        return computeTagSize(fieldNumber) + computeMessageSizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code bytes} field, including tag.
     */
    public static int computeBytesSize(final int fieldNumber,
                                       final RepeatedByte value) {
        return computeTagSize(fieldNumber) + computeBytesSizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code uint32} field, including tag.
     */
    public static int computeUInt32Size(final int fieldNumber, final int value) {
        return computeTagSize(fieldNumber) + computeUInt32SizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode an
     * enum field, including tag.  Caller is responsible for converting the
     * enum value to its numeric value.
     */
    public static int computeEnumSize(final int fieldNumber, final int value) {
        return computeTagSize(fieldNumber) + computeEnumSizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode an
     * {@code sfixed32} field, including tag.
     */
    public static int computeSFixed32Size(final int fieldNumber,
                                          final int value) {
        return computeTagSize(fieldNumber) + computeSFixed32SizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode an
     * {@code sfixed64} field, including tag.
     */
    public static int computeSFixed64Size(final int fieldNumber,
                                          final long value) {
        return computeTagSize(fieldNumber) + computeSFixed64SizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode an
     * {@code sint32} field, including tag.
     */
    public static int computeSInt32Size(final int fieldNumber, final int value) {
        return computeTagSize(fieldNumber) + computeSInt32SizeNoTag(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode an
     * {@code sint64} field, including tag.
     */
    public static int computeSInt64Size(final int fieldNumber, final long value) {
        return computeTagSize(fieldNumber) + computeSInt64SizeNoTag(value);
    }

    // -----------------------------------------------------------------

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code double} field, including tag.
     */
    public static int computeDoubleSizeNoTag(final double value) {
        return FIXED_64_SIZE;
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code float} field, including tag.
     */
    public static int computeFloatSizeNoTag(final float value) {
        return FIXED_32_SIZE;
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code uint64} field, including tag.
     */
    public static int computeUInt64SizeNoTag(final long value) {
        return computeRawVarint64Size(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode an
     * {@code int64} field, including tag.
     */
    public static int computeInt64SizeNoTag(final long value) {
        return computeRawVarint64Size(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode an
     * {@code int32} field, including tag.
     */
    public static int computeInt32SizeNoTag(final int value) {
        if (value >= 0) {
            return computeRawVarint32Size(value);
        } else {
            // Must sign-extend.
            return 10;
        }
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code fixed64} field.
     */
    public static int computeFixed64SizeNoTag(final long value) {
        return FIXED_64_SIZE;
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code fixed32} field.
     */
    public static int computeFixed32SizeNoTag(final int value) {
        return FIXED_32_SIZE;
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code bool} field.
     */
    public static int computeBoolSizeNoTag(final boolean value) {
        return 1;
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code string} field.
     */
    public static int computeStringSizeNoTag(final Utf8String value) {
        final int length = value.size();
        return computeRawVarint32Size(length) + length;
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code string} field.
     */
    public static int computeStringSizeNoTag(final CharSequence value) {
        final int length = Utf8.encodedLength(value);
        return computeRawVarint32Size(length) + length;
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code group} field.
     */
    public static int computeGroupSizeNoTag(final ProtoMessage value) {
        return value.getSerializedSize();
    }

    /**
     * Compute the number of bytes that would be needed to encode an embedded
     * message field.
     */
    public static int computeMessageSizeNoTag(final ProtoMessage value) {
        final int size = value.getSerializedSize();
        return computeRawVarint32Size(size) + size;
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code bytes} field.
     */
    public static int computeBytesSizeNoTag(final RepeatedByte value) {
        return computeRawVarint32Size(value.length) + value.length;
    }

    /**
     * Compute the number of bytes that would be needed to encode a
     * {@code uint32} field.
     */
    public static int computeUInt32SizeNoTag(final int value) {
        return computeRawVarint32Size(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode an enum field.
     * Caller is responsible for converting the enum value to its numeric value.
     */
    public static int computeEnumSizeNoTag(final int value) {
        return computeRawVarint32Size(value);
    }

    /**
     * Compute the number of bytes that would be needed to encode an
     * {@code sfixed32} field.
     */
    public static int computeSFixed32SizeNoTag(final int value) {
        return FIXED_32_SIZE;
    }

    /**
     * Compute the number of bytes that would be needed to encode an
     * {@code sfixed64} field.
     */
    public static int computeSFixed64SizeNoTag(final long value) {
        return FIXED_64_SIZE;
    }

    /**
     * Compute the number of bytes that would be needed to encode an
     * {@code sint32} field.
     */
    public static int computeSInt32SizeNoTag(final int value) {
        return computeRawVarint32Size(encodeZigZag32(value));
    }

    /**
     * Compute the number of bytes that would be needed to encode an
     * {@code sint64} field.
     */
    public static int computeSInt64SizeNoTag(final long value) {
        return computeRawVarint64Size(encodeZigZag64(value));
    }

    /**
     * Compute the number of bytes that would be needed to encode
     * length bytes and a length delimiter.
     */
    public static int computeDelimitedSize(final int length) {
        return computeRawVarint32Size(length) + length;
    }

    // =================================================================

    /**
     * Returns remaining space when there is a known limit.
     * Otherwise, throws {@code UnsupportedOperationException}.
     */
    public abstract int spaceLeft();

    /**
     * Verifies that {@link #spaceLeft()} returns zero.  It's common to create
     * a byte array that is exactly big enough to hold a message, then write to
     * it with a {@code ProtoSink}.  Calling {@code checkNoSpaceLeft()}
     * after writing verifies that the message was actually as big as expected,
     * which can help catch bugs.
     */
    public void checkNoSpaceLeft() {
        if (spaceLeft() != 0) {
            throw new IllegalStateException(
                    "Did not write as much data as expected.");
        }
    }

    /**
     * Returns the position within the internal buffer.
     */
    public abstract int position();

    /**
     * Resets the position within the internal buffer to zero.
     *
     * @see #position
     * @see #spaceLeft
     */
    public abstract ProtoSink reset();

    /**
     * If you create a ProtoSink around a simple flat array, you must
     * not attempt to write more bytes than the array has space.  Otherwise,
     * this exception will be thrown.
     */
    public static class OutOfSpaceException extends IOException {
        private static final long serialVersionUID = -6947486886997889499L;

        OutOfSpaceException(int position, int limit) {
            super("ProtoSink was writing to a flat byte array and ran " +
                    "out of space (pos " + position + " limit " + limit + ").");
        }
    }

    /** Write a single byte. */
    public abstract void writeRawByte(final byte value) throws IOException;

    /** Write a single byte, represented by an integer value. */
    public void writeRawByte(final int value) throws IOException {
        writeRawByte((byte) value);
    }

    /** Write an array of bytes. */
    public void writeRawBytes(final byte[] value) throws IOException {
        writeRawBytes(value, 0, value.length);
    }

    /** Write part of an array of bytes. */
    public void writeRawBytes(final byte[] value, int offset, int length) throws IOException {
        final int limit = offset + length;
        for (int i = offset; i < limit; i++) {
            writeRawByte(value[i]);
        }
    }

    /** Compute the number of bytes that would be needed to encode a tag. */
    public static int computeTagSize(final int fieldNumber) {
        return computeRawVarint32Size(WireFormat.makeTag(fieldNumber, 0));
    }

    /** Write a {@code uint32} field to the sink. */
    public void writeUInt32NoTag(int value) throws IOException {
        while (true) {
            if ((value & ~0x7F) == 0) {
                writeRawByte((byte) value);
                return;
            } else {
                writeRawByte((byte) (value | 0x80));
                value >>>= 7;
            }
        }
    }

    /**
     * Compute the number of bytes that would be needed to encode a varint.
     * {@code value} is treated as unsigned, so it won't be sign-extended if
     * negative.
     */
    public static int computeRawVarint32Size(final int value) {
        if ((value & (0xffffffff << 7)) == 0) return 1;
        if ((value & (0xffffffff << 14)) == 0) return 2;
        if ((value & (0xffffffff << 21)) == 0) return 3;
        if ((value & (0xffffffff << 28)) == 0) return 4;
        return 5;
    }

    /** Encode and write a varint. */
    public void writeRawVarint64(long value) throws IOException {
        while (true) {
            if ((value & ~0x7FL) == 0) {
                writeRawByte((byte) value);
                return;
            } else {
                writeRawByte((((int) value) | 0x80));
                value >>>= 7;
            }
        }
    }

    /** Compute the number of bytes that would be needed to encode a varint. */
    public static int computeRawVarint64Size(long value) {
        // handle two popular special cases up front ...
        if ((value & (~0L << 7)) == 0L) {
            return 1;
        }
        if (value < 0L) {
            return 10;
        }
        // ... leaving us with 8 remaining, which we can divide and conquer
        int n = 2;
        if ((value & (~0L << 35)) != 0L) {
            n += 4;
            value >>>= 28;
        }
        if ((value & (~0L << 21)) != 0L) {
            n += 2;
            value >>>= 14;
        }
        if ((value & (~0L << 14)) != 0L) {
            n += 1;
        }
        return n;
    }

    /** Write a little-endian 16-bit integer. */
    public void writeRawLittleEndian16(final short value) throws IOException {
        writeRawByte((byte) (value & 0xFF));
        writeRawByte((byte) (value >>> 8));
    }

    /** Write a little-endian 32-bit integer. */
    public void writeRawLittleEndian32(final int value) throws IOException {
        writeRawByte((byte) (value & 0xFF));
        writeRawByte((byte) (value >>> 8));
        writeRawByte((byte) (value >>> 16));
        writeRawByte((byte) (value >>> 24));
    }

    /** Write a little-endian 64-bit integer. */
    public void writeRawLittleEndian64(final long value) throws IOException {
        writeRawByte((byte) (value & 0xFF));
        writeRawByte((byte) (value >>> 8));
        writeRawByte((byte) (value >>> 16));
        writeRawByte((byte) (value >>> 24));
        writeRawByte((byte) (value >>> 32));
        writeRawByte((byte) (value >>> 40));
        writeRawByte((byte) (value >>> 48));
        writeRawByte((byte) (value >>> 56));
    }

    /**
     * Encode a ZigZag-encoded 32-bit value.  ZigZag encodes signed integers
     * into values that can be efficiently encoded with varint.  (Otherwise,
     * negative values must be sign-extended to 64 bits to be varint encoded,
     * thus always taking 10 bytes on the wire.)
     *
     * @param n A signed 32-bit integer.
     * @return An unsigned 32-bit integer, stored in a signed int because
     * Java has no explicit unsigned support.
     */
    public static int encodeZigZag32(final int n) {
        // Note:  the right-shift must be arithmetic
        return (n << 1) ^ (n >> 31);
    }

    /**
     * Encode a ZigZag-encoded 64-bit value.  ZigZag encodes signed integers
     * into values that can be efficiently encoded with varint.  (Otherwise,
     * negative values must be sign-extended to 64 bits to be varint encoded,
     * thus always taking 10 bytes on the wire.)
     *
     * @param n A signed 64-bit integer.
     * @return An unsigned 64-bit integer, stored in a signed int because
     * Java has no explicit unsigned support.
     */
    public static long encodeZigZag64(final long n) {
        // Note:  the right-shift must be arithmetic
        return (n << 1) ^ (n >> 63);
    }

    /**
     * Encode and write a varint.  {@code value} is treated as
     * unsigned, so it won't be sign-extended if negative.
     */
    @Deprecated
    public void writeRawVarint32(int value) throws IOException {
        writeUInt32NoTag(value);
    }

    static class StreamSink extends ProtoSink {
        StreamSink(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public ProtoSink wrap(byte[] buffer, long offset, int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int spaceLeft() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int position() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProtoSink reset() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeRawByte(byte value) throws IOException {
            outputStream.write(value);
        }

        final OutputStream outputStream;
    }

    static class BufferSink extends ProtoSink {

        BufferSink(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public ProtoSink wrap(byte[] buffer, long offset, int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int spaceLeft() {
            return buffer.remaining();
        }

        @Override
        public int position() {
            return buffer.position();
        }

        @Override
        public ProtoSink reset() {
            buffer.rewind();
            return this;
        }

        @Override
        public void writeRawByte(byte value) throws IOException {
            buffer.put(value);
        }

        final ByteBuffer buffer;
    }

}
