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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import static us.hebi.quickbuf.ProtoSource.*;

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
     * <p>
     * The following is equal to Protobuf-Java's "msg.writeDelimitedTo(output)"
     * <p>
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
     * <p>
     * The following is equal to Protobuf-Java's "msg.readDelimitedFrom(input)"
     * <p>
     * int length = readRawVarint32(input);
     * byte[] data = new byte[length];
     * if(input.readData(data) != length) {
     * throw new IOException("truncated message");
     * }
     * return MyMessage.parseFrom(data);
     *
     * @param input source stream
     * @return value of the decoded varint
     * @throws EOFException                   if the input has no more data
     * @throws InvalidProtocolBufferException if the varint is malformed
     * @throws IOException                    if the stream can't be read
     */
    public static int readRawVarint32(InputStream input) throws IOException {
        int x = readRawByte(input);
        if (x >= 0) {
            return x;
        } else if ((x ^= (readRawByte(input) << 7)) < 0) {
            return x ^ signs7;
        } else if ((x ^= (readRawByte(input) << 14)) >= 0) {
            return x ^ signs14;
        } else if ((x ^= (readRawByte(input) << 21)) < 0) {
            return x ^ signs21;
        } else {

            // Discard upper 32 bits.
            final int y = readRawByte(input);
            if (y < 0
                    && readRawByte(input) < 0
                    && readRawByte(input) < 0
                    && readRawByte(input) < 0
                    && readRawByte(input) < 0
                    && readRawByte(input) < 0) {
                throw InvalidProtocolBufferException.malformedVarint();
            }

            return x ^ (y << 28) ^ signs28i;
        }
    }

    private static byte readRawByte(InputStream input) throws IOException {
        int value = input.read();
        if (value < 0) {
            throw new EOFException();
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

    /**
     * Decodes utf8 bytes into a reusable StringBuilder object. Going through a builder
     * has benefits on JDK8, but is (significantly) slower when decoding ascii on JDK13.
     */
    public static void decodeUtf8(byte[] bytes, int offset, int length, StringBuilder output) {
        Utf8.decodeArray(bytes, offset, length, output);
    }

    // =========== Internal utility methods used by the runtime API ===========

    /**
     * Hash code for JSON field name lookup. Any changes need to be
     * synchronized between FieldUtil::hash32 and ProtoUtil::hash32.
     */
    static int hash32(CharSequence value) {
        // To start off with we use a simple hash copied from String::hashCode
        // Note that we can't use String::hashCode directly because the implementation
        // may change between JDK releases and could break the generated messages.
        int hash = 0;
        for (int i = 0; i < value.length(); i++) {
            hash =  31 * hash + value.charAt(i);
        }
        return hash;
    }

    static final Utf8Decoder DEFAULT_UTF8_DECODER = new Utf8Decoder() {
        @Override
        public String decode(byte[] bytes, int offset, int length) {
            return new String(bytes, offset, length, Charsets.UTF_8);
        }
    };

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
