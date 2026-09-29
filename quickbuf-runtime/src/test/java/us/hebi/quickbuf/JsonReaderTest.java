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
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
import static us.hebi.quickbuf.JsonReader.Token.*;

/**
 * @author Florian Enner
 * @since 29 Sep 2026
 */
public class JsonReaderTest {

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
    public void testByteOrderMark() throws IOException {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, '[', ']'};
        assertEquals(Arrays.asList("[", "]"), tokens(JsonReader.newInstance(bom)));
        assertEquals(Arrays.asList("[", "]"), tokens(JsonReader.newInstance(new SlowInputStream(bom, 1))));

        byte[] invalid = {(byte) 0xEF, (byte) 0xBB, '[', ']'};
        assertError("Invalid byte order mark at offset 2", () -> JsonReader.newInstance(invalid).peek());

        byte[] afterWhitespace = {' ', (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, '[', ']'};
        assertError("Expected a value but got '\u00ef' at offset 1", () -> JsonReader.newInstance(afterWhitespace).peek());
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
        assertError("Unescaped control character 0x0a in string at offset 2", () -> JsonReader.newInstance("[\"\n\"]").skipValue());
        assertError("Unexpected end of input at offset 4", () -> JsonReader.newInstance("\"abc").readString());

        // unpaired surrogates can be represented in Java strings, but not in UTF-8
        assertEquals("\ud800", JsonReader.newInstance("\"\\ud800\"").readString());
        assertError("Unpaired surrogate in string at offset 7", () -> JsonReader.newInstance("\"\\ud800\"").readString(Utf8String.newEmptyInstance()));
        assertError("Unpaired surrogate in string at offset 6", () -> JsonReader.newInstance("\"\\udc00\"").readString(Utf8String.newEmptyInstance()));
        assertError("Unpaired surrogate in string at offset 12", () -> JsonReader.newInstance("\"\\ud800\\u0041\"").readString(Utf8String.newEmptyInstance()));
    }

