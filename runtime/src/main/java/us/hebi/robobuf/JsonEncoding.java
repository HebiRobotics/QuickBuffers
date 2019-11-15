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
 * Utility methods for encoding values in a JSON
 * compatible way.
 *
 * @author Florian Enner
 * @since 12 Nov 2019
 */
class JsonEncoding {

    static class Base64Encoding {

        /**
         * RFC4648
         *
         * @param bytes
         * @param length
         * @param output
         */
        static void writeQuotedBase64(final byte[] bytes, final int length, RepeatedByte output) {

            // Size output buffer
            final int encodedLength = ((length + 2) / 3) << 2;
            int pos = output.addLength(encodedLength + 2 /* quotes */);
            final byte[] buffer = output.array;
            buffer[pos++] = '"';

            // Encode 24-bit blocks
            int i;
            final int blockableLength = (length / 3) * 3;
            for (i = 0; i < blockableLength; i += 3, pos += 4) {
                // Copy next three bytes into lower 24 bits of int
                final int bits = (bytes[i] & 0xff) << 16 | (bytes[i + 1] & 0xff) << 8 | (bytes[i + 2] & 0xff);

                // Encode the 24 bits into four 6 bit characters
                buffer[pos/**/] = BASE64[(bits >>> 18) & 0x3f];
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
                buffer[pos/**/] = BASE64[bits >> 12];
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
            final int numChars = sequence.length();
            int i = 0;

            // Fast-path: no utf8 and escape support
            fastpath:
            {
                final int offset = output.addLength(numChars + 2) + 1;
                final byte[] ascii = output.array;
                ascii[offset - 1] = '"';

                for (; i < numChars; i++) {
                    final char c = sequence.charAt(i);
                    if (c < 128 && CAN_DIRECT_WRITE[c]) {
                        ascii[offset + i] = (byte) c;
                    } else {
                        output.length = offset + i;
                        break fastpath;
                    }
                }

                ascii[offset + i] = '"';
                return;
            }

            // Slow-path: utf8 and/or escaping
            for (; i < numChars; i++) {
                final char c = sequence.charAt(i);

                if (c < 0x80) { // ascii
                    if (CAN_DIRECT_WRITE[c]) {
                        final int offset = output.addLength(1);
                        output.array[offset] = (byte) c;
                    } else {
                        writeEscapedAscii(c, output);
                    }

                } else if (c < 0x800) { // 11 bits, two UTF-8 bytes
                    final int offset = output.addLength(2);
                    output.array[offset/**/] = (byte) ((0xF << 6) | (c >>> 6));
                    output.array[offset + 1] = (byte) (0x80 | (0x3F & c));

                } else if ((c < Character.MIN_SURROGATE || Character.MAX_SURROGATE < c)) {
                    // Maximum single-char code point is 0xFFFF, 16 bits, three UTF-8 bytes
                    final int offset = output.addLength(3);
                    output.array[offset/**/] = (byte) ((0xF << 5) | (c >>> 12));
                    output.array[offset + 1] = (byte) (0x80 | (0x3F & (c >>> 6)));
                    output.array[offset + 2] = (byte) (0x80 | (0x3F & c));

                } else {
                    // Minimum code point represented by a surrogate pair is 0x10000, 17 bits, four UTF-8 bytes
                    final char low;
                    if (i + 1 == numChars || !Character.isSurrogatePair(c, (low = sequence.charAt(++i)))) {
                        throw new IllegalArgumentException("Unpaired surrogate at index " + (i - 1));
                    }
                    int codePoint = Character.toCodePoint(c, low);
                    final int offset = output.addLength(4);
                    output.array[offset/**/] = (byte) ((0xF << 4) | (codePoint >>> 18));
                    output.array[offset + 1] = (byte) (0x80 | (0x3F & (codePoint >>> 12)));
                    output.array[offset + 2] = (byte) (0x80 | (0x3F & (codePoint >>> 6)));
                    output.array[offset + 3] = (byte) (0x80 | (0x3F & codePoint));
                }
            }

            final int offset = output.addLength(1);
            output.array[offset] = '"';

        }

        private static void writeEscapedAscii(char c, RepeatedByte output) {
            final byte escapeChar = ESCAPE_CHAR[c];
            if (escapeChar != 0) {
                // escaped with slash, e.g., \\t
                final int offset = output.addLength(2);
                output.array[offset] = '\\';
                output.array[offset + 1] = escapeChar;
            } else {
                // slash-U escaping, e.g., control character
                final int offset = output.addLength(6);
                output.array[offset] = '\\';
                output.array[offset + 1] = 'u';
                output.array[offset + 2] = ITOA[c >> 12 & 0xf];
                output.array[offset + 3] = ITOA[c >> 8 & 0xf];
                output.array[offset + 4] = ITOA[c >> 4 & 0xf];
                output.array[offset + 5] = ITOA[c & 0xf];
            }
        }

        private static final byte[] ITOA = new byte[]{
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f'};

        private static final boolean[] CAN_DIRECT_WRITE = new boolean[128];
        private static final byte[] ESCAPE_CHAR = new byte[128];

        static {
            for (int i = 0; i < CAN_DIRECT_WRITE.length; i++) {
                if (i > 31 && i < 126 && i != '"' && i != '\\') {
                    CAN_DIRECT_WRITE[i] = true;
                }
            }
            ESCAPE_CHAR['"'] = '"';
            ESCAPE_CHAR['\\'] = '\\';
            ESCAPE_CHAR['\b'] = 'b';
            ESCAPE_CHAR['\f'] = 'f';
            ESCAPE_CHAR['\n'] = 'n';
            ESCAPE_CHAR['\r'] = 'r';
            ESCAPE_CHAR['\t'] = 't';
        }

    }

    /**
     * Implementation based on JsonIter and DSL-Platform
     * https://github.com/json-iterator/java/blob/master/src/main/java/com/jsoniter/output/StreamImplNumber.java
     * https://github.com/ngs-doo/dsl-json
     */
    static class NumberEncoding {

        private final static int[] DIGITS = new int[1000];

        static {
            // Builds a lookup table that encodes three character digits
            // and meta information in the bits of an int
            for (int i = 0; i < 1000; i++) {
                int digit100 = '0' + (i / 100);
                int digit10 = '0' + ((i / 10) % 10);
                int digit1 = '0' + (i % 10);
                int numDigits = digit100 == '0' ? digit10 == '0' ? 1 : 2 : 3;
                DIGITS[i] = numDigits << 24 | digit100 << 16 | digit10 << 8 | digit1;
            }
        }

        public static void writeInt(final int value, final RepeatedByte output) {
            output.reserve(12);
            int pos = output.length;
            final byte[] buf = output.array;
            final int q0;
            if (value < 0) {
                if (value == Integer.MIN_VALUE) {
                    output.addAll(MIN_INT);
                    return;
                }
                q0 = -value;
                buf[pos++] = '-';
            } else {
                q0 = value;
            }
            // value <= 10^3
            final int q1 = q0 / 1000;
            if (q1 == 0) {
                output.length = pos + writeTopDigits(buf, DIGITS[q0], pos);
                return;
            }
            // value <= 10^6
            final int r1 = q0 - q1 * 1000;
            final int q2 = q0 / 1000000;
            if (q2 == 0) {
                pos += writeTopDigits(buf, DIGITS[q1], pos);
                pos += writeThreeDigits(buf, DIGITS[r1], pos);
                output.length = pos;
                return;
            }
            // value <= 10^9
            final int r2 = q1 - q2 * 1000;
            final int q3 = q0 / 1000000000;
            if (q3 == 0) {
                pos += writeTopDigits(buf, DIGITS[q2], pos);
                pos += writeThreeDigits(buf, DIGITS[r2], pos);
                pos += writeThreeDigits(buf, DIGITS[r1], pos);
                output.length = pos;
                return;
            }
            // value > 10^9 to max
            final int r3 = q2 - q3 * 1000;
            buf[pos++] = (byte) ('0' + q3);
            pos += writeThreeDigits(buf, DIGITS[r3], pos);
            pos += writeThreeDigits(buf, DIGITS[r2], pos);
            pos += writeThreeDigits(buf, DIGITS[r1], pos);
            output.length = pos;
        }

        public static void writeLong(final long value, final RepeatedByte output) {
            output.reserve(22);
            int pos = output.length;
            final byte[] buf = output.array;
            final long q0;
            if (value < 0) {
                if (value == Long.MIN_VALUE) {
                    output.addAll(MIN_LONG);
                    return;
                }
                q0 = -value;
                buf[pos++] = '-';
            } else {
                q0 = value;
            }
            // value <= 10^3
            final long q1 = q0 / factor3;
            if (q1 == 0) {
                output.length = pos + writeTopDigits(buf, DIGITS[(int) q0], pos);
                return;
            }
            // value <= 10^6
            final int r1 = (int) (q0 - q1 * 1000);
            final long q2 = q0 / factor6;
            if (q2 == 0) {
                pos += writeTopDigits(buf, DIGITS[(int) q1], pos);
                pos += writeThreeDigits(buf, DIGITS[r1], pos);
                output.length = pos;
                return;
            }
            // value <= 10^9
            final int r2 = (int) (q1 - q2 * 1000);
            final long q3 = q0 / factor9;
            if (q3 == 0) {
                pos += writeTopDigits(buf, DIGITS[(int) q2], pos);
                pos += writeThreeDigits(buf, DIGITS[r2], pos);
                pos += writeThreeDigits(buf, DIGITS[r1], pos);
                output.length = pos;
                return;
            }
            // value <= 10^12
            final int r3 = (int) (q2 - q3 * 1000);
            final long q4 = q0 / factor12;
            if (q4 == 0) {
                pos += writeTopDigits(buf, DIGITS[(int) q3], pos);
                pos += writeThreeDigits(buf, DIGITS[r3], pos);
                pos += writeThreeDigits(buf, DIGITS[r2], pos);
                pos += writeThreeDigits(buf, DIGITS[r1], pos);
                output.length = pos;
                return;
            }
            // value <= 10^15
            final int r4 = (int) (q3 - q4 * 1000);
            final long q5 = q0 / factor15;
            if (q5 == 0) {
                pos += writeTopDigits(buf, DIGITS[(int) q4], pos);
                pos += writeThreeDigits(buf, DIGITS[r4], pos);
                pos += writeThreeDigits(buf, DIGITS[r3], pos);
                pos += writeThreeDigits(buf, DIGITS[r2], pos);
                pos += writeThreeDigits(buf, DIGITS[r1], pos);
                output.length = pos;
                return;
            }
            // value <= 10^18
            final int r5 = (int) (q4 - q5 * 1000);
            final long q6 = q0 / factor18;
            if (q6 == 0) {
                pos += writeTopDigits(buf, DIGITS[(int) q5], pos);
                pos += writeThreeDigits(buf, DIGITS[r5], pos);
                pos += writeThreeDigits(buf, DIGITS[r4], pos);
                pos += writeThreeDigits(buf, DIGITS[r3], pos);
                pos += writeThreeDigits(buf, DIGITS[r2], pos);
                pos += writeThreeDigits(buf, DIGITS[r1], pos);
                output.length = pos;
                return;
            }
            // value > 10^18 to max
            final int r6 = (int) (q5 - q6 * 1000);
            buf[pos++] = (byte) ('0' + q6);
            pos += writeThreeDigits(buf, DIGITS[r6], pos);
            pos += writeThreeDigits(buf, DIGITS[r5], pos);
            pos += writeThreeDigits(buf, DIGITS[r4], pos);
            pos += writeThreeDigits(buf, DIGITS[r3], pos);
            pos += writeThreeDigits(buf, DIGITS[r2], pos);
            pos += writeThreeDigits(buf, DIGITS[r1], pos);
            output.length = pos;
        }

        private static int writeTopDigits(final byte[] buf, final int v, int pos) {
            final int numDigits = v >> 24;
            switch (numDigits) {
                case 3:
                    buf[pos++] = (byte) (v >>> 16);
                case 2:
                    buf[pos++] = (byte) (v >>> 8);
                case 1:
                    buf[pos] = (byte) v;
            }
            return numDigits;
        }

        private static int writeThreeDigits(final byte[] buf, final int v, int pos) {
            buf[pos] = (byte) (v >>> 16);
            buf[pos + 1] = (byte) (v >>> 8);
            buf[pos + 2] = (byte) v;
            return 3;
        }

        public static void writeFloat(float val, final RepeatedByte output) {
            writeDouble6(val, output);
        }

        public static void writeDouble(final double val, final RepeatedByte output) {
            final double pval = writeSpecialValues(val, max3, output);
            if (pval > 0) {
                if (pval < max12) {
                    writeDouble12(pval, output);
                } else if (pval < max9) {
                    writeDouble9(pval, output);
                } else if (pval < max6) {
                    writeDouble6(pval, output);
                } else {
                    writeDouble3(pval, output);
                }
            }
        }

        public static void writeDouble12(final double val, final RepeatedByte output) {
            final double pval = writeSpecialValues(val, max12, output);
            if (pval > 0) {

                final long fval = (long) (pval * factor12 + 0.5);
                writeLong(fval / factor12, output);
                final long q0 = (long) (fval % factor12);
                if (q0 == 0) {
                    return;
                }

                int pos = output.addLength(13);
                final byte[] buffer = output.array;
                buffer[pos++] = '.';
                final long q4 = q0 / factor12;
                final long q3 = q0 / factor9;
                final long q2 = q0 / factor6;
                final long q1 = q0 / factor3;
                final int r4 = (int) (q3 - q4 * 1000);
                final int r3 = (int) (q2 - q3 * 1000);
                final int r2 = (int) (q1 - q2 * 1000);
                final int r1 = (int) (q0 - q1 * 1000);

                pos += writeThreeDigits(buffer, DIGITS[r4], pos);
                if ((r3 | r2 | r1) != 0) {
                    pos += writeThreeDigits(buffer, DIGITS[r3], pos);
                    if ((r2 | r1) != 0) {
                        pos += writeThreeDigits(buffer, DIGITS[r2], pos);
                        if (r1 != 0) {
                            pos += writeThreeDigits(buffer, DIGITS[r1], pos);
                        }
                    }
                }
                output.length = pos - countTrailingZeros(buffer, pos);

            }
        }

        public static void writeDouble9(final double val, final RepeatedByte output) {
            final double pval = writeSpecialValues(val, max9, output);
            if (pval > 0) {

                final long fval = (long) (pval * factor9 + 0.5);
                writeLong(fval / factor9, output);
                final long q0 = (long) (fval % factor9);
                if (q0 == 0) {
                    return;
                }

                int pos = output.addLength(10);
                final byte[] buffer = output.array;
                buffer[pos++] = '.';
                final long q3 = q0 / factor9;
                final long q2 = q0 / factor6;
                final long q1 = q0 / factor3;
                final int r3 = (int) (q2 - q3 * 1000);
                final int r2 = (int) (q1 - q2 * 1000);
                final int r1 = (int) (q0 - q1 * 1000);

                pos += writeThreeDigits(buffer, DIGITS[r3], pos);
                if ((r2 | r1) != 0) {
                    pos += writeThreeDigits(buffer, DIGITS[r2], pos);
                    if (r1 != 0) {
                        pos += writeThreeDigits(buffer, DIGITS[r1], pos);
                    }
                }
                output.length = pos - countTrailingZeros(buffer, pos);

            }
        }

        public static void writeDouble6(final double val, final RepeatedByte output) {
            final double pval = writeSpecialValues(val, max6, output);
            if (pval > 0) {

                final long fval = (long) (pval * factor6 + 0.5);
                writeLong(fval / factor6, output);
                final long q0 = (long) (fval % factor6);
                if (q0 == 0) {
                    return;
                }

                int pos = output.addLength(7);
                final byte[] buffer = output.array;
                buffer[pos++] = '.';
                final long q2 = q0 / factor6;
                final long q1 = q0 / factor3;
                final int r2 = (int) (q1 - q2 * 1000);
                final int r1 = (int) (q0 - q1 * 1000);

                pos += writeThreeDigits(buffer, DIGITS[r2], pos);
                if (r1 != 0) {
                    pos += writeThreeDigits(buffer, DIGITS[r1], pos);
                }
                output.length = pos - countTrailingZeros(buffer, pos);

            }
        }

        public static void writeDouble3(final double val, final RepeatedByte output) {
            final double pval = writeSpecialValues(val, max3, output);
            if (pval > 0) {

                final long fval = (long) (pval * factor3 + 0.5);
                writeLong(fval / factor3, output);
                final long q0 = (long) (fval % factor3);
                if (q0 == 0) {
                    return;
                }

                int pos = output.addLength(4);
                final byte[] buffer = output.array;
                buffer[pos++] = '.';
                final long q1 = q0 / factor3;
                final int r1 = (int) (q0 - q1 * 1000);

                pos += writeThreeDigits(buffer, DIGITS[r1], pos);
                output.length = pos - countTrailingZeros(buffer, pos);

            }
        }

        /**
         * @return the positive value, or -1 if no work needs to be done
         */
        private static double writeSpecialValues(double val, final double maxValue, final RepeatedByte output) {
            if (val < 0) {
                if (val == Double.NEGATIVE_INFINITY) {
                    output.addAll(NEGATIVE_INF);
                    return -1;
                }
                output.add((byte) '-');
                val = -val;
            }
            if (val > maxValue) {
                if (val == Double.POSITIVE_INFINITY) {
                    output.addAll(POSITIVE_INF);
                } else if (val > WHOLE_NUMBER && val < Long.MAX_VALUE) {
                    NumberEncoding.writeLong((long) val, output);
                } else {
                    StringEncoding.writeRawAscii(Double.toString(val), output);
                }
                return -1;
            } else if (Double.isNaN(val)) {
                output.addAll(NAN);
                return -1;
            }
            return val;
        }

        // Counts up to two trailing zeros. We could just encode this information in
        // the digit int, but readability would suffer and the benefit is negligible.
        private static int countTrailingZeros(final byte[] buf, final int pos) {
            if (buf[pos - 1] != '0') return 0;
            if (buf[pos - 2] != '0') return 1;
            return 2;
        }

        private static final long factor3 = 1000;
        private static final long factor6 = 1000 * factor3;
        private static final long factor9 = 1000 * factor6;
        private static final long factor12 = 1000 * factor9;
        private static final long factor15 = 1000 * factor12;
        private static final long factor18 = 1000 * factor15;

        // Numbers larger than this are whole numbers due to representation error.
        static final double WHOLE_NUMBER = 1L << 52;
        static final double max3 = WHOLE_NUMBER / factor3;
        static final double max6 = WHOLE_NUMBER / factor6;
        static final double max9 = WHOLE_NUMBER / factor9;
        static final double max12 = WHOLE_NUMBER / factor12;

        // JSON doesn't define -inf etc., so encode as String
        private static final byte[] NEGATIVE_INF = "\"-Infinity\"".getBytes(Charsets.ISO_8859_1);
        private static final byte[] POSITIVE_INF = "\"Infinity\"".getBytes(Charsets.ISO_8859_1);
        private static final byte[] NAN = "\"NaN\"".getBytes(Charsets.ISO_8859_1);
        private static final byte[] MIN_INT = "-2147483648".getBytes();
        private static final byte[] MIN_LONG = "-9223372036854775808".getBytes();

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

}
