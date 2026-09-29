/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2026 HEBI Robotics
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

import us.hebi.quickbuf.ProtoUtil.Charsets;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static us.hebi.quickbuf.ProtoUtil.*;

/**
 * Streaming reader for UTF-8 encoded JSON (RFC 8259) that works without protobuf messages.
 * Values are pulled one token at a time, e.g.,
 * <pre>{@code
 * JsonReader reader = JsonReader.newInstance(bytes);
 * reader.beginObject();
 * while (reader.hasNext()) {
 *     CharSequence name = reader.readName();
 *     if ("id".contentEquals(name)) {
 *         id = reader.readInt();
 *     } else {
 *         reader.skipValue();
 *     }
 * }
 * reader.endObject();
 * }</pre>
 * The structure is validated strictly: missing or trailing commas, missing colons and
 * mismatched brackets are errors, including inside skipped values. Number syntax is
 * validated by the typed reads. A leading UTF-8 byte order mark is skipped. Errors are
 * {@link InvalidJsonException}s that include the byte offset. Content after the root
 * value is not checked automatically, so {@link #peek()} returns {@link Token#END_DOCUMENT}
 * only at the real end of the input and whitespace separated root values can be read
 * one after another.
 * <p>
 * Nothing gets allocated in steady state except for {@link #readString()} and the number
 * parsing in {@link #readDouble()} and {@link #readFloat()}. Names and strings are read
 * into reused buffers, and input streams are read through a fixed internal buffer.
 *
 * @author Florian Enner
 * @since 29 Sep 2026
 */
public final class JsonReader implements Closeable {

    public enum Token {
        BEGIN_OBJECT,
        END_OBJECT,
        BEGIN_ARRAY,
        END_ARRAY,
        NAME,
        STRING,
        NUMBER,
        BOOLEAN,
        NULL,
        END_DOCUMENT
    }

    public static JsonReader newInstance(byte[] bytes) {
        return newInstance(bytes, 0, bytes.length);
    }

    public static JsonReader newInstance(byte[] bytes, int offset, int length) {
        ProtoUtil.checkBounds(bytes, offset, length);
        return new JsonReader(bytes, offset, offset + length, null);
    }

    public static JsonReader newInstance(RepeatedByte bytes) {
        return newInstance(bytes.array, 0, bytes.length);
    }

    /**
     * Reads the UTF-8 bytes of the string, which get encoded once up front
     */
    public static JsonReader newInstance(String string) {
        return newInstance(string.getBytes(Charsets.UTF_8));
    }

    /**
     * Reads from the stream through an internal buffer, so the stream may
     * be read ahead past the end of the root value.
     */
    public static JsonReader newInstance(InputStream inputStream) {
        return new JsonReader(new byte[STREAM_BUFFER_SIZE], 0, 0, checkNotNull(inputStream));
    }

    private JsonReader(byte[] buffer, int position, int limit, InputStream input) {
        this.buffer = buffer;
        this.pos = position;
        this.limit = limit;
        this.bufferOffset = -position;
        this.input = input;
    }

    // ==================== Structure ====================

    /**
     * Returns the type of the next token without consuming it
     */
    public Token peek() throws IOException {
        final int p = peeked == PEEKED_NONE ? doPeek() : peeked;
        if (p == PEEKED_UNQUOTED) {
            throw expected("a value", p);
        }
        return TOKENS[p];
    }

    /**
     * Returns true while the current object or array has another member, or while
     * there is another root value at the top level
     */
    public boolean hasNext() throws IOException {
        final int p = peeked == PEEKED_NONE ? doPeek() : peeked;
        return p < PEEKED_END_OBJECT;
    }

    public JsonReader beginObject() throws IOException {
        consume(PEEKED_BEGIN_OBJECT, "'{'");
        push(SCOPE_EMPTY_OBJECT);
        return this;
    }

    public JsonReader endObject() throws IOException {
        consume(PEEKED_END_OBJECT, "'}'");
        pop();
        return this;
    }

    public JsonReader beginArray() throws IOException {
        consume(PEEKED_BEGIN_ARRAY, "'['");
        push(SCOPE_EMPTY_ARRAY);
        return this;
    }

    public JsonReader endArray() throws IOException {
        consume(PEEKED_END_ARRAY, "']'");
        pop();
        return this;
    }

