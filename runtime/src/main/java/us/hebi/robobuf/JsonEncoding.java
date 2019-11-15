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

    /*
    this implementations contains significant code from https://github.com/ngs-doo/dsl-json/blob/master/LICENSE
    Copyright (c) 2015, Nova Generacija Softvera d.o.o.
    All rights reserved.
    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of Nova Generacija Softvera d.o.o. nor the names of its
      contributors may be used to endorse or promote products derived from this
      software without specific prior written permission.
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
    ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
    WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
    DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
    FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
    DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
    CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
    OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
    OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
    */

    /**
     * Adapted from JsonIter / DSL-Platform
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

        public static void writeInt(int value, final RepeatedByte output) {
            output.reserve(12);
            int pos = output.length;
            final byte[] buf = output.array;
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

        public static void writeLong(long value, final RepeatedByte output) {
            output.reserve(22);
            int pos = output.length;
            final byte[] buf = output.array;
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

        public static void writeFloat(float val, final RepeatedByte output) {
            writeDouble6(val, output);
        }

        public static void writeDouble(double val, final RepeatedByte output) {
            final double pval = Math.abs(val);
            if (pval < max12) {
                writeDouble12(val, output);
            } else if (pval < max9) {
                writeDouble9(val, output);
            } else if (pval < max6) {
                writeDouble6(val, output);
            } else {
                writeDouble3(val, output);
            }
        }

        public static void writeDouble12(final double val, final RepeatedByte output) {
            final double pval = writeSpecialValues(val, max12, output);
            if (pval > 0) {

                final long fval = (long) (val * exp12 + 0.5);
                writeLong(fval / exp12, output);
                final long q0 = (long) (fval % exp12);
                if (q0 == 0) {
                    return;
                }

                final int offset = output.addLength(13);
                output.array[offset] = '.';
                final long q4 = q0 / 1000000000000L;
                final long q3 = q0 / 1000000000;
                final long q2 = q0 / 1000000;
                final long q1 = q0 / 1000;
                final int r4 = (int) (q3 - q4 * 1000);
                final int r3 = (int) (q2 - q3 * 1000);
                final int r2 = (int) (q1 - q2 * 1000);
                final int r1 = (int) (q0 - q1 * 1000);
                writeBuf(output.array, DIGITS[r4], offset + 1);
                writeBuf(output.array, DIGITS[r3], offset + 4);
                writeBuf(output.array, DIGITS[r2], offset + 7);
                writeBuf(output.array, DIGITS[r1], offset + 10);
                removeTrailingZeros(output);

            }
        }

        public static void writeDouble9(final double val, final RepeatedByte output) {
            final double pval = writeSpecialValues(val, max9, output);
            if (pval > 0) {

                final long fval = (long) (val * exp9 + 0.5);
                writeLong(fval / exp9, output);
                final long q0 = (long) (fval % exp9);
                if (q0 == 0) {
                    return;
                }

                final int offset = output.addLength(10);
                output.array[offset] = '.';
                final long q3 = q0 / 1000000000;
                final long q2 = q0 / 1000000;
                final long q1 = q0 / 1000;
                final int r3 = (int) (q2 - q3 * 1000);
                final int r2 = (int) (q1 - q2 * 1000);
                final int r1 = (int) (q0 - q1 * 1000);
                writeBuf(output.array, DIGITS[r3], offset + 1);
                writeBuf(output.array, DIGITS[r2], offset + 4);
                writeBuf(output.array, DIGITS[r1], offset + 7);
                removeTrailingZeros(output);

            }
        }

        public static void writeDouble6(double val, final RepeatedByte output) {
            final double pval = writeSpecialValues(val, max6, output);
            if (pval > 0) {

                final long fval = (long) (val * exp6 + 0.5);
                writeLong(fval / exp6, output);
                final long q0 = (long) (fval % exp6);
                if (q0 == 0) {
                    return;
                }

                final int offset = output.addLength(7);
                output.array[offset] = '.';
                final long q2 = q0 / 1000000;
                final long q1 = q0 / 1000;
                final int r2 = (int) (q1 - q2 * 1000);
                final int r1 = (int) (q0 - q1 * 1000);
                writeBuf(output.array, DIGITS[r2], offset + 1);
                writeBuf(output.array, DIGITS[r1], offset + 4);
                removeTrailingZeros(output);

            }
        }

        public static void writeDouble3(double val, final RepeatedByte output) {
            final double pval = writeSpecialValues(val, max3, output);
            if (pval > 0) {

                final long fval = (long) (val * exp3 + 0.5);
                writeLong(fval / exp3, output);
                final long q0 = (long) (fval % exp3);
                if (q0 == 0) {
                    return;
                }

                final int offset = output.addLength(4);
                output.array[offset] = '.';
                final long q1 = q0 / 1000;
                final int r1 = (int) (q0 - q1 * 1000);
                writeBuf(output.array, DIGITS[r1], offset + 1);
                removeTrailingZeros(output);

            }
        }

        /**
         * @return the positive value, or -1 if no work needs to be done
         */
        private static double writeSpecialValues(final double val, final double maxValue, final RepeatedByte output) {
            if (val < 0) {
                if (val == Double.NEGATIVE_INFINITY) {
                    output.addAll(NEGATIVE_INF);
                    return -1;
                }
                output.add((byte) '-');
                return -val;
            }
            if (val > maxValue) {
                if (val == Double.POSITIVE_INFINITY) {
                    output.addAll(POSITIVE_INF);
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

        private static void removeTrailingZeros(final RepeatedByte output) {
            while (output.array[output.length - 1] == '0') {
                output.length--;
            }
        }

        private static final long exp3 = 1000;
        private static final long exp6 = 1000 * exp3;
        private static final long exp9 = 1000 * exp6;
        private static final long exp12 = 1000 * exp9;
        private static final double max3 = (double) (Long.MAX_VALUE / exp3); // (cast to remove IDE warnings)
        private static final double max6 = (double) (Long.MAX_VALUE / exp6);
        private static final double max9 = (double) (Long.MAX_VALUE / exp9);
        private static final double max12 = (double) (Long.MAX_VALUE / exp12);

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
