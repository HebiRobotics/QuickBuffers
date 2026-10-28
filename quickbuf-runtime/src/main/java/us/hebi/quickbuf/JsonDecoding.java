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

import java.math.BigInteger;
import java.util.Arrays;

import static us.hebi.quickbuf.JsonDecoding.IntChar.*;

/**
 * Number parsing and lookup tables for reading JSON
 *
 * @author Florian Enner
 * @since 17 Mar 2022
 */
class JsonDecoding {

    static class IntChar {

        public final static int INT_0 = '0';
        public final static int INT_9 = '9';
        public final static int INT_MINUS = '-';

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

        /**
         * Converts w * 10^q with correct rounding. Clinger's fast path covers w and 10^|q| that are
         * exact doubles, so a single multiplication or division rounds correctly. Everything else
         * goes through Eisel-Lemire.
         *
         * @param w unsigned mantissa with at most 19 digits
         */
        static double toDouble(boolean negative, long w, int q) {
            final double value;
            if (w == 0) {
                value = 0;
            } else if (w > 0 && w <= MAX_EXACT_DOUBLE && q >= -22 && q <= 22) {
                value = q < 0 ? w / EXACT_POW10[-q] : w * EXACT_POW10[q];
            } else if (w > 0 && q > 22 && q <= 22 + 15 && w <= MAX_EXACT_DOUBLE / (long) EXACT_POW10[q - 22]) {
                value = (w * (long) EXACT_POW10[q - 22]) * EXACT_POW10[22]; // e.g. 123e30 = 123000000000e22
            } else {
                return Double.longBitsToDouble(eiselLemire(w, q) | (negative ? Long.MIN_VALUE : 0));
            }
            return negative ? -value : value;
        }

        /**
         * Converts w * 10^q with Clinger's fast path in float arithmetic, or by rounding the
         * correctly rounded double, which is only wrong if it sits exactly between two floats.
         *
         * @param w unsigned mantissa with at most 19 digits
         * @return NaN if the result may be off by double rounding
         */
        static float toFloat(boolean negative, long w, int q) {
            if (w > 0 && w <= MAX_EXACT_FLOAT && q >= -10 && q <= 10) {
                final float value = q < 0 ? w / EXACT_FLOAT_POW10[-q] : w * EXACT_FLOAT_POW10[q];
                return negative ? -value : value;
            }
            final double value = toDouble(negative, w, q);
            final long bits = Double.doubleToRawLongBits(value);
            if ((bits & FLOAT_ROUNDING_BITS) == FLOAT_MIDPOINT || (value != 0 && Math.abs(value) < Float.MIN_NORMAL)) {
                return Float.NaN; // float subnormals have fewer bits, so their midpoints differ
            }
            return (float) value;
        }

        /**
         * Eisel-Lemire as in fast_float's decimal_to_binary.h (Apache-2.0 / MIT, Copyright Daniel Lemire),
         * https://github.com/fastfloat/fast_float/blob/v8.0.2/include/fast_float/decimal_to_binary.h
         * The truncated 128 bit powers of five are always precise enough for 19 digits, see
         * Mushtak and Lemire, "Fast Number Parsing Without Fallback".
         *
         * @return the bits of the positive double closest to w * 10^q
         */
        private static long eiselLemire(long w, int q) {
            if (q < MIN_POW10) {
                return 0;
            } else if (q > MAX_POW10) {
                return INFINITY_BITS;
            }
            final int lz = Long.numberOfLeadingZeros(w);
            w <<= lz;
            final int index = 2 * (q - MIN_POW10);
            long high = unsignedMultiplyHigh(w, POW5_128[index]);
            long low = w * POW5_128[index];
            if ((high & 0x1FF) == 0x1FF) {
                final long secondHigh = unsignedMultiplyHigh(w, POW5_128[index + 1]);
                low += secondHigh;
                if (secondHigh + Long.MIN_VALUE > low + Long.MIN_VALUE) {
                    high++;
                }
            }
            final int upperBit = (int) (high >>> 63);
            final int shift = upperBit + 64 - 52 - 3;
            long mantissa = high >>> shift;
            int power2 = (((152170 + 65536) * q) >> 16) + 63 + upperBit - lz + 1023;
            if (power2 <= 0) { // subnormal
                if (-power2 + 1 >= 64) {
                    return 0;
                }
                mantissa >>>= -power2 + 1;
                mantissa += mantissa & 1;
                mantissa >>>= 1;
                return mantissa; // rounding up to the smallest normal also sets the exponent bit
            }
            if ((low & ~1L) == 0 && q >= -4 && q <= 23 && (mantissa & 3) == 1 && (mantissa << shift) == high) {
                mantissa &= ~1L; // exactly halfway, so round to even
            }
            mantissa += mantissa & 1;
            mantissa >>>= 1;
            if (mantissa >= (2L << 52)) {
                mantissa = 1L << 52;
                power2++;
            }
            if (power2 >= 0x7FF) {
                return INFINITY_BITS;
            }
            return (mantissa & ~(1L << 52)) | ((long) power2 << 52);
        }

