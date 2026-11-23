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
import java.io.InputStream;

import static us.hebi.quickbuf.ProtoUtil.*;
import static us.hebi.quickbuf.ProtoUtil.Charsets.*;

/**
 * Reads proto messages from a JSON format compatible with
 * <p>
 * https://developers.google.com/protocol-buffers/docs/proto3#json
 *
 * @author Florian Enner
 * @since 08 Sep 2020
 */
public abstract class JsonSource implements Closeable {

    /**
     * Creates a source without input that can be reused via the setInput methods
     */
    public static JsonSource newInstance() {
        return new DefaultJsonSource(JsonReader.newInstance());
    }

    public static JsonSource newInstance(byte[] bytes) {
        return newInstance(bytes, 0, bytes.length);
    }

    public static JsonSource newInstance(byte[] bytes, int offset, int length) {
        return new DefaultJsonSource(JsonReader.newInstance(bytes, offset, length));
    }

    public static JsonSource newInstance(RepeatedByte bytes) {
        return newInstance(bytes.array, 0, bytes.length);
    }

    /**
     * Reads from the stream through an internal buffer, so the stream may
     * be read ahead past the end of the root value.
     */
    public static JsonSource newInstance(InputStream inputStream) {
        return new DefaultJsonSource(JsonReader.newInstance(inputStream));
    }

    public static JsonSource newInstance(String string) {
        return newInstance(string.getBytes(UTF_8));
    }

    /**
     * Parses a root-level object of the given message type and checks for required field initialization
     */
    public <ProtoMsg extends ProtoMessage<ProtoMsg>> ProtoMsg parseMessage(MessageFactory<ProtoMsg> factory) throws IOException {
        return readMessage(factory.create()).checkInitialized();
    }

    /**
     * Parses a root-level array of the given message type and checks for required field initialization
     */
    public <ProtoMsg extends ProtoMessage<ProtoMsg>> RepeatedMessage<ProtoMsg> parseRepeatedMessage(MessageFactory<ProtoMsg> factory) throws IOException {
        return readRepeatedMessage(RepeatedMessage.newEmptyInstance(factory)).checkInitialized();
    }

    // ==================== Ignoring unknown fields ====================

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
    public JsonSource setIgnoreUnknownFields(final boolean ignoreUnknownFields) {
        this.ignoreUnknownFields = ignoreUnknownFields;
        return this;
    }

    /**
     * Changes the input and resets the state, so the source can be reused without
     * allocations. Settings and internal buffers are kept.
     */
    public final JsonSource setInput(byte[] bytes) {
        return setInput(bytes, 0, bytes.length);
    }

    public JsonSource setInput(byte[] bytes, int offset, int length) {
        throw new UnsupportedOperationException("JsonSource does not support changing the input");
    }

    public final JsonSource setInput(RepeatedByte bytes) {
        return setInput(bytes.array, 0, bytes.length);
    }

    public final JsonSource setInput(String string) {
        return setInput(string.getBytes(UTF_8));
    }

    /**
     * Reads from the stream through an internal buffer, so the stream may
     * be read ahead past the end of the root value.
     */
    public JsonSource setInput(InputStream inputStream) {
        throw new UnsupportedOperationException("JsonSource does not support changing the input");
    }

    protected boolean ignoreUnknownFields = false;

    public void skipUnknownField() throws IOException {
        if (!ignoreUnknownFields) {
            throw error("Encountered unknown field: '" + fieldPath() + "'");
        }
        skipValue();
    }

    public void skipUnknownEnumValue() throws IOException {
        if (!ignoreUnknownFields) {
            throw error("Encountered unknown enum value on field: '" + fieldPath() + "'");
        }
    }

    // ==================== Core Types ====================

    /**
     * Read a {@code double} field value from the source.
     * <p>
     * JSON value will be a number or one of the special string values "NaN",
     * "Infinity", and "-Infinity". Either numbers or strings are accepted.
     * Exponent notation is also accepted. -0 is considered equivalent to 0.
     */
    public abstract double readDouble() throws IOException;

