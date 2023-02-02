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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static us.hebi.quickbuf.WireFormat.*;

/**
 * NOTE: the code was modified from Javanano's CodedInputByteBufferNano
 *
 * Reads and decodes protocol message fields.
 * <p>
 * This class contains two kinds of methods:  methods that read specific
 * protocol message constructs and field types (e.g. {@link #readTag()} and
 * {@link #readInt32()}) and methods that read low-level values (e.g.
 * {@link #readRawVarint32()} and {@link #readRawVarint64()}).  If you are reading
 * encoded protocol messages, you should use the former methods, but if you are
 * reading some other format of your own design, use the latter.
 *
 * @author kenton@google.com Kenton Varda
 * @author Florian Enner
 */
public abstract class ProtoSource {

    /** Create a new ProtoSource reading from the given byte array. */
    public static ProtoSource newInstance(final byte[] buf) {
        return newInstance(buf, 0, buf.length);
    }

    /** Create a new ProtoSource reading from the given byte array slice. */
    public static ProtoSource newInstance(final byte[] buf,
                                          final int off,
                                          final int len) {
        return newArraySource().setInput(buf, off, len);
    }

    /** Create a new ProtoSource reading from the current array of the given {@link RepeatedByte}. */
    public static ProtoSource newInstance(RepeatedByte bytes) {
        return newArraySource().setInput(bytes);
    }

    /** Create a new ProtoSource reading from the given {@link InputStream}. */
    public static ProtoSource newInstance(InputStream stream) {
        return newStreamSource().setInput(stream);
    }

    /** Create a new ProtoSource reading from the given {@link ByteBuffer}. */
    public static ProtoSource newInstance(ByteBuffer buffer) {
        return newBufferSource().setInput(buffer);
    }

    /**
     * Creates a new {@code ProtoSource} that reads directly from a byte array.
     *
     * This method will return the fastest implementation available for the
     * current platform and may leverage features from sun.misc.Unsafe if
     * available.
     */
    public static ProtoSource newArraySource() {
        return new ArraySource();
    }

    /**
     * Creates a new {@code ProtoSource} that reads from a byte array or
     * direct / off-heap memory. It is similar to the array source, but
     * it allows null buffers to support raw memory addresses.
     *
     * This source requires availability of sun.misc.Unsafe. Be aware that
     * passing incorrect memory addresses may cause the entire runtime to
     * segfault.
     */
    public static ProtoSource newDirectSource() {
        return new ArraySource.DirectArraySource();
    }

    /**
     * Creates a new {@code ProtoSource} that reads from an {@link InputStream}.
     *
     * The current implementation is a very lightweight wrapper that reads
     * byte-by-byte and does not do any internal buffering. This is slower
     * than reading from an array, but it does not require extra memory.
     */
    public static ProtoSource newStreamSource() {
        return new StreamSource();
    }

    /**
     * Creates a new {@code ProtoSource} that reads from an {@link ByteBuffer}.
     *
     * The current implementation is a very lightweight wrapper that reads
     * individual bytes. This is slower than reading from an array, but it
     * works on all platforms.
     */
    public static ProtoSource newBufferSource() {
        return new BufferSource();
    }

    /**
     * Changes the input to the given array. This resets any existing
     * internal state such as position and is equivalent to creating
     * a new instance.
     */
    public final ProtoSource setInput(byte[] buffer) {
        return setInput(buffer, 0, buffer.length);
    }

    /**
     * Changes the input to the given array. This resets any existing
     * internal state such as position and is equivalent to creating
     * a new instance.
     */
    public ProtoSource setInput(byte[] buffer, long off, int len) {
        throw new UnsupportedOperationException("source does not support reading from a byte array");
    }

    /**
     * Changes the input to the current array of the given {@link RepeatedByte}.
     */
    public final ProtoSource setInput(RepeatedByte bytes) {
        return setInput(bytes.array(), 0, bytes.length());
    }

    /**
     * Changes the input to the given stream. This resets any existing
     * internal state such as position and is equivalent to creating
     * a new instance.
     */
    public ProtoSource setInput(InputStream stream) {
        throw new UnsupportedOperationException("source does not support reading from an InputStream");
    }

    /**
     * Changes the input to the given buffer. This resets any existing
     * internal state such as position and is equivalent to creating
     * a new instance.
     *
     * A BufferSource wraps the buffer directly and modifies the buffer state.
     * ArraySource and DirectSource wrap the backing memory and do not modify the
     * buffer state.
     */
    public ProtoSource setInput(ByteBuffer buffer) {
        throw new UnsupportedOperationException("source does not support reading from a ByteBuffer");
    }

    /**
     * Clears internal state and removes any references to previous inputs.
     */
    public abstract ProtoSource clear();

    // -----------------------------------------------------------------

    /**
     * Attempt to read a field tag, returning zero if we have reached EOF.
     * Protocol message parsers use this to read tags, since a protocol message
     * may legally end wherever a tag occurs, and zero is not a valid tag number.
     */
    public int readTag() throws IOException {
        if (isAtEnd()) {
            lastTag = 0;
            return 0;
        }

        lastTag = readRawVarint32();
        if (WireFormat.getTagFieldNumber(lastTag) == 0) {
            // If we actually read zero (or any tag number corresponding
            // to field number zero), that's not a valid tag.
            throw InvalidProtocolBufferException.invalidTag();
        }
        return lastTag;
    }

