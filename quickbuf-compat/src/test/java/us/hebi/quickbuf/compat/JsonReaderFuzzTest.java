/*-
 * #%L
 * quickbuf-compat
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

package us.hebi.quickbuf.compat;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.junit.Test;
import us.hebi.quickbuf.InvalidJsonException;
import us.hebi.quickbuf.JsonReader;
import us.hebi.quickbuf.JsonReaderTest;
import us.hebi.quickbuf.JsonReaderTest.SlowInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Compares the token streams and accept/reject decisions of the JsonReader with
 * Jackson's strict default parser for random documents and random mutations of them.
 * <p>
 * Inputs are a single root value, as Jackson requires whitespace after root-level numbers.
 * <p>
 * Jackson accepts invalid UTF-8 and unpaired surrogate escapes in strings, the reader rejects both.
 *
 * @author Florian Enner
 * @since 29 Sep 2026
 */
public class JsonReaderFuzzTest {

    @Test
    public void testRandomDocuments() throws IOException {
        Random rnd = new Random(0);
        for (int i = 0; i < 5000; i++) {
            StringBuilder json = new StringBuilder();
            whitespace(rnd, json);
            randomValue(rnd, json, 0);
            whitespace(rnd, json);
            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            assertAccepted(bytes);
            compare(bytes);
            for (int m = 0; m < 8; m++) {
                compare(mutate(rnd, bytes));
            }
        }
        assertTrue("too few inputs were rejected: " + rejected, rejected > 10000);
        assertTrue("too few inputs were accepted: " + accepted, accepted > 10000);
    }