    /**
     * Read an {@code int32} field value from the source.
     * Either numbers or strings are accepted.
     */
    public abstract int readInt32() throws IOException;

    /**
     * Read an {@code int64} field value from the source.
     * Either numbers or strings are accepted.
     */
    public abstract long readInt64() throws IOException;

    /**
     * Read a {@code bool} field value from the source.
     * Parsers accept true or false.
     */
    public abstract boolean readBool() throws IOException;

    /**
     * Read a {@code enum} field value from the source.
     * <p>
     * The name of the enum value as specified in proto is used.
     * Parsers accept both enum names and integer values.
     *
     * @return enum object, or null if unknown
     */
    public abstract <T extends ProtoEnum<?>> T readEnum(final ProtoEnum.EnumConverter<T> converter) throws IOException;

    /**
     * Read a {@code string} field value from the source.
     */
    public abstract void readString(final Utf8String store) throws IOException;

    /**
     * Read a {@code bytes} field value from the source.
     * <p>
     * Either standard or URL-safe base64 encoding with/without paddings are accepted.
     */
    public abstract void readBytes(RepeatedByte store) throws IOException;

    /**
     * @return a ProtoSource of the binary bytes
     */
    public abstract ProtoSource readBytesAsSource() throws IOException;

    /**
     * Reads a message value from the source and merges the contents into the provided message
     */
    public <ProtoMsg extends ProtoMessage<ProtoMsg>> ProtoMsg readMessage(final ProtoMsg msg) throws IOException {
        if (recursionDepth >= recursionLimit) {
            throw InvalidJsonException.recursionLimitExceeded();
        }
        ++recursionDepth;
        msg.mergeFrom(this);
        --recursionDepth;
        return msg;
    }

    /**
     * Set the maximum message recursion depth. In order to prevent malicious
     * messages from causing stack overflows, {@code JsonSource} limits
     * how deeply messages may be nested. The default limit is 64.
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

    int recursionDepth = 0;
    private int recursionLimit = DEFAULT_RECURSION_LIMIT;
    private static final int DEFAULT_RECURSION_LIMIT = 64;

    /**
     * Skips the current value
     */
    protected abstract void skipValue() throws IOException;

    // ==================== Shared Overloads ====================

    /**
     * Read a {@code float} field value from the source.
     */
    public float readFloat() throws IOException {
        return (float) this.readDouble();
    }

    /**
     * Read a {@code uint64} field value from the source. Negative values
     * written by older versions represent the same bits.
     */
    public long readUInt64() throws IOException {
        return this.readInt64(); // implementations should override to support values beyond Long.MAX_VALUE
    }

    /**
     * Read a {@code fixed64} field value from the source.
     */
    public long readFixed64() throws IOException {
        return this.readUInt64();
    }

    /**
     * Read a {@code fixed32} field value from the source.
     */
    public int readFixed32() throws IOException {
        return this.readUInt32();
    }

    /**
     * Read a {@code group} field value from the source.
     */
    public <ProtoMsg extends ProtoMessage<ProtoMsg>> ProtoMsg readGroup(final ProtoMsg msg) throws IOException {
        return readMessage(msg);
    }

    /**
     * Read a {@code uint32} field value from the source. Negative values
     * written by older versions represent the same bits.
     */
    public int readUInt32() throws IOException {
        final long value = this.readInt64();
        if (value < Integer.MIN_VALUE || value > 0xFFFFFFFFL) {
            throw outOfRange("uint32", value);
        }
        return (int) value;
    }

    /**
     * Read an {@code sfixed32} field value from the source.
     */
    public int readSFixed32() throws IOException {
        return this.readInt32();
    }

    /**
     * Read an {@code sfixed64} field value from the source.
     */
    public long readSFixed64() throws IOException {
        return this.readInt64();
    }