    /**
     * Reads the next member name of an object. The colon and the start of the value
     * get checked right away, so a missing value fails here.
     *
     * @return a reused buffer that is valid until the next call to this method
     */
    public CharSequence readName() throws IOException {
        consume(PEEKED_NAME, "a name");
        readQuotedChars(name.clear());
        peekValueAfterName();
        return name;
    }

    /**
     * Skips the next value including all nested content. When positioned at a name,
     * it skips both the name and its value.
     */
    public void skipValue() throws IOException {
        int p = peeked == PEEKED_NONE ? doPeek() : peeked;
        if (p == PEEKED_NAME) {
            skipQuoted();
            p = peekValueAfterName();
        }
        int level = 0;
        while (true) {
            switch (p) {
                case PEEKED_BEGIN_OBJECT:
                    push(SCOPE_EMPTY_OBJECT);
                    level++;
                    break;
                case PEEKED_BEGIN_ARRAY:
                    push(SCOPE_EMPTY_ARRAY);
                    level++;
                    break;
                case PEEKED_END_OBJECT:
                case PEEKED_END_ARRAY:
                    if (level == 0) {
                        throw expected("a value", p);
                    }
                    pop();
                    level--;
                    break;
                case PEEKED_NAME:
                    skipQuoted();
                    p = peekValueAfterName();
                    continue;
                case PEEKED_STRING:
                    skipQuoted();
                    break;
                case PEEKED_NULL:
                    readLiteral(NULL);
                    break;
                case PEEKED_NUMBER:
                case PEEKED_BOOLEAN:
                case PEEKED_UNQUOTED:
                    skipUnquoted(p);
                    break;
                default:
                    throw expected("a value", p);
            }
            peeked = PEEKED_NONE;
            if (level == 0) {
                return;
            }
            p = doPeek();
        }
    }

    // ==================== Values ====================

    /**
     * Reads a string value and decodes escapes into the UTF-8 store. Raw
     * UTF-8 content is copied as is without validation.
     */
    public void readString(Utf8String store) throws IOException {
        consume(PEEKED_STRING, "a string");
        readQuotedUtf8(value);
        store.copyFromUtf8(value.array, 0, value.length);
    }

    /**
     * Reads a string value and appends it to the store
     */
    public void readString(StringBuilder store) throws IOException {
        consume(PEEKED_STRING, "a string");
        readQuotedChars(chars.clear());
        store.append(chars.array, 0, chars.length);
    }

    /**
     * Reads a string value into a newly allocated String
     */
    public String readString() throws IOException {
        consume(PEEKED_STRING, "a string");
        readQuotedChars(chars.clear());
        return chars.toString();
    }

    public double readDouble() throws IOException {
        readNumber(false);
        return JsonDecoding.Numbers.readDouble(value.array, 0, value.length);
    }

    public float readFloat() throws IOException {
        readNumber(false);
        return Float.parseFloat(new String(value.array, 0, value.length, Charsets.ASCII));
    }

    /**
     * Reads an integer number. Fractions and exponents are rejected, even if the value is integral.
     */
    public long readLong() throws IOException {
        readNumber(true);
        try {
            return JsonDecoding.Numbers.readLong(value.array, 0, value.length);
        } catch (InvalidJsonException outOfRange) {
            throw numberError("Value out of range for long");
        }
    }

    /**
     * Reads an integer number. Fractions and exponents are rejected, even if the value is integral.
     */
    public int readInt() throws IOException {
        readNumber(true);
        if (value.length > 11) {
            throw numberError("Value out of range for int");
        }
        final long result = JsonDecoding.Numbers.readLong(value.array, 0, value.length);
        if ((int) result != result) {
            throw numberError("Value out of range for int");
        }
        return (int) result;
    }

    public boolean readBoolean() throws IOException {
        final int p = peeked == PEEKED_NONE ? doPeek() : peeked;
        if (p != PEEKED_BOOLEAN) {
            throw expected("a boolean", p);
        }
        peeked = PEEKED_NONE;
        final boolean result = buffer[pos] == 't';
        readLiteral(result ? TRUE : FALSE);
        return result;
    }

    public void readNull() throws IOException {
        consume(PEEKED_NULL, "null");
        readLiteral(NULL);
    }

