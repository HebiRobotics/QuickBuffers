package us.hebi.robobuf;

import us.hebi.robobuf.ProtoUtil.Charsets;

/**
 * @author Florian Enner
 * @since 12 Nov 2019
 */
class JsonUtil {

    static class Base64Encoding {

        static void writeBytes(final byte[] bytes, final int length, RepeatedByte output) {

            final int encodedLength = ((length + 2) / 3) << 2;
            output.reserve(encodedLength);

            // Encode 24-bit blocks
            int i;
            final int blockableLength = (length / 3) * 3; // Length of even 24-bits.
            for (i = 0; i < blockableLength; ) {
                // Copy next three bytes into lower 24 bits of int
                final int bits = (bytes[i++] & 0xff) << 16 | (bytes[i++] & 0xff) << 8 | (bytes[i++] & 0xff);

                // Encode the 24 bits into four characters
                output.array[output.length++] = BASE64[(bits >>> 18) & 0x3f];
                output.array[output.length++] = BASE64[(bits >>> 12) & 0x3f];
                output.array[output.length++] = BASE64[(bits >>> 6) & 0x3f];
                output.array[output.length++] = BASE64[bits & 0x3f];
            }

            // Pad and encode last bits if source isn't even 24 bits.
            final int remaining = length - blockableLength; // 0 - 2.
            if (remaining > 0) {
                // Prepare the int
                int bits = ((bytes[i++] & 0xff) << 10) | (remaining == 2 ? ((bytes[i] & 0xff) << 2) : 0);

                // Set last four bytes
                output.array[output.length++] = BASE64[bits >> 12];
                output.array[output.length++] = BASE64[(bits >>> 6) & 0x3f];
                output.array[output.length++] = remaining == 2 ? BASE64[bits & 0x3f] : (byte) '=';
                output.array[output.length++] = '=';
            }

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

        static void writeUtf8(CharSequence sequence, RepeatedByte output) {
            final int numChars = sequence.length();
            if (numChars == 0) return;
            int i = 0;

            // Fast-path: no utf8 and escape support
            output.reserve(numChars);
            for (; i < numChars; i++) {
                final char c = sequence.charAt(i);
                if (c < 128 && CAN_DIRECT_WRITE[c]) {
                    output.array[output.length++] = (byte) c;
                } else {
                    break;
                }
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
     * Copied from Java's Long.toString() implementation
     *
     * TODO: write numbers 3 digits at once. see JsonIter / DSL-Platform?
     * https://github.com/json-iterator/java/blob/master/src/main/java/com/jsoniter/output/StreamImplNumber.java
     */
    static class IntegerEncoding {

        static void writeInt(int value, RepeatedByte output) {
            if (value == Integer.MIN_VALUE) {
                output.addAll(INTEGER_MIN_VALUE_BYTES);
                return;
            }

            output.reserve(12);
            if (value < 0) {
                output.array[output.length++] = '-';
                value = -value;
            }

            output.length += stringSize(value);
            getPositiveLongBytes(value, output.length, output.array);

        }

        static void writeLong(long value, RepeatedByte output) {
            if (value == Long.MIN_VALUE) {
                output.addAll(LONG_MIN_VALUE_BYTES);
                return;
            }

            output.reserve(22);
            if (value < 0) {
                output.array[output.length++] = '-';
                value = -value;
            }

            output.length += stringSize(value);
            getPositiveLongBytes(value, output.length, output.array);
        }

        /**
         * Places bytes representing the positive integer i into the
         * byte array buf. The characters are placed into
         * the buffer backwards starting with the least significant
         * digit at the specified index (exclusive), and working
         * backwards from there.
         *
         * Will fail if i == Long.MIN_VALUE
         */
        static void getPositiveLongBytes(long i, int index, byte[] buf) {
            long q;
            int r;
            int charPos = index;

            // Get 2 digits/iteration using longs until quotient fits into an int
            while (i > Integer.MAX_VALUE) {
                q = i / 100;
                // really: r = i - (q * 100);
                r = (int) (i - ((q << 6) + (q << 5) + (q << 2)));
                i = q;
                buf[--charPos] = DigitOnes[r];
                buf[--charPos] = DigitTens[r];
            }

            // Get 2 digits/iteration using ints
            int q2;
            int i2 = (int) i;
            while (i2 >= 65536) {
                q2 = i2 / 100;
                // really: r = i2 - (q * 100);
                r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
                i2 = q2;
                buf[--charPos] = DigitOnes[r];
                buf[--charPos] = DigitTens[r];
            }

            // Fall thru to fast mode for smaller numbers
            // assert(i2 <= 65536, i2);
            for (; ; ) {
                q2 = (i2 * 52429) >>> (16 + 3);
                r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
                buf[--charPos] = digits[r];
                i2 = q2;
                if (i2 == 0) break;
            }
        }

        // Requires positive x
        static int stringSize(long x) {
            long p = 10;
            for (int i = 1; i < 19; i++) {
                if (x < p)
                    return i;
                p = 10 * p;
            }
            return 19;
        }

        // Requires positive x
        static int stringSize(int x) {
            for (int i = 0; ; i++)
                if (x <= sizeTable[i])
                    return i + 1;
        }

        final static int[] sizeTable = {9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE};
        final static byte[] INTEGER_MIN_VALUE_BYTES = String.valueOf(Integer.MIN_VALUE).getBytes(Charsets.ISO_8859_1);
        final static byte[] LONG_MIN_VALUE_BYTES = String.valueOf(Long.MIN_VALUE).getBytes(Charsets.ISO_8859_1);

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

        /**
         * All possible chars for representing a number as a String
         */
        final static byte[] digits = {
                '0', '1', '2', '3', '4', '5',
                '6', '7', '8', '9', 'a', 'b',
                'c', 'd', 'e', 'f', 'g', 'h',
                'i', 'j', 'k', 'l', 'm', 'n',
                'o', 'p', 'q', 'r', 's', 't',
                'u', 'v', 'w', 'x', 'y', 'z'
        };

    }

    /**
     * TODO: implement without allocations
     */
    static class FloatEncoding {

        static void writeFloat(float value, RepeatedByte output) {
            if (Float.isNaN(value)) {
                output.addAll(NAN_BYTES);
            } else if (Float.isInfinite(value)) {
                output.addAll(value < 0 ? NEGATIVE_INF : POSITIVE_INF);
            } else {
                StringEncoding.writeRawAscii(Float.toString(value), output);
            }
        }

        static void writeDouble(double value, RepeatedByte output) {
            if (Double.isNaN(value)) {
                output.addAll(NAN_BYTES);
            } else if (Double.isInfinite(value)) {
                output.addAll(value < 0 ? NEGATIVE_INF : POSITIVE_INF);
            } else {
                StringEncoding.writeRawAscii(Double.toString(value), output);
            }
        }

        // JSON doesn't define -inf etc., so encode as String
        private static final byte[] NEGATIVE_INF = "\"-Infinity\"".getBytes(Charsets.ISO_8859_1);
        private static final byte[] POSITIVE_INF = "\"Infinity\"".getBytes(Charsets.ISO_8859_1);
        private static final byte[] NAN_BYTES = "\"NaN\"".getBytes(Charsets.ISO_8859_1);

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