    /**
     * Read an {@code sint32} field value from the source.
     */
    public int readSInt32() throws IOException {
        return this.readInt32();
    }

    /**
     * Read an {@code sint64} field value from the source.
     */
    public long readSInt64() throws IOException {
        return this.readInt64();
    }

    public void readRepeatedFixed64(final RepeatedLong value) throws IOException {
        readRepeatedUInt64(value);
    }

    public void readRepeatedSFixed64(final RepeatedLong value) throws IOException {
        readRepeatedInt64(value);
    }

    public void readRepeatedUInt64(final RepeatedLong value) throws IOException {
        beginArray();
        while (!isAtEnd()) {
            value.add(readUInt64());
        }
        endArray();
    }

    public void readRepeatedSInt64(final RepeatedLong value) throws IOException {
        readRepeatedInt64(value);
    }

    public void readRepeatedFixed32(final RepeatedInt value) throws IOException {
        readRepeatedUInt32(value);
    }

    public void readRepeatedSFixed32(final RepeatedInt value) throws IOException {
        readRepeatedInt32(value);
    }

    public void readRepeatedUInt32(final RepeatedInt value) throws IOException {
        beginArray();
        while (!isAtEnd()) {
            value.add(readUInt32());
        }
        endArray();
    }

    public void readRepeatedSInt32(final RepeatedInt value) throws IOException {
        readRepeatedInt32(value);
    }

    public void readRepeatedGroup(final RepeatedMessage<?> value) throws IOException {
        readRepeatedMessage(value);
    }

    public void readRepeatedDouble(final RepeatedDouble value) throws IOException {
        beginArray();
        while (!isAtEnd()) {
            value.add(readDouble());
        }
        endArray();
    }

    public void readRepeatedInt64(final RepeatedLong value) throws IOException {
        beginArray();
        while (!isAtEnd()) {
            value.add(readInt64());
        }
        endArray();
    }

    public void readRepeatedFloat(final RepeatedFloat value) throws IOException {
        beginArray();
        while (!isAtEnd()) {
            value.add(readFloat());
        }
        endArray();
    }

    public void readRepeatedInt32(final RepeatedInt value) throws IOException {
        beginArray();
        while (!isAtEnd()) {
            value.add(readInt32());
        }
        endArray();
    }

    public <ProtoMsg extends ProtoMessage<ProtoMsg>> RepeatedMessage<ProtoMsg> readRepeatedMessage(final RepeatedMessage<ProtoMsg> value) throws IOException {
        beginArray();
        while (!isAtEnd()) {
            readMessage(value.next());
        }
        endArray();
        return value;
    }

    public void readRepeatedString(final RepeatedString value) throws IOException {
        beginArray();
        while (!isAtEnd()) {
            readString(value.next());
        }
        endArray();
    }

    public <E extends ProtoEnum<?>> void readRepeatedEnum(final RepeatedEnum<E> value, final ProtoEnum.EnumConverter<E> converter) throws IOException {
        beginArray();
        while (!isAtEnd()) {
            E val = readEnum(converter);
            value.addValue(val == null ? 0 : val.getNumber());
        }
        endArray();
    }

    public void readRepeatedBool(final RepeatedBoolean value) throws IOException {
        beginArray();
        while (!isAtEnd()) {
            value.add(readBool());
        }
        endArray();
    }

    public void readRepeatedBytes(final RepeatedBytes value) throws IOException {
        beginArray();
        while (!isAtEnd()) {
            readBytes(value.next());
        }
        endArray();
    }

    public abstract void beginArray() throws IOException;

    public abstract void endArray() throws IOException;

    // ==================== Methods for Object Mapping ====================

    /**
     * Consumes the begin object token. Fails if it encounters anything else.
     *
     * @return always true. Not void for backwards compatibility reasons.
     */
    public abstract boolean beginObject() throws IOException;

    public abstract void endObject() throws IOException;

    /**
     * Returns true if the source has reached the end of the input,
     * or the end of an object or array.
     */
    public abstract boolean isAtEnd() throws IOException;

