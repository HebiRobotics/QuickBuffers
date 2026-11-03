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
import us.hebi.quickbuf.JsonReader.Token;
import us.hebi.quickbuf.ProtoUtil.Charsets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;
import static us.hebi.quickbuf.JsonReader.Token.*;

/**
 * @author Florian Enner
 * @since 29 Sep 2026
 */
public class JsonReaderTest {

    @Test
    public void testReadNameIntoBuilder() throws IOException {
        JsonReader reader = JsonReader.newInstance("{\"a\\nb\":1,\"c\":2}").beginObject();
        StringBuilder names = new StringBuilder(">");
        reader.readName(names);
        reader.skipValue();
        reader.readName(names);
        assertEquals(">a\nbc", names.toString());
        assertEquals(2, reader.readInt());
    }

    @Test
    public void testAllTokens() throws IOException {
        JsonReader reader = JsonReader.newInstance("{\"a\":1,\"b\":[true,false,null,\"s\",-2.5e3],\"c\":{}}");
        assertEquals(BEGIN_OBJECT, reader.peek());
        reader.beginObject();
        assertTrue(reader.hasNext());
        assertEquals(NAME, reader.peek());
        assertEquals("a", reader.readName().toString());
        assertEquals(NUMBER, reader.peek());
        assertEquals(1, reader.readInt());
        assertEquals("b", reader.readName().toString());
        assertEquals(BEGIN_ARRAY, reader.peek());
        reader.beginArray();
        assertEquals(BOOLEAN, reader.peek());
        assertTrue(reader.readBoolean());
        assertFalse(reader.readBoolean());
        assertEquals(NULL, reader.peek());
        reader.readNull();
        assertEquals(STRING, reader.peek());
        assertEquals("s", reader.readString());
        assertEquals(-2500, reader.readDouble(), 0);
        assertFalse(reader.hasNext());
        assertEquals(END_ARRAY, reader.peek());
        reader.endArray();
        assertEquals("c", reader.readName().toString());
        reader.beginObject();
        assertFalse(reader.hasNext());
        assertEquals(END_OBJECT, reader.peek());
        reader.endObject();
        assertFalse(reader.hasNext());
        reader.endObject();
        assertFalse(reader.hasNext());
        assertEquals(END_DOCUMENT, reader.peek());
        assertEquals(END_DOCUMENT, reader.peek());
    }

    @Test
    public void testNestedContainers() throws IOException {
        assertTokens("[[[]],[{}],{\"a\":[{\"b\":[1]}]}]",
                "[", "[", "[", "]", "]", "[", "{", "}", "]", "{", "name:a", "[", "{", "name:b", "[", "number:1.0", "]", "}", "]", "}", "]");
        assertTokens(" \t\r\n{ \"a\" : [ 1 , 2 ] }\n", "{", "name:a", "[", "number:1.0", "number:2.0", "]", "}");
    }

    @Test
    public void testDeepNesting() throws IOException {
        final int depth = 100;
        StringBuilder json = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            json.append("[{\"a\":");
        }
        json.append('1');
        for (int i = 0; i < depth; i++) {
            json.append("}]");
        }

        JsonReader reader = JsonReader.newInstance(json.toString());
        for (int i = 0; i < depth; i++) {
            reader.beginArray().beginObject();
            assertEquals("a", reader.readName().toString());
        }
        assertEquals(1, reader.readInt());
        for (int i = 0; i < depth; i++) {
            reader.endObject().endArray();
        }
        reader.endDocument();

