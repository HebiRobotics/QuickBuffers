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
import java.util.Arrays;

import static us.hebi.quickbuf.JsonDecoding.IntChar.*;
import static us.hebi.quickbuf.ProtoUtil.*;

/**
 * JSON spec: https://datatracker.ietf.org/doc/html/rfc7159
 * <p>
 * Initial implementation based on some code from QSON and JsonIter.
 * Exception handling and number parsing etc. needs to be improved,
 * but a basic version is working.
 *
 * @author Florian Enner
 * @since 17 Mär 2022
 */
class JsonDecoding {

    public static class IntChar {

        public final static int INT_EOF = -1;
        public final static int INT_TAB = '\t';
        public final static int INT_LF = '\n';
        public final static int INT_CR = '\r';
        public final static int INT_SPACE = 0x0020;

        // Markup
        public final static int INT_LBRACKET = '[';
        public final static int INT_RBRACKET = ']';
        public final static int INT_LCURLY = '{';
        public final static int INT_RCURLY = '}';
        public final static int INT_QUOTE = '"';
        public final static int INT_APOS = '\'';
        public final static int INT_BACKSLASH = '\\';
        public final static int INT_SLASH = '/';
        public final static int INT_ASTERISK = '*';
        public final static int INT_COLON = ':';
        public final static int INT_COMMA = ',';
        public final static int INT_HASH = '#';

        // Number chars
        public final static int INT_0 = '0';
        public final static int INT_9 = '9';
        public final static int INT_MINUS = '-';
        public final static int INT_PLUS = '+';

        public final static int INT_PERIOD = '.';
        public final static int INT_e = 'e';
        public final static int INT_E = 'E';

        public final static int INT_t = 't';
        public final static int INT_r = 'r';
        public final static int INT_u = 'u';
        public final static int INT_f = 'f';
        public final static int INT_a = 'a';
        public final static int INT_l = 'l';
        public final static int INT_s = 's';

        public final static int INT_n = 'n';


        public static boolean isDigit(int ch) {
            return ch >= INT_0 && ch <= INT_9;
        }

        public static boolean isWhitespace(int ch) {
            return ch == INT_SPACE
                    || ch == INT_LF
                    || ch == INT_CR
                    || ch == INT_TAB;
        }

        public static boolean isBreak(int ch) {
            return breaks[ch];
        }

        private static final boolean[] breaks = new boolean[127];
        final static int[] sHexValues = new int[256];
        final static int[] TRUE_VALUE = {INT_t, INT_r, INT_u, INT_e};
        final static int[] FALSE_VALUE = {INT_f, INT_a, INT_l, INT_s, INT_e};
        final static int[] NULL_VALUE = {INT_n, INT_u, INT_l, INT_l};

        static {
            breaks[' '] = true;
            breaks['\t'] = true;
            breaks['\n'] = true;
            breaks['\r'] = true;
            breaks[','] = true;
            breaks['}'] = true;
            breaks[']'] = true;

            Arrays.fill(sHexValues, -1);
            for (int i = 0; i < 10; ++i) {
                sHexValues['0' + i] = i;
            }
            for (int i = 0; i < 6; ++i) {
                sHexValues['a' + i] = 10 + i;
                sHexValues['A' + i] = 10 + i;
            }
        }

    }


    static class StringDecoding {

        /**
         * current token needs to be at the beginning quote. Ends at the last quote read.
         */
        static void readQuotedUtf8(JsonSource source, RepeatedByte result) throws IOException {
            result.clear();
            while (true) {
                final int ch = source.readNotEOF();
                if (ch == INT_QUOTE) {
                    return;
                } else if (ch != INT_BACKSLASH) {
                    // standard ascii or UTF8
                    result.add((byte) ch);
                } else {
                    // Convert JSON specific escaping to raw UTF8
                    int escapedChar = source.readNotEOF();
                    if (escapedChar == INT_u) {
                        char c = readEscapedHexChar(source);
                        if (c < 0x80) {
                            result.add((byte) c);
                        } else if (c < 0x800) {
                            // 11 bits, two UTF-8 bytes
                            result.add((byte) ((0xF << 6) | (c >>> 6)));
                            result.add((byte) (0x80 | (0x3F & c)));
                        } else if ((c < Character.MIN_SURROGATE || Character.MAX_SURROGATE < c)) {
                            // Maximum single-char code point is 0xFFFF, 16 bits, three UTF-8 bytes
                            result.add((byte) ((0xF << 5) | (c >>> 12)));
                            result.add((byte) (0x80 | (0x3F & (c >>> 6))));
                            result.add((byte) (0x80 | (0x3F & c)));
                        } else {
                            // Minimum code point represented by a surrogate pair is 0x10000, 17 bits, four UTF-8 bytes
                            checkArgument(source.readNotEOF() == '\\', "expected surrogate pair");
                            checkArgument(source.readNotEOF() == 'u', "expected surrogate pair");
                            final char low = readEscapedHexChar(source);
                            int codePoint = Character.toCodePoint(c, low);
                            result.add((byte) ((0xF << 4) | (codePoint >>> 18)));
                            result.add((byte) (0x80 | (0x3F & (codePoint >>> 12))));
                            result.add((byte) (0x80 | (0x3F & (codePoint >>> 6))));
                            result.add((byte) (0x80 | (0x3F & codePoint)));
                        }
                    } else {
                        result.add((byte) escapedToRawChar(escapedChar));
                    }
                }
            }
        }