    public boolean trySkipNullValue() throws IOException {
        if (isAtNull()) {
            skipValue();
            return true;
        }
        return false;
    }

    protected abstract boolean isAtNull() throws IOException;

    public final int readFieldHash() throws IOException {
        currentField = readFieldName();
        if (currentField instanceof JsonReader.CharBuffer) {
            return ((JsonReader.CharBuffer) currentField).hash(); // computed while decoding
        }
        return ProtoUtil.hash32(currentField);
    }

    /**
     * @return next field hash or zero if hasNext() returns false
     */
    public final int readFieldHashOrZero() throws IOException {
        return isAtEnd() ? 0 : readFieldHash();
    }

    /**
     * @param fieldName expected field
     * @return true if the next value is for the expected field and not null
     */
    public boolean isAtField(FieldName fieldName) {
        return (ProtoUtil.isEqual(fieldName.getJsonName(), currentField) ||
                ProtoUtil.isEqual(fieldName.getProtoName(), currentField));
    }

    /**
     * @return a char sequence that does not get modified between subsequent calls to this method
     */
    protected abstract CharSequence readFieldName() throws IOException;

    CharSequence currentField = null;

    // ==================== Implementation ====================

    InvalidJsonException outOfRange(String type, Object value) {
        return fieldError("Value out of range for " + type + ": " + value);
    }

    InvalidJsonException fieldError(String message) {
        String atField = (currentField == null ? "" : " for field '" + currentField + "'");
        return new InvalidJsonException(message + atField);
    }

    /** Error for a message that already names the field */
    InvalidJsonException error(String message) {
        return new InvalidJsonException(message);
    }

    CharSequence fieldPath() {
        return currentField;
    }

    // Note: proto3 json accepts base64 with and without padding
    protected static void decodeBase64(String input, RepeatedByte output) {
        final byte[] bytes = input.getBytes(ISO_8859_1); // other chars become an ignored '?'
        output.addAll(decodeBase64Unchecked(bytes, bytes.length));
    }

    protected static void decodeBase64(RepeatedByte input, RepeatedByte output) {
        output.addAll(decodeBase64Unchecked(input.array, input.length));
    }

    static byte[] decodeBase64(byte[] input, int length) throws InvalidJsonException {
        final byte[] bytes = Base64.decode(input, 0, length);
        if (bytes == null) {
            throw InvalidJsonException.invalidBase64();
        }
        return bytes;
    }

    static void decodeBase64(byte[] input, int length, RepeatedByte output) throws InvalidJsonException {
        final int decodedLength = Base64.decodedLength(input, 0, length);
        if (decodedLength < 0) {
            throw InvalidJsonException.invalidBase64();
        }
        final int offset = output.addLength(decodedLength);
        Base64.decode(input, 0, length, output.array, offset, decodedLength);
    }

    private static byte[] decodeBase64Unchecked(byte[] input, int length) {
        final byte[] bytes = Base64.decode(input, 0, length);
        if (bytes == null) {
            throw new IllegalArgumentException("Invalid base64 value.");
        }
        return bytes;
    }


    /**
     * Maps protobuf messages onto a lenient JsonReader. Scalars are read as raw bytes because
     * the protobuf mapping also accepts quoted numbers and the strings NaN, Infinity and -Infinity.
     */
    static final class DefaultJsonSource extends JsonSource {

        DefaultJsonSource(JsonReader reader) {
            this.reader = reader;
            this.reader.lenient = true;
        }

        @Override
        public JsonSource setInput(byte[] bytes, int offset, int length) {
            reader.setInput(bytes, offset, length);
            currentField = null;
            recursionDepth = 0;
            return this;
        }

        @Override
        public JsonSource setInput(InputStream inputStream) {
            reader.setInput(inputStream);
            currentField = null;
            recursionDepth = 0;
            return this;
        }