    /**
     * @return the offset of the next unread byte relative to the start of the input
     */
    public long getPosition() {
        return bufferOffset + pos;
    }

    @Override
    public void close() throws IOException {
        if (input != null) {
            input.close();
        }
    }

    // ==================== Hooks for JsonSource ====================

    boolean isAtNull() throws IOException {
        return (peeked == PEEKED_NONE ? doPeek() : peeked) == PEEKED_NULL;
    }

    /**
     * Copies the raw bytes of the next scalar, i.e., the undecoded contents of a string or the
     * text of any unquoted value including non-standard ones such as NaN, into the output.
     *
     * @return false without consuming anything if the value is null
     */
    boolean readValueBytes(RepeatedByte output) throws IOException {
        final int p = peeked == PEEKED_NONE ? doPeek() : peeked;
        if (p == PEEKED_STRING) {
            peeked = PEEKED_NONE;
            readQuotedBytes(output);
            return true;
        } else if (p == PEEKED_NULL) {
            return false;
        } else if (p >= PEEKED_NUMBER && p <= PEEKED_UNQUOTED) {
            peeked = PEEKED_NONE;
            readUnquotedBytes(output);
            return true;
        }
        throw expected("a value", p);
    }

    // ==================== Implementation ====================

    private void consume(int expected, String description) throws IOException {
        final int p = peeked == PEEKED_NONE ? doPeek() : peeked;
        if (p != expected) {
            throw expected(description, p);
        }
        peeked = PEEKED_NONE;
    }

    private void push(int newScope) {
        if (depth == stack.length) {
            stack = Arrays.copyOf(stack, stack.length * 2);
        }
        stack[depth++] = scope;
        scope = newScope;
    }

    private void pop() {
        scope = stack[--depth];
    }

    /**
     * Consumes separators and determines the type of the next token. Scopes are
     * updated here, so consuming a value only needs to reset the peeked state.
     * The common cases are split into small methods that the JIT can inline.
     */
    private int doPeek() throws IOException {
        if (scope == SCOPE_NONEMPTY_OBJECT) {
            return peekNextMember();
        } else if (scope == SCOPE_NONEMPTY_ARRAY) {
            return peekNextElement();
        }
        return peekFirst();
    }

    private int peekNextMember() throws IOException {
        int c = nextNonWhitespace();
        if (c == ',') {
            c = nextNonWhitespace();
            if (c == '"') {
                return peeked = PEEKED_NAME;
            }
            throw c == '}' ? trailingComma(c) : syntaxError("a name", c);
        } else if (c == '}') {
            return peeked = PEEKED_END_OBJECT;
        }
        throw syntaxError("',' or '}'", c);
    }

    private int peekNextElement() throws IOException {
        int c = nextNonWhitespace();
        if (c == ',') {
            c = nextNonWhitespace();
            if (c == ']') {
                throw trailingComma(c);
            }
            return peekValue(c);
        } else if (c == ']') {
            return peeked = PEEKED_END_ARRAY;
        }
        throw syntaxError("',' or ']'", c);
    }

    private int peekFirst() throws IOException {
        final int c;
        switch (scope) {
            case SCOPE_EMPTY_OBJECT:
                scope = SCOPE_NONEMPTY_OBJECT;
                c = nextNonWhitespace();
                if (c == '"') {
                    return peeked = PEEKED_NAME;
                } else if (c == '}') {
                    return peeked = PEEKED_END_OBJECT;
                }
                throw syntaxError("a name or '}'", c);

            case SCOPE_EMPTY_ARRAY:
                scope = SCOPE_NONEMPTY_ARRAY;
                c = nextNonWhitespace();
                return c == ']' ? (peeked = PEEKED_END_ARRAY) : peekValue(c);

            case SCOPE_EMPTY_DOCUMENT:
                skipByteOrderMark();
                scope = SCOPE_NONEMPTY_DOCUMENT; // fallthrough
            default:
                c = nextNonWhitespace();
                return c < 0 ? (peeked = PEEKED_END_DOCUMENT) : peekValue(c);
        }
    }

    private int peekValueAfterName() throws IOException {
        final int c = nextNonWhitespace();
        if (c != ':') {
            throw syntaxError("':'", c);
        }
        return peekValue(nextNonWhitespace());
    }