        private static long unsignedMultiplyHigh(long a, long b) {
            return JdkMath.multiplyHigh(a, b) + ((a >> 63) & b) + ((b >> 63) & a);
        }

        /**
         * 128 bit approximations of 5^q for q in [-342, 308] as pairs of high and low bits,
         * generated like fast_float's script/table_generation.py
         */
        private static long[] computePowersOfFive() {
            final long[] table = new long[2 * (MAX_POW10 - MIN_POW10 + 1)];
            BigInteger pow5 = BigInteger.ONE;
            for (int q = 0; q <= Math.max(-MIN_POW10, MAX_POW10); q++, pow5 = pow5.multiply(BigInteger.valueOf(5))) {
                if (q <= MAX_POW10) {
                    setPow5(table, q, pow5.shiftLeft(128 - pow5.bitLength())); // truncated
                }
                if (q > 0 && -q >= MIN_POW10) {
                    final int z = pow5.bitLength();
                    final int b = q <= 27 ? z + 127 : 2 * z + 128;
                    final BigInteger c = BigInteger.ONE.shiftLeft(b).divide(pow5).add(BigInteger.ONE);
                    setPow5(table, -q, c.shiftRight(Math.max(0, c.bitLength() - 128)));
                }
            }
            return table;
        }

        private static void setPow5(long[] table, int q, BigInteger value) {
            table[2 * (q - MIN_POW10)] = value.shiftRight(64).longValue();
            table[2 * (q - MIN_POW10) + 1] = value.longValue();
        }

        static long readUnsignedLong(byte[] buffer, int tokenStart, int tokenEnd) throws InvalidJsonException {
            if (tokenEnd <= tokenStart || buffer[tokenStart] == INT_MINUS) {
                return readLong(buffer, tokenStart, tokenEnd);
            }
            long result = 0;
            for (int i = tokenStart; i < tokenEnd; i++) {
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
                    } else {
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
        private static final long MAX_EXACT_FLOAT = 1L << 24;
        private static final float[] EXACT_FLOAT_POW10 = {1e0f, 1e1f, 1e2f, 1e3f, 1e4f, 1e5f, 1e6f, 1e7f, 1e8f, 1e9f, 1e10f};
        private static final long FLOAT_ROUNDING_BITS = (1L << 29) - 1; // double bits below the float mantissa
        private static final long FLOAT_MIDPOINT = 1L << 28;
        private static final int MIN_POW10 = -342; // 19 digits * 10^-343 round to zero
        private static final int MAX_POW10 = 308; // 1 * 10^309 rounds to infinity
        private static final long INFINITY_BITS = Double.doubleToRawLongBits(Double.POSITIVE_INFINITY);
        private static final long[] POW5_128 = computePowersOfFive();
    }

    private JsonDecoding() {
    }

}
