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

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 29 Sep 2026
 */
public class JsonWriterTest {

    @Test
    public void testNesting() {
        JsonWriter writer = JsonWriter.newInstance()
                .beginObject()
                .name("a").value(1)
                .name("b").beginArray()
                .beginObject().name("c").beginArray().endArray().endObject()
                .beginArray().value(true).beginObject().endObject().endArray()
                .value(false)
                .endArray()
                .name("d").beginObject()
                .name("e").beginObject().name("f").nullValue().endObject()
                .name("g").value("h")
                .endObject()
                .endObject();
        assertEquals("{\"a\":1,\"b\":[{\"c\":[]},[true,{}],false],\"d\":{\"e\":{\"f\":null},\"g\":\"h\"}}", writer.toString());
    }

    @Test
    public void testTopLevelValues() {
        assertEquals("[1,2,3]", JsonWriter.newInstance().beginArray().value(1).value(2L).value(3f).endArray().toString());
        assertEquals("[]", JsonWriter.newInstance().beginArray().endArray().toString());
        assertEquals("{}", JsonWriter.newInstance().beginObject().endObject().toString());
        assertEquals("[[],[[]]]", JsonWriter.newInstance().beginArray().beginArray().endArray()
                .beginArray().beginArray().endArray().endArray().endArray().toString());
        assertEquals("1", JsonWriter.newInstance().value(1).toString());
        assertEquals("\"s\"", JsonWriter.newInstance().value("s").toString());
        assertEquals("true", JsonWriter.newInstance().value(true).toString());
        assertEquals("null", JsonWriter.newInstance().nullValue().toString());
        assertEquals("{}{}", JsonWriter.newInstance().beginObject().endObject().beginObject().endObject().toString());
    }

    @Test
    public void testPrettyPrinting() {
        JsonWriter writer = JsonWriter.newPrettyInstance()
                .beginObject()
                .name("a").value(1)
                .name("b").beginArray()
                .beginObject().name("c").beginArray().value(1).value(2).endArray().endObject()
                .beginObject().endObject()
                .endArray()
                .name("d").beginObject().name("e").nullValue().endObject()
                .name("f").beginObject().endObject()
                .endObject();
        assertEquals("{\n" +
                "  \"a\": 1,\n" +
                "  \"b\": [{\n" +
                "    \"c\": [1, 2]\n" +
                "  }, {\n" +
                "  }],\n" +
                "  \"d\": {\n" +
                "    \"e\": null\n" +
                "  },\n" +
                "  \"f\": {\n" +
                "  }\n" +
                "}", writer.toString());

        assertEquals("[1, [2, 3], []]", JsonWriter.newPrettyInstance().beginArray().value(1)
                .beginArray().value(2).value(3).endArray().beginArray().endArray().endArray().toString());
        assertEquals("{\n}", JsonWriter.newPrettyInstance().beginObject().endObject().toString());
    }

    @Test
    public void testEscaping() {
        String value = "q\"b\\s/\b\f\n\r\t\u0000\u001f\u007f\u00e9\u4e2d\ud83d\ude00";
        String expected = "\"q\\\"b\\\\s/\\b\\f\\n\\r\\t\\u0000\\u001f\\u007f\u00e9\u4e2d\ud83d\ude00\"";
        assertEquals("{" + expected + ":" + expected + "}", JsonWriter.newInstance()
                .beginObject().name(value).value(value).endObject().toString());
        assertEquals(expected, JsonWriter.newInstance().value(Utf8String.newInstance(value)).toString());
        assertEquals(expected, JsonWriter.newInstance().value(Utf8String.newEmptyInstance().copyFromEncoded(value)).toString());
    }

    @Test
    public void testNumbers() {
        assertEquals("[\"NaN\",\"Infinity\",\"-Infinity\",0,0.1,1E21,1.5,-2147483648,-9223372036854775808]", JsonWriter.newInstance()
                .beginArray()
                .value(Double.NaN)
                .value(Double.POSITIVE_INFINITY)
                .value(Float.NEGATIVE_INFINITY)
                .value(-0.0)
                .value(0.1f)
                .value(1E21)
                .value(1.5)
                .value(Integer.MIN_VALUE)
                .value(Long.MIN_VALUE)
                .endArray()
                .toString());
        assertEquals("[18446744073709551615]", JsonWriter.newInstance().beginArray().unsignedValue(-1).endArray().toString());
        assertEquals("[\"AQID\"]", JsonWriter.newInstance().beginArray()
                .base64Value(RepeatedByte.newInstance(new byte[]{1, 2, 3})).endArray().toString());
    }

    @Test
    public void testFieldNames() {
        FieldName name = FieldName.forField("jsonName", "proto_name");
        assertEquals("{\"jsonName\":1,\"proto_name\":2}", JsonWriter.newInstance()
                .beginObject()
                .name(name).value(1)
                .name(name.getProtoKeyBytes()).value(2)
                .endObject()
                .toString());
    }

    @Test
    public void testReuse() throws IOException {
        RepeatedByte output = RepeatedByte.newEmptyInstance();
        JsonWriter writer = JsonWriter.newInstance(output);
        assertSame(output, writer.getBytes());
        writer.beginArray().beginObject().name("a");
        writer.clear();
        writer.beginArray().value(1).endArray();
        assertEquals("[1]", writer.getChars().toString());

        RepeatedByte other = RepeatedByte.newEmptyInstance();
        writer.setOutput(other).beginObject().endObject();
        assertEquals("[1]", new String(output.toArray(), "UTF-8"));
        assertEquals("{}", new String(other.toArray(), "UTF-8"));

        writer.clear().reserve(1024);
        assertTrue(writer.getBytes().capacity() >= 1024);
        assertEquals("", writer.toString());
    }

    @Test
    public void testRoundTripThroughReader() throws IOException {
        JsonWriter writer = JsonWriter.newPrettyInstance()
                .beginArray()
                .beginObject().name("a").value("x\ny").name("b").beginArray().value(1.5).nullValue().endArray().endObject()
                .value(-3)
                .endArray();
        JsonReader reader = JsonReader.newInstance(writer.getBytes());
        assertEquals(JsonReaderTest.tokens(JsonReader.newInstance("[{\"a\":\"x\\ny\",\"b\":[1.5,null]},-3]")), JsonReaderTest.tokens(reader));
    }

    @Test
    public void testInvalidStates() {
        assertIllegalState("Names are only allowed inside objects", () -> JsonWriter.newInstance().name("a"));
        assertIllegalState("Names are only allowed inside objects", () -> JsonWriter.newInstance().beginArray().name("a"));
        assertIllegalState("Not inside an object", () -> JsonWriter.newInstance().beginArray().endObject());
        assertIllegalState("Not inside an object", () -> JsonWriter.newInstance().endObject());
        assertIllegalState("Not inside an array", () -> JsonWriter.newInstance().beginObject().endArray());
        assertIllegalState("Not inside an array", () -> JsonWriter.newInstance().endArray());
    }

    private static void assertIllegalState(String message, Runnable action) {
        try {
            action.run();
            fail("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals(message, e.getMessage());
        }
    }

}