    @Test
    public void testHandwrittenInputs() throws IOException {
        String[] inputs = {
                "", " ", "{}", "[]", "1", "-0", "-0.0", "0e1", "1E-2", "01", "-01", "1.", ".5", "+1", "-", "1e", "1e+", "0x10",
                "NaN", "Infinity", "-Infinity", "true", "tru", "truex", "True", "null", "nul", "nulll", "\"a\"", "\"a", "'a'",
                "[1,]", "[,1]", "[1 2]", "[1,,2]", "{\"a\":1,}", "{,}", "{\"a\" 1}", "{\"a\"::1}", "{\"a\":}", "{a:1}",
                "[1}", "{\"a\":1]", "]", "[]]", "[[]", "{}}", "{} x", "1 x", "[\"\\x\"]", "[\"\\u00\"]", "[\"\\u00e9\"]",
                "[\"\\ud83d\\ude00\"]", "[\"\\ud800\"]", "[\"\\udc00\"]", "[\"\\/\"]", "[\"\t\"]", "[\"\u007f\"]",
                "\ufeff[]", " \ufeff[]", "[\u00a0]", "[\u000b1]", "[1\f]", "{\"a\":{\"b\":{\"c\":[[[{}]]]}}}",
                "12345678901234567890123", "1e400", "-1e400", "4.9e-324", "2.4703282292062328e-324", "9007199254740993",
        };
        for (String input : inputs) {
            compare(input.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void compare(byte[] input) throws IOException {
        Result expected = parseJackson(input);
        Result actual = parseReader(JsonReader.newInstance(input));
        if (expected.accepted != actual.accepted || (actual.accepted && !expected.tokens.equals(actual.tokens))) {
            boolean knownUtf8Divergence = expected.accepted && !actual.accepted && actual.error.startsWith("Invalid UTF-8");
            boolean knownSurrogateDivergence = expected.accepted && !actual.accepted && actual.error.startsWith("Unpaired surrogate");
            if (!knownUtf8Divergence && !knownSurrogateDivergence) {
                fail("divergence for input '" + new String(input, StandardCharsets.UTF_8) + "'\n" +
                        "jackson: " + expected + "\n" +
                        "reader:  " + actual);
            }
        }
        if (actual.accepted) {
            accepted++;
        } else {
            rejected++;
        }

        // streams with refills at every position must behave like arrays
        Result streamed = parseReader(JsonReader.newInstance(new SlowInputStream(input, 3)));
        assertEquals(actual.toString(), streamed.toString());
    }

    private static Result parseJackson(byte[] input) throws IOException {
        Result result = new Result();
        try (JsonParser parser = factory.createParser(input)) {
            int level = 0;
            do {
                JsonToken token = parser.nextToken();
                if (token == null) {
                    result.fail("empty input");
                    return result;
                }
                switch (token) {
                    case START_OBJECT:
                        result.tokens.add("{");
                        level++;
                        break;
                    case END_OBJECT:
                        result.tokens.add("}");
                        level--;
                        break;
                    case START_ARRAY:
                        result.tokens.add("[");
                        level++;
                        break;
                    case END_ARRAY:
                        result.tokens.add("]");
                        level--;
                        break;
                    case FIELD_NAME:
                        result.tokens.add("name:" + parser.getText());
                        break;
                    case VALUE_STRING:
                        result.tokens.add("string:" + parser.getText());
                        break;
                    case VALUE_NUMBER_INT:
                    case VALUE_NUMBER_FLOAT:
                        if (Double.isInfinite(parser.getDoubleValue())) {
                            result.fail("out of range"); // rejected by the reader like protobuf does
                            return result;
                        }
                        result.tokens.add(number(parser.getDoubleValue()));
                        break;
                    case VALUE_TRUE:
                    case VALUE_FALSE:
                        result.tokens.add("bool:" + parser.getBooleanValue());
                        break;
                    case VALUE_NULL:
                        result.tokens.add("null");
                        break;
                    default:
                        throw new AssertionError(token);
                }
            } while (level > 0);
            if (parser.nextToken() != null) {
                result.fail("trailing content");
                return result;
            }
            result.accepted = true;
        } catch (IOException e) {
            result.fail(e.getMessage());
        }
        return result;
    }

    private static Result parseReader(JsonReader reader) throws IOException {
        Result result = new Result();
        try {
            if (reader.peek() == JsonReader.Token.END_DOCUMENT) {
                result.fail("empty input");
                return result;
            }
            int level = 0;
            do {
                String token = JsonReaderTest.readToken(reader);
                if (token.startsWith("number:")) {
                    token = number(Double.parseDouble(token.substring("number:".length())));
                } else if (token.equals("{") || token.equals("[")) {
                    level++;
                } else if (token.equals("}") || token.equals("]")) {
                    level--;
                }
                result.tokens.add(token);
            } while (level > 0);
            if (reader.peek() != JsonReader.Token.END_DOCUMENT) {
                result.fail("trailing content");
                return result;
            }
            result.accepted = true;
        } catch (InvalidJsonException e) {
            result.fail(e.getMessage());
        }
        return result;
    }

    private static String number(double value) {
        return "number:" + (value == 0 ? 0.0 : value); // Jackson reads -0 as an int without sign
    }

    private static void randomValue(Random rnd, StringBuilder json, int depth) {
        int type = rnd.nextInt(depth < 4 ? 8 : 6);
        switch (type) {
            case 0:
                randomString(rnd, json);
                break;
            case 1:
            case 2:
                randomNumber(rnd, json);
                break;
            case 3:
                json.append("true");
                break;
            case 4:
                json.append("false");
                break;
            case 5:
                json.append("null");
                break;
            case 6: {
                json.append('{');
                int n = rnd.nextInt(5);
                for (int i = 0; i < n; i++) {
                    if (i > 0) {
                        json.append(',');
                    }
                    whitespace(rnd, json);
                    randomString(rnd, json);
                    whitespace(rnd, json);
                    json.append(':');
                    whitespace(rnd, json);
                    randomValue(rnd, json, depth + 1);
                    whitespace(rnd, json);
                }
                if (n == 0) {
                    whitespace(rnd, json);
                }
                json.append('}');
                break;
            }
            default: {
                json.append('[');
                int n = rnd.nextInt(5);
                for (int i = 0; i < n; i++) {
                    if (i > 0) {
                        json.append(',');
                    }
                    whitespace(rnd, json);
                    randomValue(rnd, json, depth + 1);
                    whitespace(rnd, json);
                }
                if (n == 0) {
                    whitespace(rnd, json);
                }
                json.append(']');
            }
        }
    }

    private static void randomString(Random rnd, StringBuilder json) {
        json.append('"');
        int n = rnd.nextInt(6);
        for (int i = 0; i < n; i++) {
            switch (rnd.nextInt(8)) {
                case 0:
                    json.append(ESCAPES[rnd.nextInt(ESCAPES.length)]);
                    break;
                case 1:
                    int c = rnd.nextInt(0x10000 - 0x800);
                    json.append(String.format("\\u%04x", c < 0xD800 ? c : c + 0x800)); // no surrogates
                    break;
                case 2:
                    json.append(RAW[rnd.nextInt(RAW.length)]);
                    break;
                default:
                    json.append((char) (' ' + rnd.nextInt(95)));
                    if (json.charAt(json.length() - 1) == '"' || json.charAt(json.length() - 1) == '\\') {
                        json.setLength(json.length() - 1);
                    }
            }
        }
        json.append('"');
    }

    private static void randomNumber(Random rnd, StringBuilder json) {
        if (rnd.nextBoolean()) {
            json.append('-');
        }
        if (rnd.nextInt(4) == 0) {
            json.append('0');
        } else {
            json.append(1 + rnd.nextInt(9));
            digits(rnd, json, rnd.nextInt(rnd.nextInt(8) == 0 ? 25 : 6));
        }
        if (rnd.nextInt(3) == 0) {
            json.append('.');
            digits(rnd, json, 1 + rnd.nextInt(8));
        }
        if (rnd.nextInt(4) == 0) {
            json.append(rnd.nextBoolean() ? 'e' : 'E');
            int sign = rnd.nextInt(3);
            if (sign > 0) {
                json.append(sign == 1 ? '+' : '-');
            }
            digits(rnd, json, 1 + rnd.nextInt(3));
        }
    }

    private static void digits(Random rnd, StringBuilder json, int count) {
        for (int i = 0; i < count; i++) {
            json.append(rnd.nextInt(10));
        }
    }

    private static void whitespace(Random rnd, StringBuilder json) {
        if (rnd.nextInt(3) == 0) {
            json.append(WHITESPACE[rnd.nextInt(WHITESPACE.length)]);
        }
    }

    private static byte[] mutate(Random rnd, byte[] input) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int pos = input.length == 0 ? 0 : rnd.nextInt(input.length);
        int remaining = input.length - pos;
        switch (rnd.nextInt(7)) {
            case 0: // delete
                out.write(input, 0, pos);
                out.write(input, pos + 1, Math.max(remaining - 1, 0));
                break;
            case 1: // insert
                out.write(input, 0, pos);
                out.write(ALPHABET.charAt(rnd.nextInt(ALPHABET.length())));
                out.write(input, pos, remaining);
                break;
            case 2: // replace
                out.write(input, 0, pos);
                out.write(ALPHABET.charAt(rnd.nextInt(ALPHABET.length())));
                out.write(input, pos + 1, Math.max(remaining - 1, 0));
                break;
            case 3: // truncate
                out.write(input, 0, pos);
                break;
            case 4: // duplicate a range
                int length = Math.min(1 + rnd.nextInt(10), remaining);
                out.write(input, 0, pos + length);
                out.write(input, pos, remaining);
                break;
            case 5: // delete a range
                int deleted = Math.min(1 + rnd.nextInt(10), remaining);
                out.write(input, 0, pos);
                out.write(input, pos + deleted, remaining - deleted);
                break;
            default: // random byte, but no zeros that would make Jackson detect UTF-16 or UTF-32
                out.write(input, 0, pos);
                out.write(1 + rnd.nextInt(255));
                out.write(input, pos + 1, Math.max(remaining - 1, 0));
        }
        return out.toByteArray();
    }

    private static void assertAccepted(byte[] input) throws IOException {
        Result result = parseReader(JsonReader.newInstance(input));
        boolean outOfRange = !result.accepted && result.error.startsWith("Value out of range");
        assertTrue(new String(input, StandardCharsets.UTF_8) + ": " + result, result.accepted || outOfRange);
    }

    static class Result {

        void fail(String error) {
            this.accepted = false;
            this.error = String.valueOf(error);
        }

        @Override
        public String toString() {
            return (accepted ? "accepted " : "rejected (" + error + ") ") + tokens;
        }

        boolean accepted;
        String error = "";
        final List<String> tokens = new ArrayList<>();

    }

    int accepted = 0;
    int rejected = 0;

    private static final JsonFactory factory = new JsonFactory();
    private static final String ALPHABET = "{}[],:\" \t\n0123456789-+.eEtrufalsnx\\/u";
    private static final String[] WHITESPACE = {" ", "\t", "\n", "\r\n", "  "};
    private static final String[] ESCAPES = {"\\\"", "\\\\", "\\/", "\\b", "\\f", "\\n", "\\r", "\\t", "\\ud83d\\ude00"};
    private static final String[] RAW = {"\u00e9", "\u4e2d", "\ud83d\ude00", "\u2028", "\u007f"};

}
