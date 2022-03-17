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

import static us.hebi.quickbuf.IntChar.*;
import static us.hebi.quickbuf.ProtoUtil.*;

/**
 * @author Florian Enner
 * @since 17 Mär 2022
 */
class JsonDecoding {

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
                    // Remove JSON specific escaping
                    int escapedChar = source.readNotEOF();
                    if (escapedChar == INT_u) {
                        result.add((byte) '\\');
                        result.add((byte) 'u');
                        result.add((byte) readHexDigit(source));
                        result.add((byte) readHexDigit(source));
                        result.add((byte) readHexDigit(source));
                        result.add((byte) readHexDigit(source));
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
                    if (ch2 == INT_u)
                        result.append(readEscapedHexChar(source));
                    else {
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
                            throw new QsonException("Invalid UTF8 2 byte encoding");
                        }
                        c = ((c & 0x1F) << 6) | (d & 0x3F);
                    } else if (tmp == 0xE0) {
                        // 3 byte
                        c &= 0x0F;
                        int d = source.readNotEOF();
                        if ((d & 0xC0) != 0x080) {
                            throw new QsonException("Invalid UTF8 3 byte encoding");
                        }
                        c = (c << 6) | (d & 0x3F);
                        d = source.readNotEOF();
                        if ((d & 0xC0) != 0x080) {
                            throw new QsonException("Invalid UTF8 3 byte encoding");
                        }
                        c = (c << 6) | (d & 0x3F);
                    } else if (tmp == 0xF0) {
                        // 4 byte
                        int d = source.readNotEOF();
                        if ((d & 0xC0) != 0x080) {
                            throw new QsonException("Invalid UTF8 4 byte encoding");
                        }
                        c = ((c & 0x07) << 6) | (d & 0x3F);
                        d = source.readNotEOF();
                        if ((d & 0xC0) != 0x080) {
                            throw new QsonException("Invalid UTF8 4 byte encoding");
                        }
                        c = (c << 6) | (d & 0x3F);
                        d = source.readNotEOF();
                        if ((d & 0xC0) != 0x080) {
                            throw new QsonException("Invalid UTF8 4 byte encoding");
                        }
                        c = ((c << 6) | (d & 0x3F)) - 0x10000;
                        result.append((char) (0xD800 | (c >> 10)));
                        c = 0xDC00 | (c & 0x3FF);
                    }
                    result.append((char) c);

                }

            }
        }

        static char escapedToRawChar(int escapedChar) {
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
                    throw new QsonException("Unknown character format in result: '" + (char) escapedChar + "'");
            }
        }

        static char readEscapedHexChar(JsonSource source) throws IOException {
            return (char) (readHexDigit(source) << 12
                    | readHexDigit(source) << 8
                    | readHexDigit(source) << 4
                    | readHexDigit(source)
            );
        }

        static int readHexDigit(JsonSource source) throws IOException {
            try {
                int value = source.readRawByte();
                if (CharArrays.sHexValues[value] < 0) {
                    throw new QsonException("expected a hex-digit, but found: '" + (char) value + "'");
                }
                return value;
            } catch (ArrayIndexOutOfBoundsException oob) {
                throw new QsonException("While parsing a json message, the input ended unexpectedly in the" +
                        "middle of a field. This could mean that the input has been truncated.");
            }
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

        static long readLong(byte[] buffer, int tokenStart, int tokenEnd) {
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
                        throw new QsonException("Illegal number format");
                    }

                    if (len == 1) {
                        throw new QsonException("Illegal number format");
                    }

                    ++i;
                }

                long multmin = limit / (long) 10;

                long result;
                int digit;
                for (result = 0L; i < len; result -= (long) digit) {
                    digit = (buffer[i++ + tokenStart] & 0xFF) - INT_0;
                    if (digit < 0 || result < multmin) {
                        throw new QsonException("Illegal number format");
                    }

                    result *= 10L;
                    if (result < limit + (long) digit) {
                        throw new QsonException("Illegal number format");
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
