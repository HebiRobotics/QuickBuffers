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

import us.hebi.quickbuf.JsonEncoding.Base64Encoding;
import us.hebi.quickbuf.JsonEncoding.BooleanEncoding;
import us.hebi.quickbuf.JsonEncoding.NumberEncoding;
import us.hebi.quickbuf.JsonEncoding.StringEncoding;
import us.hebi.quickbuf.ProtoUtil.Charsets;

import java.util.Arrays;

/**
 * Streaming writer for UTF-8 encoded JSON that works without protobuf messages, e.g.,
 * <pre>{@code
 * JsonWriter writer = JsonWriter.newInstance();
 * writer.beginObject()
 *         .name("id").value(42)
 *         .name("tags").beginArray().value("a").value("b").endArray()
 *         .endObject();
 * byte[] utf8 = writer.getBytes().toArray();
 * }</pre>
 * Commas are inserted based on the nesting, so values can be nested in any combination
 * and written at the top level. Consecutive top-level values are written without a
 * separator. Names outside of objects and mismatched ends throw an {@link IllegalStateException}.
 * Names and values are not checked to alternate, which would make every value more
 * expensive, so each name needs to be followed by exactly one value.
 * <p>
 * Strings get escaped as needed, and NaN and infinite numbers are written as the
 * strings "NaN", "Infinity", and "-Infinity". Pretty printing puts each object member
 * on a new line indented by two spaces per object level, and keeps arrays on one line.
 * <p>
 * The output is a growing {@link RepeatedByte} that can be reused after {@link #clear()},
 * so nothing gets allocated in steady state.
 *
 * @author Florian Enner
 * @since 29 Sep 2026
 */
public final class JsonWriter {

    /**
     * Creates a writer that appends minimized JSON to the given output
     */
    public static JsonWriter newInstance(RepeatedByte output) {
        return new JsonWriter().setOutput(output);
    }

    /**
     * Creates a writer with a new internal buffer for minimized JSON
     */
    public static JsonWriter newInstance() {
        return newInstance(RepeatedByte.newEmptyInstance());
    }

    /**
     * Creates a writer with a new internal buffer for human readable JSON
     */
    public static JsonWriter newPrettyInstance() {
        return newInstance().setPrettyPrinting(true);
    }

    private JsonWriter() {
    }

    /**
     * Changes the output to the given bytes and resets the nesting state
     */
    public JsonWriter setOutput(RepeatedByte output) {
        this.output = ProtoUtil.checkNotNull(output);
        return reset();
    }

    public JsonWriter setPrettyPrinting(boolean prettyPrinting) {
        this.pretty = prettyPrinting;
        return this;
    }

    /**
     * Clears the output and resets the nesting state
     */
    public JsonWriter clear() {
        output.clear();
        return reset();
    }

    /**
     * Ensures that the output can hold at least the desired number of additional bytes
     */
    public JsonWriter reserve(int length) {
        output.reserve(length);
        return this;
    }

    /**
     * @return the current output as raw utf8 bytes
     */
    public RepeatedByte getBytes() {
        return output;
    }

    /**
     * @return the current output as char sequence
     */
    public CharSequence getChars() {
        return toString();
    }

    @Override
    public String toString() {
        return new String(output.array, 0, output.length, Charsets.UTF_8);
    }

    // ==================== Structure ====================

    public JsonWriter beginObject() {
        beforeValue();
        writeByte('{');
        push(SCOPE_EMPTY_OBJECT);
        return this;
    }

    public JsonWriter endObject() {
        if (scope < SCOPE_EMPTY_OBJECT) {
            throw illegalState("Not inside an object");
        }
        pop();
        writeNewline();
        writeByte('}');
        return this;
    }

    public JsonWriter beginArray() {
        beforeValue();
        writeByte('[');
        push(SCOPE_EMPTY_ARRAY);
        return this;
    }

    public JsonWriter endArray() {
        if (scope != SCOPE_EMPTY_ARRAY && scope != SCOPE_NONEMPTY_ARRAY) {
            throw illegalState("Not inside an array");
        }
        pop();
        writeByte(']');
        return this;
    }

    public JsonWriter name(CharSequence name) {
        if (beforeName() != 0) {
            writeByte(',');
        }
        writeNewline();
        StringEncoding.writeQuotedUtf8(name, output);
        writeByte(':');
        writeSpace();
        return this;
    }

    /**
     * Writes the pre-encoded json name
     */
    public JsonWriter name(FieldName name) {
        return name(name.getJsonKeyBytes());
    }

    /**
     * @param key utf8 encoded bytes including the quotes and the colon
     */
    JsonWriter name(byte[] key) {
        final int comma = beforeName();
        if (pretty) {
            return prettyName(key, comma);
        }
        final int pos = output.addLength(comma + key.length);
        final byte[] array = output.array;
        array[pos] = ','; // gets overwritten by the key if there is no comma
        System.arraycopy(key, 0, array, pos + comma, key.length);
        return this;
    }

