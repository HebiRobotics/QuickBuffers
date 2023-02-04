/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 HEBI Robotics
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

import us.hebi.quickbuf.Schubfach.DoubleToDecimal;

import java.util.Arrays;

import static us.hebi.quickbuf.ProtoUtil.Charsets.*;
import static us.hebi.quickbuf.Schubfach.*;

/**
 * Utility methods for encoding values in a JSON compatible way.
 * <p>
 * The implementation was inspired by DSL-Platform and JsonIter (copied from DSL)
 * <p>
 * https://github.com/ngs-doo/dsl-json/blob/master/library/src/main/java/com/dslplatform/json/NumberConverter.java
 * https://github.com/json-iterator/java/blob/master/src/main/java/com/jsoniter/output/StreamImplNumber.java
 *
 * @author Florian Enner
 * @since 12 Nov 2019
 */
class JsonEncoding {

    static class Base64Encoding {

        /**
         * RFC4648
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
                buffer[pos/**/] = BASE64[bits >>> 12];
                buffer[pos + 1] = BASE64[(bits >>> 6) & 0x3f];
                buffer[pos + 2] = remaining == 2 ? BASE64[bits & 0x3f] : (byte) '=';
                buffer[pos + 3] = '=';
                pos += 4;
            }

            buffer[pos] = '"';

        }

        private static final byte[] BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(ASCII);

    }

    static class StringEncoding {

        static void writeRawAscii(CharSequence sequence, RepeatedByte output) {
            final int length = sequence.length();
            output.reserve(length);
            for (int i = 0; i < length; i++) {
                output.array[output.length++] = (byte) sequence.charAt(i);
            }
        }

        static void writeQuotedUtf8(Utf8String sequence, RepeatedByte output) {
            final int numBytes = sequence.size();
            final byte[] utf8 = sequence.bytes();
            int i = 0;

            // Fast-path: no escape support
            fastpath:
            {
                final int offset = output.addLength(numBytes + 2) + 1;
                final byte[] out = output.array;
                out[offset - 1] = '"';

                for (; i < numBytes; i++) {
                    final byte c = utf8[i];
                    if (CAN_DIRECT_WRITE_UTF8[c & 0xFF]) {
                        out[offset + i] = c;
                    } else {
                        output.length = offset + i;
                        break fastpath;
                    }
                }

                out[offset + i] = '"';
                return;
            }

            // Slow-path: with escape support
            for (; i < numBytes; i++) {
                final byte c = utf8[i];
                if (CAN_DIRECT_WRITE_UTF8[c & 0xFF]) {
                    final int offset = output.addLength(1);
                    output.array[offset] = c;
                } else {
                    writeEscapedAscii((char) c, output);
                }
            }

            final int offset = output.addLength(1);
            output.array[offset] = '"';

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
                    if (c < 128 && CAN_DIRECT_WRITE_ASCII[c]) {
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
                    if (CAN_DIRECT_WRITE_ASCII[c]) {
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
                output.array[offset + 2] = BASE16[c >> 12 & 0xf];
                output.array[offset + 3] = BASE16[c >> 8 & 0xf];
                output.array[offset + 4] = BASE16[c >> 4 & 0xf];
                output.array[offset + 5] = BASE16[c & 0xf];
            }
        }

        private static final byte[] BASE16 = "0123456789abcdef".getBytes(ASCII);
        private static final boolean[] CAN_DIRECT_WRITE_ASCII = new boolean[128];
        private static final boolean[] CAN_DIRECT_WRITE_UTF8 = new boolean[256];
        private static final byte[] ESCAPE_CHAR = new byte[128];

        static {
            Arrays.fill(CAN_DIRECT_WRITE_UTF8, true);
            for (int i = 0; i < CAN_DIRECT_WRITE_ASCII.length; i++) {
                if (i > 31 && i < 126 && i != '"' && i != '\\') {
                    CAN_DIRECT_WRITE_ASCII[i] = true;
                }
                CAN_DIRECT_WRITE_UTF8[i] = CAN_DIRECT_WRITE_ASCII[i];
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

    static class NumberEncoding {

        public static void writeInt(final int value, final RepeatedByte output) {
            output.reserve(MAX_INT_SIZE);
            if (value < 0) {
                if (value == Integer.MIN_VALUE) {
                    output.addAll(MIN_INT);
                    return;
                }
                output.array[output.length++] = '-';
                output.length = writePositiveInt(-value, output.array, output.length);
            } else {
                output.length = writePositiveInt(+value, output.array, output.length);
            }
        }

        public static void writeLong(final long value, final RepeatedByte output) {
            output.reserve(MAX_LONG_SIZE);
            if (value < 0) {
                if (value == Long.MIN_VALUE) {
                    output.addAll(MIN_LONG);
                    return;
                }
                output.array[output.length++] = '-';
                output.length = writePositiveLong(-value, output.array, output.length);
            } else {
                output.length = writePositiveLong(+value, output.array, output.length);
            }
        }

        private static int writePositiveInt(final int q10, final byte[] buf, int pos) {
            final int q7, q4, q1; // highest N digits
            if ((q7 = q10 / pow3) == 0) {

                // value < 10^3
                pos += writeFirstDigits(buf, pos, q10);
                return pos;

            } else if ((q4 = q10 / pow6) == 0) {

                // value < 10^6
                pos += writeFirstDigits(buf, pos, q7);
                pos += writeThreeDigits(buf, pos, (q10 - q7 * pow3));
                return pos;

            } else if ((q1 = q10 / pow9) == 0) {

                // value < 10^9
                pos += writeFirstDigits(buf, pos, q4);
                pos += writeThreeDigits(buf, pos, (q7 - q4 * pow3));
                pos += writeThreeDigits(buf, pos, (q10 - q7 * pow3));
                return pos;

            } else {

                // value > 10^9 to max
                pos += writeSingleDigit(buf, pos, q1);
                pos += writeThreeDigits(buf, pos, (q4 - q1 * pow3));
                pos += writeThreeDigits(buf, pos, (q7 - q4 * pow3));
                pos += writeThreeDigits(buf, pos, (q10 - q7 * pow3));
                return pos;

            }
        }

        private static int writePositiveLong(final long q19, final byte[] buf, int pos) {
            final long q16, q13, q10, q7, q4, q1; // highest N digits
            if ((q16 = q19 / pow3) == 0) {

                // value < 10^3
                pos += writeFirstDigits(buf, pos, q19);
                return pos;

            } else if ((q13 = q19 / pow6) == 0) {

                // value < 10^6
                pos += writeFirstDigits(buf, pos, q16);
                pos += writeThreeDigits(buf, pos, (q19 - q16 * pow3));
                return pos;

            } else if ((q10 = q19 / pow9) == 0) {

                // value < 10^9
                pos += writeFirstDigits(buf, pos, q13);
                pos += writeThreeDigits(buf, pos, (q16 - q13 * pow3));
                pos += writeThreeDigits(buf, pos, (q19 - q16 * pow3));
                return pos;

            } else if ((q7 = q19 / pow12) == 0) {

                // value < 10^12
                pos += writeFirstDigits(buf, pos, q10);
                pos += writeThreeDigits(buf, pos, (q13 - q10 * pow3));
                pos += writeThreeDigits(buf, pos, (q16 - q13 * pow3));
                pos += writeThreeDigits(buf, pos, (q19 - q16 * pow3));
                return pos;

            } else if ((q4 = q19 / pow15) == 0) {

                // value < 10^15
                pos += writeFirstDigits(buf, pos, q7);
                pos += writeThreeDigits(buf, pos, (q10 - q7 * pow3));
                pos += writeThreeDigits(buf, pos, (q13 - q10 * pow3));
                pos += writeThreeDigits(buf, pos, (q16 - q13 * pow3));
                pos += writeThreeDigits(buf, pos, (q19 - q16 * pow3));
                return pos;

            } else if ((q1 = q19 / pow18) == 0) {

                // value < 10^18
                pos += writeFirstDigits(buf, pos, q4);
                pos += writeThreeDigits(buf, pos, (q7 - q4 * pow3));
                pos += writeThreeDigits(buf, pos, (q10 - q7 * pow3));
                pos += writeThreeDigits(buf, pos, (q13 - q10 * pow3));
                pos += writeThreeDigits(buf, pos, (q16 - q13 * pow3));
                pos += writeThreeDigits(buf, pos, (q19 - q16 * pow3));
                return pos;

            } else {

                // value > 10^18 to max
                pos += writeSingleDigit(buf, pos, q1);
                pos += writeThreeDigits(buf, pos, (q4 - q1 * pow3));
                pos += writeThreeDigits(buf, pos, (q7 - q4 * pow3));
                pos += writeThreeDigits(buf, pos, (q10 - q7 * pow3));
                pos += writeThreeDigits(buf, pos, (q13 - q10 * pow3));
                pos += writeThreeDigits(buf, pos, (q16 - q13 * pow3));
                pos += writeThreeDigits(buf, pos, (q19 - q16 * pow3));
                return pos;

            }
        }

        public static void writeFloat(final float val, final RepeatedByte output) {
            writeDouble6(val, output);
        }

        public static void writeDouble(final double val, final RepeatedByte output) {
            output.reserve(MAX_FIXED_DOUBLE_SIZE + 1); // sign before subsequent reserve
            final double pval = writeSpecialValues(val, max3, output);
            if (pval >= 0) {
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

        /**
         * Converts the double to a 64 bit fixed-point number and writes the pre and after
         * comma digits as two separate values.
         * <p>
         * Note that a 64 bit long can hold 19 digits total, so choosing a high fixed
         * resolution can overflow for large values. For example, a method with 12
         * digits precision should not be used for values that are larger than 10^7.
         * If values are unexpectedly large, this method will fall back to writing
         * only the pre-comma digits.
         * <p>
         * {@link #writeDouble(double, RepeatedByte)} adaptively lowers the after
         * comma precision depending on the value size.
         */
        static void writeDouble12(final double val, final RepeatedByte output) {
            output.reserve(MAX_FIXED_DOUBLE_SIZE);
            final double pval = writeSpecialValues(val, max12, output);
            if (pval >= 0) {

                final byte[] buffer = output.array;
                final long q19 = (long) (pval * pow12 + 0.5);
                final long q7 = q19 / pow12;
                int pos = writePositiveInt((int) q7, buffer, output.length);

                final long q12 = q19 - q7 * pow12;
                if (q12 != 0) {

                    final long q9 = q12 / pow3;
                    final long q6 = q12 / pow6;
                    final long q3 = q12 / pow9;
                    final long r6 = q6 - q3 * pow3;
                    final long r9 = q9 - q6 * pow3;
                    final long r12 = q12 - q9 * pow3;

                    buffer[pos++] = '.';
                    if (r12 != 0) {
                        pos += writeThreeDigits(buffer, pos, q3);
                        pos += writeThreeDigits(buffer, pos, r6);
                        pos += writeThreeDigits(buffer, pos, r9);
                        pos += writeFinalDigits(buffer, pos, r12);
                    } else if (r9 != 0) {
                        pos += writeThreeDigits(buffer, pos, q3);
                        pos += writeThreeDigits(buffer, pos, r6);
                        pos += writeFinalDigits(buffer, pos, r9);
                    } else if (r6 != 0) {
                        pos += writeThreeDigits(buffer, pos, q3);
                        pos += writeFinalDigits(buffer, pos, r6);
                    } else {
                        pos += writeFinalDigits(buffer, pos, q3);
                    }

                }

                output.length = pos;

            }
        }

        static void writeDouble9(final double val, final RepeatedByte output) {
            output.reserve(MAX_FIXED_DOUBLE_SIZE);
            final double pval = writeSpecialValues(val, max9, output);
            if (pval >= 0) {

                final byte[] buffer = output.array;
                final long q19 = (long) (pval * pow9 + 0.5);
                final long q10 = q19 / pow9;
                int pos = writePositiveLong(q10, buffer, output.length);

                final int q9 = (int) (q19 - q10 * pow9);
                if (q9 != 0) {
                    buffer[pos++] = '.';
                    pos = writeFraction9(q9, buffer, pos);
                }
                output.length = pos;

            }
        }

        static int writeFraction9(final int q9, final byte[] buffer, int pos) {
            final int q6 = q9 / pow3;
            final int q3 = q9 / pow6;
            final int r6 = q6 - q3 * pow3;
            final int r9 = q9 - q6 * pow3;
            if (r9 != 0) {
                pos += writeThreeDigits(buffer, pos, q3);
                pos += writeThreeDigits(buffer, pos, r6);
                pos += writeFinalDigits(buffer, pos, r9);
            } else if (r6 != 0) {
                pos += writeThreeDigits(buffer, pos, q3);
                pos += writeFinalDigits(buffer, pos, r6);
            } else {
                pos += writeFinalDigits(buffer, pos, q3);
            }
            return pos;
        }

        static void writeDouble6(final double val, final RepeatedByte output) {
            output.reserve(MAX_FIXED_DOUBLE_SIZE);
            final double pval = writeSpecialValues(val, max6, output);
            if (pval >= 0) {

                final byte[] buffer = output.array;
                final long q19 = (long) (pval * pow6 + 0.5);
                final long q13 = q19 / pow6;
                int pos = writePositiveLong(q13, buffer, output.length);

                final int q6 = (int) (q19 - q13 * pow6);
                if (q6 != 0) {
                    buffer[pos++] = '.';
                    pos = writeFraction6(q6, buffer, pos);
                }

                output.length = pos;

            }
        }

        static int writeFraction6(final int q6, final byte[] buffer, int pos) {
            final int q3 = q6 / pow3;
            final int r6 = q6 - q3 * pow3;
            if (r6 != 0) {
                pos += writeThreeDigits(buffer, pos, q3);
                pos += writeFinalDigits(buffer, pos, r6);
            } else {
                pos += writeFinalDigits(buffer, pos, q3);
            }
            return pos;
        }

        static void writeDouble3(final double val, final RepeatedByte output) {
            output.reserve(MAX_FIXED_DOUBLE_SIZE);
            final double pval = writeSpecialValues(val, max3, output);
            if (pval >= 0) {

                final byte[] buffer = output.array;
                final long q19 = (long) (pval * pow3 + 0.5);
                final long q16 = q19 / pow3;
                int pos = writePositiveLong(q16, buffer, output.length);

                final int q3 = (int) (q19 - q16 * pow3);
                if (q3 != 0) {
                    buffer[pos++] = '.';
                    pos = writeFraction3(q3, buffer, pos);
                }

                output.length = pos;

            }
        }

        static int writeFraction3(final int q3, final byte[] buffer, int pos) {
            return pos + writeFinalDigits(buffer, pos, q3);
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

        private static int writeSingleDigit(final byte[] buf, final int pos, final long number) {
            buf[pos] = (byte) ('0' + number);
            return 1;
        }

        private static int writeFirstDigits(final byte[] buf, final int pos, final long number) {
            return writeFirstDigits(buf, pos, (int) number);
        }

        private static int writeFinalDigits(final byte[] buf, int pos, final long number) {
            return writeFinalDigits(buf, pos, (int) number);
        }

        private static int writeThreeDigits(final byte[] buf, final int pos, final long number) {
            writeThreeDigits(buf, pos, (int) number);
            return 3;
        }

        private static int writeFirstDigits(final byte[] buf, int pos, final int number) {
            final int v = THREE_DIGITS[number];
            switch (v >>> NUM_DIGITS) {
                case 1:
                    buf[pos] = (byte) (v >>> DIGIT_00X);
                    return 1;
                case 2:
                    ByteUtil.writeLittleEndian16(buf, pos, (short) (v >>> DIGIT_0X0));
                    return 2;
                case 3:
                    ByteUtil.writeLittleEndian32(buf, pos, v);
                    return 3;
            }
            throw new AssertionError("invalid number");
        }

        private static int writeFinalDigits(final byte[] buf, int pos, final int number) {
            final int v = THREE_DIGITS[number];
            switch ((v >>> NUM_FINAL_DIGITS) & NUM_DIGITS_MASK) {
                case 1:
                    buf[pos] = (byte) (v);
                    return 1;
                case 2:
                    ByteUtil.writeLittleEndian16(buf, pos, (short) (v));
                    return 2;
                case 3:
                    ByteUtil.writeLittleEndian32(buf, pos, v);
                    return 3;
            }
            throw new AssertionError("invalid number");
        }

        private static int writeThreeDigits(final byte[] buf, final int pos, final int number) {
            // Write 4 bytes at once and ignore the last one
            ByteUtil.writeLittleEndian32(buf, pos, THREE_DIGITS[number]);
            return 3;
        }

        // Lookup table that packs three character digits and size information
        // into an int. This way we can write up to three characters at once. It
        // is arranged in little endian order so the 3 digit case can be optimized
        // into a single 4 byte write.
        private static final int[] THREE_DIGITS = new int[1000];
        private static final int DIGIT_00X = 16;
        private static final int DIGIT_0X0 = 8;
        private static final int DIGIT_X00 = 0;
        private static final int NUM_DIGITS = 28;
        private static final int NUM_FINAL_DIGITS = 24;
        private static final int NUM_DIGITS_MASK = 0x3;

        static {
            for (int i = 0; i < 1000; i++) {
                int digit100 = '0' + (i / 100);
                int digit10 = '0' + ((i % 100) / 10);
                int digit1 = '0' + (i % 10);
                int numDigits = digit100 == '0' ? digit10 == '0' ? 1 : 2 : 3; // before comma
                int numFinalDigits = digit1 == '0' ? digit10 == '0' ? 1 : 2 : 3; // after comma
                THREE_DIGITS[i] = numDigits << NUM_DIGITS | numFinalDigits << NUM_FINAL_DIGITS |
                        digit100 << DIGIT_X00 | digit10 << DIGIT_0X0 | digit1 << DIGIT_00X;
            }
        }

        // Common powers of 10^n
        private static final int pow3 = 1000;
        private static final int pow6 = 1000 * pow3;
        private static final int pow9 = 1000 * pow6;
        private static final long pow12 = 1000L * pow9;
        private static final long pow15 = 1000L * pow12;
        private static final long pow18 = 1000L * pow15;

        // Numbers larger than this are whole numbers due to representation error.
        static final double WHOLE_NUMBER = 1L << 52;
        static final double max3 = WHOLE_NUMBER / pow3;
        static final double max6 = WHOLE_NUMBER / pow6;
        static final double max9 = WHOLE_NUMBER / pow9;
        static final double max12 = WHOLE_NUMBER / pow12;

        // JSON numbers don't allow inf/nan etc., so we need to encode them as String
        private static final byte[] NEGATIVE_INF = "\"-Infinity\"".getBytes(ASCII);
        private static final byte[] POSITIVE_INF = "\"Infinity\"".getBytes(ASCII);
        private static final byte[] NAN = "\"NaN\"".getBytes(ASCII);
        private static final byte[] MIN_INT = "-2147483648".getBytes(ASCII);
        private static final byte[] MIN_LONG = "-9223372036854775808".getBytes(ASCII);

        // Maximum representation size in characters. Fixed double values get
        // stored in a long that contains the pre and post comma digits, so
        // the total number of digits can't be larger than a long with comma.
        // We add +1 because 3 digits may be written as 4 bytes.
        private static final int MAX_INT_SIZE = MIN_INT.length + 1;
        private static final int MAX_LONG_SIZE = MIN_LONG.length + 1;
        private static final int MAX_FIXED_DOUBLE_SIZE = MAX_LONG_SIZE + 1;

    }

    static class FloatEncoder implements FloatToDecimal.FloatEncoder, DoubleToDecimal.DoubleEncoder {

        public void writeFloat(float value, RepeatedByte output) {
            if (ENCODE_FLOAT_FIXED) {

                // deprecated way with fixed precision
                NumberEncoding.writeFloat(value, output);

            } else if (ENCODE_FLOAT_NO_COMMA) {

                // Always integer with exponent (co comma)
                this.output = output.reserve(MAX_DOUBLE_SIZE);
                final int type = FloatToDecimal.encode(value, this);
                if (type != DoubleToDecimal.NON_SPECIAL) {
                    writeSpecial(type, output);
                }
                this.output = null;

            } else if (ENCODE_FLOAT_MINIMAL) {

                // Java-like minimal representation, but without trailing zero
                final int type = schubfachf.encodeFloat(value);
                if (type == FloatToDecimal.NON_SPECIAL) {
                    int position = output.addLength(schubfachf.getEncodedLength());
                    schubfachf.getEncoded(output.array, position, output.length);
                    removeTrailingZero(output);
                } else {
                    writeSpecial(type, output);
                }

            } else {
                throw new AssertionError("bad configuration");
            }
        }

        public void writeDouble(double value, RepeatedByte output) {
            if (ENCODE_FLOAT_FIXED) {

                // deprecated way with fixed precision
                NumberEncoding.writeDouble(value, output);

            } else if(ENCODE_FLOAT_NO_COMMA) {

                // Always integer with exponent (co comma)
                this.output = output.reserve(MAX_DOUBLE_SIZE);
                final int type = DoubleToDecimal.encode(value, this);
                if (type != DoubleToDecimal.NON_SPECIAL) {
                    writeSpecial(type, output);
                }
                this.output = null;

            }  else if(ENCODE_FLOAT_MINIMAL) {

                // Java-like minimal representation, but without trailing zero
                final int type = schubfachd.encodeDouble(value);
                if (type == DoubleToDecimal.NON_SPECIAL) {
                    int position = output.addLength(schubfachd.getEncodedLength());
                    schubfachd.getEncoded(output.array, position, output.length);
                    removeTrailingZero(output);
                } else {
                    writeSpecial(type, output);
                }

            } else {
                throw new AssertionError("bad configuration");
            }
        }

        private void removeTrailingZero(RepeatedByte output) {
            // Schubfach encodes according to the Java rules w/ a minimal encoding
            // and a trailing zero. Protobuf and JSON does not require the trailing
            // zero, so we can omit it and save 2 characters.
            short lastTwoBytes = ByteUtil.readLittleEndian16(output.array, output.length - 2);
            if (lastTwoBytes == DOT_ZERO_LE) {
                output.length -= 2;
            }
        }

        @Override
        public void encodeFloat(boolean negative, int significand, int exponent) {
            writeFloatWithoutComma(negative, significand, exponent);
        }

        private void writeFloatWithoutComma(boolean negative, int significand, int exponent) {
            final byte[] buffer = output.array;
            if (negative) output.add(NEGATIVE_SIGN);
            int newLength = NumberEncoding.writePositiveInt(significand, buffer, output.length);
            final int trailingZeros = getNumTrailingZeros(buffer, output.length, newLength - 1);
            newLength -= trailingZeros;
            exponent += trailingZeros;
            output.length = newLength;
            if (exponent != 0) {
                if (exponent > 0 && exponent <= 2 && trailingZeros >= exponent) {
                    output.length += exponent;
                } else {
                    output.add(E);
                    NumberEncoding.writeInt(exponent, output);
                }
            }
        }

        @Override
        public void encodeDouble(boolean negative, long significand, int exponent) {
            writeDoubleWithoutComma(negative, significand, exponent);
        }

        private void writeDoubleWithoutComma(boolean negative, long significand, int exponent) {
            final byte[] buffer = output.array;
            if (negative) output.add(NEGATIVE_SIGN);
            int newLength = NumberEncoding.writePositiveLong(significand, buffer, output.length);
            final int trailingZeros = getNumTrailingZeros(buffer, output.length, newLength - 1);
            newLength -= trailingZeros;
            exponent += trailingZeros;
            output.length = newLength;
            if (exponent != 0) {
                if (exponent > 0 && exponent <= 2 && trailingZeros >= exponent) {
                    output.length += exponent;
                } else {
                    output.add(E);
                    NumberEncoding.writeInt(exponent, output);
                }
            }
        }

        static int getNumTrailingZeros(byte[] buffer, int startIx, int maxIx) {
            for (int i = maxIx; i > startIx; i--) {
                if (buffer[i] != ZERO) {
                    return maxIx - i;
                }
            }
            return maxIx - startIx;
        }

        private void writeSpecial(int type, RepeatedByte output) {
            switch (type) {
                case FloatToDecimal.NON_SPECIAL:
                    throw new IllegalArgumentException("not a special type");
                case FloatToDecimal.PLUS_ZERO:
                    output.addAll(PLUS_ZERO);
                    break;
                case FloatToDecimal.MINUS_ZERO:
                    output.addAll(MINUS_ZERO);
                    break;
                case FloatToDecimal.PLUS_INF:
                    output.addAll(PLUS_INF);
                    break;
                case FloatToDecimal.MINUS_INF:
                    output.addAll(MINUS_INF);
                    break;
                default:
                    output.addAll(NAN);
                    break;
            }
        }

        RepeatedByte output;
        final FloatToDecimal.DefaultFloatEncoder schubfachf = ENCODE_FLOAT_MINIMAL ? new FloatToDecimal.DefaultFloatEncoder() : null;
        final DoubleToDecimal.DefaultDoubleEncoder schubfachd = ENCODE_FLOAT_MINIMAL ? new DoubleToDecimal.DefaultDoubleEncoder() : null;

        static final int MAX_DOUBLE_SIZE = 17 + 6;

        static final byte ZERO = (byte) '0';
        static final short DOT_ZERO_LE = '.' | '0' << 8;
        static final byte E = (byte) 'E';
        static final byte NEGATIVE_SIGN = (byte) '-';
        static final byte[] PLUS_ZERO = "0".getBytes(ASCII);
        static final byte[] MINUS_ZERO = PLUS_ZERO;
        static final byte[] PLUS_INF = "\"Infinity\"".getBytes(ASCII);
        static final byte[] MINUS_INF = "\"-Infinity\"".getBytes(ASCII);
        static final byte[] NAN = "\"NaN\"".getBytes(ASCII);

        private static final String FLOAT_ENCODING_TYPE = System.getProperty("quickbuf.json.float_encoding", "minimal");
        private static final boolean ENCODE_FLOAT_NO_COMMA = "no_comma".equalsIgnoreCase(FLOAT_ENCODING_TYPE);
        static final boolean ENCODE_FLOAT_FIXED = "fixed".equalsIgnoreCase(FLOAT_ENCODING_TYPE);
        private static final boolean ENCODE_FLOAT_MINIMAL = !ENCODE_FLOAT_NO_COMMA && !ENCODE_FLOAT_FIXED;

    }

    static class BooleanEncoding {

        static void writeBoolean(boolean value, RepeatedByte output) {
            if (value) {
                output.addAll(TRUE);
            } else {
                output.addAll(FALSE);
            }
        }

        private static final byte[] TRUE = {'t', 'r', 'u', 'e'};
        private static final byte[] FALSE = {'f', 'a', 'l', 's', 'e'};

    }

}
