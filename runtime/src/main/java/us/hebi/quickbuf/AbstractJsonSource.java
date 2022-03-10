/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2020 HEBI Robotics
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

import java.io.Closeable;
import java.io.IOException;

/**
 * Reads proto messages from a JSON format compatible with
 * <p>
 * https://developers.google.com/protocol-buffers/docs/proto3#json
 *
 * @author Florian Enner
 * @since 08 Sep 2020
 */
public abstract class AbstractJsonSource<SubType extends AbstractJsonSource<SubType>> implements Closeable {

    // ==================== Common Type Forwarders ====================

    /**
     * Read a {@code double} field value from the source.
     */
    public double readDouble() throws IOException {
        return nextDouble();
    }

    /**
     * Read an {@code int32} field value from the source.
     */
    public int readInt32() throws IOException {
        return nextInt();
    }

    /**
     * Read an {@code int64} field value from the source.
     */
    public long readInt64() throws IOException {
        return nextLong();
    }

    /**
     * Read a {@code bool} field value from the source.
     */
    public boolean readBool() throws IOException {
        return nextBoolean();
    }

    /**
     * Read a {@code enum} field value from the source.
     *
     * @return enum object, or null if unknown
     */
    public <T extends ProtoEnum<?>> T readEnum(final ProtoEnum.EnumConverter<T> converter) throws IOException {
        return nextEnum(converter);
    }

    /**
     * Read a {@code string} field value from the source.
     */
    public void readString(final Utf8String store) throws IOException {
        nextString(store);
    }

    /**
     * Read a nested message value from the source
     */
    public void readMessage(final ProtoMessage<?> msg) throws IOException {
        msg.mergeFrom(this);
    }

    /**
     * Read a {@code bytes} field value from the source.
     */
    public void readBytes(RepeatedByte store) throws IOException {
        nextBase64(store);
    }

    /**
     * Read a {@code float} field value from the source.
     */
    public float readFloat() throws IOException {
        return (float) nextDouble();
    }

    /**
     * Read a {@code uint64} field value from the source.
     */
    public long readUInt64() throws IOException {
        return nextLong();
    }

    /**
     * Read a {@code fixed64} field value from the source.
     */
    public long readFixed64() throws IOException {
        return nextLong();
    }

    /**
     * Read a {@code fixed32} field value from the source.
     */
    public int readFixed32() throws IOException {
        return nextInt();
    }

    /**
     * Read a {@code group} field value from the source.
     */
    public void readGroup(final ProtoMessage<?> msg)
            throws IOException {
        readMessage(msg);
    }

    /**
     * Read a {@code uint32} field value from the source.
     */
    public int readUInt32() throws IOException {
        return nextInt();
    }

    /**
     * Read an {@code sfixed32} field value from the source.
     */
    public int readSFixed32() throws IOException {
        return nextInt();
    }

    /**
     * Read an {@code sfixed64} field value from the source.
     */
    public long readSFixed64() throws IOException {
        return nextLong();
    }

    /**
     * Read an {@code sint32} field value from the source.
     */
    public int readSInt32() throws IOException {
        return nextInt();
    }

    /**
     * Read an {@code sint64} field value from the source.
     */
    public long readSInt64() throws IOException {
        return nextLong();
    }

    public void readRepeatedFixed64(final RepeatedLong value) throws IOException {
        readRepeatedInt64(value);
    }

    public void readRepeatedSFixed64(final RepeatedLong value) throws IOException {
        readRepeatedInt64(value);
    }

    public void readRepeatedUInt64(final RepeatedLong value) throws IOException {
        readRepeatedInt64(value);
    }

    public void readRepeatedSInt64(final RepeatedLong value) throws IOException {
        readRepeatedInt64(value);
    }

    public void readRepeatedFixed32(final RepeatedInt value) throws IOException {
        readRepeatedInt32(value);
    }

    public void readRepeatedSFixed32(final RepeatedInt value) throws IOException {
        readRepeatedInt32(value);
    }

    public void readRepeatedUInt32(final RepeatedInt value) throws IOException {
        readRepeatedInt32(value);
    }

    public void readRepeatedSInt32(final RepeatedInt value) throws IOException {
        readRepeatedInt32(value);
    }

    public void readRepeatedGroup(final RepeatedMessage<?> value) throws IOException {
        readRepeatedMessage(value);
    }

    // ==================== Shared Implementations ====================

    public void readRepeatedDouble(final RepeatedDouble value) throws IOException {
        if (!beginArray()) return;
        while (hasNext()) {
            value.add(readDouble());
        }
        endArray();
    }

    public void readRepeatedInt64(final RepeatedLong value) throws IOException {
        if (!beginArray()) return;
        while (hasNext()) {
            value.add(readInt64());
        }
        endArray();
    }

    public void readRepeatedFloat(final RepeatedFloat value) throws IOException {
        if (!beginArray()) return;
        while (hasNext()) {
            value.add(readFloat());
        }
        endArray();
    }

