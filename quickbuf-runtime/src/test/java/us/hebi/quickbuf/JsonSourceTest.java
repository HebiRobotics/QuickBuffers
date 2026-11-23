/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2020 HEBI Robotics
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

import org.junit.Assert;
import org.junit.Test;
import protos.test.quickbuf.ForeignMessage;
import protos.test.quickbuf.LazyTypes.LazyMessage;
import protos.test.quickbuf.TestAllTypes;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 13 Jul 2020
 */
public class JsonSourceTest {

    @Test
    public void testReadJsonSink() throws IOException {
        TestAllTypes expected = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());
        TestAllTypes actual = TestAllTypes.newInstance();

        String json = JsonSink.newInstance().setWriteEnumsAsInts(false).writeMessage(expected).toString();
        actual.clear().mergeFrom(newJsonSource(json));
        Assert.assertEquals(expected, actual);

        json = JsonSink.newInstance().setWriteEnumsAsInts(true).writeMessage(expected).toString();
        actual.clear().mergeFrom(newJsonSource(json));
        Assert.assertEquals(expected, actual);

        json = JsonSink.newInstance().setPreserveProtoFieldNames(true).writeMessage(expected).toString();
        actual.clear().mergeFrom(newJsonSource(json));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSkipping() throws Exception {
        TestAllTypes expected = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());
        String json = JsonSink.newInstance().setWriteEnumsAsInts(false).writeMessage(expected).toString();
        ForeignMessage wrongMsg = ForeignMessage.newInstance();

        // Ignore unknown fields
        wrongMsg.clear().mergeFrom(newJsonSource(json).setIgnoreUnknownFields(true));