    private JsonWriter prettyName(byte[] key, int comma) {
        if (comma != 0) {
            writeByte(',');
        }
        writeIndentedNewline();
        final int pos = output.addLength(key.length);
        System.arraycopy(key, 0, output.array, pos, key.length);
        writeByte(' ');
        return this;
    }

    // ==================== Values ====================

    public JsonWriter value(double value) {
        beforeValue();
        floatEncoder.writeDouble(value, output);
        return this;
    }

    public JsonWriter value(float value) {
        beforeValue();
        floatEncoder.writeFloat(value, output);
        return this;
    }

    public JsonWriter value(long value) {
        beforeValue();
        NumberEncoding.writeLong(value, output);
        return this;
    }

    public JsonWriter value(int value) {
        beforeValue();
        NumberEncoding.writeInt(value, output);
        return this;
    }

    public JsonWriter value(boolean value) {
        beforeValue();
        BooleanEncoding.writeBoolean(value, output);
        return this;
    }

    public JsonWriter value(CharSequence value) {
        beforeValue();
        StringEncoding.writeQuotedUtf8(value, output);
        return this;
    }

    public JsonWriter value(Utf8String value) {
        beforeValue();
        if (value.hasBytes()) {
            StringEncoding.writeQuotedUtf8(value, output);
        } else {
            StringEncoding.writeQuotedUtf8(value.getString(), output);
        }
        return this;
    }

    public JsonWriter nullValue() {
        beforeValue();
        output.addAll(NULL);
        return this;
    }

    JsonWriter unsignedValue(long value) {
        beforeValue();
        NumberEncoding.writeUnsignedLong(value, output);
        return this;
    }

    JsonWriter base64Value(RepeatedByte value) {
        beforeValue();
        Base64Encoding.writeQuotedBase64(value.array, value.length, output);
        return this;
    }

    // ==================== Implementation ====================

    // Hot methods stay below the default JIT inline size of 35 bytes, uncommon cases get moved out

    /**
     * @return the number of commas that need to be written before the name
     */
    private int beforeName() {
        if (scope == SCOPE_NONEMPTY_OBJECT) {
            return 1;
        } else if (scope == SCOPE_EMPTY_OBJECT) {
            scope = SCOPE_NONEMPTY_OBJECT;
            return 0;
        }
        throw illegalState("Names are only allowed inside objects");
    }

    private void beforeValue() {
        if (scope < SCOPE_EMPTY_OBJECT) {
            beforeArrayOrTopLevelValue();
        }
    }

    private void beforeArrayOrTopLevelValue() {
        if (scope == SCOPE_NONEMPTY_ARRAY) {
            writeByte(',');
            writeSpace();
        } else if (scope == SCOPE_EMPTY_ARRAY) {
            scope = SCOPE_NONEMPTY_ARRAY;
        }
    }

    private static IllegalStateException illegalState(String message) {
        return new IllegalStateException(message);
    }

    private void push(int newScope) {
        if (depth == stack.length) {
            growStack();
        }
        stack[depth++] = scope;
        scope = newScope;
    }

    private void growStack() {
        stack = Arrays.copyOf(stack, depth * 2);
    }

    private void pop() {
        scope = stack[--depth];
    }

    private JsonWriter reset() {
        depth = 0;
        scope = SCOPE_DOCUMENT;
        return this;
    }

    private void writeSpace() {
        if (pretty) {
            writeByte(' ');
        }
    }

    private void writeNewline() {
        if (pretty) {
            writeIndentedNewline();
        }
    }

    private void writeIndentedNewline() {
        int objectDepth = scope >= SCOPE_EMPTY_OBJECT ? 1 : 0;
        for (int i = 0; i < depth; i++) {
            objectDepth += stack[i] >= SCOPE_EMPTY_OBJECT ? 1 : 0;
        }
        final int numSpaces = objectDepth * SPACES_PER_LEVEL;
        int pos = output.addLength(numSpaces + 1);
        output.array[pos++] = '\n';
        Arrays.fill(output.array, pos, output.length, (byte) ' ');
    }

    private void writeByte(char c) {
        final int pos = output.addLength(1);
        output.array[pos] = (byte) c;
    }

    // objects last so that checks and the indentation can use a single comparison
    private static final int SCOPE_DOCUMENT = 0;
    private static final int SCOPE_EMPTY_ARRAY = 1;
    private static final int SCOPE_NONEMPTY_ARRAY = 2;
    private static final int SCOPE_EMPTY_OBJECT = 3;
    private static final int SCOPE_NONEMPTY_OBJECT = 4;
    private static final int SPACES_PER_LEVEL = 2;
    private static final byte[] NULL = {'n', 'u', 'l', 'l'};

    private RepeatedByte output;
    private boolean pretty = false;
    private int scope = SCOPE_DOCUMENT;
    private int[] stack = new int[16];
    private int depth = 0;
    private final JsonEncoding.FloatEncoder floatEncoder = JsonEncoding.getFloatEncoder();

}
