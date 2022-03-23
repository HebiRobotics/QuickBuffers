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

import org.junit.Before;
import org.junit.Test;
import us.hebi.quickbuf.JsonDecoding.JsonLexer;
import us.hebi.quickbuf.JsonDecoding.StringDecoding;
import us.hebi.quickbuf.JsonEncoding.Base64Encoding;
import us.hebi.quickbuf.JsonEncoding.BooleanEncoding;
import us.hebi.quickbuf.JsonEncoding.NumberEncoding;
import us.hebi.quickbuf.JsonEncoding.StringEncoding;
import us.hebi.quickbuf.JsonSource.ArraySource;
import us.hebi.quickbuf.ProtoUtil.Charsets;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 13 Nov 2019
 */
public class JsonEncodingTest {

    final Random rnd = new Random(0);
    final RepeatedByte bytes = RepeatedByte.newEmptyInstance();
    final static int n = 10000;

    @Before
    public void setUp() throws Exception {
        rnd.setSeed(0);
    }

    @Test
    public void testBase64Encoding() throws Exception {
        for (int i = 0; i < 100; i++) {
            byte[] expected = new byte[i];
            rnd.nextBytes(expected);
            Base64Encoding.writeQuotedBase64(expected, expected.length, bytes.setLength(0));
            String result = new String(bytes.array, 1, bytes.length - 2, Charsets.UTF_8); // without quotes
            byte[] actual = java.util.Base64.getDecoder().decode(result); // IntelliJ complains, but works in Maven (test sources 8)
            assertArrayEquals(result, expected, actual);
        }
    }

    @Test
    public void testStringEncoding() throws Exception {
        // base ascii
        testString("ascii");

        // escaped ascii chars
        testString("\"\\\b\f\n\r\t", "\\\"\\\\\\b\\f\\n\\r\\t");

        // non-unicode escape chars
        testString("\0\u0001\u007F", "\\u0000\\u0001\\u007f");

        // Some UTF-8 corner cases
        // https://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-test.txt
        testString("\u0080\uD800\uDC00\uDBD0\uDC00");
        testString("\u07FF\uFFFF\uDBBF\uDFFF");
        testString("\uDBFF\uDFFF");

        // Randomly copied string
        // https://stackoverflow.com/questions/1319022/really-good-bad-utf-8-example-test-data
        testString("ăѣ\uD835\uDD20ծềſģȟᎥ\uD835\uDC8Bǩľḿꞑȯ\uD835\uDE31\uD835\uDC5E\uD835\uDDCB\uD835\uDE34ȶ\uD835\uDF84\uD835\uDF08ψ\uD835\uDC99\uD835\uDE06\uD835\uDEA31234567890!@#$%^&*()-_=+[{]};:',<.>/?\uD835\uDE08Ḇ\uD835\uDDA2\uD835\uDD6F٤ḞԍНǏ\uD835\uDE45ƘԸⲘ\uD835\uDE49০Ρ\uD835\uDDE4Ɍ\uD835\uDCE2ȚЦ\uD835\uDCB1Ѡ\uD835\uDCE7ƳȤѧᖯć\uD835\uDDF1ễ\uD835\uDC53\uD835\uDE5CႹ\uD835\uDFB2\uD835\uDC57\uD835\uDC8Cļṃŉо\uD835\uDF8E\uD835\uDC92ᵲꜱ\uD835\uDE69ừ\uD835\uDDCFŵ\uD835\uDC99\uD835\uDC9Aź1234567890!@#$%^&*()-_=+[{]};:',<.>/?АḂⲤ\uD835\uDDD7\uD835\uDDA4\uD835\uDDD9ꞠꓧȊ\uD835\uDC09\uD835\uDF25ꓡ\uD835\uDC40\uD835\uDC75Ǭ\uD835\uDE7F\uD835\uDC44Ŗ\uD835\uDC46\uD835\uDCAF\uD835\uDDB4\uD835\uDE1D\uD835\uDE1EꓫŸ\uD835\uDF21ả\uD835\uDE22ƀ\uD835\uDDBCḋếᵮℊ\uD835\uDE5DᎥ\uD835\uDD5Bкιṃդⱺ\uD835\uDCC5\uD835\uDE32\uD835\uDD63\uD835\uDD98ŧ\uD835\uDC62ṽẉ\uD835\uDE05ყž1234567890!@#$%^&*()-_=+[{]};:',<.>/?Ѧ\uD835\uDE71ƇᗞΣℱԍҤ١\uD835\uDD0DК\uD835\uDCDB\uD835\uDCDCƝȎ\uD835\uDEB8\uD835\uDC44Ṛ\uD835\uDCE2ṮṺƲᏔꓫ\uD835\uDE88\uD835\uDEAD\uD835\uDF36Ꮟçძ\uD835\uDC52\uD835\uDDBF\uD835\uDDC0ḧ\uD835\uDDC2\uD835\uDC23ҝɭḿ\uD835\uDD5F\uD835\uDC28\uD835\uDF54\uD835\uDD62ṛ\uD835\uDCFCтú\uD835\uDD33ẃ⤬\uD835\uDF72\uD835\uDDD31234567890!@#$%^&*()-_=+[{]};:',<.>/?\uD835\uDDA0Β\uD835\uDC9E\uD835\uDE0B\uD835\uDE74\uD835\uDCD5ĢȞỈ\uD835\uDD75ꓗʟ\uD835\uDE7Cℕ০\uD835\uDEB8\uD835\uDDE4ՀꓢṰǓⅤ\uD835\uDD1AⲬ\uD835\uDC4C\uD835\uDE55\uD835\uDE22\uD835\uDD64");
    }

