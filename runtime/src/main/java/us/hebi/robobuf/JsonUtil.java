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
     * Copied from JDK12's Long.toString() implementation
     *
     * TODO: write numbers 3 digits at once. see JsonIter / DSL-Platform?
     * https://github.com/json-iterator/java/blob/master/src/main/java/com/jsoniter/output/StreamImplNumber.java
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

    /**
     * TODO: implement without allocations
     */
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