    public void readRepeatedInt32(final RepeatedInt value) throws IOException {
        if (!beginArray()) return;
        while (hasNext()) {
            value.add(readInt32());
        }
        endArray();
    }

    public void readRepeatedMessage(final RepeatedMessage<?> value) throws IOException {
        if (!beginArray()) return;
        while (hasNext()) {
            readMessage(value.next());
        }
        endArray();
    }

    public void readRepeatedString(final RepeatedString value) throws IOException {
        if (!beginArray()) return;
        while (hasNext()) {
            readString(value.next());
        }
        endArray();
    }

    public <E extends ProtoEnum<?>> void readRepeatedEnum(final RepeatedEnum<E> value, final ProtoEnum.EnumConverter<E> converter) throws IOException {
        if (!beginArray()) return;
        while (hasNext()) {
            E val = nextEnum(converter);
            value.addValue(val == null ? 0 : val.getNumber());
        }
        endArray();
    }

    public void readRepeatedBool(final RepeatedBoolean value) throws IOException {
        if (!beginArray()) return;
        while (hasNext()) {
            value.add(readBool());
        }
        endArray();
    }

    public void readRepeatedBytes(final RepeatedBytes value) throws IOException {
        if (!beginArray()) return;
        while (hasNext()) {
            readBytes(value.next());
        }
        endArray();
    }

    public void skipUnknownField() throws IOException {
        if (!ignoreUnknownFields) {
            throw new IllegalArgumentException("Encountered unknown field: " + currentField);
        }
        skipValue();
    }

    public void skipUnknownEnumValue() throws IOException {
        if (!ignoreUnknownFields) {
            throw new IllegalArgumentException("Encountered unknown enum value on field: " + currentField);
        }
    }

    // ==================== Configuration ====================

    /**
     * Allows to serialize enums as human readable strings or
     * as JSON numbers. Compatible parsers are able to parse
     * either case.
     * <p>
     * Unknown values will still be serialized as numbers.
     *
     * @param ignoreUnknownFields true if values should use strings
     * @return this
     */
    public SubType setIgnoreUnknownFields(final boolean ignoreUnknownFields) {
        this.ignoreUnknownFields = ignoreUnknownFields;
        return thisObj();
    }

    protected boolean ignoreUnknownFields = false;

    // ==================== Implementation Interface ====================

    protected abstract SubType thisObj();

    /**
     * JSON value will be a number or one of the special string values "NaN",
     * "Infinity", and "-Infinity". Either numbers or strings are accepted.
     * Exponent notation is also accepted. -0 is considered equivalent to 0.
     */
    public abstract double nextDouble() throws IOException;

    /**
     * Either numbers or strings are accepted.
     */
    public abstract int nextInt() throws IOException;

    /**
     * Either numbers or strings are accepted.
     */
    public abstract long nextLong() throws IOException;

    public abstract boolean nextBoolean() throws IOException;

    /**
     * The name of the enum value as specified in proto is used. Parsers accept both enum names and integer values.
     */
    public abstract <T extends ProtoEnum<?>> T nextEnum(final ProtoEnum.EnumConverter<T> converter) throws IOException;

    public abstract void nextString(final Utf8String store) throws IOException;

    /**
     * Either standard or URL-safe base64 encoding with/without paddings are accepted.
     */
    public void nextBase64(RepeatedByte store) throws IOException {
        try {
            store.clear();
            nextString(tmpString);
            if (tmpString.hasString()) {
                store.copyFrom(Base64.decodeFast(tmpString.getString()));
            } else {
                store.copyFrom(Base64.decode(tmpString.bytes(), 0, tmpString.size()));
            }
        } finally {
            tmpString.clear();
        }
    }

    public abstract void skipValue() throws IOException;

    /**
     * Consumes the begin element token or null element.
     *
     * @return true if the begin object element was consumed, i.e., not null
     */
    public abstract boolean beginObject() throws IOException;

    public abstract void endObject() throws IOException;

    /**
     * Consumes the begin array token or null element.
     *
     * @return true if the begin array element was consumed, i.e., not null
     */
    protected abstract boolean beginArray() throws IOException;

    public abstract void endArray() throws IOException;

    /**
     * @return true if the current object or array has more elements
     */
    public abstract boolean hasNext() throws IOException;

    /**
     * @return next field hash or zero if hasNext() returns false
     */
    public final int nextFieldHashOrZero() throws IOException {
        return hasNext() ? nextFieldHash() : 0;
    }

    public final int nextFieldHash() throws IOException {
        currentField = nextName();
        return ProtoUtil.hash32(currentField);
    }

    /**
     * @return a char sequence that does not get modified between subsequent calls to this method
     */
    protected abstract CharSequence nextName() throws IOException;

    public boolean isAtField(FieldName fieldName) {
        return ProtoUtil.isEqual(fieldName.getJsonName(), currentField) ||
                ProtoUtil.isEqual(fieldName.getProtoName(), currentField);
    }

    CharSequence currentField = null;
    Utf8String tmpString = Utf8String.newEmptyInstance();

}