        reader = JsonReader.newInstance(json.toString());
        reader.skipValue();
        reader.endDocument();
    }

    @Test
    public void testNestingLimit() throws IOException {
        JsonReader reader = JsonReader.newInstance("[{\"a\":[1]}] [[[[]]]]").setNestingLimit(3);
        reader.skipValue();
        reader.beginArray().beginArray().beginArray();
        assertError("Exceeded the nesting limit of 3 at offset 15", reader::beginArray);
        assertError("Exceeded the nesting limit of 1 at offset 1", () -> JsonReader.newInstance("[[1]]").setNestingLimit(1).skipValue());
        assertError("Exceeded the nesting limit of 0 at offset 0", () -> JsonReader.newInstance("{}").setNestingLimit(0).beginObject());
    }

    @Test
    public void testReadValue() throws IOException {
        Object value = JsonReader.newInstance("{\"b\":[1.5,\"x\",true,null,{},[]],\"a\":{\"c\":-2},\"b\":false}").readValue();
        assertTrue(value instanceof LinkedHashMap);
        Map<?, ?> map = (Map<?, ?>) value;
        assertEquals(Arrays.asList("b", "a"), new ArrayList<>(map.keySet()));
        assertEquals(false, map.get("b"));
        assertEquals(Collections.singletonMap("c", -2L), map.get("a"));

        value = JsonReader.newInstance("[1.5,\"x\",true,null,{},[]]").readValue();
        assertTrue(value instanceof ArrayList);
        assertEquals(Arrays.asList(1.5, "x", true, null, Collections.emptyMap(), Collections.emptyList()), value);

        JsonReader reader = JsonReader.newInstance(" 1 \"s\" true null {} [] ");
        assertEquals(1L, reader.readValue());
        assertEquals("s", reader.readValue());
        assertEquals(true, reader.readValue());
        assertNull(reader.readValue());
        assertEquals(Collections.emptyMap(), reader.readValue());
        assertEquals(Collections.emptyList(), reader.readValue());
        assertEquals(END_DOCUMENT, reader.peek());

        assertEquals(Arrays.asList(9007199254740993L, Long.MIN_VALUE, 9.223372036854775808E18, 12345678901234567890.0, 100.0, 1.0, -0.5, 0L, 0L),
                JsonReader.newInstance("[9007199254740993,-9223372036854775808,9223372036854775808,12345678901234567890,1e2,1.0,-0.5,-0,0]").readValue());

        assertEquals(getError(() -> JsonReader.newInstance("[1e999]").beginArray().readDouble()),
                getError(() -> JsonReader.newInstance("[1e999]").readValue()));
        assertError("Expected a value but reached the end of the input at offset 0", () -> JsonReader.newInstance("").readValue());
        assertError("Expected a value but reached the end of the input for field 'a[0]' at offset 6", () -> JsonReader.newInstance("{\"a\":[").readValue());
        assertError("Invalid escape sequence '\\x' for field 'a' at offset 14", () -> JsonReader.newInstance("{\"a\":{\"bb\":1,\"\\x\":2}}").readValue());
    }

    @Test
    public void testReadValueDeepNesting() throws IOException {
        StringBuilder json = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            json.append('[');
        }
        for (int i = 0; i < 100000; i++) {
            json.append(']');
        }
        Object value = JsonReader.newInstance(json.toString()).readValue();
        for (int i = 1; i < 100000; i++) {
            assertEquals(1, ((List<?>) value).size());
            value = ((List<?>) value).get(0);
        }
        assertEquals(Collections.emptyList(), value);
        assertError("Exceeded the nesting limit of 512 at offset 512", () -> JsonReader.newInstance(json.toString()).setNestingLimit(512).readValue());
    }

    @Test
    public void testTopLevelValues() throws IOException {
        assertTokens("1", "number:1.0");
        assertTokens("\"s\"", "string:s");
        assertTokens("true", "bool:true");
        assertTokens("null", "null");
        assertTokens("[1,2]", "[", "number:1.0", "number:2.0", "]");
        assertTokens("", new String[0]);
        assertTokens(" 1 2 {}[] ", "number:1.0", "number:2.0", "{", "}", "[", "]");
    }

    @Test
    public void testTrailingContent() throws IOException {
        JsonReader reader = JsonReader.newInstance("{} x");
        reader.beginObject().endObject();
        assertTrue(reader.hasNext());
        assertError("Expected a value but got 'x' at offset 3", reader::peek);
    }

    @Test
    public void testEndDocument() throws IOException {
        assertEndDocument("true", null);
        assertEndDocument("[1]  ", null);
        assertEndDocument("true 1", "Unexpected value after the root value at offset 5");
        assertEndDocument("[1] x", "Unexpected value after the root value at offset 4");
        assertEndDocument("{} {}", "Unexpected value after the root value at offset 3");

        JsonReader peeked = JsonReader.newInstance("true 1");
        peeked.skipValue();
        assertTrue(peeked.hasNext());
        assertError("Unexpected value after the root value at offset 5", peeked::endDocument);

        JsonReader open = JsonReader.newInstance("[1");
        open.beginArray().readInt();
        assertError("Expected the end of the document but an object or array is still open at offset 2", open::endDocument);
    }

    @Test
    public void testNewError() throws IOException {
        JsonReader reader = JsonReader.newInstance("[1, 2]");
        reader.beginArray().readInt();
        InvalidJsonException error = reader.newError("Value out of range");
        assertEquals("Value out of range at offset 2", error.getMessage());
        assertEquals("Value out of range", error.getDescription());
        assertEquals(2, error.getOffset());
        assertEquals("no offset", new InvalidJsonException("no offset").getDescription());
    }

    @Test
    public void testByteOrderMark() throws IOException {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, '[', ']'};
        assertEquals(Arrays.asList("[", "]"), tokens(JsonReader.newInstance(bom)));
        assertEquals(Arrays.asList("[", "]"), tokens(JsonReader.newInstance(new SlowInputStream(bom, 1))));

        byte[] invalid = {(byte) 0xEF, (byte) 0xBB, '[', ']'};
        assertError("Invalid byte order mark at offset 2", () -> JsonReader.newInstance(invalid).peek());

        byte[] afterWhitespace = {' ', (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, '[', ']'};
        assertError("Expected a value but got byte 0xEF at offset 1", () -> JsonReader.newInstance(afterWhitespace).peek());
    }

    @Test
    public void testNonAsciiErrorBytes() throws IOException {
        assertError("Expected a value but got byte 0xC3 at offset 1", () -> tokens(JsonReader.newInstance(new byte[]{'[', (byte) 0xC3, ']'})));
        assertError("Expected ',' or ']' but got byte 0xC3 at offset 3", () -> tokens(JsonReader.newInstance(new byte[]{'[', '1', ' ', (byte) 0xC3, ']'})));
        assertError("Expected a name or '}' but got byte 0xEF at offset 1", () -> tokens(JsonReader.newInstance(new byte[]{'{', (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, '}'})));
        assertError("Expected 'true' but got 'tr' followed by byte 0xC3 at offset 0", () -> JsonReader.newInstance(new byte[]{'t', 'r', (byte) 0xC3, 'e'}).readBoolean());
        assertError("Invalid escape sequence '\\' followed by byte 0xC3 at offset 1", () -> JsonReader.newInstance(new byte[]{'"', '\\', (byte) 0xC3, '"'}).readString());
        assertError("Invalid hex digit byte 0xC3 in unicode escape at offset 3", () -> JsonReader.newInstance(new byte[]{'"', '\\', 'u', (byte) 0xC3, '"'}).readString());
    }

    @Test
    public void testStringEscapes() throws IOException {
        String json = "\"\\\"\\\\\\/\\b\\f\\n\\r\\t\\u0041\\u00e9\\u4E2D\\ud83d\\ude00\"";
        String expected = "\"\\/\b\f\n\r\tA\u00e9\u4e2d\ud83d\ude00";
        assertEquals(expected, JsonReader.newInstance(json).readString());

        Utf8String utf8 = Utf8String.newEmptyInstance();
        JsonReader.newInstance(json).readString(utf8);
        assertEquals(expected, utf8.getString());

        StringBuilder builder = new StringBuilder("prefix:");
        JsonReader.newInstance(json).readString(builder);
        assertEquals("prefix:" + expected, builder.toString());
    }

    @Test
    public void testRawUtf8() throws IOException {
        String expected = "a\u00e9\u4e2d\ud83d\ude00\u2028z";
        byte[] json = ("\"" + expected + "\"").getBytes(Charsets.UTF_8);
        assertEquals(expected, JsonReader.newInstance(json).readString());

        Utf8String utf8 = Utf8String.newEmptyInstance();
        JsonReader.newInstance(json).readString(utf8);
        assertEquals(expected, utf8.getString());

        JsonReader reader = JsonReader.newInstance(("{\"" + expected + "\":0}").getBytes(Charsets.UTF_8));
        reader.beginObject();
        assertEquals(expected, reader.readName().toString());
    }

    @Test
    public void testInvalidStrings() throws IOException {
        assertError("Invalid UTF-8 in string at offset 3", () -> JsonReader.newInstance(new byte[]{'"', (byte) 0xE4, (byte) 0xB8, '"'}).readString());
        assertError("Unexpected end of input at offset 3", () -> JsonReader.newInstance(new byte[]{'"', (byte) 0xE4, (byte) 0xB8}).readString());
        assertError("Invalid UTF-8 in string at offset 1", () -> JsonReader.newInstance(new byte[]{'"', (byte) 0x80, '"'}).readString());
        assertError("Invalid UTF-8 in string at offset 1", () -> JsonReader.newInstance(new byte[]{'"', (byte) 0xC0, (byte) 0x80, '"'}).readString());
        assertError("Invalid UTF-8 in string at offset 3", () -> JsonReader.newInstance(new byte[]{'"', (byte) 0xED, (byte) 0xA0, (byte) 0x80, '"'}).readString());
        assertError("Invalid UTF-8 in string at offset 4", () -> JsonReader.newInstance(new byte[]{'"', (byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80, '"'}).readString());
        assertError("Invalid escape sequence '\\x' at offset 2", () -> JsonReader.newInstance("\"a\\x\"").readString());
        assertError("Invalid hex digit 'g' in unicode escape at offset 5", () -> JsonReader.newInstance("\"\\u00g0\"").readString());
        assertError("Unescaped control character 0x01 in string at offset 2", () -> JsonReader.newInstance("\"a\u0001\"").readString());
        assertError("Unescaped control character 0x0A in string at offset 2", () -> JsonReader.newInstance("[\"\n\"]").skipValue());
        assertError("Unexpected end of input at offset 4", () -> JsonReader.newInstance("\"abc").readString());

        // unpaired surrogates could be represented in Java strings, but not in UTF-8
        assertError("Unpaired surrogate in string at offset 1", () -> JsonReader.newInstance("\"\\ud800\"").readString(Utf8String.newEmptyInstance()));
        assertError("Unpaired surrogate in string at offset 1", () -> JsonReader.newInstance("\"\\udc00\"").readString(Utf8String.newEmptyInstance()));
        assertError("Unpaired surrogate in string at offset 1", () -> JsonReader.newInstance("\"\\ud800\\u0041\"").readString(Utf8String.newEmptyInstance()));
    }

    @Test
    public void testUnpairedSurrogateChars() throws IOException {
        assertError("Unpaired surrogate in string at offset 1", () -> JsonReader.newInstance("\"\\ud83d\"").readString());
        assertError("Unpaired surrogate in string at offset 1", () -> JsonReader.newInstance("\"\\ud83d\\u0041\"").readString());
        assertError("Unpaired surrogate in string at offset 1", () -> JsonReader.newInstance("\"\\ude00\"").readString());
        assertError("Unpaired surrogate in string at offset 1", () -> JsonReader.newInstance("\"\\ude00\\ud83d\"").readString());
        assertEquals("\ud83d\ude00", JsonReader.newInstance("\"\\ud83d\\ude00\"").readString());

        assertError("Unpaired surrogate in string at offset 2", () -> JsonReader.newInstance("{\"\\ud83d\":0}").beginObject().readName());
        assertError("Unpaired surrogate in string at offset 2", () -> JsonReader.newInstance("{\"\\ud83d\\u0041\":0}").beginObject().readName());
        assertError("Unpaired surrogate in string at offset 2", () -> JsonReader.newInstance("{\"\\ude00\":0}").beginObject().readName());
        assertError("Unpaired surrogate in string at offset 2", () -> JsonReader.newInstance("{\"\\ude00\\ud83d\":0}").beginObject().readName());
        assertEquals("\ud83d\ude00", JsonReader.newInstance("{\"\\ud83d\\ude00\":0}").beginObject().readName().toString());
    }

    @Test
    public void testBase64() throws IOException {
        RepeatedByte bytes = RepeatedByte.newEmptyInstance().addAll(new byte[]{1, 2, 3, (byte) 0xFB, (byte) 0xFF});
        String json = JsonWriter.newInstance().writeBase64(bytes).toString();
        RepeatedByte store = RepeatedByte.newEmptyInstance().addAll(new byte[]{9});
        JsonReader.newInstance(json).readBase64(store);
        assertEquals(bytes, store);

        JsonReader.newInstance("\"AQID-_8\"").readBase64(store);
        assertEquals(bytes, store);
        JsonReader.newInstance("\"\"").readBase64(store);
        assertEquals(0, store.length());

        assertError("Invalid base64 string for field 'a' at offset 6", () -> {
            JsonReader reader = JsonReader.newInstance("{\"a\":\"AQIDB\"}");
            reader.beginObject().readName();
            reader.readBase64(RepeatedByte.newEmptyInstance());
        });
        assertError("Expected a string but got '1' at offset 0", () -> JsonReader.newInstance("1").readBase64(RepeatedByte.newEmptyInstance()));
    }

    @Test
    public void testNumbers() throws IOException {
        assertEquals(Double.doubleToLongBits(-0.0), Double.doubleToLongBits(JsonReader.newInstance("-0").readDouble()));
        assertEquals(Float.floatToIntBits(-0f), Float.floatToIntBits(JsonReader.newInstance("-0.0").readFloat()));
        assertEquals(0, JsonReader.newInstance("-0").readLong());
        assertEquals(1000, JsonReader.newInstance("1e3").readDouble(), 0);
        assertEquals(1000, JsonReader.newInstance("1E+3").readDouble(), 0);
        assertEquals(0.001, JsonReader.newInstance("1e-3").readDouble(), 0);
        assertEquals(-0.0025, JsonReader.newInstance("-2.5E-3").readDouble(), 0);
        assertEquals(0.1f, JsonReader.newInstance("0.1").readFloat(), 0);

        // 2^53+1 rounds to the nearest double, but stays exact as a long
        assertEquals(9007199254740992.0, JsonReader.newInstance("9007199254740993").readDouble(), 0);
        assertEquals(9007199254740993L, JsonReader.newInstance("9007199254740993").readLong());

        assertEquals(Long.MAX_VALUE, JsonReader.newInstance("9223372036854775807").readLong());
        assertEquals(Long.MIN_VALUE, JsonReader.newInstance("-9223372036854775808").readLong());
        assertEquals(Integer.MAX_VALUE, JsonReader.newInstance("2147483647").readInt());
        assertEquals(Integer.MIN_VALUE, JsonReader.newInstance("-2147483648").readInt());
    }

    @Test
    public void testNumbersAcrossRefills() throws IOException {
        char[] zeros = new char[20000];
        Arrays.fill(zeros, '0');
        String longNumber = "1." + new String(zeros);
        byte[] json = ("[12345678901234567890123,-0.000123e-5,9223372036854775807,-2147483648,0.5," + longNumber + "]").getBytes(Charsets.UTF_8);
        List<String> expected = tokens(JsonReader.newInstance(json));
        assertEquals("number:1.0", expected.get(expected.size() - 2));
        for (int maxChunk = 1; maxChunk <= 3; maxChunk++) {
            assertEquals(expected, tokens(JsonReader.newInstance(new SlowInputStream(json, maxChunk))));
        }
        assertEquals(expected, tokens(JsonReader.newInstance(new ByteArrayInputStream(json))));

        JsonReader reader = JsonReader.newInstance(new SlowInputStream(json, 2)).beginArray();
        reader.skipValue();
        reader.skipValue();
        assertEquals(Long.MAX_VALUE, reader.readLong());
        assertEquals(Integer.MIN_VALUE, reader.readInt());

        byte[] invalid = ("[" + longNumber + "x]").getBytes(Charsets.UTF_8);
        String error = getError(() -> tokens(JsonReader.newInstance(invalid)));
        assertEquals("Invalid number '" + longNumber + "x' at offset 1", error);
        assertEquals(error, getError(() -> tokens(JsonReader.newInstance(new SlowInputStream(invalid, 3)))));
    }

    @Test
    public void testInvalidNumbers() throws IOException {
        assertError("Invalid number '01' at offset 1", () -> JsonReader.newInstance("[01]").beginArray().readDouble());
        assertError("Invalid number '01' at offset 1", () -> JsonReader.newInstance("[01]").skipValue());
        assertError("Invalid fraction in number '1.' at offset 0", () -> JsonReader.newInstance("1.").readDouble());
        assertError("Invalid number '-' at offset 0", () -> JsonReader.newInstance("-").readDouble());
        assertError("Invalid number '-a' at offset 0", () -> JsonReader.newInstance("-a").readDouble());
        assertError("Invalid exponent in number '1e' at offset 0", () -> JsonReader.newInstance("1e").readDouble());
        assertError("Invalid exponent in number '1e+' at offset 0", () -> JsonReader.newInstance("1e+").readDouble());
        assertError("Invalid number '1x' at offset 0", () -> JsonReader.newInstance("1x").readDouble());
        assertError("Invalid number '1d' at offset 0", () -> JsonReader.newInstance("1d").readFloat());
        assertError("Invalid number '1\u00e9' at offset 0", () -> JsonReader.newInstance("1\u00e9").readDouble());
        assertError("Expected a number but got '.' at offset 0", () -> JsonReader.newInstance(".5").readDouble());
        assertError("Expected a value but got '+' at offset 0", () -> JsonReader.newInstance("+1").peek());
        assertError("Expected a number but got 'N' at offset 1", () -> JsonReader.newInstance("[NaN]").beginArray().readDouble());
        assertError("Expected a number but got '\"' at offset 0", () -> JsonReader.newInstance("\"1\"").readInt());
    }

    @Test
    public void testIntegerRanges() throws IOException {
        assertError("Expected an integer but got '1.5' at offset 1", () -> JsonReader.newInstance("[1.5]").beginArray().readInt());
        assertError("Expected an integer but got '1.0' at offset 0", () -> JsonReader.newInstance("1.0").readLong());
        assertError("Expected an integer but got '1e2' at offset 0", () -> JsonReader.newInstance("1e2").readInt());
        assertError("Value out of range for int '2147483648' at offset 0", () -> JsonReader.newInstance("2147483648").readInt());
        assertError("Value out of range for int '-2147483649' at offset 0", () -> JsonReader.newInstance("-2147483649").readInt());
        assertError("Value out of range for int '123456789012345678901' at offset 0", () -> JsonReader.newInstance("123456789012345678901").readInt());
        assertError("Value out of range for long '9223372036854775808' at offset 0", () -> JsonReader.newInstance("9223372036854775808").readLong());
        assertError("Value out of range for long '-9223372036854775809' at offset 0", () -> JsonReader.newInstance("-9223372036854775809").readLong());
        assertError("Value out of range for double '1e999' at offset 1", () -> JsonReader.newInstance("[1e999]").beginArray().readDouble());
        assertError("Value out of range for double '-1e999' at offset 0", () -> JsonReader.newInstance("-1e999").readDouble());
        assertError("Value out of range for float '1e39' at offset 0", () -> JsonReader.newInstance("1e39").readFloat());
        assertEquals(Double.MAX_VALUE, JsonReader.newInstance("1.7976931348623157e308").readDouble(), 0);
        assertEquals(Float.MAX_VALUE, JsonReader.newInstance("3.4028235e38").readFloat(), 0);
        assertEquals(0, JsonReader.newInstance("1e-999").readDouble(), 0);
        assertEquals(0, JsonReader.newInstance("1e-99").readFloat(), 0);
    }

    @Test
    public void testInvalidStructure() throws IOException {
        assertInvalid("[1,]", "Trailing comma before ']' at offset 3");
        assertInvalid("[,1]", "Expected a value but got ',' at offset 1");
        assertInvalid("[1 2]", "Expected ',' or ']' but got '2' at offset 3");
        assertInvalid("[1,,2]", "Expected a value but got ',' at offset 3");
        assertInvalid("{\"a\":1,}", "Trailing comma before '}' for field 'a' at offset 7");
        assertInvalid("{,\"a\":1}", "Expected a name or '}' but got ',' at offset 1");
        assertInvalid("{\"a\":1 \"b\":2}", "Expected ',' or '}' but got '\"' for field 'a' at offset 7");
        assertInvalid("{\"a\":1,,\"b\":2}", "Expected a name but got ',' for field 'a' at offset 7");
        assertInvalid("{\"a\" 1}", "Expected ':' but got '1' for field 'a' at offset 5");
        assertInvalid("{\"a\"::1}", "Expected a value but got ':' for field 'a' at offset 5");
        assertInvalid("{\"a\":}", "Expected a value but got '}' for field 'a' at offset 5");
        assertInvalid("{'a':1}", "Expected a name or '}' but got ''' at offset 1");
        assertInvalid("{a:1}", "Expected a name or '}' but got 'a' at offset 1");
        assertInvalid("{1:1}", "Expected a name or '}' but got '1' at offset 1");
        assertInvalid("[1}", "Expected ',' or ']' but got '}' at offset 2");
        assertInvalid("{\"a\":1]", "Expected ',' or '}' but got ']' for field 'a' at offset 6");
        assertInvalid("]", "Expected a value but got ']' at offset 0");
        assertInvalid("[]]", "Expected a value but got ']' at offset 2");
        assertInvalid("[tru]", "Expected 'true' but got 'tru]' at offset 1");
        assertInvalid("[nul]", "Expected 'null' but got 'nul]' at offset 1");
        assertInvalid("[truex]", "Expected ',' or ']' but got 'x' at offset 5");
        assertInvalid("[\"a\"\"b\"]", "Expected ',' or ']' but got '\"' at offset 4");
    }

    @Test
    public void testMismatchedTypes() throws IOException {
        assertError("Expected '{' but got 'n' at offset 0", () -> JsonReader.newInstance("null").beginObject());
        assertError("Expected '[' but got '{' at offset 0", () -> JsonReader.newInstance("{}").beginArray());
        assertError("Expected ']' but got '}' at offset 1", () -> JsonReader.newInstance("{}").beginObject().endArray());
        assertError("Expected a name but got '}' at offset 1", () -> JsonReader.newInstance("{}").beginObject().readName());
        assertError("Expected a string but got '1' at offset 0", () -> JsonReader.newInstance("1").readString());
        assertError("Expected a boolean but got '\"' at offset 0", () -> JsonReader.newInstance("\"true\"").readBoolean());
        assertError("Expected null but got 'f' at offset 0", () -> JsonReader.newInstance("false").readNull());
        assertError("Expected a string but reached the end of the input at offset 0", () -> JsonReader.newInstance("").readString());
        assertError("Expected a value but got '}' at offset 1", () -> JsonReader.newInstance("{}").beginObject().skipValue());
        assertError("Expected a value but reached the end of the input at offset 2", () -> JsonReader.newInstance("{}").beginObject().endObject().skipValue());
    }

    @Test
    public void testTruncatedInput() throws IOException {
        assertInvalid("[", "Expected a value but reached the end of the input at offset 1");
        assertInvalid("[1", "Expected ',' or ']' but reached the end of the input at offset 2");
        assertInvalid("[1,", "Expected a value but reached the end of the input at offset 3");
        assertInvalid("{", "Expected a name or '}' but reached the end of the input at offset 1");
        assertInvalid("{\"a\"", "Expected ':' but reached the end of the input for field 'a' at offset 4");
        assertInvalid("{\"a\":", "Expected a value but reached the end of the input for field 'a' at offset 5");
        assertInvalid("{\"a\":1", "Expected ',' or '}' but reached the end of the input for field 'a' at offset 6");
        assertInvalid("{\"a", "Unexpected end of input at offset 3");
        assertInvalid("[\"a\\", "Unexpected end of input at offset 4");
        assertInvalid("[\"\\u00", "Unexpected end of input at offset 6");
        assertInvalid("tru", "Unexpected end of input at offset 3");
    }

    @Test
    public void testSkipValueMismatch() throws IOException {
        assertError("Expected ',' or '}' but got ']' for field 'a' at offset 10", () -> JsonReader.newInstance("{\"a\":[1,2]]}").skipValue());
        assertError("Expected ',' or ']' but got '}' at offset 5", () -> JsonReader.newInstance("[[1,2}]").skipValue());
        assertError("Expected a value but got '}' for field 'a.b[0]' at offset 11", () -> JsonReader.newInstance("{\"a\":{\"b\":[}]}").skipValue());
        assertError("Expected ':' but got ',' for field '[0].b' at offset 11", () -> JsonReader.newInstance("[{\"a\":1,\"b\",2}]").skipValue());
        assertError("Trailing comma before ']' at offset 4", () -> JsonReader.newInstance("[[1,]]").skipValue());
        assertError("Expected ',' or ']' but reached the end of the input at offset 6", () -> JsonReader.newInstance("[[1,2]").skipValue());
        assertError("Unexpected end of input for field 'a' at offset 14", () -> JsonReader.newInstance("{\"a\":{\"b\":1,\"c").skipValue());
    }

    @Test
    public void testSkipValueAtEveryPosition() throws IOException {
        String json = "{\"a\":[1,{\"b\":null,\"c\":[]}],\"d\":\"x\",\"e\":{\"f\":[true,{}]},\"g\":-1.5e3}";
        List<String> expected = tokens(JsonReader.newInstance(json));
        for (int k = 0; k < expected.size(); k++) {
            String token = expected.get(k);
            JsonReader reader = JsonReader.newInstance(json);
            for (int i = 0; i < k; i++) {
                readToken(reader);
            }
            if (token.equals("}") || token.equals("]")) {
                try {
                    reader.skipValue();
                    fail("expected skipping to fail at " + token);
                } catch (InvalidJsonException expectedError) {
                }
                continue;
            }
            reader.skipValue();
            int next = token.startsWith("name:") ? endOfValue(expected, k + 1) : endOfValue(expected, k);
            assertEquals("skipped at " + k, expected.subList(next, expected.size()), tokens(reader));
        }
    }

    @Test
    public void testSkipValueAcrossRefills() throws IOException {
        byte[] json = "[{\"a\":\"x\\\"y\\/z\",\"b\":[12345.5e1,true,null,\"long string value\"]},\"c\"]".getBytes(Charsets.UTF_8);
        for (int maxChunk = 1; maxChunk <= 3; maxChunk++) {
            for (boolean lenient : new boolean[]{false, true}) {
                JsonReader reader = JsonReader.newInstance(new SlowInputStream(json, maxChunk));
                reader.lenient = lenient;
                reader.beginArray().skipValue();
                assertEquals("c", reader.readString());
                reader.endArray();
            }
        }
        assertError("Unescaped control character 0x0A in string at offset 8",
                () -> JsonReader.newInstance(new SlowInputStream("[\"abcdef\n\"]".getBytes(Charsets.UTF_8), 2)).skipValue());
    }

    @Test
    public void testLenientMode() throws IOException {
        JsonReader reader = JsonReader.newInstance("[\"a\tb\",{\"x\":[NaN,+1,tru,1x,\"\\q\"]},\"c\"]");
        reader.lenient = true;
        reader.beginArray();
        assertEquals("a\tb", reader.readString());
        reader.skipValue();
        assertEquals("c", reader.readString());
        reader.endArray();

        reader = JsonReader.newInstance("[\"a\tb\"]");
        reader.lenient = true;
        Utf8String utf8 = Utf8String.newEmptyInstance();
        reader.beginArray().readString(utf8);
        assertEquals("a\tb", utf8.getString());

        // skipped containers only get their brackets counted
        reader = JsonReader.newInstance("[{\"x\":[NaN,],\"y\" 1 \"}\"},[[1 :]]]");
        reader.lenient = true;
        reader.beginArray().skipValue();
        reader.skipValue();
        reader.endArray().endDocument();

        // the structure around them stays strict
        JsonReader invalid = JsonReader.newInstance("[{},]");
        invalid.lenient = true;
        invalid.beginArray().skipValue();
        assertError("Trailing comma before ']' at offset 4", invalid::hasNext);
        assertError("Unexpected end of input at offset 7", () -> {
            JsonReader truncated = JsonReader.newInstance("[{\"a\":[");
            truncated.lenient = true;
            truncated.beginArray().skipValue();
        });
    }

    @Test
    public void testValueRange() throws IOException {
        byte[] json = "[12345, \"6\\\"7\", null, NaN]".getBytes(Charsets.UTF_8);
        for (JsonReader reader : new JsonReader[]{JsonReader.newInstance(json), JsonReader.newInstance(new SlowInputStream(json, 2))}) {
            reader.beginArray();
            assertTrue(reader.readValueRange());
            assertEquals("12345", new String(reader.valueArray, reader.valueStart, reader.valueEnd - reader.valueStart, Charsets.UTF_8));
            assertTrue(reader.readValueRange());
            assertEquals("6\\\"7", new String(reader.valueArray, reader.valueStart, reader.valueEnd - reader.valueStart, Charsets.UTF_8));
            assertFalse(reader.readValueRange());
            reader.readNull();
            assertTrue(reader.readValueRange());
            assertEquals("NaN", new String(reader.valueArray, reader.valueStart, reader.valueEnd - reader.valueStart, Charsets.UTF_8));
            reader.endArray();
        }
    }

    @Test
    public void testArraySlice() throws IOException {
        byte[] bytes = "xx[1,2]yy".getBytes(Charsets.UTF_8);
        JsonReader reader = JsonReader.newInstance(bytes, 2, 5);
        assertEquals(Arrays.asList("[", "number:1.0", "number:2.0", "]"), tokens(reader));
        assertEquals(5, reader.getPosition());

        byte[] invalid = "xx[1 2]yy".getBytes(Charsets.UTF_8);
        assertError("Expected ',' or ']' but got '2' at offset 3", () -> tokens(JsonReader.newInstance(invalid, 2, 5)));
        assertError("Expected a value but got 'y' at offset 5", () -> tokens(JsonReader.newInstance(bytes, 2, 7)));

        RepeatedByte repeated = RepeatedByte.newEmptyInstance().addAll("[true]".getBytes(Charsets.UTF_8));
        repeated.reserve(100);
        assertEquals(Arrays.asList("[", "bool:true", "]"), tokens(JsonReader.newInstance(repeated)));
    }

    @Test
    public void testInputStreamRefills() throws IOException {
        StringBuilder json = new StringBuilder("{\"items\":[");
        Random rnd = new Random(0);
        for (int i = 0; i < 1000; i++) {
            json.append(i == 0 ? "" : ",").append("{\"name\u00e9").append(i)
                    .append("\":\"value\\n\\u00e9\u4e2d\ud83d\ude00").append(rnd.nextLong())
                    .append("\",\"number\":").append(rnd.nextDouble() * 1E10)
                    .append(",\"flag\":").append(rnd.nextBoolean())
                    .append(",\"none\":null}");
        }
        json.append("]}");
        byte[] bytes = json.toString().getBytes(Charsets.UTF_8);
        assertTrue(bytes.length > 64 * 1024);

        List<String> expected = tokens(JsonReader.newInstance(bytes));
        for (int maxChunk = 1; maxChunk <= 3; maxChunk++) {
            assertEquals(expected, tokens(JsonReader.newInstance(new SlowInputStream(bytes, maxChunk))));
        }
        assertEquals(expected, tokens(JsonReader.newInstance(new ByteArrayInputStream(bytes))));

        // raw values across refills
        JsonReader reader = JsonReader.newInstance(new SlowInputStream("[\"a\\\"b\", 12345.5e1, true]".getBytes(Charsets.UTF_8), 1));
        RepeatedByte raw = RepeatedByte.newEmptyInstance();
        reader.beginArray();
        reader.readValueBytes(raw);
        assertEquals("a\\\"b", new String(raw.toArray(), Charsets.UTF_8));
        reader.readValueBytes(raw);
        assertEquals("12345.5e1", new String(raw.toArray(), Charsets.UTF_8));
        reader.readValueBytes(raw);
        assertEquals("true", new String(raw.toArray(), Charsets.UTF_8));
        reader.endArray();
        assertEquals(END_DOCUMENT, reader.peek());

        // same offsets as array input, but names that left the buffer print as '?'
        byte[] invalid = (json.substring(0, 9000) + ",]").getBytes(Charsets.UTF_8);
        String error = getError(() -> tokens(JsonReader.newInstance(invalid)));
        assertTrue(error, error.contains(" for field 'items[86].number' at offset "));
        String streamError = error.replace("'items[", "'?[");
        assertEquals(streamError, getError(() -> tokens(JsonReader.newInstance(new SlowInputStream(invalid, 3)))));
        assertEquals(streamError, getError(() -> tokens(JsonReader.newInstance(new ByteArrayInputStream(invalid)))));
    }

    @Test
    public void testReadNameReusesBuffer() throws IOException {
        JsonReader reader = JsonReader.newInstance("{\"first\":1,\"second\":2}");
        reader.beginObject();
        CharSequence first = reader.readName();
        assertEquals("first", first.toString());
        reader.skipValue();
        CharSequence second = reader.readName();
        assertSame(first, second);
        assertEquals("second", second.toString());
    }

    @Test
    public void testReuse() throws IOException {
        JsonReader reader = JsonReader.newInstance();
        assertEquals(END_DOCUMENT, reader.peek());

        reader.setInput("{\"a\":[1,");
        reader.beginObject().readName();
        reader.beginArray();
        assertEquals(1, reader.readInt());

        reader.setInput("xx[true]".getBytes(Charsets.UTF_8), 2, 6);
        reader.beginArray();
        assertTrue(reader.readBoolean());
        reader.endArray().endDocument();
        assertEquals(6, reader.getPosition());

        reader.setInput(new ByteArrayInputStream("{\"b\":\"c\"}".getBytes(Charsets.UTF_8)));
        reader.beginObject();
        assertEquals("b", reader.readName().toString());
        assertEquals("c", reader.readString());
        reader.endObject().endDocument();

        reader.setInput("[]");
        reader.beginArray().endArray().endDocument();
        assertEquals(2, reader.getPosition());
    }

    @Test
    public void testNameHash() throws IOException {
        for (String name : new String[]{"", "optionalInt32", "\u00e9t\u00e9", "\ud83d\ude00x"}) {
            JsonReader.CharBuffer raw = (JsonReader.CharBuffer) JsonReader.newInstance("{\"" + name + "\":0}").beginObject().readName();
            assertEquals(name, raw.toString());
            assertEquals(name.hashCode(), raw.hash());
        }
        JsonReader.CharBuffer escaped = (JsonReader.CharBuffer) JsonReader.newInstance("{\"\\ud83d\\ude00x\":0}").beginObject().readName();
        assertEquals("\ud83d\ude00x".hashCode(), escaped.hash());
    }

    @Test
    public void testPosition() throws IOException {
        JsonReader reader = JsonReader.newInstance(" [ 1 ]");
        assertEquals(0, reader.getPosition());
        reader.beginArray();
        assertEquals(2, reader.getPosition());
        reader.readInt();
        assertEquals(4, reader.getPosition());
    }

    @Test
    public void testCloseClosesStream() throws IOException {
        final boolean[] closed = {false};
        InputStream stream = new ByteArrayInputStream(new byte[0]) {
            @Override
            public void close() {
                closed[0] = true;
            }
        };
        JsonReader.newInstance(stream).close();
        assertTrue(closed[0]);
    }

    private static int endOfValue(List<String> tokens, int start) {
        int level = 0;
        for (int i = start; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.equals("{") || token.equals("[")) {
                level++;
            } else if (token.equals("}") || token.equals("]")) {
                level--;
            }
            if (level == 0) {
                return i + 1;
            }
        }
        throw new AssertionError("unbalanced tokens");
    }

    private static void assertTokens(String json, String... expected) throws IOException {
        assertEquals(Arrays.asList(expected), tokens(JsonReader.newInstance(json)));
    }

    private static void assertEndDocument(String json, String error) throws IOException {
        byte[] bytes = json.getBytes(Charsets.UTF_8);
        for (JsonReader reader : new JsonReader[]{JsonReader.newInstance(bytes), JsonReader.newInstance(new SlowInputStream(bytes, 1))}) {
            reader.skipValue();
            if (error == null) {
                reader.endDocument();
            } else {
                assertError(error, reader::endDocument);
            }
        }
    }

    private static void assertInvalid(String json, String error) {
        assertError(error, () -> tokens(JsonReader.newInstance(json)));
        assertError(error, () -> {
            JsonReader reader = JsonReader.newInstance(json);
            reader.skipValue();
            reader.peek();
        });
        assertError(error, () -> tokens(JsonReader.newInstance(new SlowInputStream(json.getBytes(Charsets.UTF_8), 2))));
    }

    public static List<String> tokens(JsonReader reader) throws IOException {
        List<String> tokens = new ArrayList<>();
        for (String token = readToken(reader); token != null; token = readToken(reader)) {
            tokens.add(token);
        }
        return tokens;
    }

    public static String readToken(JsonReader reader) throws IOException {
        Token token = reader.peek();
        switch (token) {
            case BEGIN_OBJECT:
                reader.beginObject();
                return "{";
            case END_OBJECT:
                reader.endObject();
                return "}";
            case BEGIN_ARRAY:
                reader.beginArray();
                return "[";
            case END_ARRAY:
                reader.endArray();
                return "]";
            case NAME:
                return "name:" + reader.readName();
            case STRING:
                return "string:" + reader.readString();
            case NUMBER:
                return "number:" + reader.readDouble();
            case BOOLEAN:
                return "bool:" + reader.readBoolean();
            case NULL:
                reader.readNull();
                return "null";
            case END_DOCUMENT:
                return null;
        }
        throw new AssertionError(token);
    }

    interface Action {
        void run() throws IOException;
    }

    private static void assertError(String expected, Action action) {
        assertEquals(expected, getError(action));
    }

    private static String getError(Action action) {
        try {
            action.run();
        } catch (InvalidJsonException e) {
            assertEquals(e.getDescription() + " at offset " + e.getOffset(), e.getMessage());
            return e.getMessage();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        throw new AssertionError("expected an error");
    }

    /**
     * Returns at most a few bytes per read to exercise refills at every position
     */
    public static class SlowInputStream extends InputStream {

        public SlowInputStream(byte[] bytes, int maxChunk) {
            this.bytes = bytes;
            this.maxChunk = maxChunk;
        }

        @Override
        public int read() {
            return position < bytes.length ? bytes[position++] & 0xFF : -1;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (position == bytes.length) {
                return -1;
            }
            int n = Math.min(Math.min(len, 1 + random.nextInt(maxChunk)), bytes.length - position);
            System.arraycopy(bytes, position, b, off, n);
            position += n;
            return n;
        }

        final byte[] bytes;
        final int maxChunk;
        final Random random = new Random(0);
        int position = 0;

    }

}