        static void readQuotedUtf8(JsonSource source, StringBuilder result) throws IOException {
            result.setLength(0);
            while (true) {
                int c = source.readNotEOF();
                if (c == INT_QUOTE) {
                    return;

                } else if (c == INT_BACKSLASH) {

                    // escaped char or unicode
                    int ch2 = source.readNotEOF();
                    if (ch2 == INT_u) {
                        result.append(readEscapedHexChar(source));
                    } else {
                        result.append(escapedToRawChar(ch2));
                    }

                } else if (c < 128) {

                    // single char
                    result.append((char) c);

                } else {

                    // multi-char (from QSON)
                    int tmp = c & 0xF0; // mask out top 4 bits to test for multibyte
                    if (tmp == 0xC0 || tmp == 0xD0) {
                        // 2 byte
                        int d = source.readNotEOF();
                        if ((d & 0xC0) != 0x080) {
                            throw new InvalidJsonException("Invalid UTF8 2 byte encoding");
                        }
                        c = ((c & 0x1F) << 6) | (d & 0x3F);
                    } else if (tmp == 0xE0) {
                        // 3 byte
                        c &= 0x0F;
                        int d = source.readNotEOF();
                        if ((d & 0xC0) != 0x080) {
                            throw new InvalidJsonException("Invalid UTF8 3 byte encoding");
                        }
                        c = (c << 6) | (d & 0x3F);
                        d = source.readNotEOF();
                        if ((d & 0xC0) != 0x080) {
                            throw new InvalidJsonException("Invalid UTF8 3 byte encoding");
                        }
                        c = (c << 6) | (d & 0x3F);
                    } else if (tmp == 0xF0) {
                        // 4 byte
                        int d = source.readNotEOF();
                        if ((d & 0xC0) != 0x080) {
                            throw new InvalidJsonException("Invalid UTF8 4 byte encoding");
                        }
                        c = ((c & 0x07) << 6) | (d & 0x3F);
                        d = source.readNotEOF();
                        if ((d & 0xC0) != 0x080) {
                            throw new InvalidJsonException("Invalid UTF8 4 byte encoding");
                        }
                        c = (c << 6) | (d & 0x3F);
                        d = source.readNotEOF();
                        if ((d & 0xC0) != 0x080) {
                            throw new InvalidJsonException("Invalid UTF8 4 byte encoding");
                        }
                        c = ((c << 6) | (d & 0x3F)) - 0x10000;
                        result.append((char) (0xD800 | (c >> 10)));
                        c = 0xDC00 | (c & 0x3FF);
                    }
                    result.append((char) c);

                }

            }
        }

        static char escapedToRawChar(int escapedChar) throws InvalidJsonException {
            switch (escapedChar) {
                case '\\':
                    return '\\';
                case '"':
                    return '"';
                case 'b':
                    return '\b';
                case 't':
                    return '\t';
                case 'n':
                    return '\n';
                case 'f':
                    return '\f';
                case 'r':
                    return '\r';
                default:
                    throw new InvalidJsonException("Unknown character format in result: '" + (char) escapedChar + "'");
            }
        }

        static char readEscapedHexChar(JsonSource source) throws IOException {
            return (char) (readHexDigit(source) << 12
                    | readHexDigit(source) << 8
                    | readHexDigit(source) << 4
                    | readHexDigit(source));
        }

