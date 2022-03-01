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

import us.hebi.quickbuf.JsonEncoding.Base64Encoding;
import us.hebi.quickbuf.JsonEncoding.BooleanEncoding;
import us.hebi.quickbuf.JsonEncoding.NumberEncoding;
import us.hebi.quickbuf.JsonEncoding.StringEncoding;
import us.hebi.quickbuf.ProtoUtil.Charsets;

import java.io.IOException;
import java.util.Arrays;

/**
 * Json output using a custom encoder that does not require
 * any intermediate memory allocations.
 *
 * @author Florian Enner
 * @since 26 Oct 2019
 */
public class JsonSink extends AbstractJsonSink<JsonSink> {

    /**
     * Create a new {@code JsonSink} that writes directly to the
     * given output. If more bytes are written than fit in the
     * array, the size will grow as needed.
     * <p>
     * Note that growing is quite inefficient, so the buffers
     * should have an appropriate initial size and be reused.
     */
    public static JsonSink newInstance(RepeatedByte output) {
        return new JsonSink().wrap(output);
    }

    /**
     * Create a new {@code JsonSink} with a new internal buffer. The
     * size of the buffer will grow as needed. The resulting buffer
     * can be accessed with {@link JsonSink#getBuffer()}
     * <p>
     * The output is minimized JSON without extra whitespace for
     * sending data over the wire.
     */
    public static JsonSink newInstance() {
        return newInstance(RepeatedByte.newEmptyInstance());
    }

    /**
     * Create a new {@code JsonSink} with a new internal buffer. The
     * size of the buffer will grow as needed. The resulting buffer
     * can be accessed with {@link JsonSink#getBuffer()}
     * <p>
     * The output contains extra whitespace to improve human readability
     */
    public static JsonSink newPrettyInstance() {
        return newInstance(RepeatedByte.newEmptyInstance())
                .setPretty(true)
                .setWriteEnumStrings(true);
    }

    // ==================== Extra API ====================

    @Override
    public JsonSink setWriteEnumStrings(boolean value) {
        super.setWriteEnumStrings(value);
        return this;
    }

    /**
     * Changes the output to the given buffer
     *
     * @param output new output buffer
     * @return this
     */
    public JsonSink wrap(RepeatedByte output) {
        if (output == null)
            throw new NullPointerException();
        this.output = output;
        return this;
    }

    /**
     * Clears the internal buffer
     *
     * @return this
     */
    public JsonSink clear() {
        this.output.clear();
        return this;
    }

    /**
     * Sets the output to be pretty printed (newlines and spaces) to be
     * more human readable, or minified (default without extra characters)
     * to be more efficient.
     *
     * @param pretty true to format the result more human readable
     * @return this
     */
    public JsonSink setPretty(boolean pretty) {
        this.pretty = pretty;
        return this;
    }

    /**
     * Ensures that the underlying buffer can hold at least
     * the desired number of bytes.
     *
     * @param length
     * @return this
     */
    public JsonSink reserve(int length) {
        output.reserve(length);
        return this;
    }

    /**
     * @return internal output buffer
     */
    public RepeatedByte getBuffer() {
        return output;
    }

    public JsonSink writeMessage(ProtoMessage value) {
        try {
            value.writeTo(this);
        } catch (IOException e) {
            IllegalStateException unexpected = new IllegalStateException("IOException while writing to memory");
            unexpected.initCause(e);
            throw unexpected;
        }
        return this;
    }

    // ==================== Encoding Implementations ====================

    @Override
    protected void writeFieldName(final FieldName name) {
        final byte[] key = name.getJsonKeyBytes();
        final int pos = output.addLength(key.length);
        System.arraycopy(key, 0, output.array, pos, key.length);
        writeSpaceAfterFieldName();
    }

    @Override
    protected void writeNumber(double value) {
        NumberEncoding.writeDouble(value, output);
        writeMore();
    }

    @Override
    protected void writeNumber(float value) {
        NumberEncoding.writeFloat(value, output);
        writeMore();
    }

    @Override
    protected void writeNumber(long value) {
        NumberEncoding.writeLong(value, output);
        writeMore();
    }

    @Override
    protected void writeNumber(int value) {
        NumberEncoding.writeInt(value, output);
        writeMore();
    }

    @Override
    protected void writeBoolean(boolean value) {
        BooleanEncoding.writeBoolean(value, output);
        writeMore();
    }

    @Override
    protected void writeString(Utf8String value) {
        StringEncoding.writeQuotedUtf8(value, output);
        writeMore();
    }

    @Override
    protected void writeString(CharSequence value) {
        StringEncoding.writeQuotedUtf8(value, output);
        writeMore();
    }

    @Override
    protected void writeBinary(RepeatedByte value) {
        Base64Encoding.writeQuotedBase64(value.array, value.length, output);
        writeMore();
    }

    @Override
    public void writeMessageValue(ProtoMessage<?> value) throws IOException {
        value.writeTo(this);
        writeMore();
    }

    @Override
    public JsonSink beginObject() {
        isEmptyObjectOrArray = true;
        writeChar('{');
        indentLevel++;
        writeNewline();
        return this;
    }

    @Override
    public JsonSink endObject() {
        indentLevel--;
        removeTrailingComma();
        if (!isEmptyObjectOrArray) {
            writeNewline();
        }
        writeChar('}');
        return this;
    }

    @Override
    protected void beginArray() {
        isEmptyObjectOrArray = true;
        writeChar('[');
        indentLevel++;
        writeNewline();
    }

    @Override
    protected void endArray() {
        removeTrailingComma();
        indentLevel--;
        if (!isEmptyObjectOrArray) {
            writeNewline();
        }
        writeChar(']');
        writeMore();
    }

    @Override
    protected JsonSink thisObj() {
        return this;
    }

    // ==================== Utilities ====================

    private final void writeMore() {
        isEmptyObjectOrArray = false;
        final int pos = output.length;
        writeChar(',');
        writeNewline();
        trailingSpace = output.length - pos;
    }

    private final void removeTrailingComma() {
        // Called after at least one character, so no need to check bounds
        output.length -= trailingSpace;
        trailingSpace = 0;
    }

    protected final void writeSpaceAfterFieldName() {
        if (pretty) {
            writeChar(' ');
        }
    }

    private final void writeNewline() {
        if (pretty) {
            final int numSpaces = indentLevel * SPACES_PER_LEVEL;
            int pos = output.addLength(numSpaces + 1);
            output.array[pos++] = '\n';
            Arrays.fill(output.array, pos, output.length, (byte) ' ');
            trailingSpace = numSpaces + 1;
        } else {
            trailingSpace = 0;
        }
    }

    private final void writeChar(char c) {
        final int pos = output.addLength(1);
        output.array[pos] = (byte) c;
    }

    @Override
    public String toString() {
        return new String(output.array, 0, output.length, Charsets.UTF_8);
    }

    @Override
    public void close() throws IOException {
        output.extendCapacityTo(0);
    }

    @Override
    public void flush() throws IOException {
    }

    protected JsonSink() {
    }

    protected RepeatedByte output;
    protected boolean pretty = false;
    protected int indentLevel = 0;
    protected int trailingSpace = 0;
    private boolean isEmptyObjectOrArray = false;

    private static final int SPACES_PER_LEVEL = 2;

}