        /** Values beyond the double range become infinite like 1.4, the JsonReader rejects them */
        @Override
        public double readDouble() throws IOException {
            readValueRange();
            if (isValueNumber() || isValueNumberWithLeadingZeros()) {
                return reader.toDouble(reader.valueArray, reader.valueStart, reader.valueEnd);
            }
            return readSpecialValue();
        }

        /**
         * Rounds once, unlike 1.4 which cast the double. Values beyond the float range become
         * infinite like 1.4, the JsonReader rejects them.
         */
        @Override
        public float readFloat() throws IOException {
            readValueRange();
            if (isValueNumber() || isValueNumberWithLeadingZeros()) {
                return reader.toFloat(reader.valueArray, reader.valueStart, reader.valueEnd);
            }
            return (float) readSpecialValue();
        }

        @Override
        public int readInt32() throws IOException {
            final long value = this.readInt64();
            if ((int) value != value) {
                throw outOfRange("int32", value);
            }
            return (int) value;
        }

        @Override
        public long readInt64() throws IOException {
            readValueRange();
            if (isValueInteger() && (reader.numberMantissa >= 0 || (reader.numberNegative && reader.numberMantissa == Long.MIN_VALUE))) {
                return reader.numberNegative ? -reader.numberMantissa : reader.numberMantissa;
            }
            try {
                return JsonDecoding.Numbers.readLong(reader.valueArray, reader.valueStart, reader.valueEnd);
            } catch (InvalidJsonException invalid) {
                throw invalidNumber();
            }
        }

        @Override
        public long readUInt64() throws IOException {
            readValueRange();
            if (isValueInteger()) {
                if (!reader.numberNegative) {
                    return reader.numberMantissa;
                } else if (reader.numberMantissa >= 0 || reader.numberMantissa == Long.MIN_VALUE) {
                    return -reader.numberMantissa;
                }
            }
            try {
                return JsonDecoding.Numbers.readUnsignedLong(reader.valueArray, reader.valueStart, reader.valueEnd);
            } catch (InvalidJsonException invalid) {
                throw invalidNumber();
            }
        }

        @Override
        public boolean readBool() throws IOException {
            return reader.readBoolean();
        }

        @Override
        public <T extends ProtoEnum<?>> T readEnum(ProtoEnum.EnumConverter<T> converter) throws IOException {
            readValueBytes();
            if (JsonDecoding.Numbers.isInteger(buffer.array, 0, buffer.length)) {
                return converter.forNumber((int) JsonDecoding.Numbers.readLong(buffer.array, 0, buffer.length));
            } else {
                return converter.forName(bufferViewAscii);
            }
        }

        @Override
        public void readString(Utf8String store) throws IOException {
            checkNotAtNull();
            reader.readString(store);
        }

        @Override
        public void readBytes(RepeatedByte store) throws IOException {
            readEscapedString();
            store.clear();
            decodeBase64(buffer.array, buffer.length, store);
        }

        @Override
        public ProtoSource readBytesAsSource() throws IOException {
            readEscapedString();
            return ProtoSource.newInstance(decodeBase64(buffer.array, buffer.length));
        }

        @Override
        public void skipValue() throws IOException {
            reader.skipValue();
        }

        @Override
        public boolean beginObject() throws IOException {
            reader.beginObject();
            return true;
        }

        @Override
        public void endObject() throws IOException {
            reader.endObject();
        }

        @Override
        public void beginArray() throws IOException {
            reader.beginArray();
        }

        @Override
        public void endArray() throws IOException {
            reader.endArray();
        }

        @Override
        public boolean isAtEnd() throws IOException {
            return !reader.hasNext();
        }

        @Override
        public boolean isAtField(FieldName fieldName) {
            return reader.isAtName(fieldName.getJsonName()) || reader.isAtName(fieldName.getProtoName());
        }

        @Override
        protected boolean isAtNull() throws IOException {
            return reader.isAtNull();
        }

