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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Utility methods for working with protobuf messages
 *
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public final class ProtoUtil {

    /**
     * Decodes utf8 bytes into a reusable StringBuilder object. Going through a builder
     * has benefits on JDK8, but is (significantly) slower when decoding ascii on JDK13.
     */
    public static void decodeUtf8(byte[] bytes, int offset, int length, StringBuilder output) {
        Utf8.decodeArray(bytes, offset, length, output);
    }

    /**
     * Parses binary data from base64 encoded String segments. Byte arrays are stored very
     * inefficiently in class files, so it is better to store binary data (e.g. descriptors)
     * in string form and parse it at runtime. String literals are limited to 64K,
     * so the data may be split into multiple parts.
     *
     * @param expectedSize size hint for allocating the initial array
     * @param parts binary parts encoded as base64 strings
     * @return the byte representation of the descriptor
     */
    public static RepeatedByte decodeBase64(int expectedSize, String... parts) {
        RepeatedByte bytes = RepeatedByte.newEmptyInstance().reserve(expectedSize);
        for (String part : parts) {
            bytes.addAll(Base64.decode(part));
        }
        return bytes;
    }

    /**
     * Hash code for JSON field name lookup. Any changes need to be
     * synchronized between FieldUtil::hash32 and ProtoUtil::hash32.
     */
    public static int hash32(CharSequence value) {
        // To start off with we use a simple hash identical to String::hashCode. The
        // algorithm has been documented since JDK 1.2, so it can't change without
        // breaking backwards compatibility.
        if (value instanceof String) {
            return ((String) value).hashCode();
        } else {
            // Same algorithm, but generalized for CharSequences
            int hash = 0;
            final int length = value.length();
            for (int i = 0; i < length; i++) {
                hash = 31 * hash + value.charAt(i);
            }
            return hash;
        }
    }

    public static void checkState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    public static void checkArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static <T> T checkNotNull(T object) {
        if (object == null) {
            throw new NullPointerException();
        }
        return object;
    }

    public static void checkBounds(byte[] buffer, int offset, int length) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        } else if (offset < 0 || length < 0 || offset + length > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public static boolean isEqual(CharSequence a, CharSequence b) {
        if (a.length() != b.length())
            return false;

        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i))
                return false;
        }

        return true;
    }

    public static boolean isEqual(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    public static boolean isEqual(float a, float b) {
        return Float.floatToIntBits(a) == Float.floatToIntBits(b);
    }

    public static boolean isEqual(boolean a, boolean b) {
        return a == b;
    }

    public static boolean isEqual(long a, long b) {
        return a == b;
    }

    public static boolean isEqual(int a, int b) {
        return a == b;
    }

    public static boolean isEqual(byte a, byte b) {
        return a == b;
    }

    static final Utf8Decoder DEFAULT_UTF8_DECODER = new Utf8Decoder() {
        @Override
        public String decode(byte[] bytes, int offset, int length) {
            return new String(bytes, offset, length, Charsets.UTF_8);
        }
    };

    static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.wrap(EMPTY_BYTE_ARRAY);

    private ProtoUtil() {
    }

    static class Charsets {
        static final Charset UTF_8 = Charset.forName("UTF-8");
        static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
        static final Charset ASCII = Charset.forName("US-ASCII");
    }

}