    /**
     * Verifies that the last call to readTag() returned the given tag value.
     * This is used to verify that a nested group ended with the correct
     * end tag.
     *
     * @throws InvalidProtocolBufferException {@code value} does not match the
     *                                        last tag.
     */
    public void checkLastTagWas(final int value) throws InvalidProtocolBufferException {
        if (lastTag != value) {
            throw InvalidProtocolBufferException.invalidEndTag();
        }
    }

    /**
     * Reads and discards a single field, given its tag value.
     *
     * @return {@code false} if the tag is an endgroup tag, in which case
     * nothing is skipped.  Otherwise, returns {@code true}.
     */
    public boolean skipField(final int tag) throws IOException {
        switch (WireFormat.getTagWireType(tag)) {
            case WireFormat.WIRETYPE_VARINT:
                readRawVarint32(); // Note: not worth adding a skipVarint method
                return true;
            case WireFormat.WIRETYPE_FIXED64:
                skipRawBytes(FIXED_64_SIZE);
                return true;
            case WireFormat.WIRETYPE_LENGTH_DELIMITED:
                skipRawBytes(readLength());
                return true;
            case WireFormat.WIRETYPE_START_GROUP:
                skipMessage();
                int endGroupTag = WireFormat.makeTag(WireFormat.getTagFieldNumber(tag), WireFormat.WIRETYPE_END_GROUP);
                checkLastTagWas(endGroupTag);
                return true;
            case WireFormat.WIRETYPE_END_GROUP:
                return false;
            case WireFormat.WIRETYPE_FIXED32:
                skipRawBytes(FIXED_32_SIZE);
                return true;
            default:
                throw InvalidProtocolBufferException.invalidWireType();
        }
    }

    /**
     * Reads and discards a single field, given its tag value. Discarded bytes
     * are added to unknownBytes.
     *
     * @return {@code false} if the tag is an endgroup tag, in which case
     * nothing is skipped.  Otherwise, returns {@code true}.
     */
    public boolean skipField(final int tag, final RepeatedByte unknownBytes) throws IOException {
        if (shouldDiscardUnknownFields) {
            return skipField(tag);
        }
        switch (WireFormat.getTagWireType(tag)) {
            case WireFormat.WIRETYPE_VARINT: {
                long value = readRawVarint64();
                ByteUtil.writeUInt32(unknownBytes, tag);
                ByteUtil.writeVarint64(unknownBytes, value);
                return true;
            }
            case WireFormat.WIRETYPE_FIXED64: {
                ByteUtil.writeUInt32(unknownBytes, tag);
                ByteUtil.writeBytes(unknownBytes, FIXED_64_SIZE, this);
                return true;
            }
            case WireFormat.WIRETYPE_LENGTH_DELIMITED:{
                int length = readLength();
                ByteUtil.writeUInt32(unknownBytes, tag);
                ByteUtil.writeUInt32(unknownBytes, length);
                ByteUtil.writeBytes(unknownBytes, length, this);
                return true;
            }
            case WireFormat.WIRETYPE_START_GROUP:{
                int endTag = WireFormat.makeTag(WireFormat.getTagFieldNumber(tag), WireFormat.WIRETYPE_END_GROUP);
                ByteUtil.writeUInt32(unknownBytes, tag);
                skipMessage(unknownBytes);
                checkLastTagWas(endTag);
                ByteUtil.writeUInt32(unknownBytes, endTag);
                return true;
            }
            case WireFormat.WIRETYPE_END_GROUP: {
                return false;
            }
            case WireFormat.WIRETYPE_FIXED32:{
                ByteUtil.writeUInt32(unknownBytes, tag);
                ByteUtil.writeBytes(unknownBytes, FIXED_32_SIZE, this);
                return true;
            }
            default:
                throw InvalidProtocolBufferException.invalidWireType();
        }
    }

    /**
     * Skips an enum that has already been read, but had a value that is not known. Discarded
     * bytes are added to unknownBytes.
     */
    public void skipEnum(final int tag, final int value, final RepeatedByte unknownBytes) throws IOException {
        if (!shouldDiscardUnknownFields) {
            ByteUtil.writeUInt32(unknownBytes, tag);
            ByteUtil.writeUInt32(unknownBytes, value);
        }
    }

    /**
     * Reads and discards an entire message.  This will read either until EOF
     * or until an endgroup tag, whichever comes first.
     */
    protected void skipMessage() throws IOException {
        while (true) {
            final int tag = readTag();
            if (tag == 0 || !skipField(tag)) {
                return;
            }
        }
    }

    protected void skipMessage(RepeatedByte unknownBytes) throws IOException {
        while (true) {
            final int tag = readTag();
            if (tag == 0 || !skipField(tag, unknownBytes)) {
                return;
            }
        }
    }

    // ------------------------------ FIXED WIDTH TYPES ------------------------------

    /** Read a repeated (packed) {@code double} field value from the source. */
    public void readPackedDouble(RepeatedDouble store) throws IOException {
        final int length = readLength();
        final int limit = pushLimit(length);
        final int count = roundedCount64(length);
        final int offset = store.addLength(count);
        readRawDoubles(store.array, offset, count);
        popLimit(limit);
    }

    protected void readRawDoubles(double[] values, int offset, int length) throws IOException {
        final int limit = offset + length;
        for (int i = offset; i < limit; i++) {
            values[i] = readDouble();
        }
    }