    private int peekValue(int c) throws IOException {
        final int p = c < 0 ? PEEKED_NONE : VALUE_TYPES[c];
        if (p >= PEEKED_NUMBER) {
            pos--; // unquoted values get read including the first character
        } else if (p == PEEKED_NONE) {
            throw syntaxError("a value", c);
        }
        return peeked = p;
    }

    private void skipByteOrderMark() throws IOException {
        if ((pos < limit || fill()) && buffer[pos] == (byte) 0xEF) {
            pos++;
            if (readByte() != 0xBB || readByte() != 0xBF) {
                throw error("Invalid byte order mark", getPosition() - 1);
            }
        }
    }

    private int nextNonWhitespace() throws IOException {
        final byte[] buf = buffer;
        do {
            final int end = limit;
            int i = pos;
            while (i < end) {
                final int c = buf[i++] & 0xFF;
                if (c > ' ' || (c != ' ' && c != '\n' && c != '\r' && c != '\t')) {
                    pos = i;
                    return c;
                }
            }
            pos = i;
        } while (fill());
        return -1;
    }

    private int readByte() throws IOException {
        if (pos == limit && !fill()) {
            throw truncated();
        }
        return buffer[pos++] & 0xFF;
    }

    /**
     * Replaces the buffer contents with new data from the stream. Only
     * called once all bytes in the buffer have been consumed.
     */
    private boolean fill() throws IOException {
        if (input == null) {
            return false;
        }
        bufferOffset += limit;
        pos = 0;
        limit = 0;
        int n;
        do {
            n = input.read(buffer, 0, buffer.length);
        } while (n == 0);
        if (n < 0) {
            return false;
        }
        limit = n;
        return true;
    }

    private void readLiteral(byte[] literal) throws IOException {
        for (int i = 0; i < literal.length; i++) {
            final int c = readByte();
            if (c != literal[i]) {
                String actual = new String(literal, 0, i, Charsets.ASCII) + (char) c;
                throw error("Expected '" + new String(literal, Charsets.ASCII) + "' but got '" + actual + "'", getPosition() - i - 1);
            }
        }
    }

    private void skipUnquoted(int peeked) throws IOException {
        if (peeked == PEEKED_NUMBER) {
            readNumberBytes(value);
        } else if (peeked == PEEKED_BOOLEAN) {
            readLiteral(buffer[pos] == 't' ? TRUE : FALSE);
        } else {
            throw expected("a value", peeked);
        }
    }

    private void readNumber(boolean integer) throws IOException {
        final int p = peeked == PEEKED_NONE ? doPeek() : peeked;
        if (p != PEEKED_NUMBER) {
            throw expected("a number", p);
        }
        peeked = PEEKED_NONE;
        if (readNumberBytes(value) && integer) {
            throw numberError("Expected an integer but got");
        }
    }

    /**
     * Reads the text of a number and validates it against the JSON grammar
     *
     * @return true if the number has a fraction or exponent
     */
    private boolean readNumberBytes(RepeatedByte output) throws IOException {
        readUnquotedBytes(output);
        final byte[] bytes = output.array;
        final int length = output.length;
        int i = 0;
        if (i < length && bytes[i] == '-') {
            i++;
        }
        if (i < length && bytes[i] == '0') {
            i++;
        } else {
            i = skipDigits(bytes, i, length, "Invalid number");
        }
        boolean fractional = false;
        if (i < length && bytes[i] == '.') {
            i = skipDigits(bytes, i + 1, length, "Invalid fraction in number");
            fractional = true;
        }
        if (i < length && (bytes[i] == 'e' || bytes[i] == 'E')) {
            i++;
            if (i < length && (bytes[i] == '+' || bytes[i] == '-')) {
                i++;
            }
            i = skipDigits(bytes, i, length, "Invalid exponent in number");
            fractional = true;
        }
        if (i != length) {
            throw numberError("Invalid number");
        }
        return fractional;
    }

    private int skipDigits(byte[] bytes, int i, int length, String error) throws InvalidJsonException {
        final int start = i;
        while (i < length && bytes[i] >= '0' && bytes[i] <= '9') {
            i++;
        }
        if (i == start) {
            throw numberError(error);
        }
        return i;
    }

