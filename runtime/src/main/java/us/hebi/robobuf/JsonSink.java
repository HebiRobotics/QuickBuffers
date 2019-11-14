/*-
 * #%L
 * robobuf-runtime
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package us.hebi.robobuf;

import us.hebi.robobuf.JsonUtil.Base64Encoding;
import us.hebi.robobuf.JsonUtil.BooleanEncoding;
import us.hebi.robobuf.JsonUtil.NumberEncoding;
import us.hebi.robobuf.JsonUtil.StringEncoding;
import us.hebi.robobuf.ProtoUtil.Charsets;

import java.util.Arrays;

/**
 * Prints proto messages in a JSON compatible format
 *
 * @author Florian Enner
 * @since 26 Oct 2019
 */
public class JsonSink {

    /**
     * Create a new {@code JsonSink} that writes directly to the
     * given output. If more bytes are written than fit in the
     * array, the size will grow as needed.
     *
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
     *
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
     *
     * The output contains extra whitespace to improve human readability
     */
    public static JsonSink newPrettyInstance() {
        return newInstance(RepeatedByte.newEmptyInstance()).setIndentCount(2);
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
     * Sets the indentation count for newlines. If the indent
     * count is zero or negative, prettifying is disabled and
     * the output will be minimized (default).
     *
     * @param indentCount number of spaces for each indent
     * @return this
     */
    public JsonSink setIndentCount(int indentCount) {
        this.indentCount = indentCount;
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
     * Clears the internal buffer
     *
     * @return this
     */
    public JsonSink clear() {
        this.output.clear();
        return this;
    }

    /**
     * @return internal output buffer
     */
    public RepeatedByte getBuffer() {
        return output;
    }

    public JsonSink writeMessage(ProtoMessage value) {
        value.writeTo(this);
        return this;
    }

    public void writeField(byte[] key, boolean value) {
        writeKey(key);
        BooleanEncoding.writeBoolean(value, output);
        writeMore();
    }

    public void writeField(byte[] key, int value) {
        writeKey(key);
        NumberEncoding.writeInt(value, output);
        writeMore();
    }

    public void writeField(byte[] key, long value) {
        writeKey(key);
        NumberEncoding.writeLong(value, output);
        writeMore();
    }

    public void writeField(byte[] key, float value) {
        writeKey(key);
        NumberEncoding.writeFloat(value, output);
        writeMore();
    }

    public void writeField(byte[] key, double value) {
        writeKey(key);
        NumberEncoding.writeDouble(value, output);
        writeMore();
    }

    public void writeField(byte[] key, ProtoMessage value) {
        writeKey(key);
        value.writeTo(this);
        writeMore();
    }

    public void writeField(byte[] key, StringBuilder value) {
        writeKey(key);
        StringEncoding.writeQuotedUtf8(value, output);
        writeMore();
    }

    public void writeField(byte[] key, RepeatedByte value) {
        writeKey(key);
        Base64Encoding.writeQuotedBase64(value.array, value.length, output);
        writeMore();
    }

    public void writeField(byte[] key, RepeatedBoolean value) {
        writeArrayStart(key);
        for (int i = 0; i < value.length; i++) {
            BooleanEncoding.writeBoolean(value.array[i], output);
            writeMore();
        }
        writeArrayEnd();
    }

    public void writeField(byte[] key, RepeatedInt value) {
        writeArrayStart(key);
        for (int i = 0; i < value.length; i++) {
            NumberEncoding.writeInt(value.array[i], output);
            writeMore();
        }
        writeArrayEnd();
    }

    public void writeField(byte[] key, RepeatedLong value) {
        writeArrayStart(key);
        for (int i = 0; i < value.length; i++) {
            NumberEncoding.writeLong(value.array[i], output);
            writeMore();
        }
        writeArrayEnd();
    }

    public void writeField(byte[] key, RepeatedFloat value) {
        writeArrayStart(key);
        for (int i = 0; i < value.length; i++) {
            NumberEncoding.writeFloat(value.array[i], output);
            writeMore();
        }
        writeArrayEnd();
    }

    public void writeField(byte[] key, RepeatedDouble value) {
        writeArrayStart(key);
        for (int i = 0; i < value.length; i++) {
            NumberEncoding.writeDouble(value.array[i], output);
            writeMore();
        }
        writeArrayEnd();
    }

    public void writeField(byte[] key, RepeatedMessage value) {
        writeArrayStart(key);
        for (int i = 0; i < value.length; i++) {
            writeMessage((ProtoMessage) value.array[i]);
            writeMore();
        }
        writeArrayEnd();
    }

    public void writeField(byte[] key, RepeatedString value) {
        writeArrayStart(key);
        for (int i = 0; i < value.length; i++) {
            StringEncoding.writeQuotedUtf8(value.array[i], output);
            writeMore();
        }
        writeArrayEnd();
    }

    public void writeField(byte[] key, RepeatedBytes values) {
        writeArrayStart(key);
        for (int i = 0; i < values.length; i++) {
            final RepeatedByte value = values.get(i);
            Base64Encoding.writeQuotedBase64(value.array, value.length, output);
            writeMore();
        }
        writeArrayEnd();
    }

    protected void writeKey(byte[] key) {
        final int pos = output.addLength(key.length);
        System.arraycopy(key, 0, output.array, pos, key.length);
    }

    public void writeObjectStart() {
        writeChar('{');
        indentLevel++;
        writeNewline();
    }

    public void writeObjectEnd() {
        indentLevel--;
        removeTrailingComma();
        writeNewline();
        writeChar('}');
    }

    protected void writeArrayStart(byte[] key) {
        writeKey(key);
        writeChar('[');
        indentLevel++;
        writeNewline();
    }

    protected void writeArrayEnd() {
        removeTrailingComma();
        indentLevel--;
        writeNewline();
        writeChar(']');
        writeMore();
    }

    private final void writeMore() {
        final int pos = output.length;
        writeChar(',');
        writeNewline();
        trailingSpace = output.length - pos;
    }

    protected void removeTrailingComma() {
        // Called after at least one character, so no need to check bounds
        output.length -= trailingSpace;
        trailingSpace = 0;
    }

    private final void writeNewline() {
        if (indentCount <= 0)
            return;
        final int numSpaces = indentLevel * indentCount;
        int pos = output.addLength(numSpaces + 1);
        output.array[pos++] = '\n';
        Arrays.fill(output.array, pos, output.length, (byte) ' ');
    }

    private final void writeChar(char c) {
        final int pos = output.addLength(1);
        output.array[pos] = (byte) c;
    }

    @Override
    public String toString() {
        return new String(output.array, 0, output.length, Charsets.UTF_8);
    }

    protected JsonSink() {
    }

    protected RepeatedByte output;
    protected int indentLevel = 0;
    protected int indentCount = 0;
    protected int trailingSpace = 0;

}
