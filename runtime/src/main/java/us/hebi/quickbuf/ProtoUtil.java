/*-
 * #%L
 * quickbuf-runtime
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

package us.hebi.quickbuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Utility methods for working with protobuf messages
 *
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class ProtoUtil {

    /**
     * Encode and write a varint to an OutputStream.  {@code value} is
     * treated as unsigned, so it won't be sign-extended if negative.
     *
     * The following is equal to Protobuf-Java's "msg.writeDelimitedTo(output)"
     *
     * byte[] data = msg.toByteArray();
     * writeRawVarint32(data.length, output);
     * output.write(data);
     *
     * @param value  int32 value to be encoded as varint
     * @param output target stream
     * @return number of written bytes
     */
    public static int writeRawVarint32(int value, OutputStream output) throws IOException {
        int numBytes = 1;
        while (true) {
            if ((value & ~0x7F) == 0) {
                output.write(value);
                return numBytes;
            } else {
                output.write((value & 0x7F) | 0x80);
                value >>>= 7;
                numBytes++;
            }
        }
    }

    /**
     * Reads and decodes a varint from the given input stream. If larger than 32
     * bits, discard the upper bits.
     *
     * The following is equal to Protobuf-Java's "msg.readDelimitedFrom(input)"
     *
     * int length = readRawVarint32(input);
     * byte[] data = new byte[length];
     * if(input.readData(data) != length) {
     * throw new IOException("truncated message");
     * }
     * return MyMessage.parseFrom(data);
     *
     * @param input source stream
     * @return value of the decoded varint
     */
    public static int readRawVarint32(InputStream input) throws IOException {
        byte tmp = readRawByte(input);
        if (tmp >= 0) {
            return tmp;
        }
        int result = tmp & 0x7f;
        if ((tmp = readRawByte(input)) >= 0) {
            result |= tmp << 7;
        } else {
            result |= (tmp & 0x7f) << 7;
            if ((tmp = readRawByte(input)) >= 0) {
                result |= tmp << 14;
            } else {
                result |= (tmp & 0x7f) << 14;
                if ((tmp = readRawByte(input)) >= 0) {
                    result |= tmp << 21;
                } else {
                    result |= (tmp & 0x7f) << 21;
                    result |= (tmp = readRawByte(input)) << 28;
                    if (tmp < 0) {
                        // Discard upper 32 bits.
                        for (int i = 0; i < 5; i++) {
                            if (readRawByte(input) >= 0) {
                                return result;
                            }
                        }
                        throw InvalidProtocolBufferException.malformedVarint();
                    }
                }
            }
        }
        return result;
    }

    private static byte readRawByte(InputStream input) throws IOException {
        int value = input.read();
        if (value < 0) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        return (byte) (value);
    }

    /**
     * Compares whether the contents of two CharSequences are equal
     *
     * @param a sequence A
     * @param b sequence B
     * @return true if the contents of both sequences are the same
     */
    public static boolean isEqual(CharSequence a, CharSequence b) {
        if (a.length() != b.length())
            return false;

        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i))
                return false;
        }

        return true;
    }


    // =========== Internal utility methods used by the runtime API ===========

    static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    static boolean isEqual(boolean a, boolean b) {
        return a == b;
    }

    static boolean isEqual(byte a, byte b) {
        return a == b;
    }

    static boolean isEqual(int a, int b) {
        return a == b;
    }

    static boolean isEqual(long a, long b) {
        return a == b;
    }

    static boolean isEqual(float a, float b) {
        return Float.floatToIntBits(a) == Float.floatToIntBits(b);
    }

    static boolean isEqual(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    private ProtoUtil() {
    }

    static class Charsets {
        static final Charset UTF_8 = Charset.forName("UTF-8");
        static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
        static final Charset ASCII = Charset.forName("US-ASCII");
    }
}