    private void readUnquotedBytes(RepeatedByte output) throws IOException {
        output.clear();
        do {
            final byte[] buf = buffer;
            final int end = limit;
            int i = pos;
            while (i < end && !IS_DELIMITER[buf[i] & 0xFF]) {
                output.add(buf[i++]);
            }
            pos = i;
            if (i < end) {
                return;
            }
        } while (fill());
    }

    private void readQuotedBytes(RepeatedByte output) throws IOException {
        output.clear();
        while (true) {
            final int c = readByte();
            if (c == '"') {
                return;
            } else if (c < 0x20) {
                throw controlCharacter(c);
            }
            output.add((byte) c);
            if (c == '\\') {
                output.add((byte) readByte());
            }
        }
    }

    private void readQuotedUtf8(RepeatedByte output) throws IOException {
        output.clear();
        while (true) {
            final byte[] buf = buffer;
            final int start = pos;
            final int end = limit;
            int i = start;
            while (i < end && !IS_STRING_SPECIAL[buf[i] & 0xFF]) {
                i++;
            }
            output.addAll(buf, start, i - start);
            pos = i;
            final int c = readByte();
            if (c == '"') {
                return;
            } else if (c == '\\') {
                readEscapedUtf8(output);
            } else if (c < 0x20) {
                throw controlCharacter(c);
            } else {
                output.add((byte) c); // regular byte after a refill
            }
        }
    }

    private void readEscapedUtf8(RepeatedByte output) throws IOException {
        final int escaped = readByte();
        if (escaped != 'u') {
            output.add((byte) unescape(escaped));
            return;
        }
        final int c = readHexChar();
        if (c < 0x80) {
            output.add((byte) c);
        } else if (c < 0x800) {
            final int offset = output.addLength(2);
            output.array[offset] = (byte) (0xC0 | (c >>> 6));
            output.array[offset + 1] = (byte) (0x80 | (0x3F & c));
        } else if (c < Character.MIN_SURROGATE || c > Character.MAX_SURROGATE) {
            final int offset = output.addLength(3);
            output.array[offset] = (byte) (0xE0 | (c >>> 12));
            output.array[offset + 1] = (byte) (0x80 | (0x3F & (c >>> 6)));
            output.array[offset + 2] = (byte) (0x80 | (0x3F & c));
        } else {
            final int codePoint = readLowSurrogate(c);
            final int offset = output.addLength(4);
            output.array[offset] = (byte) (0xF0 | (codePoint >>> 18));
            output.array[offset + 1] = (byte) (0x80 | (0x3F & (codePoint >>> 12)));
            output.array[offset + 2] = (byte) (0x80 | (0x3F & (codePoint >>> 6)));
            output.array[offset + 3] = (byte) (0x80 | (0x3F & codePoint));
        }
    }

    /**
     * UTF-8 has no encoding for unpaired surrogates, so they are rejected
     * instead of silently getting replaced.
     */
    private int readLowSurrogate(int high) throws IOException {
        if (Character.isHighSurrogate((char) high) && readByte() == '\\' && readByte() == 'u') {
            final int low = readHexChar();
            if (Character.isLowSurrogate((char) low)) {
                return Character.toCodePoint((char) high, (char) low);
            }
        }
        throw error("Unpaired surrogate in string", getPosition() - 1);
    }

    private void readQuotedChars(CharBuffer output) throws IOException {
        while (true) {
            final byte[] buf = buffer;
            final int end = limit;
            int i = pos;
            for (int b; i < end && (b = buf[i]) >= 0x20 && b != '"' && b != '\\'; i++) {
                output.append((char) b);
            }
            pos = i;

            int c = readByte();
            if (c < 0x80) {
                if (!IS_STRING_SPECIAL[c]) {
                    output.append((char) c);
                } else if (c == '"') {
                    return;
                } else if (c == '\\') {
                    final int escaped = readByte();
                    output.append(escaped == 'u' ? (char) readHexChar() : unescape(escaped));
                } else {
                    throw controlCharacter(c);
                }
            } else if (c < 0xC2) {
                throw invalidUtf8();
            } else if (c < 0xE0) {
                output.append((char) (((c & 0x1F) << 6) | readContinuationByte()));
            } else if (c < 0xF0) {
                c = ((c & 0x0F) << 12) | (readContinuationByte() << 6) | readContinuationByte();
                if (c < 0x800 || (c >= Character.MIN_SURROGATE && c <= Character.MAX_SURROGATE)) {
                    throw invalidUtf8();
                }
                output.append((char) c);
            } else if (c < 0xF5) {
                c = ((c & 0x07) << 18) | (readContinuationByte() << 12) | (readContinuationByte() << 6) | readContinuationByte();
                if (c < Character.MIN_SUPPLEMENTARY_CODE_POINT || c > Character.MAX_CODE_POINT) {
                    throw invalidUtf8();
                }
                output.append(Utf8.DecodeUtil.highSurrogate(c));
                output.append(Utf8.DecodeUtil.lowSurrogate(c));
            } else {
                throw invalidUtf8();
            }
        }
    }

