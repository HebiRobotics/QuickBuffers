/*-
 * #%L
 * robobuf-runtime
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package us.hebi.robobuf;

import us.hebi.robobuf.ProtoUtil.Charsets;

/**
 * @author Florian Enner
 * @since 12 Nov 2019
 */
class JsonUtil {

    static class Base64Encoding {

        static void writeQuotedBase64(final byte[] bytes, final int length, RepeatedByte output) {

            // Size output buffer
            int pos = output.length;
            final int encodedLength = ((length + 2) / 3) << 2;
            output.setLength(pos + encodedLength + 2 /* quotes */);
            final byte[] buffer = output.array;
            buffer[pos++] = '"';

            // Encode 24-bit blocks
            int i;
            final int blockableLength = (length / 3) * 3;
            for (i = 0; i < blockableLength; i += 3, pos += 4) {
                // Copy next three bytes into lower 24 bits of int
                final int bits = (bytes[i] & 0xff) << 16 | (bytes[i + 1] & 0xff) << 8 | (bytes[i + 2] & 0xff);

                // Encode the 24 bits into four 6 bit characters
                buffer[pos] = BASE64[(bits >>> 18) & 0x3f];
                buffer[pos + 1] = BASE64[(bits >>> 12) & 0x3f];
                buffer[pos + 2] = BASE64[(bits >>> 6) & 0x3f];
                buffer[pos + 3] = BASE64[bits & 0x3f];
            }

            // Pad and encode last bits if source isn't even 24 bits
            final int remaining = length - blockableLength; // 0 - 2.
            if (remaining > 0) {
                // Prepare the int
                final int bits = ((bytes[i] & 0xff) << 10) | (remaining == 2 ? ((bytes[i + 1] & 0xff) << 2) : 0);

                // Set last four bytes
                buffer[pos] = BASE64[bits >> 12];
                buffer[pos + 1] = BASE64[(bits >>> 6) & 0x3f];
                buffer[pos + 2] = remaining == 2 ? BASE64[bits & 0x3f] : (byte) '=';
                buffer[pos + 3] = '=';
                pos += 4;
            }

            buffer[pos] = '"';

        }

        private static final byte[] BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(Charsets.ISO_8859_1);

    }

    static class StringEncoding {

        static void writeRawAscii(CharSequence sequence, RepeatedByte output) {
            final int length = sequence.length();
            output.reserve(length);
            for (int i = 0; i < length; i++) {
                output.array[output.length++] = (byte) sequence.charAt(i);
            }
        }

        static void writeQuotedUtf8(CharSequence sequence, RepeatedByte output) {
            // Fast-path: no utf8 and escape support
            final int numChars = sequence.length();
            output.reserve(numChars + 2);
            output.array[output.length++] = '"';
            int i = 0;
            fastpath:
            {
                for (; i < numChars; i++) {
                    final char c = sequence.charAt(i);
                    if (c < 128 && CAN_DIRECT_WRITE[c]) {
                        output.array[output.length++] = (byte) c;
                    } else {
                        break fastpath;
                    }
                }
                output.array[output.length++] = '"';
                return;
            }

            // Slow-path: utf8 and/or escaping
            for (; i < numChars; i++) {
                final char c = sequence.charAt(i);

                if (c < 0x80) { // ascii
                    if (CAN_DIRECT_WRITE[c]) {
                        write(output, (byte) c);
                    } else {
                        writeEscapedAscii(c, output);
                    }

                } else if (c < 0x800) { // 11 bits, two UTF-8 bytes
                    write(output,
                            (byte) ((0xF << 6) | (c >>> 6)),
                            (byte) (0x80 | (0x3F & c))
                    );
                } else if ((c < Character.MIN_SURROGATE || Character.MAX_SURROGATE < c)) {
                    // Maximum single-char code point is 0xFFFF, 16 bits, three UTF-8 bytes
                    write(output,
                            (byte) ((0xF << 5) | (c >>> 12)),
                            (byte) (0x80 | (0x3F & (c >>> 6))),
                            (byte) (0x80 | (0x3F & c))
                    );
                } else {
                    // Minimum code point represented by a surrogate pair is 0x10000, 17 bits, four UTF-8 bytes
                    final char low;
                    if (i + 1 == numChars || !Character.isSurrogatePair(c, (low = sequence.charAt(++i)))) {
                        throw new IllegalArgumentException("Unpaired surrogate at index " + (i - 1));
                    }
                    int codePoint = Character.toCodePoint(c, low);
                    write(output,
                            (byte) ((0xF << 4) | (codePoint >>> 18)),
                            (byte) (0x80 | (0x3F & (codePoint >>> 12))),
                            (byte) (0x80 | (0x3F & (codePoint >>> 6))),
                            (byte) (0x80 | (0x3F & codePoint))

                    );
                }
            }

            write(output, (byte) '"');

        }

