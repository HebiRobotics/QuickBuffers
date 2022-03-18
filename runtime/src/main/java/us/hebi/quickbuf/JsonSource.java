/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2022 HEBI Robotics
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

import java.io.IOException;

import static us.hebi.quickbuf.JsonDecoding.IntChar.*;
import static us.hebi.quickbuf.ProtoUtil.*;
import static us.hebi.quickbuf.ProtoUtil.Charsets.*;

/**
 * @author Florian Enner
 * @since 14 Mär 2022
 */
public class JsonSource extends AbstractJsonSource<JsonSource> {

    static interface ByteSource {
        int read() throws IOException;
    }

    static class ByteArraySource implements ByteSource {

        ByteArraySource(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public int read() {
            try {
                return bytes[position++] & 0xFF;
            } catch (ArrayIndexOutOfBoundsException oob) {
                return -1;
            }
        }

        int position = 0;
        final byte[] bytes;
    }

    public static JsonSource newInstance(byte[] bytes) {
        return new JsonSource(new ByteArraySource(bytes));
    }

    // for testing
    static JsonSource newInstance(String string) {
        return newInstance(string.getBytes(UTF_8));
    }

    private JsonSource(ByteSource source) {
        this.source = source;
    }

    @Override
    public double nextDouble() throws IOException {
        getValueAsBytes(buffer);
        return JsonDecoding.Numbers.readDouble(buffer.array, 0, buffer.length);
    }

    @Override
    public int nextInt() throws IOException {
        return (int) nextLong();
    }

    @Override
    public long nextLong() throws IOException {
        getValueAsBytes(buffer);
        return JsonDecoding.Numbers.readLong(buffer.array, 0, buffer.length);
    }

    @Override
    public boolean nextBoolean() throws IOException {
        if (token == INT_t) {
            checkExpected(TRUE_VALUE, "expected true");
            nextToken();
            return true;
        } else if (token == INT_f) {
            checkExpected(FALSE_VALUE, "expected false");
            nextToken();
            return false;
        } else if (token == INT_n) {
            skipNull();
            return false;
        } else {
            throw new InvalidJsonException("Unsupported boolean value");
        }
    }

    @Override
    public <T extends ProtoEnum<?>> T nextEnum(ProtoEnum.EnumConverter<T> converter) throws IOException {
        getValueAsBytes(buffer);
        if (JsonDecoding.Numbers.isInteger(buffer.array, 0, buffer.length)) {
            return converter.forNumber((int) JsonDecoding.Numbers.readLong(buffer.array, 0, buffer.length));
        } else {
            return converter.forName(bufferViewAscii);
        }
    }

    @Override
    public void nextString(Utf8String store) throws IOException {
        if (token == JsonDecoding.IntChar.INT_n) {
            skipNull();
            return;
        }
        checkArgument(token == INT_QUOTE, "Expected quotes");
        JsonDecoding.StringDecoding.readQuotedUtf8(this, buffer);
        store.copyFromUtf8(buffer.array, 0, buffer.length);
        token = nextToken();
    }

    @Override
    public void skipValue() throws IOException {
        switch (token) {
            case INT_n:
                skipNull();
                token = nextToken();
                break;
            case INT_QUOTE:
                readUntilQuote(buffer);
                token = nextToken();
                break;
            case INT_LBRACKET:
                skipArray();
                token = nextToken();
                break;
            case INT_LCURLY:
                skipObject();
                token = nextToken();
                break;
            default:
                token = readUntilBreak(buffer, token);
                break;
        }
    }

