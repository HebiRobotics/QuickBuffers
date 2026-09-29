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

import java.util.Arrays;

import static us.hebi.quickbuf.JsonDecoding.IntChar.*;
import static us.hebi.quickbuf.ProtoUtil.*;

/**
 * Number parsing and lookup tables for reading JSON
 *
 * @author Florian Enner
 * @since 17 Mar 2022
 */
class JsonDecoding {

    static class IntChar {

        public final static int INT_0 = '0';
        public final static int INT_MINUS = '-';
        public final static int INT_PLUS = '+';

        final static int[] sHexValues = new int[256];
        final static int[] sEscapedValues = new int[256]; // char for the byte after a backslash, except 'u'

        static {
            Arrays.fill(sEscapedValues, -1);
            sEscapedValues['"'] = '"';
            sEscapedValues['\\'] = '\\';
            sEscapedValues['/'] = '/';
            sEscapedValues['b'] = '\b';
            sEscapedValues['f'] = '\f';
            sEscapedValues['n'] = '\n';
            sEscapedValues['r'] = '\r';
            sEscapedValues['t'] = '\t';

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