        private static void writeEscapedAscii(char c, RepeatedByte output) {
            switch (c) {
                case '"':
                    write(output, (byte) '\\', (byte) '"');
                    break;
                case '\\':
                    write(output, (byte) '\\', (byte) '\\');
                    break;
                case '\b':
                    write(output, (byte) '\\', (byte) 'b');
                    break;
                case '\f':
                    write(output, (byte) '\\', (byte) 'f');
                    break;
                case '\n':
                    write(output, (byte) '\\', (byte) 'n');
                    break;
                case '\r':
                    write(output, (byte) '\\', (byte) 'r');
                    break;
                case '\t':
                    write(output, (byte) '\\', (byte) 't');
                    break;
                default:
                    writeAsSlashU(c, output);
            }
        }

        private static void writeAsSlashU(int c, RepeatedByte output) {
            output.reserve(6);
            output.array[output.length++] = '\\';
            output.array[output.length++] = 'u';
            output.array[output.length++] = ITOA[c >> 12 & 0xf];
            output.array[output.length++] = ITOA[c >> 8 & 0xf];
            output.array[output.length++] = ITOA[c >> 4 & 0xf];
            output.array[output.length++] = ITOA[c & 0xf];
        }

        private static final byte[] ITOA = new byte[]{
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f'};

        private static final boolean[] CAN_DIRECT_WRITE = new boolean[128];

        static {
            for (int i = 0; i < CAN_DIRECT_WRITE.length; i++) {
                if (i > 31 && i < 126 && i != '"' && i != '\\') {
                    CAN_DIRECT_WRITE[i] = true;
                }
            }
        }

    }

    /**
     * Copied from JDK12's Long.toString() implementation
     */
    static class IntegerEncoding {

        static void writeInt(int value, RepeatedByte output) {
            output.setLength(output.length + stringSize(value));
            getChars(value, output.length, output.array);
        }

        static void writeLong(long value, RepeatedByte output) {
            output.setLength(output.length + stringSize(value));
            getChars(value, output.length, output.array);
        }

        /**
         * Places characters representing the long i into the
         * character array buf. The characters are placed into
         * the buffer backwards starting with the least significant
         * digit at the specified index (exclusive), and working
         * backwards from there.
         *
         * @param i     value to convert
         * @param index next index, after the least significant digit
         * @param buf   target buffer, Latin1-encoded
         * @return index of the most significant digit or minus sign, if present
         * @implNote This method converts positive inputs into negative
         * values, to cover the Long.MIN_VALUE case. Converting otherwise
         * (negative to positive) will expose -Long.MIN_VALUE that overflows
         * long.
         */
        static void getChars(long i, int index, byte[] buf) {
            long q;
            int r;
            int charPos = index;

            boolean negative = (i < 0);
            if (!negative) {
                i = -i;
            }

            // Get 2 digits/iteration using longs until quotient fits into an int
            while (i <= Integer.MIN_VALUE) {
                q = i / 100;
                r = (int) ((q * 100) - i);
                i = q;
                buf[--charPos] = DigitOnes[r];
                buf[--charPos] = DigitTens[r];
            }

            // Get 2 digits/iteration using ints
            int q2;
            int i2 = (int) i;
            while (i2 <= -100) {
                q2 = i2 / 100;
                r = (q2 * 100) - i2;
                i2 = q2;
                buf[--charPos] = DigitOnes[r];
                buf[--charPos] = DigitTens[r];
            }

            // We know there are at most two digits left at this point.
            q2 = i2 / 10;
            r = (q2 * 10) - i2;
            buf[--charPos] = (byte) ('0' + r);

            // Whatever left is the remaining digit.
            if (q2 < 0) {
                buf[--charPos] = (byte) ('0' - q2);
            }

            if (negative) {
                buf[--charPos] = (byte) '-';
            }

        }

