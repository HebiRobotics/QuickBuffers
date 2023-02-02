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

import us.hebi.quickbuf.JsonDecoding.JsonLexer;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static us.hebi.quickbuf.JsonDecoding.IntChar.*;
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

    public static JsonSource newInstance(byte[] bytes) {
        return newInstance(bytes, 0, bytes.length);
    }

    public static JsonSource newInstance(byte[] bytes, int offset, int length) {
        return new ArraySource(bytes, offset, length);
    }

    public static JsonSource newInstance(RepeatedByte bytes) {
        return newInstance(bytes.array, 0, bytes.length);
    }

    public static JsonSource newInstance(InputStream inputStream) {
        return new InputStreamSource(inputStream);
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

    protected boolean ignoreUnknownFields = false;

    public void skipUnknownField() throws IOException {
        if (!ignoreUnknownFields) {
            throw new InvalidJsonException("Encountered unknown field: '" + currentField + "'");
        }
        skipValue();
    }

    public void skipUnknownEnumValue() throws IOException {
        if (!ignoreUnknownFields) {
            throw new InvalidJsonException("Encountered unknown enum value on field: '" + currentField + "'");
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
        msg.mergeFrom(this);
        return msg;
    }

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
     * Read a {@code uint64} field value from the source.
     */
    public long readUInt64() throws IOException {
        return this.readInt64(); // TODO: read unsigned 64
    }

    /**
     * Read a {@code fixed64} field value from the source.
     */
    public long readFixed64() throws IOException {
        return this.readInt64(); // TODO: read unsigned 64
    }

    /**
     * Read a {@code fixed32} field value from the source.
     */
    public int readFixed32() throws IOException {
        return this.readInt32(); // TODO: read unsigned 32
    }

    /**
     * Read a {@code group} field value from the source.
     */
    public <ProtoMsg extends ProtoMessage<ProtoMsg>> ProtoMsg readGroup(final ProtoMsg msg) throws IOException {
        return readMessage(msg);
    }

    /**
     * Read a {@code uint32} field value from the source.
     */
    public int readUInt32() throws IOException {
        return this.readInt32(); // TODO: read unsigned 32
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
        readRepeatedInt64(value); // TODO: read unsigned 64
    }

    public void readRepeatedSFixed64(final RepeatedLong value) throws IOException {
        readRepeatedInt64(value);
    }

    public void readRepeatedUInt64(final RepeatedLong value) throws IOException {
        readRepeatedInt64(value); // TODO: read unsigned 64
    }

    public void readRepeatedSInt64(final RepeatedLong value) throws IOException {
        readRepeatedInt64(value);
    }

    public void readRepeatedFixed32(final RepeatedInt value) throws IOException {
        readRepeatedInt32(value); // TODO: read unsigned 32
    }

    public void readRepeatedSFixed32(final RepeatedInt value) throws IOException {
        readRepeatedInt32(value);
    }

    public void readRepeatedUInt32(final RepeatedInt value) throws IOException {
        readRepeatedInt32(value); // TODO: read unsigned 32
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

    protected static void decodeBase64(String input, RepeatedByte output) {
        output.addAll(Base64.decodeFast(input));
    }

    protected static void decodeBase64(RepeatedByte input, RepeatedByte output) {
        output.addAll(Base64.decode(input.array, 0, input.length));
    }


    static class ArraySource extends DefaultJsonSource {

        ArraySource(byte[] bytes, int offset, int length) {
            ProtoUtil.checkBounds(bytes, offset, length);
            this.bytes = bytes;
            this.position = offset;
            this.limit = offset + length;
        }

        @Override
        public int readRawByte() {
            if (position == limit) {
                return -1;
            }
            return bytes[position++] & 0xFF;
        }

        int position = 0;
        final int limit;
        final byte[] bytes;

        @Override
        public void close() throws IOException {
        }

    }


    static class InputStreamSource extends DefaultJsonSource {

        InputStreamSource(InputStream inputStream) {
            this.inputStream = checkNotNull(inputStream);
        }

        @Override
        public int readRawByte() throws IOException {
            return inputStream.read();
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }

        final InputStream inputStream;

    }

    abstract static class DefaultJsonSource extends JsonSource implements JsonLexer {

        @Override
        public double readDouble() throws IOException {
            getValueAsBytes(buffer);
            return JsonDecoding.Numbers.readDouble(buffer.array, 0, buffer.length);
        }

        @Override
        public int readInt32() throws IOException {
            return (int) this.readInt64();
        }

        @Override
        public long readInt64() throws IOException {
            getValueAsBytes(buffer);
            return JsonDecoding.Numbers.readLong(buffer.array, 0, buffer.length);
        }

        @Override
        public boolean readBool() throws IOException {
            if (token == INT_t) {
                readExpectedBytes(TRUE_VALUE);
                readNextToken();
                return true;
            } else if (token == INT_f) {
                readExpectedBytes(FALSE_VALUE);
                readNextToken();
                return false;
            } else {
                throw new InvalidJsonException("Unsupported boolean value");
            }
        }

        @Override
        public <T extends ProtoEnum<?>> T readEnum(ProtoEnum.EnumConverter<T> converter) throws IOException {
            getValueAsBytes(buffer);
            if (JsonDecoding.Numbers.isInteger(buffer.array, 0, buffer.length)) {
                return converter.forNumber((int) JsonDecoding.Numbers.readLong(buffer.array, 0, buffer.length));
            } else {
                return converter.forName(bufferViewAscii);
            }
        }

        @Override
        public void readString(Utf8String store) throws IOException {
            checkNotAtNull();
            checkCurrentToken(INT_QUOTE);
            JsonDecoding.StringDecoding.readQuotedUtf8(this, buffer);
            store.copyFromUtf8(buffer.array, 0, buffer.length);
            token = readNextToken();
        }

        @Override
        public void readBytes(RepeatedByte store) throws IOException {
            getValueAsBytes(buffer);
            store.clear();
            decodeBase64(buffer, store);
        }

        @Override
        public ProtoSource readBytesAsSource() throws IOException {
            getValueAsBytes(buffer);
            return ProtoSource.newInstance(Base64.decode(buffer.array, 0, buffer.length));
        }

        @Override
        public void skipValue() throws IOException {
            switch (token) {
                case INT_n:
                    readExpectedBytes(NULL_VALUE);
                    token = readNextToken();
                    break;
                case INT_QUOTE:
                    skipString();
                    token = readNextToken();
                    break;
                case INT_LBRACKET:
                    skipArray();
                    token = readNextToken();
                    break;
                case INT_LCURLY:
                    skipObject();
                    token = readNextToken();
                    break;
                default:
                    token = readValueBytes(buffer, token);
                    break;
            }
        }

        private void skipArray() throws IOException {
            checkCurrentToken(INT_LBRACKET);
            int level = 1;
            while (level != 0) {
                switch (readNextToken()) {
                    case INT_QUOTE:
                        skipString();
                        break;
                    case INT_LBRACKET:
                        level++;
                        break;
                    case INT_RBRACKET:
                        level--;
                        break;
                }
            }
        }

        private void skipObject() throws IOException {
            checkCurrentToken(INT_LCURLY);
            int level = 1;
            while (level != 0) {
                switch (readNextToken()) {
                    case INT_QUOTE:
                        readStringBytes(buffer);
                        break;
                    case INT_LCURLY:
                        level++;
                        break;
                    case INT_RCURLY:
                        level--;
                        break;
                }
            }
        }

        @Override
        public boolean beginObject() throws IOException {
            // initialize on first call
            if (token == INT_UNINITIALIZED) {
                token = readNextToken();
            }
            checkCurrentToken(INT_LCURLY);
            token = readNextToken();
            return true;
        }

        @Override
        public void endObject() throws IOException {
            checkCurrentToken(INT_RCURLY);
            token = readNextToken();
        }

        @Override
        public void beginArray() throws IOException {
            // in case we haven't read the first token yet
            if (token == INT_UNINITIALIZED) {
                token = readNextToken();
            }
            checkCurrentToken(INT_LBRACKET);
            token = readNextToken();
        }

        @Override
        public void endArray() throws IOException {
            checkCurrentToken(INT_RBRACKET);
            token = readNextToken();
        }

        @Override
        public boolean isAtEnd() throws IOException {
            if (token == INT_COMMA) {
                token = readNextToken();
                return false;
            }
            return token == INT_RCURLY || token == INT_RBRACKET || token == INT_EOF;
        }

        @Override
        protected boolean isAtNull() {
            return token == INT_n;
        }

        @Override
        protected CharSequence readFieldName() throws IOException {
            checkJson(token == INT_QUOTE, "Expected key quotes");
            JsonDecoding.StringDecoding.readQuotedUtf8(this, key);
            checkJson(readNextToken() == INT_COLON, "Expected colon after key name");
            readNextToken();
            return key;
        }

        private void getValueAsBytes(RepeatedByte buffer) throws IOException {
            checkNotAtNull();
            if (token == INT_QUOTE) {
                readStringBytes(buffer);
                token = readNextToken();
            } else {
                token = readValueBytes(buffer, token);
            }
        }

        private int readValueBytes(RepeatedByte buffer, int ch) throws IOException {
            buffer.clear();
            while (!isBreak(ch)) {
                buffer.add((byte) ch);
                ch = readByte();
            }
            if (isWhitespace(ch)) {
                ch = readNextToken();
            }
            return token = ch;
        }

        private void readStringBytes(RepeatedByte buffer) throws IOException {
            checkNotAtNull();
            buffer.clear();
            for (int ch = readByte(); ch != INT_QUOTE; ch = readByte()) {
                buffer.add((byte) ch);
                if (ch == INT_BACKSLASH) {
                    buffer.add((byte) readByte()); // handle escaped quotes
                }
            }
        }

        public void skipString() throws IOException {
            for (int ch = readByte(); ch != INT_QUOTE; ch = readByte()) {
                if (ch == INT_BACKSLASH) {
                    readByte(); // handle escaped quotes
                }
            }
        }

        protected static void checkJson(boolean condition, String error) throws InvalidJsonException {
            if (!condition) {
                throw new InvalidJsonException(error);
            }
        }

        protected void checkNotAtNull() throws InvalidJsonException {
            if (isAtNull()) {
                String atField = (currentField == null ? "" : " for field '" + currentField + "'");
                throw new InvalidJsonException("Expected non-null value" + atField);
            }
        }

        protected void checkExpected(boolean condition, String expected, String actual) throws InvalidJsonException {
            if (!condition) {
                String atField = (currentField == null ? "" : " for field '" + currentField + "'");
                throw new InvalidJsonException("Expected '" + expected + "' but got '" + actual + "'" + atField);
            }
        }

        protected void checkCurrentToken(int expectedToken) throws InvalidJsonException {
            if (expectedToken != token) {
                String atField = (currentField == null ? "" : " for field '" + currentField + "'");
                throw new InvalidJsonException("Expected '" + ((char) expectedToken) + "' but got '" + ((char) token) + "'" + atField);
            }
        }

        protected void readExpectedBytes(int[] expected) throws IOException {
            for (int i = 1; i < expected.length; i++) {
                int ch = readByte();
                if (expected[i] != ch) {
                    int[] actual = Arrays.copyOfRange(expected, 0, i + 1);
                    actual[i] = ch;
                    checkExpected(false, toCharString(expected), toCharString(actual));
                }
            }
        }

        private String toCharString(int... bytes) {
            StringBuilder chars = new StringBuilder(bytes.length);
            for (int b : bytes) {
                chars.append((char) b);
            }
            return chars.toString();
        }

        @Override
        public int readByte() throws IOException {
            int value = readRawByte();
            if (value == INT_EOF) {
                throw InvalidJsonException.truncatedMessage();
            }
            return value;
        }

        @Override
        public int readNextToken() throws IOException {
            int ch = readRawByte();
            while (isWhitespace(ch)) {
                ch = readRawByte();
            }
            return token = ch;
        }

        private int token = INT_UNINITIALIZED;
        private final RepeatedByte buffer = RepeatedByte.newEmptyInstance();
        private final StringBuilder key = new StringBuilder(16);

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
