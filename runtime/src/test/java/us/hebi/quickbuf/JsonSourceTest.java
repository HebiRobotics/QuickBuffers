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
import protos.test.quickbuf.TestAllTypes;

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
        } catch (IOException e) {
            assertEquals("Encountered unknown field: 'optionalDouble'", e.getMessage());
        }

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
        testError(CompatibilityTest.JSON_NULL, "Expected '{' but got 'n'");
        testError(CompatibilityTest.JSON_LIST_EMPTY, "Expected '{' but got '['");
        testError(CompatibilityTest.JSON_REPEATED_BYTES_NULL_VALUE, "Expected non-null value for field 'repeatedBytes'");
        testError(CompatibilityTest.JSON_REPEATED_MSG_NULL_VALUE, "Expected '{' but got 'n' for field 'repeatedForeignMessage'");
        testError(CompatibilityTest.JSON_BAD_BOOLEAN, "Expected 'false' but got 'fals}' for field 'optionalBool'");
        testError(CompatibilityTest.JSON_UNKNOWN_FIELD, "Encountered unknown field: 'unknownField'");
        testError(CompatibilityTest.JSON_UNKNOWN_FIELD_NULL, "Encountered unknown field: 'unknownField'");
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
                assertEquals(error, ioe.getMessage());
            }
        }
    }

    private final JsonSink minimized = JsonSink.newInstance();
    private final JsonSink pretty = JsonSink.newPrettyInstance();

}