    private void skipArray() throws IOException {
        checkArgument(token == INT_LBRACKET, "Skipping non-array");
        int level = 1;
        while (level != 0) {
            switch (nextToken()) {
                case INT_QUOTE:
                    readUntilQuote(buffer);
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
        checkArgument(token == INT_LCURLY, "Skipping non-object");
        int level = 1;
        while (level != 0) {
            switch (nextToken()) {
                case INT_QUOTE:
                    readUntilQuote(buffer);
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

    private void skipNull() throws IOException {
        checkExpected(NULL_VALUE, "expected null");
        token = nextToken();
    }

    @Override
    public boolean beginObject() throws IOException {
        // initialize on first call
        int c = token == INT_EOF ? nextToken() : token;
        if (c == JsonDecoding.IntChar.INT_n) {
            skipNull();
            return false;
        } else if (c == INT_LCURLY) {
            token = nextToken();
            return true;
        } else {
            throw new InvalidProtocolBufferException("Expected null or begin object");
        }
    }

    @Override
    public void endObject() throws IOException {
        checkArgument(token == INT_RCURLY, "Expected '}'");
        token = nextToken();
    }

    @Override
    protected boolean beginArray() throws IOException {
        if (token == JsonDecoding.IntChar.INT_n) {
            skipNull();
            return false;
        }
        checkArgument(token == INT_LBRACKET, "Expected '['");
        token = nextToken();
        return true;
    }

    @Override
    public void endArray() throws IOException {
        checkArgument(token == INT_RBRACKET, "Expected ']'");
        token = nextToken();
    }

    @Override
    public boolean hasNext() throws IOException {
        if (token == INT_COMMA) {
            token = nextToken();
            return true;
        }
        return token != INT_RCURLY && token != INT_RBRACKET && token != INT_EOF;
    }

    @Override
    protected CharSequence nextName() throws IOException {
        checkArgument(token == INT_QUOTE, "Expected key quotes");
        JsonDecoding.StringDecoding.readQuotedUtf8(this, key);
        checkArgument(nextToken() == INT_COLON, "Expected colon after key name");
        nextToken();
        return key;
    }

    @Override
    public void close() throws IOException {

    }

    private void getValueAsBytes(RepeatedByte buffer) throws IOException {
        if (token == INT_QUOTE) {
            readUntilQuote(buffer);
            token = nextToken();
        } else {
            token = readUntilBreak(buffer, token);
        }
    }

    private int readUntilBreak(RepeatedByte buffer, int firstChar) throws IOException {
        buffer.clear();
        int b = firstChar;
        do {
            buffer.add((byte) b);
            b = readNotEOF();
        } while (!JsonDecoding.IntChar.isBreak(b));
        return isWhitespace(b) ? nextToken() : b;
    }

    private void readUntilQuote(RepeatedByte buffer) throws IOException {
        buffer.clear();
        while (true) {
            int b = readNotEOF();
            if (b == INT_QUOTE) {
                return;
            }
            buffer.add((byte) b);
            if (b == INT_BACKSLASH) {
                // gets rid of escaped backslashes
                buffer.add((byte) readNotEOF());
            }
        }
    }

    @Override
    protected JsonSource thisObj() {
        return this;
    }

    private int nextToken() throws IOException {
        int ch = 0;
        do {
            ch = source.read();
            if (isWhitespace(ch)) continue;
            return token = ch;
        } while (true);
    }

    public int skipToQuote() throws IOException {
        boolean escaped = false;
        int ch = 0;
        do {
            ch = source.read();
            // make sure that last character wasn't escape character
            if (ch > 0 && (ch != INT_QUOTE || escaped)) {
                escaped = !escaped && ch == INT_BACKSLASH;
                continue;
            }
            return token = ch;
        } while (true);
    }

    int readNotEOF() throws IOException {
        int value = source.read();
        if (value == INT_EOF) {
            throw InvalidJsonException.truncatedMessage();
        }
        return value;
    }

    int readRawByte() throws IOException {
        return source.read();
    }

    void checkExpected(int[] expected, String error) throws IOException {
        for (int i = 1; i < expected.length; i++) {
            if (expected[i] != readNotEOF()) {
                throw new InvalidJsonException(error);
            }
        }
    }

    private int token = INT_EOF;
    private final ByteSource source;
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
