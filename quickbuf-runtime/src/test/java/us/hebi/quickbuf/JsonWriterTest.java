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
import us.hebi.quickbuf.ProtoUtil.Charsets;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

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
                .writeName("a").writeInt(1)
                .writeName("b").beginArray()
                .beginObject().writeName("c").beginArray().endArray().endObject()
                .beginArray().writeBoolean(true).beginObject().endObject().endArray()
                .writeBoolean(false)
                .endArray()
                .writeName("d").beginObject()
                .writeName("e").beginObject().writeName("f").writeNull().endObject()
                .writeName("g").writeString("h")
                .endObject()
                .endObject();
        assertEquals("{\"a\":1,\"b\":[{\"c\":[]},[true,{}],false],\"d\":{\"e\":{\"f\":null},\"g\":\"h\"}}", writer.toString());
    }

    @Test
    public void testDeepNesting() {
        final int depth = 100;
        JsonWriter writer = JsonWriter.newInstance();
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            writer.beginArray().beginObject().writeName("a");
            expected.append("[{\"a\":");
        }
        writer.writeInt(1);
        expected.append('1');
        for (int i = 0; i < depth; i++) {
            writer.endObject().endArray();
            expected.append("}]");
        }
        assertEquals(expected.toString(), writer.toString());
    }

    @Test
    public void testTopLevelValues() {
        assertEquals("[1,2,3]", JsonWriter.newInstance().beginArray().writeInt(1).writeLong(2L).writeFloat(3f).endArray().toString());
        assertEquals("[]", JsonWriter.newInstance().beginArray().endArray().toString());
        assertEquals("{}", JsonWriter.newInstance().beginObject().endObject().toString());
        assertEquals("[[],[[]]]", JsonWriter.newInstance().beginArray().beginArray().endArray()
                .beginArray().beginArray().endArray().endArray().endArray().toString());
        assertEquals("1", JsonWriter.newInstance().writeInt(1).toString());
        assertEquals("\"s\"", JsonWriter.newInstance().writeString("s").toString());
        assertEquals("true", JsonWriter.newInstance().writeBoolean(true).toString());
        assertEquals("null", JsonWriter.newInstance().writeNull().toString());
        assertEquals("{}\n{}", JsonWriter.newInstance().beginObject().endObject().beginObject().endObject().toString());
        assertEquals("1\n2\n\"a\"\ntrue\n[]", JsonWriter.newInstance().writeInt(1).writeInt(2).writeString("a").writeBoolean(true)
                .beginArray().endArray().toString());
        assertEquals("1", JsonWriter.newInstance().writeInt(0).clear().writeInt(1).toString());
    }

    @Test
    public void testPrettyPrinting() {
        JsonWriter writer = JsonWriter.newPrettyInstance()
                .beginObject()
                .writeName("a").writeInt(1)
                .writeName("b").beginArray()
                .beginObject().writeName("c").beginArray().writeInt(1).writeInt(2).endArray().endObject()
                .beginObject().endObject()
                .endArray()
                .writeName("d").beginObject().writeName("e").writeNull().endObject()
                .writeName("f").beginObject().endObject()
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

        assertEquals("[1, [2, 3], []]", JsonWriter.newPrettyInstance().beginArray().writeInt(1)
                .beginArray().writeInt(2).writeInt(3).endArray().beginArray().endArray().endArray().toString());
        assertEquals("{\n}", JsonWriter.newPrettyInstance().beginObject().endObject().toString());
    }

    @Test
    public void testEscaping() {
        String value = "q\"b\\s/\b\f\n\r\t\u0000\u001f\u007f\u00e9\u4e2d\ud83d\ude00";
        String expected = "\"q\\\"b\\\\s/\\b\\f\\n\\r\\t\\u0000\\u001f\\u007f\u00e9\u4e2d\ud83d\ude00\"";
        assertEquals("{" + expected + ":" + expected + "}", JsonWriter.newInstance()
                .beginObject().writeName(value).writeString(value).endObject().toString());
        assertEquals(expected, JsonWriter.newInstance().writeString(Utf8String.newInstance(value)).toString());
        assertEquals(expected, JsonWriter.newInstance().writeString(Utf8String.newEmptyInstance().copyFromEncoded(value)).toString());
    }

    @Test
    public void testNumbers() {
        assertEquals("[\"NaN\",\"Infinity\",\"-Infinity\",0,0.1,1E21,1.5,-2147483648,-9223372036854775808]", JsonWriter.newInstance()
                .beginArray()
                .writeDouble(Double.NaN)
                .writeDouble(Double.POSITIVE_INFINITY)
                .writeFloat(Float.NEGATIVE_INFINITY)
                .writeDouble(-0.0)
                .writeFloat(0.1f)
                .writeDouble(1E21)
                .writeDouble(1.5)
                .writeInt(Integer.MIN_VALUE)
                .writeLong(Long.MIN_VALUE)
                .endArray()
                .toString());
        assertEquals("[\"AQID\"]", JsonWriter.newInstance().beginArray()
                .writeBase64(RepeatedByte.newInstance(new byte[]{1, 2, 3})).endArray().toString());
    }

    @Test
    public void testNullReferences() {
        assertEquals("[null,null,null,null,null,null,null,null]", JsonWriter.newInstance().beginArray()
                .writeString((CharSequence) null)
                .writeString((Utf8String) null)
                .writeUtf8(null, 0, 0)
                .writeBase64(null)
                .writeBase64(null, 0, 0)
                .writeRaw((CharSequence) null)
                .writeRaw((RepeatedByte) null)
                .writeRaw(null, 0, 0)
                .endArray().toString());
    }

    @Test
    public void testRawValues() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance().setOptionalInt32(1);
        RepeatedByte json = JsonSink.newInstance().writeMessage(msg).getBytes();
        assertEquals("{\"a\":{\"optionalInt32\":1},\"b\":[\"\u00e9\",[1, 2]]}", JsonWriter.newInstance()
                .beginObject()
                .writeName("a").writeRaw(json)
                .writeName("b").beginArray().writeRaw("\"\u00e9\"").writeRaw("[1, 2]").endArray()
                .endObject().toString());
    }

    @Test
    public void testByteSlices() {
        byte[] utf8 = "xq\"b\n\u0001é中😀x".getBytes(Charsets.UTF_8);
        assertEquals("\"q\\\"b\\n\\u0001é中😀\"", JsonWriter.newInstance()
                .writeUtf8(utf8, 1, utf8.length - 2).toString());

        byte[] bytes = {9, 1, 2, 3, 4, 9};
        assertEquals(JsonWriter.newInstance().writeBase64(RepeatedByte.newInstance(new byte[]{1, 2, 3, 4})).toString(),
                JsonWriter.newInstance().writeBase64(bytes, 1, 4).toString());

        byte[] json = "x{\"a\": [1, \"é\"]}x".getBytes(Charsets.UTF_8);
        assertEquals("[{\"a\": [1, \"é\"]}]", JsonWriter.newInstance().beginArray()
                .writeRaw(json, 1, json.length - 2).endArray().toString());
    }

    @Test
    public void testFieldNames() {
        FieldName name = FieldName.forField("jsonName", "proto_name");
        assertEquals("{\"jsonName\":1,\"proto_name\":2}", JsonWriter.newInstance()
                .beginObject()
                .writeName(name).writeInt(1)
                .writeName(name.getProtoKeyBytes()).writeInt(2)
                .endObject()
                .toString());
    }

    @Test
    public void testReuse() throws IOException {
        RepeatedByte output = RepeatedByte.newEmptyInstance();
        JsonWriter writer = JsonWriter.newInstance(output);
        assertSame(output, writer.getBytes());
        writer.beginArray().beginObject().writeName("a");
        writer.clear();
        writer.beginArray().writeInt(1).endArray();
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
                .beginObject().writeName("a").writeString("x\ny").writeName("b").beginArray().writeDouble(1.5).writeNull().endArray().endObject()
                .writeInt(-3)
                .endArray();
        JsonReader reader = JsonReader.newInstance(writer.getBytes());
        assertEquals(JsonReaderTest.tokens(JsonReader.newInstance("[{\"a\":\"x\\ny\",\"b\":[1.5,null]},-3]")), JsonReaderTest.tokens(reader));
    }

    @Test
    public void testWriteValue() {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put("d", 1.5);
        map.put("f", 0.25f);
        map.put("l", Long.MIN_VALUE);
        map.put("i", -3);
        map.put("s", (short) 4);
        map.put("b", (byte) 5);
        map.put("big", new BigDecimal("0.5"));
        map.put("nan", Double.NaN);
        map.put("str", new StringBuilder("x\ny"));
        map.put("c", 'c');
        map.put("t", true);
        map.put("n", null);
        map.put(7, Arrays.asList(new Object[]{"a", null}, Collections.emptyMap(), new LinkedHashSet<>(Arrays.asList(1, 2))));
        assertEquals("{\"d\":1.5,\"f\":0.25,\"l\":-9223372036854775808,\"i\":-3,\"s\":4,\"b\":5,\"big\":0.5,\"nan\":\"NaN\"," +
                "\"str\":\"x\\ny\",\"c\":\"c\",\"t\":true,\"n\":null,\"7\":[[\"a\",null],{},[1,2]]}", JsonWriter.newInstance().writeValue(map).toString());
        assertEquals("null", JsonWriter.newInstance().writeValue(null).toString());
        assertEquals("[]", JsonWriter.newInstance().writeValue(new Object[0]).toString());
        assertEquals("{}", JsonWriter.newInstance().writeValue(Collections.emptyMap()).toString());
    }

    @Test
    public void testNullNameKeepsState() {
        JsonWriter writer = JsonWriter.newInstance().beginObject();
        try {
            writer.writeName((CharSequence) null);
            fail("expected a NullPointerException");
        } catch (NullPointerException expected) {
        }
        assertEquals("{\"a\":1}", writer.writeName("a").writeInt(1).endObject().toString());
    }

    @Test
    public void testWriteValueRoundTrip() throws IOException {
        String json = "{\"a\":[1.5,-3,\"x\\ny\",true,null,{},[]],\"b\":{\"c\":[[0.25]],\"d\":false},\"e\":\"\"}";
        Object value = JsonReader.newInstance(json).readValue();
        String written = JsonWriter.newInstance().writeValue(value).toString();
        assertEquals(json, written);
        assertEquals(value, JsonReader.newInstance(written).readValue());
    }

    @Test
    public void testWriteValueUnsupportedType() {
        try {
            JsonWriter.newInstance().writeValue(Collections.singletonList(new Object()));
            fail("expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Unsupported value type: java.lang.Object", e.getMessage());
        }
    }

    @Test
    public void testInvalidStates() {
        assertIllegalState("Names are only allowed inside objects", () -> JsonWriter.newInstance().writeName("a"));
        assertIllegalState("Names are only allowed inside objects", () -> JsonWriter.newInstance().beginArray().writeName("a"));
        assertIllegalState("Not inside an object", () -> JsonWriter.newInstance().beginArray().endObject());
        assertIllegalState("Not inside an object", () -> JsonWriter.newInstance().endObject());
        assertIllegalState("Not inside an array", () -> JsonWriter.newInstance().beginObject().endArray());
        assertIllegalState("Not inside an array", () -> JsonWriter.newInstance().endArray());
    }

    @Test
    public void testGrowthPastReservedCapacity() {
        JsonWriter writer = JsonWriter.newInstance().reserve(100).beginArray();
        RepeatedByte bytes = writer.getBytes();
        int reallocations = 0;
        for (int i = 0; i < 100000; i++) {
            int capacity = bytes.capacity();
            writer.writeDouble(i + 0.5).writeInt(i).writeLong(i);
            reallocations += capacity != bytes.capacity() ? 1 : 0;
        }
        assertTrue("reallocations: " + reallocations, reallocations < 40);

        assertEquals(10, RepeatedByte.newEmptyInstance().reserve(10).capacity());
        assertEquals(15, RepeatedByte.newEmptyInstance().reserve(10).setLength(10).reserveGrowing(1).capacity());
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