        // Expect a failure
        try {
            wrongMsg.mergeFrom(newJsonSource(json).setIgnoreUnknownFields(false));
            fail("expected to fail on unknown fields");
        } catch (InvalidJsonException e) {
            assertEquals("Encountered unknown field: 'optionalDouble'", e.getDescription());
        }

    }

    @Test(timeout = 5000)
    public void testSkippingTruncatedInput() throws Exception {
        for (String json : new String[]{"{\"unknown\":[1", "{\"unknown\":{\"a\":1"}) {
            try {
                ForeignMessage.newInstance().mergeFrom(newJsonSource(json).setIgnoreUnknownFields(true));
                fail("expected to fail on truncated input: " + json);
            } catch (IOException expected) {
            }
        }
    }

    @Test
    public void testInvalidStructure() {
        assertStructureError("{\"optionalInt32\":1,}", "Trailing comma before '}' for field 'optionalInt32' at offset 19");
        assertStructureError("{\"repeatedInt32\":[1,]}", "Trailing comma before ']' for field 'repeatedInt32[1]' at offset 20");
        assertStructureError("{\"optionalInt32\":1 \"optionalInt64\":2}", "Expected ',' or '}' but got '\"' for field 'optionalInt32' at offset 19");
        assertStructureError("{\"optionalInt32\":}", "Expected a value but got '}' for field 'optionalInt32' at offset 17");
        assertStructureError("{\"optionalInt32\" 1}", "Expected ':' but got '1' for field 'optionalInt32' at offset 17");
        assertStructureError("{\"repeatedInt32\":[1}", "Expected ',' or ']' but got '}' for field 'repeatedInt32[0]' at offset 19");
    }

    private static void assertStructureError(String json, String error) {
        try {
            TestAllTypes.parseFrom(JsonSource.newInstance(json));
            fail("expected error: " + error);
        } catch (IOException e) {
            assertEquals(error, e.getMessage());
        }
    }

    @Test
    public void testSkippingInvalidUnknownFields() throws Exception {
        String json = "{\"unknown\":{\"a\":[1,],\"b\":,\"c\" 2},\"unknown2\":[{],\"optionalInt32\":1}";
        TestAllTypes msg = TestAllTypes.parseFrom(JsonSource.newInstance(json).setIgnoreUnknownFields(true));
        assertEquals(1, msg.getOptionalInt32());
    }

    @Test
    public void testReuse() throws IOException {
        JsonSource source = JsonSource.newInstance().setIgnoreUnknownFields(true);
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.clear().mergeFrom(source.setInput("{\"optionalInt32\":1,\"unknown\":{}}"));
        assertEquals(1, msg.getOptionalInt32());

        msg.clear().mergeFrom(source.setInput(new ByteArrayInputStream("{\"optionalInt32\":2}".getBytes(ProtoUtil.Charsets.UTF_8))));
        assertEquals(2, msg.getOptionalInt32());

        try {
            msg.clear().mergeFrom(source.setInput("{\"optionalInt32\":"));
            fail("expected truncated input to fail");
        } catch (IOException expected) {
        }
        msg.clear().mergeFrom(source.setInput("{\"optionalInt32\":3}"));
        assertEquals(3, msg.getOptionalInt32());

        source.setRecursionLimit(1);
        try {
            msg.clear().mergeFrom(source.setInput("{\"optionalNestedMessage\":{\"bb\":"));
            fail("expected truncated input to fail");
        } catch (IOException expected) {
        }
        msg.clear().mergeFrom(source.setInput("{\"optionalNestedMessage\":{\"bb\":4}}"));
        assertEquals(4, msg.getOptionalNestedMessage().getBb());
    }

    @Test
    public void testInvalidUtf8InNames() {
        byte[] json = {'{', '"', (byte) 0x80, '"', ':', '1', '}'};
        try {
            TestAllTypes.parseFrom(JsonSource.newInstance(json).setIgnoreUnknownFields(true));
            fail("expected invalid utf-8 to fail");
        } catch (IOException e) {
            assertEquals("Invalid UTF-8 in string at offset 2", e.getMessage());
        }
    }

    @Test
    public void testUnpaddedBase64() throws Exception {
        assertArrayEquals(new byte[]{1}, parseJson("{\"optionalBytes\":\"AQ\"}").getOptionalBytes().toArray());
        assertArrayEquals(new byte[]{1, 2}, parseJson("{\"optionalBytes\":\"AQI\"}").getOptionalBytes().toArray());
        assertArrayEquals(new byte[]{1, 2, 3}, parseJson("{\"optionalBytes\":\"AQID\"}").getOptionalBytes().toArray());
        assertArrayEquals(new byte[]{1, 2}, parseJson("{\"optionalBytes\":\"AQI=\"}").getOptionalBytes().toArray());

        // escapes get decoded first, so escaped line breaks get ignored like other illegal chars
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6}, parseJson("{\"optionalBytes\":\"AQID\\r\\nBAUG\"}").getOptionalBytes().toArray());
        assertArrayEquals(new byte[]{1, 2}, parseJson("{\"optionalBytes\":\"\\u0041QI\"}").getOptionalBytes().toArray());

        for (String json : new String[]{"{\"optionalBytes\":\"A\"}", "{\"optionalBytes\":\"AQIDB\"}"}) {
            try {
                parseJson(json);
                fail("expected to fail on invalid base64 length: " + json);
            } catch (IOException expected) {
            }
        }
    }

    @Test
    public void testEscapedSolidus() throws Exception {
        assertEquals("a/b", parseJson("{\"optionalString\":\"a\\/b\"}").getOptionalString());

        // PHP escapes forward slashes in base64 values
        assertArrayEquals(new byte[]{(byte) 0xAB, (byte) 0xFF}, parseJson("{\"optionalBytes\":\"q\\/8=\"}").getOptionalBytes().toArray());
        assertArrayEquals(new byte[]{(byte) 0xAB, (byte) 0xFF}, parseJson("{\"optionalBytes\":\"q\\/8\"}").getOptionalBytes().toArray());
    }

    @Test
    public void testInvalidUnquotedValues() throws Exception {
        for (String json : new String[]{"{\"optionalInt32\":1" + (char) 0xE9 + "}", "{\"optionalInt32\":12a}", "{\"optionalInt32\":"}) {
            try {
                parseJson(json);
                fail("expected to fail on invalid input: " + json);
            } catch (IOException expected) {
            }
        }
    }

    @Test
    public void testFloatingPointValues() throws Exception {
        TestAllTypes msg = parseJson("{\"repeatedDouble\":[\"NaN\",\"Infinity\",\"-Infinity\",NaN,Infinity,-Infinity,\"1e2\",007,\"-00.5\"]," +
                "\"repeatedFloat\":[\"NaN\",\"Infinity\",\"-Infinity\",\"1.5\",-007]}");
        Assert.assertArrayEquals(new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 100, 7, -0.5}, msg.getRepeatedDouble().toArray(), 0);
        Assert.assertArrayEquals(new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                1.5f, -7}, msg.getRepeatedFloat().toArray(), 0);
    }

    @Test
    public void testInvalidFloatingPointValues() throws Exception {
        for (String value : new String[]{"\"Inf\"", "\"inf\"", "\"nan\"", "\"-NaN\"", "\"+Infinity\"", "\"--1\"",
                "\"\"", "\"-\"", "\" 1\"", "\"1d\"", "\"0x1p3\"", "\"1.\"", "\".5\"", "+1"}) {
            for (String field : new String[]{"optionalDouble", "optionalFloat"}) {
                String json = "{\"" + field + "\":" + value + "}";
                try {
                    parseJson(json);
                    fail("expected to fail on invalid value: " + json);
                } catch (InvalidJsonException expected) {
                }
            }
        }
    }

    @Test
    public void testIntegralValues() throws Exception {
        TestAllTypes msg = parseJson("{\"repeatedInt32\":[1.0,\"1e2\",-2.5e1,\"0.0\",007]," +
                "\"repeatedInt64\":[\"9.223372036854775807e18\",\"-9223372036854775808\",-9.223372036854775808e18,12345678900e-2]}");
        Assert.assertArrayEquals(new int[]{1, 100, -25, 0, 7}, msg.getRepeatedInt32().toArray());
        Assert.assertArrayEquals(new long[]{Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, 123456789},
                msg.getRepeatedInt64().toArray());
    }

    @Test
    public void testInvalidIntegerValues() throws Exception {
        for (String json : new String[]{
                "{\"optionalInt32\":\"\"}",
                "{\"optionalInt64\":\"\"}",
                "{\"optionalInt32\":1.5}",
                "{\"optionalInt64\":\"1e-1\"}",
                "{\"optionalInt64\":1e19}",
                "{\"optionalInt64\":9223372036854775808}",
                "{\"optionalInt32\":\"+1\"}",
                "{\"optionalInt64\":+1}",
                "{\"optionalUint64\":\"+1\"}",
                "{\"optionalInt32\":2147483648.0}"}) {
            try {
                parseJson(json);
                fail("expected to fail on invalid value: " + json);
            } catch (InvalidJsonException expected) {
            }
        }
    }

    @Test
    public void testIntegersOutOfRange() throws Exception {
        for (String json : new String[]{
                "{\"optionalInt32\":2147483648}",
                "{\"optionalInt32\":4294967297}",
                "{\"optionalUint32\":4294967296}",
                "{\"optionalUint32\":-2147483649}",
                "{\"optionalUint64\":18446744073709551616}",
                "{\"optionalUint64\":-9223372036854775809}",
                "{\"repeatedUint64\":[99999999999999999999]}"}) {
            try {
                parseJson(json);
                fail("expected to fail on out of range value: " + json);
            } catch (IOException expected) {
            }
        }
    }

    @Test
    public void testUnsignedInt32() throws Exception {
        TestAllTypes msg = parseJson("{\"optionalUint32\":4294967295,\"optionalFixed32\":2147483648," +
                "\"repeatedUint32\":[4294967295,-1],\"repeatedFixed32\":[2147483648]}");
        assertEquals(-1, msg.getOptionalUint32());
        assertEquals(Integer.MIN_VALUE, msg.getOptionalFixed32());
        assertEquals(-1, msg.getRepeatedUint32().get(0));
        assertEquals(-1, msg.getRepeatedUint32().get(1));
        assertEquals(Integer.MIN_VALUE, msg.getRepeatedFixed32().get(0));
    }

    @Test
    public void testUnsignedInt64() throws Exception {
        TestAllTypes msg = parseJson("{\"optionalUint64\":18446744073709551615,\"optionalFixed64\":\"9223372036854775808\"," +
                "\"repeatedUint64\":[9223372036854775807,10000000000000000000],\"repeatedFixed64\":[1,\"9223372036854775809\"]}");
        assertEquals(-1L, msg.getOptionalUint64());
        assertEquals(Long.MIN_VALUE, msg.getOptionalFixed64());
        assertArrayEquals(new long[]{Long.MAX_VALUE, -8446744073709551616L}, msg.getRepeatedUint64().toArray());
        assertArrayEquals(new long[]{1, Long.MIN_VALUE + 1}, msg.getRepeatedFixed64().toArray());

        // signed values written by older versions
        msg = parseJson("{\"optionalUint64\":-1,\"optionalFixed64\":-9223372036854775808}");
        assertEquals(-1L, msg.getOptionalUint64());
        assertEquals(Long.MIN_VALUE, msg.getOptionalFixed64());
    }

    @Test
    public void testUnsignedIntegralValues() throws Exception {
        assertEquals(-8446744073709551616L, parseJson("{\"optionalUint64\":1e19}").getOptionalUint64());
        assertEquals(-446744073709551616L, parseJson("{\"optionalUint64\":\"1.8e19\"}").getOptionalUint64());
    }

    @Test
    public void testUnsignedRoundTrip() throws Exception {
        TestAllTypes msg = TestAllTypes.newInstance()
                .setOptionalUint64(-1L) // 2^64-1
                .setOptionalFixed64(Long.MIN_VALUE) // 2^63
                .setOptionalUint32(-1) // 2^32-1
                .setOptionalFixed32(Integer.MIN_VALUE); // 2^31
        msg.getMutableRepeatedUint64().addAll(new long[]{0, -1L, Long.MIN_VALUE, Long.MAX_VALUE});
        msg.getMutableRepeatedFixed32().addAll(new int[]{0, -1, Integer.MIN_VALUE});
        testRoundTrip(msg);
    }

    @Test
    public void testRecursionLimit() throws Exception {
        LazyMessage msg = LazyMessage.newInstance();
        LazyMessage nested = msg;
        for (int i = 0; i < 32; i++) { // 64 levels of nested messages
            nested = nested.getMutableOptionalNestedMessage().getMutableRecursiveMessage();
        }
        String json = JsonSink.newInstance().writeMessage(msg).toString();
        assertEquals(msg, LazyMessage.newInstance().mergeFrom(newJsonSource(json)));

        String deeper = "{\"optionalNestedMessage\":{\"recursiveMessage\":" + json + "}}";
        try {
            LazyMessage.newInstance().mergeFrom(newJsonSource(deeper));
            fail("expected to fail above the recursion limit");
        } catch (InvalidJsonException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("setRecursionLimit"));
        }

        JsonSource source = newJsonSource(deeper);
        assertEquals(64, source.setRecursionLimit(66));
        assertEquals(66, source.setRecursionLimit(66));
        LazyMessage.newInstance().mergeFrom(source);
    }

    @Test
    public void testManualInput() throws Exception {
        TestAllTypes msg = parseJson(CompatibilityTest.JSON_MANUAL_INPUT);
        assertEquals(CompatibilityTest.JSON_MANUAL_INPUT, msg.toString());
    }

    @Test
    public void testEmptyInput() throws Exception {
        TestAllTypes msg = parseJson(CompatibilityTest.JSON_EMPTY);
        assertEquals(CompatibilityTest.JSON_EMPTY, msg.toString());
        assertTrue(msg.isEmpty());
    }

    @Test
    public void testObjectsNullInput() throws Exception {
        TestAllTypes msg = parseJson(CompatibilityTest.JSON_OBJECT_TYPES_NULL);
        assertTrue(msg.hasOptionalForeignMessage());
        assertTrue(msg.getOptionalNestedMessage().isEmpty());
        assertFalse(msg.hasRepeatedString());
        Assert.assertEquals(0, msg.getRepeatedString().length());
        Assert.assertEquals(0, msg.getRepeatedBytes().length());
    }

    @Test
    public void testAllTypesNullInput() throws Exception {
        TestAllTypes msg = parseJson(CompatibilityTest.JSON_ALL_TYPES_NULL);
        assertTrue(msg.isEmpty());
    }

    @Test
    public void testSpecialNumbers() throws Exception {
        TestAllTypes msg = parseJson(CompatibilityTest.JSON_SPECIAL_NUMBERS);

        Assert.assertArrayEquals(new double[]{
                Double.NaN,
                Double.NEGATIVE_INFINITY,
                0,
                -28.3,
                3E6,
                -0,
                17E-3,
                Double.POSITIVE_INFINITY}, msg.getRepeatedDouble().toArray(), 0);

        Assert.assertArrayEquals(new int[]{
                0,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE,
                0,
                1,
                2}, msg.getRepeatedInt32().toArray());

    }

    @Test
    public void testBadInputs() {
        testError(CompatibilityTest.JSON_NULL, "Expected '{' but got 'n' at offset 0");
        testError(CompatibilityTest.JSON_LIST_EMPTY, "Expected '{' but got '[' at offset 0");
        testError(CompatibilityTest.JSON_REPEATED_BYTES_NULL_VALUE, "Expected non-null value for field 'repeatedBytes[0]' at offset 19");
        testError(CompatibilityTest.JSON_REPEATED_MSG_NULL_VALUE, "Expected '{' but got 'n' for field 'repeatedForeignMessage[0]' at offset 28");
        testError(CompatibilityTest.JSON_BAD_BOOLEAN, "Expected 'false' but got 'fals}' for field 'optionalBool' at offset 17");
        testError(CompatibilityTest.JSON_UNKNOWN_FIELD, "Encountered unknown field: 'unknownField' at offset 17");
        testError(CompatibilityTest.JSON_UNKNOWN_FIELD_NULL, "Encountered unknown field: 'unknownField' at offset 17");
        testError("{\"optionalNestedMessage\":{\"bb\":1,\"unknownField\":2}}", "Encountered unknown field: 'optionalNestedMessage.unknownField' at offset 48");
        testError("{\"optionalNestedMessage\":{\"bb\":1},\"optionalNestedEnum\":\"BOGUS\"}", "Encountered unknown enum value on field: 'optionalNestedEnum' at offset 62");
        testError("{\"optionalInt64\":\"12a\"}", "Invalid number '12a' for field 'optionalInt64' at offset 22");
        testError("{\"optionalUint64\":\"18446744073709551616\"}", "Invalid number '18446744073709551616' for field 'optionalUint64' at offset 40");
    }

    @Test
    public void testParseRootList() throws Exception {
        RepeatedMessage<TestAllTypes> list = newJsonSource(CompatibilityTest.JSON_ROOT_LIST)
                .parseRepeatedMessage(TestAllTypes.getFactory());
        assertEquals(3, list.length());
    }

    @Test
    public void testAllMessages() throws Exception {
        for (byte[] bytes : CompatibilityTest.getAllMessages()) {
            testRoundTrip(TestAllTypes.parseFrom(bytes));
        }
    }

    @Test
    public void testCombinedMessage() throws Exception {
        testRoundTrip(TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage()));
    }

    @Test
    public void testEnums() throws Exception {
        testRoundTrip(TestAllTypes.parseFrom(CompatibilityTest.optionalEnums()));
        testRoundTrip(TestAllTypes.parseFrom(CompatibilityTest.repeatedEnums()));
    }

    private void testRoundTrip(ProtoMessage<?> msg) throws Exception {
        testRoundTrip(msg, minimized);
        testRoundTrip(msg, pretty);
    }

    private void testRoundTrip(ProtoMessage<?> msg, JsonSink sink) throws Exception {
        msg.writeTo(sink.clear());
        ProtoMessage<?> msg2 = msg
                .clone()
                .clear()
                .mergeFrom(newJsonSource(sink));
        Assert.assertEquals(msg, msg2);
    }

    protected TestAllTypes parseJson(String json) throws IOException {
        return TestAllTypes.parseFrom(newJsonSource(json));
    }

    protected JsonSource newJsonSource(String json) throws IOException {
        return JsonSource.newInstance(json);
    }

    protected JsonSource newJsonSource(JsonSink sink) throws IOException {
        return JsonSource.newInstance(sink.getBytes());
    }

    protected void testError(String input, String error) {
        try {
            parseJson(input);
            fail("expected error: " + error);
        } catch (IOException ioe) {
            if (error != null) {
                assertEquals(firstLine(error), firstLine(ioe.getMessage()));
            }
        }
    }

    /** Third party libraries append varying details such as source locations after the first line */
    protected static String firstLine(String message) {
        int index = message.indexOf('\n');
        return index < 0 ? message : message.substring(0, index);
    }

    private final JsonSink minimized = JsonSink.newInstance();
    private final JsonSink pretty = JsonSink.newPrettyInstance();

}
