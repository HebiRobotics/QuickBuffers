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
 * Nothing gets allocated in steady state except for {@link #readString()} and rare numbers
 * that fall back to the JDK parsers, e.g., with more than 19 significant digits. Names and
 * strings are read into reused buffers, and input streams are read through an internal
 * buffer that only grows for unquoted values that do not fit.
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

    /**
     * Limits how deeply objects and arrays may be nested, including inside skipped values. There
     * is no limit by default, since the reader does not recurse and only needs memory per level.
     *
     * @param limit maximum number of open objects and arrays
     * @return this
     */
    public JsonReader setNestingLimit(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Nesting limit cannot be negative: " + limit);
        }
        nestingLimit = limit;
        return this;
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
     * Reads a string value and decodes escapes into the UTF-8 store. Invalid
     * UTF-8 content is rejected like in the other string reads.
     */
    public void readString(Utf8String store) throws IOException {
        consume(PEEKED_STRING, "a string");
        readQuotedUtf8(value);
        store.copyFromUtf8(value.array, 0, value.length);
    }

    /**
     * Reads a string value and decodes escapes into the UTF-8 output
     */
    void readString(RepeatedByte output) throws IOException {
        consume(PEEKED_STRING, "a string");
        readQuotedUtf8(output);
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
        final int start = readNumber(false);
        return toDouble(buffer, start, pos);
    }

    public float readFloat() throws IOException {
        final int start = readNumber(false);
        return toFloat(buffer, start, pos);
    }

    /**
     * Reads an integer number. Fractions and exponents are accepted if the value is integral, e.g., 1.0 or 1e3.
     */
    public long readLong() throws IOException {
        final int start = readNumber(true);
        if (numberDigits > 18 && (numberDigits > 19 || (numberMantissa < 0 && !(numberNegative && numberMantissa == Long.MIN_VALUE)))) {
            throw numberError("Value out of range for long", start);
        }
        return numberNegative ? -numberMantissa : numberMantissa;
    }

    /**
     * Reads an integer number. Fractions and exponents are accepted if the value is integral, e.g., 1.0 or 1e3.
     */
    public int readInt() throws IOException {
        final int start = readNumber(true);
        final long result = numberNegative ? -numberMantissa : numberMantissa;
        if (numberDigits > 19 || numberMantissa < 0 || (int) result != result) {
            throw numberError("Value out of range for int", start);
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

    boolean isAtName(String expected) {
        return name.contentEquals(expected);
    }

    boolean isAtNull() throws IOException {
        return (peeked == PEEKED_NONE ? doPeek() : peeked) == PEEKED_NULL;
    }

    /**
     * Copies the bytes of the next scalar, i.e., the unescaped contents of a string or the
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

    /**
     * Same as readValueBytes, but unquoted values are not copied. The bytes
     * are available as valueArray[valueStart, valueEnd) until the next read.
     *
     * @return false without consuming anything if the value is null
     */
    boolean readValueRange() throws IOException {
        final int p = peeked == PEEKED_NONE ? doPeek() : peeked;
        if (p >= PEEKED_NUMBER && p <= PEEKED_UNQUOTED && p != PEEKED_NULL) {
            peeked = PEEKED_NONE;
            valueEnd = readUnquotedRange();
            valueArray = buffer;
            valueStart = pos;
            pos = valueEnd;
            return true;
        }
        if (!readValueBytes(value)) {
            return false;
        }
        valueArray = value.array;
        valueStart = 0;
        valueEnd = value.length;
        return true;
    }

    // ==================== Implementation ====================

    private void consume(int expected, String description) throws IOException {
        final int p = peeked == PEEKED_NONE ? doPeek() : peeked;
        if (p != expected) {
            throw expected(description, p);
        }
        peeked = PEEKED_NONE;
    }

    private void push(int newScope) throws InvalidJsonException {
        if (depth >= nestingLimit) {
            throw error("Exceeded the nesting limit of " + nestingLimit, getPosition() - 1);
        }
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
     * Moves the unread bytes from pos to the front and appends new data from the stream,
     * so a value that starts at pos stays contiguous. The buffer only grows for values
     * that do not fit.
     */
    private boolean fill() throws IOException {
        if (input == null) {
            return false;
        }
        final int kept = limit - pos;
        if (kept == buffer.length) {
            buffer = Arrays.copyOf(buffer, 2 * kept);
        } else if (kept > 0 && pos > 0) {
            System.arraycopy(buffer, pos, buffer, 0, kept);
        }
        bufferOffset += pos;
        pos = 0;
        limit = kept;
        int n;
        do {
            n = input.read(buffer, kept, buffer.length - kept);
        } while (n == 0);
        if (n < 0) {
            return false;
        }
        limit += n;
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
        if (lenient) {
            skipUnquotedBytes();
        } else if (peeked == PEEKED_NUMBER) {
            readNumberRange();
        } else if (peeked == PEEKED_BOOLEAN) {
            readLiteral(buffer[pos] == 't' ? TRUE : FALSE);
        } else {
            throw expected("a value", peeked);
        }
    }

    /**
     * @return the start of the number, which ends at pos
     */
    private int readNumber(boolean integer) throws IOException {
        final int p = peeked == PEEKED_NONE ? doPeek() : peeked;
        if (p != PEEKED_NUMBER) {
            throw expected("a number", p);
        }
        peeked = PEEKED_NONE;
        final int start = readNumberRange();
        if (integer && numberFractional && !scaleToInteger()) {
            throw numberError("Expected an integer but got", start);
        }
        return start;
    }

    /**
     * Scales the mantissa of the last parsed number to the integer it represents, e.g., for
     * 1.0 or 1e3 like protobuf's JsonFormat. Integers beyond 64 bits get more than 19 digits.
     *
     * @return false if the number has a fraction
     */
    boolean scaleToInteger() {
        if (numberDigits > 19) {
            return false; // could be integral, e.g., 1.00000000000000000000, but is not worth supporting
        }
        long value = numberMantissa;
        if (value == 0) {
            return true;
        }
        for (int q = numberExponent; q < 0; q++) {
            final long quotient = (value >>> 1) / 5; // unsigned division by 10
            if (value != 10 * quotient) {
                return false;
            }
            value = quotient;
        }
        for (int q = numberExponent; q > 0; q--) {
            if (value + Long.MIN_VALUE > MAX_UINT64_DIV_10 + Long.MIN_VALUE) {
                numberDigits = 20;
                return true;
            }
            value *= 10;
        }
        numberMantissa = value;
        numberDigits = 19; // the unsigned magnitude needs the full range check
        return true;
    }

    /**
     * Parses and validates the number at pos and moves pos to its end
     *
     * @return the start of the number
     */
    private int readNumberRange() throws IOException {
        int end = parseNumber(buffer, pos, limit);
        if (end == limit && input != null) {
            final int valueEnd = readUnquotedRange();
            end = parseNumber(buffer, pos, valueEnd);
        }
        if (numberError != null || (end < limit && !IS_DELIMITER[buffer[end] & 0xFF])) {
            final int valueEnd = readUnquotedRange();
            final int start = pos;
            pos = valueEnd;
            throw numberError(numberError != null ? numberError : "Invalid number", start);
        }
        final int start = pos;
        pos = end;
        return start;
    }

    /**
     * Parses a JSON number in a single pass. The first 19 significant digits get accumulated
     * into an unsigned mantissa, and further digits only adjust the decimal exponent.
     *
     * @return the index after the number, or the index at which the number is invalid
     */
    int parseNumber(final byte[] buf, int i, final int end) {
        numberError = null;
        numberFractional = false;
        numberNegative = i < end && buf[i] == '-';
        if (numberNegative) {
            i++;
        }
        long mantissa = 0;
        int digits = 0;
        int exponent = 0;
        int d;
        if (i < end && buf[i] == '0') {
            i++;
        } else if (i < end && (d = buf[i] - '0') >= 0 && d <= 9) {
            do {
                if (digits++ < 19) {
                    mantissa = 10 * mantissa + d;
                } else {
                    exponent++;
                }
            } while (++i < end && (d = buf[i] - '0') >= 0 && d <= 9);
        } else {
            numberError = "Invalid number";
            return i;
        }
        if (i < end && buf[i] == '.') {
            numberFractional = true;
            if (++i == end || (d = buf[i] - '0') < 0 || d > 9) {
                numberError = "Invalid fraction in number";
                return i;
            }
            do {
                if (digits == 0 && d == 0) {
                    exponent--; // leading zeros are not significant
                } else if (digits++ < 19) {
                    mantissa = 10 * mantissa + d;
                    exponent--;
                }
            } while (++i < end && (d = buf[i] - '0') >= 0 && d <= 9);
        }
        if (i < end && (buf[i] | 0x20) == 'e') {
            numberFractional = true;
            final boolean negativeExponent = ++i < end && buf[i] == '-';
            if (i < end && (buf[i] == '-' || buf[i] == '+')) {
                i++;
            }
            if (i == end || (d = buf[i] - '0') < 0 || d > 9) {
                numberError = "Invalid exponent in number";
                return i;
            }
            int value = 0;
            do {
                if (value < MAX_EXPONENT) {
                    value = 10 * value + d;
                }
            } while (++i < end && (d = buf[i] - '0') >= 0 && d <= 9);
            exponent += negativeExponent ? -value : value;
        }
        numberMantissa = mantissa;
        numberDigits = digits;
        numberExponent = exponent;
        return i;
    }

    /**
     * Converts the last parsed number, or its text if it can not be converted exactly
     */
    double toDouble(byte[] text, int start, int end) {
        if (numberDigits <= 19) {
            final double value = JsonDecoding.Numbers.toDouble(numberNegative, numberMantissa, numberExponent);
            if (!Double.isNaN(value)) {
                return value;
            }
        }
        return Double.parseDouble(new String(text, start, end - start, Charsets.ASCII));
    }

    /**
     * Converts the last parsed number, or its text if it can not be converted exactly
     */
    float toFloat(byte[] text, int start, int end) {
        if (numberDigits <= 19) {
            final float value = JsonDecoding.Numbers.toFloat(numberNegative, numberMantissa, numberExponent);
            if (!Float.isNaN(value)) {
                return value;
            }
        }
        return Float.parseFloat(new String(text, start, end - start, Charsets.ASCII));
    }

    /**
     * @return the end of the unquoted value at pos, which stays contiguous across refills
     */
    private int readUnquotedRange() throws IOException {
        int i = pos;
        while (true) {
            final byte[] buf = buffer;
            final int end = limit;
            while (i < end && !IS_DELIMITER[buf[i] & 0xFF]) {
                i++;
            }
            final int length = i - pos;
            if (i < end || !fill()) {
                return pos + length;
            }
            i = pos + length;
        }
    }

    private void readUnquotedBytes(RepeatedByte output) throws IOException {
        final int end = readUnquotedRange();
        output.clear();
        output.addAll(buffer, pos, end - pos);
        pos = end;
    }

    private void skipUnquotedBytes() throws IOException {
        do {
            final byte[] buf = buffer;
            final int end = limit;
            int i = pos;
            while (i < end && !IS_DELIMITER[buf[i] & 0xFF]) {
                i++;
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
            } else if (c < 0x20 && !lenient) {
                throw controlCharacter(c);
            }
            if (c == '\\') {
                readEscapedUtf8(output); // e.g. escaped digits in a quoted number
            } else {
                output.add((byte) c);
            }
        }
    }

    private void readQuotedUtf8(RepeatedByte output) throws IOException {
        output.clear();
        final boolean[] special = lenient ? IS_STRING_SPECIAL : IS_STRING_SPECIAL_OR_UTF8;
        while (true) {
            final byte[] buf = buffer;
            final int start = pos;
            final int end = limit;
            int i = start;
            while (i < end && !special[buf[i] & 0xFF]) {
                i++;
            }
            output.addAll(buf, start, i - start);
            pos = i;
            final int c = readByte();
            if (c == '"') {
                return;
            } else if (c == '\\') {
                readEscapedUtf8(output);
            } else if (lenient || (c >= 0x20 && c < 0x80)) {
                output.add((byte) c); // regular byte after a refill
            } else if (c < 0x20) {
                throw controlCharacter(c);
            } else {
                writeUtf8(output, readCodePoint(c));
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
        writeUtf8(output, c < Character.MIN_SURROGATE || c > Character.MAX_SURROGATE ? c : readLowSurrogate(c));
    }

    private static void writeUtf8(RepeatedByte output, int codePoint) {
        if (codePoint < 0x80) {
            output.add((byte) codePoint);
        } else if (codePoint < 0x800) {
            final int offset = output.addLength(2);
            output.array[offset] = (byte) (0xC0 | (codePoint >>> 6));
            output.array[offset + 1] = (byte) (0x80 | (0x3F & codePoint));
        } else if (codePoint < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            final int offset = output.addLength(3);
            output.array[offset] = (byte) (0xE0 | (codePoint >>> 12));
            output.array[offset + 1] = (byte) (0x80 | (0x3F & (codePoint >>> 6)));
            output.array[offset + 2] = (byte) (0x80 | (0x3F & codePoint));
        } else {
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
            pos = output.appendAscii(buffer, pos, limit);
            int c = readByte();
            if (c < 0x80) {
                if (!IS_STRING_SPECIAL[c]) {
                    output.append((char) c);
                } else if (c == '"') {
                    return;
                } else if (c == '\\') {
                    final int escaped = readByte();
                    output.append(escaped == 'u' ? (char) readHexChar() : unescape(escaped));
                } else if (lenient) {
                    output.append((char) c);
                } else {
                    throw controlCharacter(c);
                }
            } else if ((c = readCodePoint(c)) < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                output.append((char) c);
            } else {
                output.append(Utf8.DecodeUtil.highSurrogate(c));
                output.append(Utf8.DecodeUtil.lowSurrogate(c));
            }
        }
    }

    /**
     * Decodes a multi-byte sequence and rejects overlong encodings, surrogates, and code points above U+10FFFF
     */
    private int readCodePoint(int c) throws IOException {
        if (c < 0xC2) {
            throw invalidUtf8();
        } else if (c < 0xE0) {
            return ((c & 0x1F) << 6) | readContinuationByte();
        } else if (c < 0xF0) {
            c = ((c & 0x0F) << 12) | (readContinuationByte() << 6) | readContinuationByte();
            if (c < 0x800 || (c >= Character.MIN_SURROGATE && c <= Character.MAX_SURROGATE)) {
                throw invalidUtf8();
            }
            return c;
        } else if (c < 0xF5) {
            c = ((c & 0x07) << 18) | (readContinuationByte() << 12) | (readContinuationByte() << 6) | readContinuationByte();
            if (c < Character.MIN_SUPPLEMENTARY_CODE_POINT || c > Character.MAX_CODE_POINT) {
                throw invalidUtf8();
            }
            return c;
        }
        throw invalidUtf8();
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
            final byte[] buf = buffer;
            final int end = limit;
            int i = pos;
            while (i < end && !IS_STRING_SPECIAL[buf[i] & 0xFF]) {
                i++;
            }
            pos = i;
            final int c = readByte();
            if (c == '"') {
                return;
            } else if (c == '\\') {
                final int escaped = readByte();
                if (lenient) {
                    continue;
                } else if (escaped == 'u') {
                    readHexChar();
                } else {
                    unescape(escaped);
                }
            } else if (c < 0x20 && !lenient) {
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

    private InvalidJsonException numberError(String message, int start) {
        final String number = new String(buffer, start, pos - start, Charsets.ISO_8859_1);
        return error(message + " '" + number + "'", bufferOffset + start);
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
    private static final long MAX_UINT64_DIV_10 = 0x1999999999999999L; // 18446744073709551615 / 10
    private static final int MAX_EXPONENT = 100000000; // saturates, as larger exponents round to zero or infinity anyway

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
    private static final boolean[] IS_STRING_SPECIAL_OR_UTF8 = new boolean[256];

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
        for (int i = 0; i < 256; i++) {
            IS_STRING_SPECIAL_OR_UTF8[i] = IS_STRING_SPECIAL[i] || i >= 0x80;
        }
    }

    private byte[] buffer;
    private final InputStream input;
    private int pos;
    private int limit;
    private long bufferOffset;

    /**
     * Accepts unescaped control characters, copies raw bytes of strings read as UTF-8 without
     * validation, and skips values without checking their contents, which JsonSource has
     * always allowed and keeps allowing.
     */
    boolean lenient = false;

    private int peeked = PEEKED_NONE;
    private int scope = SCOPE_EMPTY_DOCUMENT;
    private int[] stack = new int[16];
    private int depth = 0;
    private int nestingLimit = Integer.MAX_VALUE;

    private final CharBuffer name = new CharBuffer();
    private final CharBuffer chars = new CharBuffer();
    private final RepeatedByte value = RepeatedByte.newEmptyInstance();

    // range of the last readValueRange
    byte[] valueArray;
    int valueStart;
    int valueEnd;

    // result of the last parseNumber
    String numberError;
    boolean numberNegative;
    boolean numberFractional;
    long numberMantissa;
    int numberDigits;
    int numberExponent;

    /**
     * Reused buffer for decoded strings that also computes the String hash code,
     * so hashing names for field lookups does not need a second pass.
     */
    static final class CharBuffer implements CharSequence {

        CharBuffer clear() {
            length = 0;
            hash = 0;
            return this;
        }

        void append(char c) {
            if (length == array.length) {
                array = Arrays.copyOf(array, length * 2);
            }
            array[length++] = c;
            hash = 31 * hash + c;
        }

        /**
         * Appends and hashes plain ASCII until a quote, backslash, control character, or non-ASCII
         * byte. Working on locals is faster than appending one char at a time.
         *
         * @return the index of the first byte that was not appended
         */
        int appendAscii(byte[] bytes, int start, int end) {
            char[] chars = array;
            int n = length;
            int h = hash;
            int i = start;
            for (int b; i < end && (b = bytes[i]) >= 0x20 && b != '"' && b != '\\'; i++) {
                if (n == chars.length) {
                    chars = array = Arrays.copyOf(chars, n * 2);
                }
                chars[n++] = (char) b;
                h = 31 * h + b;
            }
            length = n;
            hash = h;
            return i;
        }

        /**
         * @return the same value as String::hashCode
         */
        int hash() {
            return hash;
        }

        boolean contentEquals(String other) {
            if (other.length() != length) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (array[i] != other.charAt(i)) {
                    return false;
                }
            }
            return true;
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
        int hash = 0;

    }

}
