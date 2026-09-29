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

import java.io.InputStream;
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
 * @since 17 Mar 2022
 */
class JsonDecoding {

    static class IntChar {

        public final static int INT_UNINITIALIZED = -2;
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

        final static int[] sHexValues = new int[256];

        static {
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


    static class Numbers {

        /**
         * Converts w * 10^q via Clinger's fast path, where w and 10^|q| are exact doubles, so
         * a single multiplication or division rounds correctly.
         *
         * @param w unsigned mantissa
         * @return NaN if the result would not be exact
         */
        static double toDouble(boolean negative, long w, int q) {
            double value = Double.NaN;
            if (w == 0) {
                value = 0;
            } else if (w > 0 && w <= MAX_EXACT_DOUBLE) {
                if (q >= 0 && q <= 22) {
                    value = w * EXACT_POW10[q];
                } else if (q < 0 && q >= -22) {
                    value = w / EXACT_POW10[-q];
                } else if (q > 22 && q <= 22 + 15 && w <= MAX_EXACT_DOUBLE / (long) EXACT_POW10[q - 22]) {
                    value = (w * (long) EXACT_POW10[q - 22]) * EXACT_POW10[22]; // e.g. 123e30 = 123000000000e22
                }
            }
            return negative ? -value : value;
        }

        static double readDouble(byte[] buffer, int tokenStart, int tokenEnd) throws InvalidJsonException {
            if (tokenEnd <= tokenStart) {
                throw InvalidJsonException.illegalNumberFormat();
            }
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

        static long readUnsignedLong(byte[] buffer, int tokenStart, int tokenEnd) throws InvalidJsonException {
            if (tokenEnd <= tokenStart || buffer[tokenStart] == INT_MINUS) {
                return readLong(buffer, tokenStart, tokenEnd);
            }
            int i = buffer[tokenStart] == INT_PLUS ? tokenStart + 1 : tokenStart;
            if (i == tokenEnd) {
                throw InvalidJsonException.illegalNumberFormat();
            }
            long result = 0;
            for (; i < tokenEnd; i++) {
                final int digit = (buffer[i] & 0xFF) - INT_0;
                if (digit < 0 || digit > 9 || result < 0 || result > MAX_UINT64_DIV_10
                        || (result == MAX_UINT64_DIV_10 && digit > MAX_UINT64_LAST_DIGIT)) {
                    throw InvalidJsonException.illegalNumberFormat();
                }
                result = result * 10 + digit;
            }
            return result;
        }

        static long readLong(byte[] buffer, int tokenStart, int tokenEnd) throws InvalidJsonException {
            boolean negative = false;
            int i = 0;
            int len = tokenEnd - tokenStart;
            long limit = -9223372036854775807L;
            if (len <= 0) {
                throw InvalidJsonException.illegalNumberFormat();
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
                    if (digit < 0 || digit > 9 || result < multmin) {
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
                if (buffer[i] < INT_0 || buffer[i] > INT_9) {
                    return false;
                }
            }
            return true;
        }

        private static final long MAX_UINT64_DIV_10 = 0x1999999999999999L; // 18446744073709551615 / 10
        private static final int MAX_UINT64_LAST_DIGIT = 5; // 18446744073709551615 % 10
        private static final long MAX_EXACT_DOUBLE = 1L << 53;
        private static final double[] EXACT_POW10 = {
                1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11,
                1e12, 1e13, 1e14, 1e15, 1e16, 1e17, 1e18, 1e19, 1e20, 1e21, 1e22};
    }

    private JsonDecoding() {
    }

}