        static int readHexDigit(JsonSource source) throws IOException {
            int raw = source.readNotEOF();
            int value = sHexValues[raw];
            if (value < 0) {
                throw new InvalidJsonException("expected a hex-digit, but found: '" + (char) raw + "'");
            }
            return value;
        }

    }

    static class Numbers {

        static double readDouble(byte[] buffer, int tokenStart, int tokenEnd) {
            int first = buffer[tokenStart];
            if (first == '-') {
                return -readDouble(buffer, tokenStart + 1, tokenEnd);
            } else if (first == 'N') {
                checkArgument((tokenEnd - tokenStart) == 3, "invalid double value");
                checkArgument(buffer[tokenStart + 1] == 'a', "invalid double value");
                checkArgument(buffer[tokenStart + 2] == 'N', "invalid double value");
                return Double.NaN;
            } else if (first == 'I') {
                checkArgument((tokenEnd - tokenStart) == 8, "invalid double value");
                checkArgument(buffer[tokenStart + 1] == 'n', "invalid double value");
                checkArgument(buffer[tokenStart + 2] == 'f', "invalid double value");
                checkArgument(buffer[tokenStart + 3] == 'i', "invalid double value");
                checkArgument(buffer[tokenStart + 4] == 'n', "invalid double value");
                checkArgument(buffer[tokenStart + 5] == 'i', "invalid double value");
                checkArgument(buffer[tokenStart + 6] == 't', "invalid double value");
                checkArgument(buffer[tokenStart + 7] == 'y', "invalid double value");
            }
            String str = new String(buffer, tokenStart, tokenEnd - tokenStart, Charsets.ASCII);
            return Double.parseDouble(str);
        }

        static long readLong(byte[] buffer, int tokenStart, int tokenEnd) throws InvalidJsonException {
            boolean negative = false;
            int i = 0;
            int len = tokenEnd - tokenStart;
            long limit = -9223372036854775807L;
            if (len <= 0) {
                return 0;
            } else {
                int firstChar = buffer[tokenStart] & 0xFF;
                if (firstChar < INT_0) {
                    if (firstChar == INT_MINUS) {
                        negative = true;
                        limit = -9223372036854775808L;
                    } else if (firstChar != INT_PLUS) {
                        throw InvalidJsonException.illegalNumberFormat();
                    }

                    if (len == 1) {
                        throw InvalidJsonException.illegalNumberFormat();
                    }

                    ++i;
                }

                long multmin = limit / (long) 10;

                long result;
                int digit;
                for (result = 0L; i < len; result -= (long) digit) {
                    digit = (buffer[i++ + tokenStart] & 0xFF) - INT_0;
                    if (digit < 0 || result < multmin) {
                        throw InvalidJsonException.illegalNumberFormat();
                    }

                    result *= 10L;
                    if (result < limit + (long) digit) {
                        throw InvalidJsonException.illegalNumberFormat();
                    }
                }

                return negative ? result : -result;
            }
        }

        static boolean isInteger(byte[] buffer, int tokenStart, int tokenEnd) {
            if (tokenEnd == tokenStart) {
                return false;
            } else if (buffer[tokenStart] == '-') {
                tokenStart++;
            }
            for (int i = tokenStart; i < tokenEnd; i++) {
                if (buffer[i] < 0 || intDigits[buffer[i]] < 0) {
                    return false;
                }
            }
            return true;
        }

        final static int[] intDigits = new int[127];
        final static int[] floatDigits = new int[127];
        final static int END_OF_NUMBER = -2;
        final static int DOT_IN_NUMBER = -3;
        final static int INVALID_CHAR_FOR_NUMBER = -1;
        static final long POW10[] = {
                1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000,
                1000000000, 10000000000L, 100000000000L, 1000000000000L,
                10000000000000L, 100000000000000L, 1000000000000000L};

        static {
            for (int i = 0; i < floatDigits.length; i++) {
                floatDigits[i] = INVALID_CHAR_FOR_NUMBER;
                intDigits[i] = INVALID_CHAR_FOR_NUMBER;
            }
            for (int i = '0'; i <= '9'; ++i) {
                floatDigits[i] = (i - '0');
                intDigits[i] = (i - '0');
            }
            floatDigits[','] = END_OF_NUMBER;
            floatDigits[']'] = END_OF_NUMBER;
            floatDigits['}'] = END_OF_NUMBER;
            floatDigits[' '] = END_OF_NUMBER;
            floatDigits['.'] = DOT_IN_NUMBER;
        }

    }

    private JsonDecoding() {
    }

}