    /** Read a repeated (non-packed) {@code double} field value from the source. */
    public int readRepeatedDouble(final RepeatedDouble store, final int tag) throws IOException {
        int nextTag;
        do {
            reserveRepeatedFieldCapacity(store, tag);
            store.add(readDouble());
        } while ((nextTag = readTag()) == tag);
        return nextTag;
    }

    /** Read a {@code double} field value from the source. */
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readRawLittleEndian64());
    }

    /** Read a repeated (packed) {@code float} field value from the source. */
    public void readPackedFloat(RepeatedFloat store) throws IOException {
        final int length = readLength();
        final int limit = pushLimit(length);
        final int count =  roundedCount32(length);
        final int offset = store.addLength(count);
        readRawFloats(store.array, offset, count);
        popLimit(limit);
    }

    protected void readRawFloats(float[] values, int offset, int length) throws IOException {
        final int limit = offset + length;
        for (int i = offset; i < limit; i++) {
            values[i] = readFloat();
        }
    }

    /** Read a repeated (non-packed) {@code float} field value from the source. */
    public int readRepeatedFloat(final RepeatedFloat store, final int tag) throws IOException {
        int nextTag;
        do {
            reserveRepeatedFieldCapacity(store, tag);
            store.add(readFloat());
        } while ((nextTag = readTag()) == tag);
        return nextTag;
    }

    /** Read a {@code float} field value from the source. */
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readRawLittleEndian32());
    }

    /** Read a repeated (packed) {@code sfixed64} field value from the source. */
    public void readPackedSFixed64(RepeatedLong store) throws IOException {
        readPackedFixed64(store);
    }

    /** Read a repeated (packed) {@code fixed64} field value from the source. */
    public void readPackedFixed64(RepeatedLong store) throws IOException {
        final int length = readLength();
        final int limit = pushLimit(length);
        final int count =  roundedCount64(length);
        final int offset = store.addLength(count);
        readRawFixed64s(store.array, offset, count);
        popLimit(limit);
    }

    protected void readRawFixed64s(long[] values, int offset, int length) throws IOException {
        final int limit = offset + length;
        for (int i = offset; i < limit; i++) {
            values[i] = readRawLittleEndian64();
        }
    }

    /** Read a repeated (non-packed) {@code sfixed64} field value from the source. */
    public int readRepeatedSFixed64(final RepeatedLong store, final int tag) throws IOException {
        return readRepeatedFixed64(store, tag);
    }

    /** Read a repeated (non-packed) {@code fixed64} field value from the source. */
    public int readRepeatedFixed64(RepeatedLong store, final int tag) throws IOException {
        int nextTag;
        do {
            reserveRepeatedFieldCapacity(store, tag);
            store.add(readFixed64());
        } while ((nextTag = readTag()) == tag);
        return nextTag;
    }

    /** Read an {@code sfixed64} field value from the source. */
    public long readSFixed64() throws IOException {
        return readRawLittleEndian64();
    }

    /** Read a {@code fixed64} field value from the source. */
    public long readFixed64() throws IOException {
        return readRawLittleEndian64();
    }

    /** Read a repeated (packed) {@code sfixed32} field value from the source. */
    public void readPackedSFixed32(RepeatedInt store) throws IOException {
        readPackedFixed32(store);
    }

    /** Read a repeated (packed) {@code fixed32} field value from the source. */
    public void readPackedFixed32(RepeatedInt store) throws IOException {
        final int length = readLength();
        final int limit = pushLimit(length);
        final int count =  roundedCount32(length);
        final int offset = store.addLength(count);
        readRawFixed32s(store.array, offset, count);
        popLimit(limit);
    }

    protected void readRawFixed32s(int[] values, int offset, int length) throws IOException {
        final int limit = offset + length;
        for (int i = offset; i < limit; i++) {
            values[i] = readRawLittleEndian32();
        }
    }

    /** Read a repeated (non-packed) {@code sfixed32} field value from the source. */
    public int readRepeatedSFixed32(final RepeatedInt store, final int tag) throws IOException {
        return readRepeatedFixed32(store, tag);
    }

    /** Read a repeated (non-packed) {@code fixed32} field value from the source. */
    public int readRepeatedFixed32(RepeatedInt store, final int tag) throws IOException {
        int nextTag;
        do {
            reserveRepeatedFieldCapacity(store, tag);
            store.add(readFixed32());
        } while ((nextTag = readTag()) == tag);
        return nextTag;
    }

    /** Read an {@code sfixed32} field value from the source. */
    public int readSFixed32() throws IOException {
        return readRawLittleEndian32();
    }

    /** Read a {@code fixed32} field value from the source. */
    public int readFixed32() throws IOException {
        return readRawLittleEndian32();
    }

    // ------------------------------ VARINT TYPES ------------------------------

    /** Read a repeated (packed) {@code uint64} field value from the source. */
    public void readPackedUInt64(final RepeatedLong store, final int tag) throws IOException {
        readPackedInt64(store, tag);
    }

    /** Read a repeated (packed) {@code int64} field value from the source. */
    public void readPackedInt64(final RepeatedLong store, final int tag) throws IOException {
        final int length = readLength();
        final int limit = pushLimit(length);
        while (!isAtEnd()) {
            reservePackedVarintCapacity(store);
            store.add(readInt64());
        }
        popLimit(limit);
    }

    /** Read a repeated (packed) {@code sint64} field value from the source. */
    public void readPackedSInt64(final RepeatedLong store, final int tag) throws IOException {
        final int length = readLength();
        final int limit = pushLimit(length);
        while (!isAtEnd()) {
            reservePackedVarintCapacity(store);
            store.add(readSInt64());
        }
        popLimit(limit);
    }

    /** Read a repeated {@code uint64} field value from the source. */
    public int readRepeatedUInt64(final RepeatedLong store, final int tag) throws IOException {
        return readRepeatedInt64(store, tag);
    }

    /** Read a repeated {@code int64} field value from the source. */
    public int readRepeatedInt64(final RepeatedLong store, final int tag) throws IOException {
        int nextTag;
        do {
            reserveRepeatedFieldCapacity(store, tag);
            store.add(readInt64());
        } while ((nextTag = readTag()) == tag);
        return nextTag;
    }

    /** Read a repeated {@code sint64} field value from the source. */
    public int readRepeatedSInt64(final RepeatedLong store, final int tag) throws IOException {
        int nextTag;
        do {
            reserveRepeatedFieldCapacity(store, tag);
            store.add(readSInt64());
        } while ((nextTag = readTag()) == tag);
        return nextTag;
    }

    /** Read a {@code uint64} field value from the source. */
    public long readUInt64() throws IOException {
        return readRawVarint64();
    }

    /** Read an {@code int64} field value from the source. */
    public long readInt64() throws IOException {
        return readRawVarint64();
    }

    /** Read an {@code sint64} field value from the source. */
    public long readSInt64() throws IOException {
        return decodeZigZag64(readRawVarint64());
    }

    /** Read a repeated (packed) {@code uint32} field value from the source. */
    public void readPackedUInt32(final RepeatedInt store, final int tag) throws IOException {
        readPackedInt32(store, tag);
    }

    /** Read a repeated (packed) {@code int32} field value from the source. */
    public void readPackedInt32(final RepeatedInt store, final int tag) throws IOException {
        final int length = readLength();
        final int limit = pushLimit(length);
        while (!isAtEnd()) {
            reservePackedVarintCapacity(store);
            store.add(readInt32());
        }
        popLimit(limit);
    }

    /** Read a repeated (packed) {@code sint32} field value from the source. */
    public void readPackedSInt32(final RepeatedInt store, final int tag) throws IOException {
        final int length = readLength();
        final int limit = pushLimit(length);
        while (!isAtEnd()) {
            reservePackedVarintCapacity(store);
            store.add(readSInt32());
        }
        popLimit(limit);
    }

    /** Read a repeated {@code uint32} field value from the source. */
    public int readRepeatedUInt32(final RepeatedInt store, final int tag) throws IOException {
        return readRepeatedInt32(store, tag);
    }

    /** Read a repeated {@code int32} field value from the source. */
    public int readRepeatedInt32(final RepeatedInt store, final int tag) throws IOException {
        int nextTag;
        do {
            reserveRepeatedFieldCapacity(store, tag);
            store.add(readInt32());
        } while ((nextTag = readTag()) == tag);
        return nextTag;
    }

    /** Read a repeated {@code sint32} field value from the source. */
    public int readRepeatedSInt32(final RepeatedInt store, final int tag) throws IOException {
        int nextTag;
        do {
            reserveRepeatedFieldCapacity(store, tag);
            store.add(readSInt32());
        } while ((nextTag = readTag()) == tag);
        return nextTag;
    }

    /** Read a {@code uint32} field value from the source. */
    public int readUInt32() throws IOException {
        return readRawVarint32();
    }

    /** Read an {@code int32} field value from the source. */
    public int readInt32() throws IOException {
        return readRawVarint32();
    }

    /** Read an {@code sint32} field value from the source. */
    public int readSInt32() throws IOException {
        return decodeZigZag32(readRawVarint32());
    }

    /** Read a repeated (packed) {@code bool} field value from the source. */
    public void readPackedBool(RepeatedBoolean store) throws IOException {
        final int length = readLength();
        final int limit = pushLimit(length);
        store.reserve(length / MIN_BOOL_SIZE);
        while (!isAtEnd()) {
            store.add(readBool());
        }
        popLimit(limit);
    }

    /** Read a repeated (non-packed) {@code bool} field value from the source. */
    public int readRepeatedBool(final RepeatedBoolean store, final int tag) throws IOException {
        int nextTag;
        do {
            reserveRepeatedFieldCapacity(store, tag);
            store.add(readBool());
        } while ((nextTag = readTag()) == tag);
        return nextTag;
    }

    /** Read a {@code bool} field value from the source. */
    public boolean readBool() throws IOException {
        return readRawVarint64() != 0;
    }

    /** Read a repeated (packed) {@code enum} field value from the source. */
    public void readPackedEnum(final RepeatedEnum<?> store, final int tag) throws IOException {
        final int length = readLength();
        final int limit = pushLimit(length);
        while (!isAtEnd()) {
            reservePackedVarintCapacity(store);
            store.addValue(readEnum());
        }
        popLimit(limit);
    }

    /** Read a repeated {@code enum} field value from the source. */
    public int readRepeatedEnum(final RepeatedEnum<?> store, final int tag) throws IOException {
        int nextTag;
        do {
            reserveRepeatedFieldCapacity(store, tag);
            store.addValue(readEnum());
        } while ((nextTag = readTag()) == tag);
        return nextTag;
    }

    /**
     * Read an enum field value from the source.  Caller is responsible
     * for converting the numeric value to an actual enum.
     */
    public int readEnum() throws IOException {
        return readRawVarint32();
    }

    /** Read a length delimiter from the source. */
    public int readLength() throws IOException {
        return readRawVarint32();
    }

    // ------------------------------ DELIMITED TYPES ------------------------------

    /** Read a repeated {@code string} field value from the source. */
    public int readRepeatedString(final RepeatedString store, final int tag) throws IOException {
        int nextTag;
        do {
            reserveRepeatedFieldCapacity(store, tag);
            readString(store.next());
        } while ((nextTag = readTag()) == tag);
        return nextTag;
    }

    /** Read a {@code string} field value from the source. */
    public void readString(final Utf8String store) throws IOException {
        final int length = readLength();
        store.setSize(length);
        readRawBytes(store.bytes(), 0, length);
    }

    /** Read a repeated {@code group} field value from the source. */
    public int readRepeatedGroup(final RepeatedMessage<?> store, final int tag) throws IOException {
        int fieldNumber = WireFormat.getTagFieldNumber(tag);
        int nextTag;
        do {
            reserveRepeatedFieldCapacity(store, tag);
            readGroup(store.next(), fieldNumber);
        } while ((nextTag = readTag()) == tag);
        return nextTag;
    }

    /** Read a {@code group} field value from the source. */
    public void readGroup(final ProtoMessage<?> msg, final int fieldNumber) throws IOException {
        if (recursionDepth >= recursionLimit) {
            throw InvalidProtocolBufferException.recursionLimitExceeded();
        }
        ++recursionDepth;
        msg.mergeFrom(this);
        checkLastTagWas(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_END_GROUP));
        --recursionDepth;
    }

    /** Read a repeated {@code message} field value from the source. */
    public int readRepeatedMessage(final RepeatedMessage<?> store, final int tag) throws IOException {
        int nextTag;
        do {
            reserveRepeatedFieldCapacity(store, tag);
            readMessage(store.next());
        } while ((nextTag = readTag()) == tag);
        return nextTag;
    }

    public void readMessage(final ProtoMessage<?> msg) throws IOException {
        final int length = readLength();
        if (recursionDepth >= recursionLimit) {
            throw InvalidProtocolBufferException.recursionLimitExceeded();
        }
        final int oldLimit = pushLimit(length);
        ++recursionDepth;
        msg.mergeFrom(this);
        checkLastTagWas(0);
        --recursionDepth;
        popLimit(oldLimit);
    }

    /** Read a repeated {@code bytes} field value from the source. */
    public int readRepeatedBytes(final RepeatedBytes store, final int tag) throws IOException {
        int nextTag;
        do {
            reserveRepeatedFieldCapacity(store, tag);
            readBytes(store.next());
        } while ((nextTag = readTag()) == tag);
        return nextTag;
    }

    /** Read a {@code bytes} field value from the source. */
    public void readBytes(RepeatedByte store) throws IOException {
        // note: bytes type gets replaced rather than merged
        final int length = readLength();
        store.setLength(length);
        readRawBytes(store.array, 0, length);
    }

    /** Read raw {@code bytes} from the source. */
    public void readRawBytes(byte[] values, int offset, int length) throws IOException {
        for (int i = 0; i < length; i++) {
            values[offset + i] = readRawByte();
        }
    }

    // =================================================================

    public int readRawVarint32() throws IOException {
        // See implementation notes for readRawVarint64
        int x = readRawByte();
        if (x >= 0) {
            return x;
        } else if ((x ^= (readRawByte() << 7)) < 0) {
            return x ^ xorBits7;
        } else if ((x ^= (readRawByte() << 14)) >= 0) {
            return x ^ xorBits14;
        } else if ((x ^= (readRawByte() << 21)) < 0) {
            return x ^ xorBits21;
        } else {

            // Discard upper 32 bits.
            final int y = readRawByte();
            if (y < 0
                    && readRawByte() < 0
                    && readRawByte() < 0
                    && readRawByte() < 0
                    && readRawByte() < 0
                    && readRawByte() < 0) {
                throw InvalidProtocolBufferException.malformedVarint();
            }

            return x ^ (y << 28) ^ xorBits28;
        }
    }

    /** Read a raw Varint from the source. */
    public long readRawVarint64() throws IOException {
        // Implementation notes:
        //
        // Slightly modified version of the Protobuf-Java method. It
        // leverages sign extension of (signed) Java bytes. Instead of
        // eagerly masking the lower 7 bits, the signs can be eliminated
        // with an xor all at once.
        int y;
        if ((y = readRawByte()) >= 0) {
            return y;
        } else if ((y ^= (readRawByte() << 7)) < 0) {
            return y ^ xorBits7;
        } else if ((y ^= (readRawByte() << 14)) >= 0) {
            return y ^ xorBits14;
        } else if ((y ^= (readRawByte() << 21)) < 0) {
            return y ^ xorBits21;
        }

        long x;
        if ((x = y ^ ((long) readRawByte() << 28)) >= 0L) {
            return x ^ xorBits28L;
        } else if ((x ^= ((long) readRawByte() << 35)) < 0L) {
            return x ^ xorBits35L;
        } else if ((x ^= ((long) readRawByte() << 42)) >= 0L) {
            return x ^ xorBits42L;
        } else if ((x ^= ((long) readRawByte() << 49)) < 0L) {
            return x ^ xorBits49L;
        } else {
            // Note: the signed bit is always set, so this does not work for
            // non-canonical varints where positive numbers have been artificially
            // extended to 10 bytes.
            x ^= ((long) readRawByte() << 56) ^ xorBits56L;
            if (x < 0L) {
                if (readRawByte() < 0) {
                    throw InvalidProtocolBufferException.malformedVarint();
                }
            }
            return x;
        }
    }

    private static final int xorBits7 = ~0 << 7;
    private static final int xorBits14 = xorBits7 ^ (~0 << 14);
    private static final int xorBits21 = xorBits14 ^ (~0 << 21);
    private static final int xorBits28 = xorBits21 ^ (~0 << 28);
    private static final long xorBits28L = xorBits21 ^ (~0L << 28);
    private static final long xorBits35L = xorBits28L ^ (~0L << 35);
    private static final long xorBits42L = xorBits35L ^ (~0L << 42);
    private static final long xorBits49L = xorBits42L ^ (~0L << 49);
    private static final long xorBits56L = xorBits49L ^ (~0L << 56);

    long readRawVarint64SlowPath() throws IOException {
        long result = 0;
        for (int shift = 0; shift < 64; shift += 7) {
            final byte b = readRawByte();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw InvalidProtocolBufferException.malformedVarint();
    }

    /** Read a 16-bit little-endian integer from the source. */
    public short readRawLittleEndian16() throws IOException {
        return (short) ((readRawByte() & 0xFF) | (readRawByte() & 0xFF) << 8);
    }

    /** Read a 32-bit little-endian integer from the source. */
    public int readRawLittleEndian32() throws IOException {
        return (readRawByte() & 0xFF) |
                (readRawByte() & 0xFF) << 8 |
                (readRawByte() & 0xFF) << 16 |
                (readRawByte() & 0xFF) << 24;
    }

    /** Read a 64-bit little-endian integer from the source. */
    public long readRawLittleEndian64() throws IOException {
        return (readRawByte() & 0xFFL) |
                (readRawByte() & 0xFFL) << 8 |
                (readRawByte() & 0xFFL) << 16 |
                (readRawByte() & 0xFFL) << 24 |
                (readRawByte() & 0xFFL) << 32 |
                (readRawByte() & 0xFFL) << 40 |
                (readRawByte() & 0xFFL) << 48 |
                (readRawByte() & 0xFFL) << 56;
    }

    /**
     * Decode a ZigZag-encoded 32-bit value.  ZigZag encodes signed integers
     * into values that can be efficiently encoded with varint.  (Otherwise,
     * negative values must be sign-extended to 64 bits to be varint encoded,
     * thus always taking 10 bytes on the wire.)
     *
     * @param n An unsigned 32-bit integer, stored in a signed int because
     *          Java has no explicit unsigned support.
     * @return A signed 32-bit integer.
     */
    public static int decodeZigZag32(final int n) {
        return (n >>> 1) ^ -(n & 1);
    }

    /**
     * Decode a ZigZag-encoded 64-bit value.  ZigZag encodes signed integers
     * into values that can be efficiently encoded with varint.  (Otherwise,
     * negative values must be sign-extended to 64 bits to be varint encoded,
     * thus always taking 10 bytes on the wire.)
     *
     * @param n An unsigned 64-bit integer, stored in a signed int because
     *          Java has no explicit unsigned support.
     * @return A signed 64-bit integer.
     */
    public static long decodeZigZag64(final long n) {
        return (n >>> 1) ^ -(n & 1);
    }

    // -----------------------------------------------------------------

    private int lastTag;

    /** See setRecursionLimit() */
    protected int recursionDepth;
    protected int recursionLimit = DEFAULT_RECURSION_LIMIT;
    private static final int DEFAULT_RECURSION_LIMIT = 64;

    /** The absolute position of the end of the current message. */
    protected int currentLimit = NO_LIMIT;
    protected static final int NO_LIMIT = Integer.MAX_VALUE;

    /** see setDiscardUnknownFields */
    private boolean shouldDiscardUnknownFields = false;

    protected ProtoSource resetInternalState() {
        lastTag = 0;
        recursionDepth = 0;
        recursionLimit = DEFAULT_RECURSION_LIMIT;
        currentLimit = NO_LIMIT;
        return this;
    }

    protected ProtoSource() {
    }

    /**
     * Set the maximum message recursion depth. In order to prevent malicious
     * messages from causing stack overflows, {@code ProtoSource} limits
     * how deeply messages may be nested.  The default limit is 64.
     *
     * @return the old limit.
     */
    public int setRecursionLimit(final int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Recursion limit cannot be negative: " + limit);
        }
        final int oldLimit = recursionLimit;
        recursionLimit = limit;
        return oldLimit;
    }

    /**
     * Only valid for {@link InputStream}-backed streams.
     *
     * <p>Set the maximum message size. In order to prevent malicious messages from exhausting memory
     * or causing integer overflows, {@code ProtoSource} limits how large a message may be. The
     * default limit is {@code Integer.MAX_VALUE}. You should set this limit as small as you can
     * without harming your app's functionality. Note that size limits only apply when reading from an
     * {@code InputStream}, not when constructed around a raw byte array.
     *
     * <p>If you want to read several messages from a single CodedInputStream, you could call {@link
     * #resetSizeCounter()} after each one to avoid hitting the size limit.
     *
     * @return the old limit.
     */
    public int setSizeLimit(final int limit) {
        throw new UnsupportedOperationException("Only valid for stream backed sources");
    }

    /**
     * Resets the current size counter to zero (see {@link #setSizeLimit(int)}). Only valid for {@link
     * InputStream}-backed streams.
     * @return
     */
    public ProtoSource resetSizeCounter() {
        throw new UnsupportedOperationException("Only valid for stream backed sources");
    }

    /**
     * Sets this {@code ProtoSource} to discard unknown fields. This only impacts
     * messages that were generated with support for retaining unknown fields.
     *
     * <p>Note calling this function alone will have NO immediate effect on the underlying input data.
     * The unknown fields will be discarded during parsing.
     */
    public final ProtoSource discardUnknownFields() {
        shouldDiscardUnknownFields = true;
        return this;
    }

    /**
     * Reverts the unknown fields preservation behavior to the default. This only applies
     * to messages that were generated with support for retaining unknown fields.
     */
    public final ProtoSource unsetDiscardUnknownFields() {
        shouldDiscardUnknownFields = false;
        return this;
    }

    /**
     * Whether unknown fields in this input stream should be discarded during parsing into full
     * runtime messages.
     */
    final boolean shouldDiscardUnknownFields() {
        return shouldDiscardUnknownFields;
    }

    /**
     * Sets {@code currentLimit} to (current position) + {@code byteLimit}.  This
     * is called when descending into a length-delimited embedded message.
     *
     * @return the old limit.
     */
    public int pushLimit(int byteLimit) throws InvalidProtocolBufferException {
        if (byteLimit < 0) {
            throw InvalidProtocolBufferException.negativeSize();
        }
        byteLimit += getTotalBytesRead();
        if (byteLimit > currentLimit) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        final int oldLimit = currentLimit;
        currentLimit = byteLimit;
        return oldLimit;
    }

    /**
     * Discards the current limit, returning to the previous limit.
     *
     * @param oldLimit The old limit, as returned by {@code pushLimit}.
     */
    public void popLimit(int oldLimit) {
        currentLimit = oldLimit;
    }

    /**
     * Returns the number of bytes to be read before the current limit.
     * If no limit is set, returns -1.
     */
    public int getBytesUntilLimit() {
        if (currentLimit == Integer.MAX_VALUE) {
            return -1;
        }
        return currentLimit - getTotalBytesRead();
    }

    /**
     * Returns true if the source has reached the end of the input.  This is the
     * case if either the end of the underlying input source has been reached or
     * if the source has reached a limit created using {@link #pushLimit(int)}.
     * This function may get blocked when using {@link InputStream}-backed
     * streams as it tries to peek ahead.
     */
    public abstract boolean isAtEnd() throws IOException;

    /**
     * The total bytes read up to the current position. Calling {@link #resetSizeCounter()}
     * resets this value to zero on supported implementations.
     */
    public abstract int getTotalBytesRead();

    /**
     * Rewinds the internal position to match the desired number of bytes read.
     */
    public abstract void rewindTo(int totalBytesRead);

    /**
     * Read one byte from the input.
     *
     * @throws InvalidProtocolBufferException The end of the source or the current
     *                                        limit was reached.
     */
    public abstract byte readRawByte() throws IOException;

    /**
     * Reads and discards {@code size} bytes.
     *
     * @throws InvalidProtocolBufferException The end of the source or the current
     *                                        limit was reached.
     */
    public abstract void skipRawBytes(final int size) throws IOException;

    /**
     * Reserves space in the repeated field store to hold additional values. We assume that in
     * the common case repeated fields are contiguously serialized, so we can look ahead to
     * see how many values are remaining to avoid unnecessary allocations. Note that this is
     * an optimization that may do nothing if looking ahead is not supported.
     *
     * @param store target store
     * @param tag tag of the contained repeated field
     * @throws IOException
     */
    protected void reserveRepeatedFieldCapacity(RepeatedField<?,?> store, int tag) throws IOException {
        // implement in child classes that support looking ahead
    }

    /**
     * Reserves space in the repeated field store to hold the number of remaining varints until
     * the limit or end of stream is reached. Note that this is an optimization that may do
     * nothing if looking ahead is not supported.
     *
     * @param store target store
     * @throws IOException
     */
    protected void reservePackedVarintCapacity(RepeatedField<?,?> store) throws IOException {
        // implement in child classes that support looking ahead
    }

    /**
     * Reads a varint from the input one byte at a time, so that it does not read any bytes after the
     * end of the varint. If you simply wrapped the stream in a StreamSource and used {@link
     * #readRawVarint32()} then you may end up reading too many bytes.
     */
    public static int readRawVarint32(final InputStream input) throws IOException {
        final int firstByte = input.read();
        if (firstByte == -1) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        return readRawVarint32(firstByte, input);
    }

    /**
     * Like {@link #readRawVarint32(InputStream)}, but expects that the caller has already read one
     * byte. This allows the caller to determine if EOF has been reached before attempting to read.
     */
    public static int readRawVarint32(final int firstByte, final InputStream input)
            throws IOException {
        if ((firstByte & 0x80) == 0) {
            return firstByte;
        }

        int result = firstByte & 0x7f;
        int offset = 7;
        for (; offset < 32; offset += 7) {
            final int b = input.read();
            if (b == -1) {
                throw InvalidProtocolBufferException.truncatedMessage();
            }
            result |= (b & 0x7f) << offset;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        // Keep reading up to 64 bits.
        for (; offset < 64; offset += 7) {
            final int b = input.read();
            if (b == -1) {
                throw InvalidProtocolBufferException.truncatedMessage();
            }
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw InvalidProtocolBufferException.malformedVarint();
    }

    static class StreamSource extends ProtoSource {

        @Override
        public ProtoSource setInput(InputStream stream){
            this.input = stream;
            return resetInternalState();
        }

        @Override
        protected ProtoSource resetInternalState() {
            peekByte = EOF;
            position = 0;
            sizeLimit = NO_LIMIT;
            return super.resetInternalState();
        }

        @Override
        public ProtoSource clear() {
            return setInput(EMPTY_INPUT_STREAM);
        }

        @Override
        public int setSizeLimit(final int limit) {
            if (limit < 0) {
                throw new IllegalArgumentException("Size limit cannot be negative: " + limit);
            }
            final int oldLimit = sizeLimit;
            sizeLimit = limit;
            return oldLimit;
        }

        @Override
        public ProtoSource resetSizeCounter() {
            position = 0;
            return this;
        }

        @Override
        public boolean isAtEnd() throws IOException {
            return position == currentLimit || peek() == EOF;
        }

        @Override
        public int getTotalBytesRead() {
            return position;
        }

        @Override
        public void rewindTo(int totalBytesRead) {
            throw new UnsupportedOperationException("Stream can't be rewound");
        }

        @Override
        public byte readRawByte() throws IOException {
            if (position == sizeLimit || position == currentLimit || peek() == EOF) {
                require(1); // throws appropriate error
            }
            try {
                position++;
                return (byte) peekByte;
            } finally {
                peekByte = EOF;
            }
        }

        private int peek() throws IOException {
            if (peekByte == EOF && (peekByte = input.read()) == EOF && getBytesUntilLimit() > 0) {
                throw InvalidProtocolBufferException.truncatedMessage();
            }
            return peekByte;
        }

        @Override
        public void skipRawBytes(int length) throws IOException {
            if (length == 0) {
                return;
            }
            require(length);
            if (peekByte != EOF) {
                peekByte = EOF;
                length--;
            }
            if (input.skip(length) < length) {
                throw InvalidProtocolBufferException.truncatedMessage();
            }
        }

        @Override
        public void readRawBytes(byte[] buffer, int offset, int length) throws IOException {
            if (length == 0) {
                return;
            }
            require(length);
            if (peekByte != EOF) {
                buffer[offset++] = (byte) peekByte;
                length--;
                peekByte = EOF;
            }
            while (length > 0) {
                int n = input.read(buffer, offset, length);
                if (n == EOF) {
                    throw InvalidProtocolBufferException.truncatedMessage();
                }
                offset += n;
                length -= n;
            }
        }

        private void require(int size) throws IOException {
            final int newPosition = position + size;
            if (size < 0) {
                throw InvalidProtocolBufferException.negativeSize();
            } else if (newPosition - sizeLimit > 0) { // int overflow conscious
                throw InvalidProtocolBufferException.sizeLimitExceeded();
            } else if (newPosition > currentLimit) {
                throw InvalidProtocolBufferException.truncatedMessage();
            }
            position = newPosition;
        }

        private int peekByte = EOF;
        private int position = 0;
        private int sizeLimit = Integer.MAX_VALUE;
        private InputStream input = EMPTY_INPUT_STREAM;

        private static final InputStream EMPTY_INPUT_STREAM = new ByteArrayInputStream(ProtoUtil.EMPTY_BYTE_ARRAY);
        private static final int EOF = -1;

    }

    static class BufferSource extends ProtoSource {

        @Override
        public ProtoSource setInput(ByteBuffer buffer) {
            this.buffer = buffer;
            return resetInternalState();
        }

        @Override
        public ProtoSource clear() {
            return setInput(ProtoUtil.EMPTY_BYTE_BUFFER);
        }

        @Override
        public boolean isAtEnd() throws IOException {
            return buffer.position() == currentLimit || buffer.remaining() == 0;
        }

        @Override
        public int getTotalBytesRead() {
            return buffer.position();
        }

        @Override
        public void rewindTo(int totalBytesRead) {
            buffer.position(totalBytesRead);
        }

        @Override
        public byte readRawByte() throws IOException {
            try {
                return buffer.get();
            } catch (BufferUnderflowException truncated) {
                throw InvalidProtocolBufferException.truncatedMessage();
            }
        }

        @Override
        public void skipRawBytes(int size) throws IOException {
            if (buffer.remaining() < size) {
                throw InvalidProtocolBufferException.truncatedMessage();
            }
            buffer.position(buffer.position() + size);
        }

        @Override
        public void readRawBytes(byte[] values, int offset, int length) throws IOException {
            try {
                buffer.get(values, offset, length);
            } catch (BufferUnderflowException truncated) {
                throw InvalidProtocolBufferException.truncatedMessage();
            }
        }

        ByteBuffer buffer = ProtoUtil.EMPTY_BYTE_BUFFER;

    }

}