    @Test
    public void testInvalidUtf8() throws IOException {
        byte[][] inputs = {
                {'"', 'a', (byte) 0xE4, (byte) 0xB8, '"'},
                {'"', (byte) 0x80, '"'},
                {'"', (byte) 0xC0, (byte) 0x80, '"'},
                {'"', (byte) 0xE0, (byte) 0x9F, (byte) 0xBF, '"'},
                {'"', (byte) 0xED, (byte) 0xA0, (byte) 0x80, '"'},
                {'"', (byte) 0xF0, (byte) 0x8F, (byte) 0xBF, (byte) 0xBF, '"'},
                {'"', (byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80, '"'},
                {'"', (byte) 0xF5, (byte) 0x80, (byte) 0x80, (byte) 0x80, '"'},
                {'"', (byte) 0xFF, '"'},
        };
        for (byte[] input : inputs) {
            String error = getError(() -> JsonReader.newInstance(input).readString());
            assertTrue(error, error.startsWith("Invalid UTF-8 in string"));
            assertEquals(error, getError(() -> JsonReader.newInstance(input).readString(Utf8String.newEmptyInstance())));
            assertEquals(error, getError(() -> JsonReader.newInstance(new SlowInputStream(input, 1)).readString(Utf8String.newEmptyInstance())));

            // lenient mode copies raw bytes like JsonSource always did
            JsonReader reader = JsonReader.newInstance(input);
            reader.lenient = true;
            RepeatedByte raw = RepeatedByte.newEmptyInstance();
            reader.readString(raw);
            assertArrayEquals(Arrays.copyOfRange(input, 1, input.length - 1), raw.toArray());
        }

        String valid = new StringBuilder("a").appendCodePoint(0x7F).appendCodePoint(0x80).appendCodePoint(0x7FF).appendCodePoint(0x800)
                .appendCodePoint(0xD7FF).appendCodePoint(0xE000).appendCodePoint(0xFFFF).appendCodePoint(0x10000).appendCodePoint(0x10FFFF).toString();
        byte[] json = ("[\"" + valid + "\",\"" + valid + "\"]").getBytes(Charsets.UTF_8);
        for (int maxChunk = 1; maxChunk <= 3; maxChunk++) {
            JsonReader reader = JsonReader.newInstance(new SlowInputStream(json, maxChunk)).beginArray();
            Utf8String utf8 = Utf8String.newEmptyInstance();
            reader.readString(utf8);
            assertEquals(valid, utf8.getString());
            assertEquals(valid, reader.readString());
        }
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
        assertEquals(Double.POSITIVE_INFINITY, JsonReader.newInstance("1e400").readDouble(), 0);

        // 2^53+1 rounds to the nearest double, but stays exact as a long
        assertEquals(9007199254740992.0, JsonReader.newInstance("9007199254740993").readDouble(), 0);
        assertEquals(9007199254740993L, JsonReader.newInstance("9007199254740993").readLong());

        assertEquals(Long.MAX_VALUE, JsonReader.newInstance("9223372036854775807").readLong());
        assertEquals(Long.MIN_VALUE, JsonReader.newInstance("-9223372036854775808").readLong());
        assertEquals(Integer.MAX_VALUE, JsonReader.newInstance("2147483647").readInt());
        assertEquals(Integer.MIN_VALUE, JsonReader.newInstance("-2147483648").readInt());

        // integral values with fractions or exponents like protobuf's JsonFormat
        assertEquals(1, JsonReader.newInstance("1.0").readInt());
        assertEquals(1000, JsonReader.newInstance("1e3").readInt());
        assertEquals(1500, JsonReader.newInstance("1.500e3").readLong());
        assertEquals(-120, JsonReader.newInstance("-1200E-1").readLong());
        assertEquals(0, JsonReader.newInstance("-0.0").readInt());
        assertEquals(0, JsonReader.newInstance("0e999999999").readInt());
        assertEquals(Integer.MIN_VALUE, JsonReader.newInstance("-2.147483648e9").readInt());
        assertEquals(Long.MIN_VALUE, JsonReader.newInstance("-9.223372036854775808e18").readLong());
        assertEquals(Long.MAX_VALUE, JsonReader.newInstance("922337203685477580.7e1").readLong());
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
        assertError("Expected a number but got '.' at offset 0", () -> JsonReader.newInstance(".5").readDouble());
        assertError("Expected a value but got '+' at offset 0", () -> JsonReader.newInstance("+1").peek());
        assertError("Expected a number but got 'N' at offset 1", () -> JsonReader.newInstance("[NaN]").beginArray().readDouble());
        assertError("Expected a number but got '\"' at offset 0", () -> JsonReader.newInstance("\"1\"").readInt());
    }

    @Test
    public void testIntegerRanges() throws IOException {
        assertError("Expected an integer but got '1.5' at offset 1", () -> JsonReader.newInstance("[1.5]").beginArray().readInt());
        assertError("Expected an integer but got '1e-1' at offset 0", () -> JsonReader.newInstance("1e-1").readLong());
        assertError("Expected an integer but got '1.00000000000000000001' at offset 0", () -> JsonReader.newInstance("1.00000000000000000001").readLong());
        assertError("Value out of range for int '2147483648.0' at offset 0", () -> JsonReader.newInstance("2147483648.0").readInt());
        assertError("Value out of range for int '1e10' at offset 0", () -> JsonReader.newInstance("1e10").readInt());
        assertError("Value out of range for long '9.3e18' at offset 0", () -> JsonReader.newInstance("9.3e18").readLong());
        assertError("Value out of range for long '1e999999999' at offset 0", () -> JsonReader.newInstance("1e999999999").readLong());
        assertError("Value out of range for int '2147483648' at offset 0", () -> JsonReader.newInstance("2147483648").readInt());
        assertError("Value out of range for int '-2147483649' at offset 0", () -> JsonReader.newInstance("-2147483649").readInt());
        assertError("Value out of range for int '123456789012345678901' at offset 0", () -> JsonReader.newInstance("123456789012345678901").readInt());
        assertError("Value out of range for long '9223372036854775808' at offset 0", () -> JsonReader.newInstance("9223372036854775808").readLong());
        assertError("Value out of range for long '-9223372036854775809' at offset 0", () -> JsonReader.newInstance("-9223372036854775809").readLong());
    }

    @Test
    public void testInvalidStructure() throws IOException {
        assertInvalid("[1,]", "Trailing comma before ']' at offset 3");
        assertInvalid("[,1]", "Expected a value but got ',' at offset 1");
        assertInvalid("[1 2]", "Expected ',' or ']' but got '2' at offset 3");
        assertInvalid("[1,,2]", "Expected a value but got ',' at offset 3");
        assertInvalid("{\"a\":1,}", "Trailing comma before '}' at offset 7");
        assertInvalid("{,\"a\":1}", "Expected a name or '}' but got ',' at offset 1");
        assertInvalid("{\"a\":1 \"b\":2}", "Expected ',' or '}' but got '\"' at offset 7");
        assertInvalid("{\"a\":1,,\"b\":2}", "Expected a name but got ',' at offset 7");
        assertInvalid("{\"a\" 1}", "Expected ':' but got '1' at offset 5");
        assertInvalid("{\"a\"::1}", "Expected a value but got ':' at offset 5");
        assertInvalid("{\"a\":}", "Expected a value but got '}' at offset 5");
        assertInvalid("{'a':1}", "Expected a name or '}' but got ''' at offset 1");
        assertInvalid("{a:1}", "Expected a name or '}' but got 'a' at offset 1");
        assertInvalid("{1:1}", "Expected a name or '}' but got '1' at offset 1");
        assertInvalid("[1}", "Expected ',' or ']' but got '}' at offset 2");
        assertInvalid("{\"a\":1]", "Expected ',' or '}' but got ']' at offset 6");
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
        assertInvalid("{\"a\"", "Expected ':' but reached the end of the input at offset 4");
        assertInvalid("{\"a\":", "Expected a value but reached the end of the input at offset 5");
        assertInvalid("{\"a\":1", "Expected ',' or '}' but reached the end of the input at offset 6");
        assertInvalid("{\"a", "Unexpected end of input at offset 3");
        assertInvalid("[\"a\\", "Unexpected end of input at offset 4");
        assertInvalid("[\"\\u00", "Unexpected end of input at offset 6");
        assertInvalid("tru", "Unexpected end of input at offset 3");
    }

    @Test
    public void testSkipValueMismatch() throws IOException {
        assertError("Expected ',' or '}' but got ']' at offset 10", () -> JsonReader.newInstance("{\"a\":[1,2]]}").skipValue());
        assertError("Expected ',' or ']' but got '}' at offset 5", () -> JsonReader.newInstance("[[1,2}]").skipValue());
        assertError("Expected a value but got '}' at offset 11", () -> JsonReader.newInstance("{\"a\":{\"b\":[}]}").skipValue());
        assertError("Expected ':' but got ',' at offset 11", () -> JsonReader.newInstance("[{\"a\":1,\"b\",2}]").skipValue());
        assertError("Trailing comma before ']' at offset 4", () -> JsonReader.newInstance("[[1,]]").skipValue());
        assertError("Expected ',' or ']' but reached the end of the input at offset 6", () -> JsonReader.newInstance("[[1,2]").skipValue());
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
        assertError("Unescaped control character 0x0a in string at offset 8",
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

        // structure stays strict
        JsonReader invalid = JsonReader.newInstance("{\"x\":[NaN,]}");
        invalid.lenient = true;
        assertError("Trailing comma before ']' at offset 10", invalid::skipValue);
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

        // same offsets as array input
        byte[] invalid = (json.substring(0, 9000) + ",]").getBytes(Charsets.UTF_8);
        String error = getError(() -> tokens(JsonReader.newInstance(invalid)));
        assertEquals(error, getError(() -> tokens(JsonReader.newInstance(new SlowInputStream(invalid, 3)))));
        assertEquals(error, getError(() -> tokens(JsonReader.newInstance(new ByteArrayInputStream(invalid)))));
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