        @Override
        protected CharSequence readFieldName() throws IOException {
            return reader.readName();
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

        private void readValueBytes() throws IOException {
            if (!reader.readValueBytes(buffer)) {
                throw nullValue();
            }
        }

        private void readEscapedString() throws IOException {
            checkNotAtNull();
            reader.readString(buffer);
        }

        private void readValueRange() throws IOException {
            if (!reader.readValueRange()) {
                throw nullValue();
            }
        }

        /** True if the value range is a JSON number. NaN and leading zeros take the slower paths. */
        private boolean isValueNumber() {
            return reader.parseNumber(reader.valueArray, reader.valueStart, reader.valueEnd) == reader.valueEnd
                    && reader.numberErrorText == null;
        }

        /**
         * Parses the value range if it is an integral JSON number, e.g., 1, 1.0 or 1e3, into
         * the unsigned magnitude of the reader
         */
        private boolean isValueInteger() {
            return isValueNumber() && (!reader.numberFractional || reader.scaleToInteger()) && reader.numberDigits <= 19;
        }

        /**
         * Earlier versions accepted leading zeros, e.g., 007 or -00.5, which JSON does not allow
         */
        private boolean isValueNumberWithLeadingZeros() {
            final byte[] buf = reader.valueArray;
            final int end = reader.valueEnd;
            final boolean negative = reader.valueStart < end && buf[reader.valueStart] == '-';
            final int start = negative ? reader.valueStart + 1 : reader.valueStart;
            int i = start;
            while (i + 1 < end && buf[i] == '0' && buf[i + 1] >= '0' && buf[i + 1] <= '9') {
                i++;
            }
            if (i == start || reader.parseNumber(buf, i, end) != end || reader.numberErrorText != null) {
                return false;
            }
            reader.numberNegative = negative;
            return true;
        }

        /**
         * Reads the special values of the protobuf mapping, quoted or unquoted
         */
        private double readSpecialValue() throws IOException {
            if (isValue(NAN)) {
                return Double.NaN;
            } else if (isValue(POSITIVE_INFINITY)) {
                return Double.POSITIVE_INFINITY;
            } else if (isValue(NEGATIVE_INFINITY)) {
                return Double.NEGATIVE_INFINITY;
            }
            throw invalidNumber();
        }

        private InvalidJsonException invalidNumber() {
            String value = new String(reader.valueArray, reader.valueStart, reader.valueEnd - reader.valueStart, Charsets.UTF_8);
            return fieldError("Invalid number '" + value + "'");
        }

        private boolean isValue(byte[] expected) {
            if (reader.valueEnd - reader.valueStart != expected.length) {
                return false;
            }
            for (int i = 0; i < expected.length; i++) {
                if (reader.valueArray[reader.valueStart + i] != expected[i]) {
                    return false;
                }
            }
            return true;
        }

        private void checkNotAtNull() throws IOException {
            if (reader.isAtNull()) {
                throw nullValue();
            }
        }

        private InvalidJsonException nullValue() {
            return fieldError("Expected non-null value");
        }

        @Override
        InvalidJsonException fieldError(String message) {
            return reader.newError(message);
        }

        @Override
        InvalidJsonException error(String message) {
            return new InvalidJsonException(message, reader.getPosition());
        }

        @Override
        CharSequence fieldPath() {
            return reader.getPath();
        }

        private static final byte[] NAN = "NaN".getBytes(ASCII);
        private static final byte[] POSITIVE_INFINITY = "Infinity".getBytes(ASCII);
        private static final byte[] NEGATIVE_INFINITY = "-Infinity".getBytes(ASCII);

        private final JsonReader reader;
        private final RepeatedByte buffer = RepeatedByte.newEmptyInstance();

        private final CharSequence bufferViewAscii = new CharSequence() {
            @Override
            public int length() {
                return buffer.length();
            }

            @Override
            public char charAt(int index) {
                return (char) buffer.get(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toString() {
                return new String(buffer.array, 0, buffer.length, Charsets.ASCII);
            }

        };

    }
}