    private int readContinuationByte() throws IOException {
        final int c = readByte();
        if ((c & 0xC0) != 0x80) {
            throw invalidUtf8();
        }
        return c & 0x3F;
    }

    private void skipQuoted() throws IOException {
        while (true) {
            final int c = readByte();
            if (c == '"') {
                return;
            } else if (c == '\\') {
                final int escaped = readByte();
                if (escaped == 'u') {
                    readHexChar();
                } else {
                    unescape(escaped);
                }
            } else if (c < 0x20) {
                throw controlCharacter(c);
            }
        }
    }

    private int readHexChar() throws IOException {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            final int c = readByte();
            final int digit = JsonDecoding.IntChar.sHexValues[c];
            if (digit < 0) {
                throw error("Invalid hex digit '" + (char) c + "' in unicode escape", getPosition() - 1);
            }
            result = (result << 4) | digit;
        }
        return result;
    }

    private char unescape(int escaped) throws InvalidJsonException {
        switch (escaped) {
            case '"':
                return '"';
            case '\\':
                return '\\';
            case '/':
                return '/';
            case 'b':
                return '\b';
            case 'f':
                return '\f';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            default:
                throw error("Invalid escape sequence '\\" + (char) escaped + "'", getPosition() - 2);
        }
    }

    // ==================== Errors ====================

    private InvalidJsonException expected(String expected, int peeked) {
        final String actual;
        final long offset;
        switch (peeked) {
            case PEEKED_END_DOCUMENT:
                return error("Expected " + expected + " but reached the end of the input", getPosition());
            case PEEKED_NUMBER:
            case PEEKED_BOOLEAN:
            case PEEKED_NULL:
            case PEEKED_UNQUOTED:
                actual = String.valueOf((char) (buffer[pos] & 0xFF));
                offset = getPosition();
                break;
            default:
                actual = String.valueOf((char) (buffer[pos - 1] & 0xFF));
                offset = getPosition() - 1;
        }
        return error("Expected " + expected + " but got '" + actual + "'", offset);
    }

    private InvalidJsonException syntaxError(String expected, int c) {
        if (c < 0) {
            return error("Expected " + expected + " but reached the end of the input", getPosition());
        }
        return error("Expected " + expected + " but got '" + (char) c + "'", getPosition() - 1);
    }

    private InvalidJsonException trailingComma(int c) {
        return error("Trailing comma before '" + (char) c + "'", getPosition() - 1);
    }

    private InvalidJsonException numberError(String message) {
        final String number = new String(value.array, 0, value.length, Charsets.ISO_8859_1);
        return error(message + " '" + number + "'", getPosition() - value.length);
    }

    private InvalidJsonException controlCharacter(int c) {
        return error("Unescaped control character " + String.format("0x%02x", c) + " in string", getPosition() - 1);
    }

    private InvalidJsonException invalidUtf8() {
        return error("Invalid UTF-8 in string", getPosition() - 1);
    }

    private InvalidJsonException truncated() {
        return error("Unexpected end of input", getPosition());
    }

    private static InvalidJsonException error(String message, long offset) {
        return new InvalidJsonException(message + " at offset " + offset);
    }

    private static final int STREAM_BUFFER_SIZE = 8 * 1024;

    // ordered so that ranges can be checked with a single comparison
    private static final int PEEKED_NONE = 0;
    private static final int PEEKED_BEGIN_OBJECT = 1;
    private static final int PEEKED_BEGIN_ARRAY = 2;
    private static final int PEEKED_NAME = 3;
    private static final int PEEKED_STRING = 4;
    private static final int PEEKED_NUMBER = 5;
    private static final int PEEKED_BOOLEAN = 6;
    private static final int PEEKED_NULL = 7;
    private static final int PEEKED_UNQUOTED = 8;
    private static final int PEEKED_END_OBJECT = 9;
    private static final int PEEKED_END_ARRAY = 10;
    private static final int PEEKED_END_DOCUMENT = 11;

    private static final Token[] TOKENS = {null,
            Token.BEGIN_OBJECT, Token.BEGIN_ARRAY, Token.NAME, Token.STRING, Token.NUMBER, Token.BOOLEAN,
            Token.NULL, null, Token.END_OBJECT, Token.END_ARRAY, Token.END_DOCUMENT};

    private static final int SCOPE_EMPTY_DOCUMENT = 0;
    private static final int SCOPE_NONEMPTY_DOCUMENT = 1;
    private static final int SCOPE_EMPTY_ARRAY = 2;
    private static final int SCOPE_NONEMPTY_ARRAY = 3;
    private static final int SCOPE_EMPTY_OBJECT = 4;
    private static final int SCOPE_NONEMPTY_OBJECT = 5;

    private static final byte[] TRUE = {'t', 'r', 'u', 'e'};
    private static final byte[] FALSE = {'f', 'a', 'l', 's', 'e'};
    private static final byte[] NULL = {'n', 'u', 'l', 'l'};

    private static final byte[] VALUE_TYPES = new byte[256];
    private static final boolean[] IS_DELIMITER = new boolean[256];
    private static final boolean[] IS_STRING_SPECIAL = new boolean[256];

    static {
        Arrays.fill(VALUE_TYPES, (byte) PEEKED_UNQUOTED);
        for (char c : new char[]{'}', ']', ',', ':'}) {
            VALUE_TYPES[c] = PEEKED_NONE;
        }
        for (char c = '0'; c <= '9'; c++) {
            VALUE_TYPES[c] = PEEKED_NUMBER;
        }
        VALUE_TYPES['-'] = PEEKED_NUMBER;
        VALUE_TYPES['{'] = PEEKED_BEGIN_OBJECT;
        VALUE_TYPES['['] = PEEKED_BEGIN_ARRAY;
        VALUE_TYPES['"'] = PEEKED_STRING;
        VALUE_TYPES['t'] = PEEKED_BOOLEAN;
        VALUE_TYPES['f'] = PEEKED_BOOLEAN;
        VALUE_TYPES['n'] = PEEKED_NULL;

        for (char c : new char[]{' ', '\t', '\n', '\r', ',', '}', ']'}) {
            IS_DELIMITER[c] = true;
        }
        for (int i = 0; i < 0x20; i++) {
            IS_STRING_SPECIAL[i] = true;
        }
        IS_STRING_SPECIAL['"'] = true;
        IS_STRING_SPECIAL['\\'] = true;
    }

    private final byte[] buffer;
    private final InputStream input;
    private int pos;
    private int limit;
    private long bufferOffset;

    private int peeked = PEEKED_NONE;
    private int scope = SCOPE_EMPTY_DOCUMENT;
    private int[] stack = new int[16];
    private int depth = 0;

    private final CharBuffer name = new CharBuffer();
    private final CharBuffer chars = new CharBuffer();
    private final RepeatedByte value = RepeatedByte.newEmptyInstance();

    /**
     * Reused buffer for decoded strings that is cheaper to append to than a StringBuilder
     */
    static final class CharBuffer implements CharSequence {

        CharBuffer clear() {
            length = 0;
            return this;
        }

        void append(char c) {
            if (length == array.length) {
                array = Arrays.copyOf(array, length * 2);
            }
            array[length++] = c;
        }

        @Override
        public int length() {
            return length;
        }

        @Override
        public char charAt(int index) {
            if (index >= length) {
                throw new IndexOutOfBoundsException("index " + index + " >= length " + length);
            }
            return array[index];
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return toString().subSequence(start, end);
        }

        @Override
        public String toString() {
            return new String(array, 0, length);
        }

        char[] array = new char[16];
        int length = 0;

    }

}
