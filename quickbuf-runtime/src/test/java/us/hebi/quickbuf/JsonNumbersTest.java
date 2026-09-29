/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2026 HEBI Robotics
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

import org.junit.Test;
import protos.test.quickbuf.TestAllTypes;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Compares the number conversions of the JsonReader and JsonSource against the JDK
 *
 * @author Florian Enner
 * @since 30 Sep 2026
 */
public class JsonNumbersTest {

    @Test
    public void testExactDoubles() throws IOException {
        for (String text : new String[]{"0", "-0", "0.0", "-0.0e5", "0e999999999", "1", "-1.5", "0.000123", "123e30",
                "9007199254740992", "9007199254740993", "1e22", "1e23", "4.35", "0.30000000000000004"}) {
            assertDouble(text);
        }
        Random rnd = new Random(0);
        for (int i = 0; i < 100000; i++) {
            long mantissa = rnd.nextLong() >>> (10 + rnd.nextInt(54));
            assertDouble(mantissa + "e" + (rnd.nextInt(60) - 22));
        }
    }

    @Test
    public void testDoubleBoundaries() throws IOException {
        for (String text : new String[]{
                "2.2250738585072013e-308", "2.2250738585072011e-308", "2.2250738585072012e-308", "2.225073858507201e-308",
                "4.9e-324", "4.9406564584124654e-324", "2.4703282292062328e-324", "2.4703282292062327e-324", "1e-324", "1e-400",
                "1.7976931348623157e308", "1.7976931348623158e308", "1.7976931348623159e308", "1e309", "1e999999999",
                "9007199254740993", "9007199254740995", "9223372036854775807", "9999999999999999999", "18446744073709551615",
                "8.98846567431158e307", "0.30000000000000004", "1.00000000000000011102230246251565404236316680908203125",
                "1.0000000000000001110", "7.2057594037927933e16", "1.448997445238699", "1e-342", "1e-343", "9999999999999999999e-343",
                "5e-324", "7e22", "7e23", "123456789012345678e22", "12345678901234567890123", "0.00000000000000000000000000000001"}) {
            assertDouble(text);
            assertDouble("-" + text);
        }
    }

    @Test
    public void testShortestRoundTripDoubles() throws IOException {
        Random rnd = new Random(0);
        double[] values = new double[1000000];
        JsonWriter writer = JsonWriter.newInstance().beginArray();
        StringBuilder jdk = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            double value = Double.longBitsToDouble(rnd.nextLong());
            if (i % 2 == 0 || Double.isNaN(value) || Double.isInfinite(value)) {
                value = rnd.nextDouble() * SCALES[rnd.nextInt(SCALES.length)];
            }
            values[i] = value;
            writer.value(value);
            jdk.append(i == 0 ? "" : ",").append(value);
        }
        assertDoubles(values, writer.endArray().getBytes().toArray());
        assertDoubles(values, jdk.append("]").toString().getBytes(ProtoUtil.Charsets.UTF_8));
    }

    @Test
    public void testRandomMantissasAndExponents() throws IOException {
        Random rnd = new Random(0);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 200000; i++) {
            text.setLength(0);
            int digits = 1 + rnd.nextInt(19);
            text.append(1 + rnd.nextInt(9));
            for (int d = 1; d < digits; d++) {
                text.append(rnd.nextInt(10));
            }
            if (rnd.nextBoolean()) {
                text.insert(1 + rnd.nextInt(digits), '.');
                if (text.charAt(text.length() - 1) == '.') {
                    text.append('0');
                }
            }
            assertDouble(text.append('e').append(rnd.nextInt(141) - 70).toString());
        }
    }

    @Test
    public void testFloats() throws IOException {
        for (String text : new String[]{"0", "1", "0.1", "16777216", "16777217", "1e10", "1e11", "1.5e-10", "3.4028235e38",
                "3.4028236e38", "3.40282357e38", "1e39", "1.17549435e-38", "1.1754942e-38", "1.4e-45", "7e-46", "7.1e-46", "1e-50",
                "1.000000059604644775", "1.000000059604644776", "1.0000000596046447753", "0.30000000000000004", "123456789012345678901"}) {
            assertFloat(text);
            assertFloat("-" + text);
        }

        Random rnd = new Random(0);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 200000; i++) {
            text.setLength(0);
            if (i % 2 == 0) {
                text.append(Float.intBitsToFloat(rnd.nextInt()));
            } else {
                // just above or below a float midpoint that the double sits on
                float value = Float.intBitsToFloat(rnd.nextInt() & 0x7EFFFFFF);
                double midpoint = ((double) value + (double) Math.nextUp(value)) / 2;
                text.append(new BigDecimal(midpoint).round(new MathContext(19)));
            }
            if (!text.toString().contains("N") && !text.toString().contains("I")) {
                assertFloat(text.toString());
            }
        }
    }

    private static void assertFloat(String text) throws IOException {
        int expected = Float.floatToIntBits(Float.parseFloat(text));
        assertEquals(text, expected, Float.floatToIntBits(JsonReader.newInstance(text).readFloat()));
        assertEquals(text, expected, Float.floatToIntBits(newJsonSource(text).readFloat()));
    }

    @Test
    public void testIntegralFieldValues() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(JsonSource.newInstance("{" +
                "\"optionalInt32\":1.0," +
                "\"optionalInt64\":\"-1.5e3\"," +
                "\"optionalUint32\":4.294967295e9," +
                "\"optionalUint64\":1.8446744073709551e19," +
                "\"optionalSfixed32\":-0," +
                "\"repeatedInt32\":[1e1,\"2.00\",30E-1]}"));
        assertEquals(1, msg.getOptionalInt32());
        assertEquals(-1500, msg.getOptionalInt64());
        assertEquals(-1, msg.getOptionalUint32());
        assertEquals(-616, msg.getOptionalUint64()); // 18446744073709551000 as unsigned
        assertEquals(0, msg.getOptionalSfixed32());
        assertArrayEquals(new int[]{10, 2, 3}, msg.getRepeatedInt32().toArray());

        for (String json : new String[]{
                "{\"optionalInt32\":1.5}",
                "{\"optionalInt32\":\"1e-1\"}",
                "{\"optionalInt32\":1e10}",
                "{\"optionalInt64\":1e19}",
                "{\"optionalInt64\":1e999999999}",
                "{\"optionalUint64\":1e20}"}) {
            try {
                TestAllTypes.parseFrom(JsonSource.newInstance(json));
                fail("expected to fail on non-integral or out of range value: " + json);
            } catch (InvalidJsonException expected) {
            }
        }
    }

    private static void assertDoubles(double[] expected, byte[] json) throws IOException {
        JsonReader reader = JsonReader.newInstance(json).beginArray();
        for (double value : expected) {
            double actual = reader.readDouble();
            if (Double.doubleToLongBits(value) != Double.doubleToLongBits(actual)) {
                fail("expected " + value + " but got " + actual);
            }
        }
        reader.endArray();
    }

    private static void assertDouble(String text) throws IOException {
        long expected = Double.doubleToLongBits(Double.parseDouble(text));
        assertEquals(text, expected, Double.doubleToLongBits(JsonReader.newInstance(text).readDouble()));
        assertEquals(text, expected, Double.doubleToLongBits(newJsonSource(text).readDouble()));
    }

    private static JsonSource newJsonSource(String text) {
        return new JsonSource.DefaultJsonSource(JsonReader.newInstance(text));
    }

    private static final double[] SCALES = {1e-300, 1e-100, 1e-20, 1e-5, 1, 1e5, 1e20, 1e100, 1e300};

}