    @Test
    public void testIntEncoding() throws Exception {
        for (int i = 0; i < n; i++) {
            testInt(1 << (i / 1000));
            testInt(i << (i / 1000));
            testInt(rnd.nextInt());
        }
        testInt(Integer.MIN_VALUE);
        testInt(Integer.MAX_VALUE);
        testInt(0);
    }

    @Test
    public void testLongEncoding() throws Exception {
        long testVal = 1;
        for (int i = 0; i < n; i++) {
            testLong(1 << (i / 100));
            testLong(i << (i / 100));
            testLong(rnd.nextLong());
        }
        testLong(Long.MIN_VALUE);
        testLong(Long.MAX_VALUE);
        testLong(0);
    }

    @Test
    public void testDoubleEncoding() throws Exception {
        double multiplier = 1E-100;
        for (int i = 0; i < n; i++) {
            testDouble(rnd.nextDouble());
            testDouble(rnd.nextDouble() * rnd.nextLong());
            testDouble(Math.PI * multiplier + 0.1);
            testDouble(Math.PI * -multiplier + 0.1);
            multiplier *= 10;
        }
        testDouble(100 - 1E-01);
        testDouble(100 - 1E-02);
        testDouble(100 - 1E-03);
        testDouble(100 - 1E-04);
        testDouble(100 - 1E-05);
        testDouble(100 - 1E-06);
        testDouble(100 - 1E-07);
        testDouble(100 - 1E-08);
        testDouble(100 - 1E-09);
        testDouble(100 - 1E-10);
        testDouble(100 - 1E-11);
        testDouble(100 - 1E-12);
        testDouble(100 - 1E-13);
        testDouble(100 - 1E-14);
        testDouble(100 - 1E-15);
        testDouble(Double.MIN_VALUE);
        testDouble(Double.MAX_VALUE);
        testDouble(0);
        assertEquals("\"Infinity\"", encodeDouble(Double.POSITIVE_INFINITY));
        assertEquals("\"-Infinity\"", encodeDouble(Double.NEGATIVE_INFINITY));
        assertEquals("\"NaN\"", encodeDouble(Double.NaN));
    }

    @Test
    public void testFloatEncoding() throws Exception {
        double multiplier = 1E-100;
        for (int i = 0; i < n; i++) {
            testFloat(rnd.nextFloat());
            testFloat(rnd.nextFloat() * rnd.nextInt());
            testFloat((float) (Math.PI * multiplier + 0.1));
            testFloat((float) (Math.PI * -multiplier + 0.1));
            multiplier *= 10;
        }
        testFloat(100 - 1E-01f);
        testFloat(100 - 1E-02f);
        testFloat(100 - 1E-03f);
        testFloat(100 - 1E-04f);
        testFloat(100 - 1E-05f);
        testFloat(100 - 1E-06f);
        testFloat(100 - 1E-07f);
        testFloat(100 - 1E-08f);
        testFloat(100 - 1E-09f);
        testFloat(100 - 1E-10f);
        testFloat(100 - 1E-11f);
        testFloat(100 - 1E-12f);
        testFloat(Float.MIN_VALUE);
        testFloat(Float.MAX_VALUE);
        testFloat(0);
        assertEquals("\"Infinity\"", encodeFloat(Float.POSITIVE_INFINITY));
        assertEquals("\"-Infinity\"", encodeFloat(Float.NEGATIVE_INFINITY));
        assertEquals("\"NaN\"", encodeFloat(Float.NaN));
    }