        /**
         * Returns the string representation size for a given int value.
         *
         * @param x int value
         * @return string size
         *
         * There are other ways to compute this: e.g. binary search,
         * but values are biased heavily towards zero, and therefore linear search
         * wins. The iteration results are also routinely inlined in the generated
         * code after loop unrolling.
         */
        static int stringSize(int x) {
            int d = 1;
            if (x >= 0) {
                d = 0;
                x = -x;
            }
            int p = -10;
            for (int i = 1; i < 10; i++) {
                if (x > p)
                    return i + d;
                p = 10 * p;
            }
            return 10 + d;
        }

        static int stringSize(long x) {
            int d = 1;
            if (x >= 0) {
                d = 0;
                x = -x;
            }
            long p = -10;
            for (int i = 1; i < 19; i++) {
                if (x > p)
                    return i + d;
                p = 10 * p;
            }
            return 19 + d;
        }

        final static byte[] DigitTens = {
                '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
                '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
                '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
                '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
                '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
                '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
                '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
                '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
                '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
                '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
        };

        final static byte[] DigitOnes = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        };

    }

    static class FloatEncoding {

        static void writeFloat(float value, RepeatedByte output) {
            if (Float.isNaN(value)) {
                output.addAll(NAN);
            } else if (Float.isInfinite(value)) {
                output.addAll(value < 0 ? NEGATIVE_INF : POSITIVE_INF);
            } else if (((int) value) == value) {
                IntegerEncoding.writeInt((int) value, output);
            } else {
                StringEncoding.writeRawAscii(Float.toString(value), output);
            }
        }

        static void writeDouble(double value, RepeatedByte output) {
            if (Double.isNaN(value)) {
                output.addAll(NAN);
            } else if (Double.isInfinite(value)) {
                output.addAll(value < 0 ? NEGATIVE_INF : POSITIVE_INF);
            } else if ((long) value == value) {
                IntegerEncoding.writeLong((long) value, output);
            } else {
                StringEncoding.writeRawAscii(Double.toString(value), output);
            }
        }

        // JSON doesn't define -inf etc., so encode as String
        private static final byte[] NEGATIVE_INF = "\"-Infinity\"".getBytes(Charsets.ISO_8859_1);
        private static final byte[] POSITIVE_INF = "\"Infinity\"".getBytes(Charsets.ISO_8859_1);
        private static final byte[] NAN = "\"NaN\"".getBytes(Charsets.ISO_8859_1);

    }

    /**
     * Copied from JsonIter / DSL-Platform
     * https://github.com/json-iterator/java/blob/master/src/main/java/com/jsoniter/output/StreamImplNumber.java
     */
    static class NumberEncoding {

        private final static int[] DIGITS = new int[1000];

        static {
            for (int i = 0; i < 1000; i++) {
                DIGITS[i] = (i < 10 ? (2 << 24) : i < 100 ? (1 << 24) : 0)
                        + (((i / 100) + '0') << 16)
                        + ((((i / 10) % 10) + '0') << 8)
                        + i % 10 + '0';
            }
        }

        private static final byte[] MIN_INT = "-2147483648".getBytes();

        public static final void writeInt(int value, final RepeatedByte output) {
            output.reserve(12);
            byte[] buf = output.array;
            int pos = output.length;
            if (value < 0) {
                if (value == Integer.MIN_VALUE) {
                    System.arraycopy(MIN_INT, 0, buf, pos, MIN_INT.length);
                    output.length = pos + MIN_INT.length;
                    return;
                }
                value = -value;
                buf[pos++] = '-';
            }
            final int q1 = value / 1000;
            if (q1 == 0) {
                pos += writeFirstBuf(buf, DIGITS[value], pos);
                output.length = pos;
                return;
            }
            final int r1 = value - q1 * 1000;
            final int q2 = q1 / 1000;
            if (q2 == 0) {
                final int v1 = DIGITS[r1];
                final int v2 = DIGITS[q1];
                int off = writeFirstBuf(buf, v2, pos);
                writeBuf(buf, v1, pos + off);
                output.length = pos + 3 + off;
                return;
            }
            final int r2 = q1 - q2 * 1000;
            final long q3 = q2 / 1000;
            final int v1 = DIGITS[r1];
            final int v2 = DIGITS[r2];
            if (q3 == 0) {
                pos += writeFirstBuf(buf, DIGITS[q2], pos);
            } else {
                final int r3 = (int) (q2 - q3 * 1000);
                buf[pos++] = (byte) (q3 + '0');
                writeBuf(buf, DIGITS[r3], pos);
                pos += 3;
            }
            writeBuf(buf, v2, pos);
            writeBuf(buf, v1, pos + 3);
            output.length = pos + 6;
        }

        private static int writeFirstBuf(final byte[] buf, final int v, int pos) {
            final int start = v >> 24;
            if (start == 0) {
                buf[pos++] = (byte) (v >> 16);
                buf[pos++] = (byte) (v >> 8);
            } else if (start == 1) {
                buf[pos++] = (byte) (v >> 8);
            }
            buf[pos] = (byte) v;
            return 3 - start;
        }

        private static void writeBuf(final byte[] buf, final int v, int pos) {
            buf[pos] = (byte) (v >> 16);
            buf[pos + 1] = (byte) (v >> 8);
            buf[pos + 2] = (byte) v;
        }

        private static final byte[] MIN_LONG = "-9223372036854775808".getBytes();

        public static final void writeLong(long value, final RepeatedByte output) {
            output.reserve(22);
            byte[] buf = output.array;
            int pos = output.length;
            if (value < 0) {
                if (value == Long.MIN_VALUE) {
                    System.arraycopy(MIN_LONG, 0, buf, pos, MIN_LONG.length);
                    output.length = pos + MIN_LONG.length;
                    return;
                }
                value = -value;
                buf[pos++] = '-';
            }
            final long q1 = value / 1000;
            if (q1 == 0) {
                pos += writeFirstBuf(buf, DIGITS[(int) value], pos);
                output.length = pos;
                return;
            }
            final int r1 = (int) (value - q1 * 1000);
            final long q2 = q1 / 1000;
            if (q2 == 0) {
                final int v1 = DIGITS[r1];
                final int v2 = DIGITS[(int) q1];
                int off = writeFirstBuf(buf, v2, pos);
                writeBuf(buf, v1, pos + off);
                output.length = pos + 3 + off;
                return;
            }
            final int r2 = (int) (q1 - q2 * 1000);
            final long q3 = q2 / 1000;
            if (q3 == 0) {
                final int v1 = DIGITS[r1];
                final int v2 = DIGITS[r2];
                final int v3 = DIGITS[(int) q2];
                pos += writeFirstBuf(buf, v3, pos);
                writeBuf(buf, v2, pos);
                writeBuf(buf, v1, pos + 3);
                output.length = pos + 6;
                return;
            }
            final int r3 = (int) (q2 - q3 * 1000);
            final int q4 = (int) (q3 / 1000);
            if (q4 == 0) {
                final int v1 = DIGITS[r1];
                final int v2 = DIGITS[r2];
                final int v3 = DIGITS[r3];
                final int v4 = DIGITS[(int) q3];
                pos += writeFirstBuf(buf, v4, pos);
                writeBuf(buf, v3, pos);
                writeBuf(buf, v2, pos + 3);
                writeBuf(buf, v1, pos + 6);
                output.length = pos + 9;
                return;
            }
            final int r4 = (int) (q3 - q4 * 1000);
            final int q5 = q4 / 1000;
            if (q5 == 0) {
                final int v1 = DIGITS[r1];
                final int v2 = DIGITS[r2];
                final int v3 = DIGITS[r3];
                final int v4 = DIGITS[r4];
                final int v5 = DIGITS[q4];
                pos += writeFirstBuf(buf, v5, pos);
                writeBuf(buf, v4, pos);
                writeBuf(buf, v3, pos + 3);
                writeBuf(buf, v2, pos + 6);
                writeBuf(buf, v1, pos + 9);
                output.length = pos + 12;
                return;
            }
            final int r5 = q4 - q5 * 1000;
            final int q6 = q5 / 1000;
            final int v1 = DIGITS[r1];
            final int v2 = DIGITS[r2];
            final int v3 = DIGITS[r3];
            final int v4 = DIGITS[r4];
            final int v5 = DIGITS[r5];
            if (q6 == 0) {
                pos += writeFirstBuf(buf, DIGITS[q5], pos);
            } else {
                final int r6 = q5 - q6 * 1000;
                buf[pos++] = (byte) (q6 + '0');
                writeBuf(buf, DIGITS[r6], pos);
                pos += 3;
            }
            writeBuf(buf, v5, pos);
            writeBuf(buf, v4, pos + 3);
            writeBuf(buf, v3, pos + 6);
            writeBuf(buf, v2, pos + 9);
            writeBuf(buf, v1, pos + 12);
            output.length = pos + 15;
        }