    @Test
    public void testBooleanEncoding() throws Exception {
        assertEquals("true", encodeBoolean(true));
        assertEquals("false", encodeBoolean(false));
    }

    private String encodeBoolean(boolean expected) {
        BooleanEncoding.writeBoolean(expected, bytes);
        return getString();
    }

    private String encodeInt(int expected) {
        NumberEncoding.writeInt(expected, bytes);
        return getString();
    }

    private String encodeLong(long expected) {
        NumberEncoding.writeLong(expected, bytes);
        return getString();
    }

    private String encodeDouble(double expected) {
        NumberEncoding.writeDouble(expected, bytes);
        return getString();
    }

    private String encodeFloat(float expected) {
        NumberEncoding.writeFloat(expected, bytes);
        return getString();
    }

    private void testInt(int expected) {
        int actual = Integer.parseInt(encodeInt(expected));
        assertEquals(expected, actual);
    }

    private void testLong(long expected) {
        long actual = Long.parseLong(encodeLong(expected));
        assertEquals(expected, actual);
    }

    private void testDouble(double expected) {
        String encoded = encodeDouble(expected);
        if (expected == Double.POSITIVE_INFINITY) {
            assertEquals("\"Infinity\"", encoded);
        } else if (expected == Double.NEGATIVE_INFINITY) {
            assertEquals("\"-Infinity\"", encoded);
        } else if (Double.isNaN(expected)) {
            assertEquals("\"NaN\"", encoded);
        } else {
            double p = Math.abs(expected);
            if (p > NumberEncoding.max3) {
                assertEquals(encoded, expected, Double.parseDouble(encoded), 1);
            } else if (p > NumberEncoding.max6) {
                assertEquals(encoded, expected, Double.parseDouble(encoded), 1E-3);
            } else if (p > NumberEncoding.max9) {
                assertEquals(encoded, expected, Double.parseDouble(encoded), 1E-6);
            } else if (p > NumberEncoding.max12) {
                assertEquals(encoded, expected, Double.parseDouble(encoded), 1E-9);
            } else {
                assertEquals(encoded, expected, Double.parseDouble(encoded), 1E-12);
            }
        }
    }

    private void testFloat(float expected) {
        String encoded = encodeFloat(expected);
        if (expected == Float.POSITIVE_INFINITY) {
            assertEquals("\"Infinity\"", encoded);
        } else if (expected == Float.NEGATIVE_INFINITY) {
            assertEquals("\"-Infinity\"", encoded);
        } else if (Float.isNaN(expected)) {
            assertEquals("\"NaN\"", encoded);
        } else {
            assertEquals(encoded, expected, Float.parseFloat(encoded), 1E-6);
        }
    }

    private void testString(String expected) throws IOException {
        testString(expected, expected);
    }

    private void testString(String input, String expected) throws IOException {
        StringEncoding.writeQuotedUtf8(input, bytes.setLength(0));
        int length = bytes.length;

        {
            // compare utf8 decoded
            String actual = getString();
            assertEquals(expected, expected, actual.substring(1, actual.length() - 1));
        }

        {
            // compare post-replacement utf8 input
            RepeatedByte utf8 = RepeatedByte.newEmptyInstance();
            JsonLexer lexer = new ArraySource(bytes.array, 1, length - 1);
            StringDecoding.readQuotedUtf8(lexer, utf8);
            assertEquals(input, new String(utf8.array, 0, utf8.length, Charsets.UTF_8));
        }

        {
            // compare fully decoded input
            StringBuilder builder = new StringBuilder(length);
            JsonLexer lexer = new ArraySource(bytes.array, 1, length - 1);
            StringDecoding.readQuotedUtf8(lexer, builder);
            assertEquals(input, builder.toString());
        }

    }

    private String getString() {
        try {
            return new String(bytes.array, 0, bytes.length, Charsets.UTF_8);
        } finally {
            bytes.setLength(0);
        }
    }

}