        private static final int POW10[] = {1, 10, 100, 1000, 10000, 100000, 1000000};

        public static final void writeFloat(float val, final RepeatedByte output) {
            if (val < 0) {
                if (val == Float.NEGATIVE_INFINITY) {
                    output.addAll(FloatEncoding.NEGATIVE_INF);
                    return;
                }
                output.add((byte) '-');
                val = -val;
            }
            if (val > 0x4ffffff) {
                if (val == Float.POSITIVE_INFINITY) {
                    output.addAll(FloatEncoding.POSITIVE_INF);
                    return;
                }
                StringEncoding.writeRawAscii(Float.toString(val), output);
                return;
            }
            int precision = 6;
            int exp = 1000000; // 6
            long lval = (long) (val * exp + 0.5);
            writeLong(lval / exp, output);
            long fval = lval % exp;
            if (fval == 0) {
                return;
            }
            output.add((byte) '.');
            output.reserve(11);
            for (int p = precision - 1; p > 0 && fval < POW10[p]; p--) {
                output.array[output.length++] = '0';
            }
            writeLong(fval, output);
            while (output.array[output.length - 1] == '0') {
                output.length--;
            }
        }

        public static final void writeDouble(double val, final RepeatedByte output) {
            if (val < 0) {
                if (val == Double.NEGATIVE_INFINITY) {
                    output.addAll(FloatEncoding.NEGATIVE_INF);
                    return;
                }
                val = -val;
                output.add((byte) '-');
            }
            if (val > 0x4ffffff) {
                if (val == Double.POSITIVE_INFINITY) {
                    output.addAll(FloatEncoding.POSITIVE_INF);
                    return;
                }
                StringEncoding.writeRawAscii(Double.toString(val), output);
                return;
            }
            int precision = 6;
            int exp = 1000000; // 6
            long lval = (long) (val * exp + 0.5);
            writeLong(lval / exp, output);
            long fval = lval % exp;
            if (fval == 0) {
                return;
            }
            output.add((byte) '.');
            output.reserve(11);
            for (int p = precision - 1; p > 0 && fval < POW10[p]; p--) {
                output.array[output.length++] = '0';
            }
            writeLong(fval, output);
            while (output.array[output.length - 1] == '0') {
                output.length--;
            }
        }

    }

    static class BooleanEncoding {

        static void writeBoolean(boolean value, RepeatedByte output) {
            if (value) {
                output.addAll(TRUE_BYTES);
            } else {
                output.addAll(FALSE_BYTES);
            }
        }

        private static final byte[] TRUE_BYTES = "true".getBytes(Charsets.ISO_8859_1);
        private static final byte[] FALSE_BYTES = "false".getBytes(Charsets.ISO_8859_1);

    }

    private static void write(RepeatedByte output, byte b0) {
        output.reserve(1);
        output.array[output.length++] = b0;
    }

    private static void write(RepeatedByte output, byte b0, byte b1) {
        output.reserve(2);
        output.array[output.length++] = b0;
        output.array[output.length++] = b1;
    }

    private static void write(RepeatedByte output, byte b0, byte b1, byte b2) {
        output.reserve(3);
        output.array[output.length++] = b0;
        output.array[output.length++] = b1;
        output.array[output.length++] = b2;
    }

    private static void write(RepeatedByte output, byte b0, byte b1, byte b2, byte b3) {
        output.reserve(4);
        output.array[output.length++] = b0;
        output.array[output.length++] = b1;
        output.array[output.length++] = b2;
        output.array[output.length++] = b3;
    }

}
