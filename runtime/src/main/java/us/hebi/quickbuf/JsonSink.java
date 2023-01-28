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
import us.hebi.quickbuf.schubfach.DoubleToDecimal;
import us.hebi.quickbuf.schubfach.FloatToDecimal;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.Arrays;

/**
 * Prints proto messages in a JSON compatible format
 *
 * @author Florian Enner
 * @since 26 Oct 2019
 */
public abstract class JsonSink implements Closeable, Flushable {

    /**
     * Create a new {@code JsonSink} that writes directly to the
     * given output. If more bytes are written than fit in the
     * array, the size will grow as needed.
     * <p>
     * Note that growing is quite inefficient, so the buffers
     * should have an appropriate initial size and be reused.
     */
    public static JsonSink newInstance(RepeatedByte output) {
        return new DefaultJsonSink().setOutput(output);
    }

    /**
     * Create a new {@code JsonSink} with a new internal buffer. The
     * size of the buffer will grow as needed. The resulting buffer
     * can be accessed with {@link JsonSink#getBytes()}
     * <p>
     * The output is minimized JSON without extra whitespace for
     * sending data over the wire. Enums are serialized as strings
     * by default.
     */
    public static JsonSink newInstance() {
        return newInstance(RepeatedByte.newEmptyInstance());
    }

    /**
     * Create a new {@code JsonSink} with a new internal buffer. The
     * size of the buffer will grow as needed. The resulting buffer
     * can be accessed with {@link JsonSink#getBytes()}
     * <p>
     * The output contains extra whitespace to improve human readability
     */
    public static JsonSink newPrettyInstance() {
        return newInstance(RepeatedByte.newEmptyInstance())
                .setPrettyPrinting(true)
                .setWriteEnumsAsInts(false);
    }

    /**
     * Changes the output to the given bytes. This resets any existing internal state
     * and is equivalent to creating a new instance.
     */
    public JsonSink setOutput(RepeatedByte output) {
        throw new UnsupportedOperationException("JsonSink does not support writing to RepeatedByte");
    }

    /**
     * Ensures that the underlying buffer can hold at least
     * the desired number of bytes.
     */
    public JsonSink reserve(int length) {
        throw new UnsupportedOperationException("JsonSink does not support reserving space");
    }

    /**
     * Clears internal state. This is equivalent to creating a new instance.
     */
    public abstract JsonSink clear();

    /**
     * @return the current output as raw utf8 bytes
     */
    public RepeatedByte getBytes() {
        throw new UnsupportedOperationException("JsonSink does not support access to byte output");
    }

    /**
     * @return the current output as char sequence
     */
    public CharSequence getChars() {
        throw new UnsupportedOperationException("JsonSink does not support access to character output");
    }

    // ==================== Configuration ====================

    /**
     * Serializes enum values as JSON integers rather than human-
     * readable strings. Compatible parsers are able to parse
     * either case.
     * <p>
     * Unknown values will still be serialized as numbers.
     *
     * @param writeEnumsAsInts true if values should use strings
     * @return this
     */
    public JsonSink setWriteEnumsAsInts(final boolean writeEnumsAsInts) {
        this.writeEnumsAsInts = writeEnumsAsInts;
        return this;
    }

    protected boolean writeEnumsAsInts = false;

    /**
     * The serialized JSON object keys map to the value defined by the 'json_name'
     * option, or the lowerCamelCase version of the field name in the proto file.
     * <p>
     * This option lets users choose to preserve the original field names as defined
     * in the proto file. Conforming parsers accept both keys.
     *
     * @param preserveProtoFieldNames true uses the original field names as JSON object keys
     * @return this
     */
    public JsonSink setPreserveProtoFieldNames(final boolean preserveProtoFieldNames) {
        this.preserveProtoFieldNames = preserveProtoFieldNames;
        return this;
    }

    protected boolean preserveProtoFieldNames = false;

    /**
     * Sets the output to be pretty printed (newlines and spaces) for increased
     * readability, or minified (default without whitespace) for efficiency.
     *
     * @param prettyPrinting true adds whitespace to make the result more human-readable
     * @return this
     */
    public JsonSink setPrettyPrinting(boolean prettyPrinting) {
        throw new UnsupportedOperationException("JsonSink does not support setPrettyPrinting");
    }

    // ==================== Common Type Forwarders ====================

    /**
     * Convenience overload for writing to in-memory sinks that don't throw IOException
     */
    public JsonSink writeMessageSilent(ProtoMessage<?> value) {
        try {
            return writeMessage(value);
        } catch (IOException e) {
            throw new AssertionError("silent write should not have errors", e);
        }
    }

    /**
     * Convenience overload for writing to in-memory sinks that don't throw IOException
     */
    public JsonSink writeRepeatedMessageSilent(RepeatedMessage<?> value) {
        try {
            return writeRepeatedMessage(value);
        } catch (IOException e) {
            throw new AssertionError("silent write should not have errors", e);
        }
    }

    /**
     * Writes a top level object {content}
     */
    public JsonSink writeMessage(ProtoMessage<?> value) throws IOException {
        value.writeTo(this);
        return this;
    }

    /**
     * Writes a top-level array [content]
     */
    public JsonSink writeRepeatedMessage(RepeatedMessage<?> value) throws IOException {
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeMessageValue(value.array[i]);
        }
        endArray();
        return this;
    }

    public JsonSink writeFixed64(final FieldName name, final long value) throws IOException {
        return writeInt64(name, value);
    }

    public JsonSink writeSFixed64(final FieldName name, final long value) throws IOException {
        return writeInt64(name, value);
    }

    public JsonSink writeUInt64(final FieldName name, final long value) throws IOException {
        return writeInt64(name, value);
    }

    public JsonSink writeSInt64(final FieldName name, final long value) throws IOException {
        return writeInt64(name, value);
    }

    public JsonSink writeFixed32(final FieldName name, final int value) throws IOException {
        return writeInt32(name, value);
    }

    public JsonSink writeSFixed32(final FieldName name, final int value) throws IOException {
        return writeInt32(name, value);
    }

    public JsonSink writeUInt32(final FieldName name, final int value) throws IOException {
        return writeInt32(name, value);
    }

    public JsonSink writeSInt32(final FieldName name, final int value) throws IOException {
        return writeInt32(name, value);
    }

    public JsonSink writeGroup(final FieldName name, final ProtoMessage value) throws IOException {
        return writeMessage(name, value);
    }

    public JsonSink writeRepeatedFixed64(final FieldName name, final RepeatedLong value) throws IOException {
        return writeRepeatedInt64(name, value);
    }

    public JsonSink writeRepeatedSFixed64(final FieldName name, final RepeatedLong value) throws IOException {
        return writeRepeatedInt64(name, value);
    }

    public JsonSink writeRepeatedUInt64(final FieldName name, final RepeatedLong value) throws IOException {
        return writeRepeatedInt64(name, value);
    }

    public JsonSink writeRepeatedSInt64(final FieldName name, final RepeatedLong value) throws IOException {
        return writeRepeatedInt64(name, value);
    }

    public JsonSink writeRepeatedFixed32(final FieldName name, final RepeatedInt value) throws IOException {
        return writeRepeatedInt32(name, value);
    }

    public JsonSink writeRepeatedSFixed32(final FieldName name, final RepeatedInt value) throws IOException {
        return writeRepeatedInt32(name, value);
    }

    public JsonSink writeRepeatedUInt32(final FieldName name, final RepeatedInt value) throws IOException {
        return writeRepeatedInt32(name, value);
    }

    public JsonSink writeRepeatedSInt32(final FieldName name, final RepeatedInt value) throws IOException {
        return writeRepeatedInt32(name, value);
    }

    public JsonSink writeRepeatedGroup(final FieldName name, final RepeatedMessage<?> value) throws IOException {
        return writeRepeatedMessage(name, value);
    }

    // ==================== Shared Implementations ====================

    public JsonSink writeDouble(final FieldName name, final double value) throws IOException {
        writeFieldName(name);
        writeNumber(value);
        return this;
    }

    public JsonSink writeFloat(final FieldName name, final float value) throws IOException {
        writeFieldName(name);
        writeNumber(value);
        return this;
    }

    public JsonSink writeInt64(final FieldName name, final long value) throws IOException {
        writeFieldName(name);
        writeNumber(value);
        return this;
    }

    public JsonSink writeInt32(final FieldName name, final int value) throws IOException {
        writeFieldName(name);
        writeNumber(value);
        return this;
    }

    public JsonSink writeEnum(final FieldName name, final int value, ProtoEnum.EnumConverter<?> converter) throws IOException {
        writeFieldName(name);
        writeEnumValue(value, converter);
        return this;
    }

    public JsonSink writeBool(final FieldName name, final boolean value) throws IOException {
        writeFieldName(name);
        writeBoolean(value);
        return this;
    }

    public JsonSink writeBytes(final FieldName name, final RepeatedByte value) throws IOException {
        writeFieldName(name);
        writeBinary(value);
        return this;
    }

    public JsonSink writeMessage(final FieldName name, final ProtoMessage<?> value) throws IOException {
        writeFieldName(name);
        writeMessageValue(value);
        return this;
    }

    public JsonSink writeString(final FieldName name, final Utf8String value) throws IOException {
        writeFieldName(name);
        writeString(value);
        return this;
    }

    public JsonSink writeString(final FieldName name, final CharSequence value) throws IOException {
        writeFieldName(name);
        writeString(value);
        return this;
    }

    public JsonSink writeRepeatedDouble(final FieldName name, final RepeatedDouble value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeNumber(value.array[i]);
        }
        endArray();
        return this;
    }

    public JsonSink writeRepeatedInt64(final FieldName name, final RepeatedLong value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeNumber(value.array[i]);
        }
        endArray();
        return this;
    }

    public JsonSink writeRepeatedFloat(final FieldName name, final RepeatedFloat value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeNumber(value.array[i]);
        }
        endArray();
        return this;
    }

    public JsonSink writeRepeatedInt32(final FieldName name, final RepeatedInt value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeNumber(value.array[i]);
        }
        endArray();
        return this;
    }

    public JsonSink writeRepeatedMessage(final FieldName name, final RepeatedMessage<?> value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeMessageValue(value.array[i]);
        }
        endArray();
        return this;
    }

    public JsonSink writeRepeatedString(final FieldName name, final RepeatedString value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeString(value.array[i]);
        }
        endArray();
        return this;
    }

    public JsonSink writeRepeatedEnum(final FieldName name, final RepeatedEnum<?> value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeEnumValue(value.array[i], value.converter);
        }
        endArray();
        return this;
    }

    public JsonSink writeRepeatedBool(final FieldName name, final RepeatedBoolean value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeBoolean(value.array[i]);
        }
        endArray();
        return this;
    }

    public JsonSink writeRepeatedBytes(final FieldName name, final RepeatedBytes value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeBinary(value.array[i]);
        }
        endArray();
        return this;
    }

    protected void writeEnumValue(final int number, final ProtoEnum.EnumConverter<?> converter) throws IOException {
        final ProtoEnum<?> value;
        if (!writeEnumsAsInts && (value = converter.forNumber(number)) != null) {
            writeString(value.getName());
        } else {
            writeNumber(number);
        }
    }

    // ==================== Child Interface ====================

    /**
     * @param name utf8 encoded bytes including end quotes and colon
     */
    protected abstract void writeFieldName(FieldName name) throws IOException;

    protected abstract void writeNumber(double value) throws IOException;

    protected abstract void writeNumber(float value) throws IOException;

    protected abstract void writeNumber(long value) throws IOException;

    protected abstract void writeNumber(int value) throws IOException;

    protected abstract void writeBoolean(boolean value) throws IOException;

    protected abstract void writeString(Utf8String value) throws IOException;

    protected abstract void writeString(CharSequence value) throws IOException;

    protected void writeBinary(RepeatedByte value) throws IOException {
        // Some libraries don't have built-in support for Base64, so we
        // provide a base implementation that first converts to String.
        if (tmpBytes == null) {
            tmpBytes = RepeatedByte.newEmptyInstance();
        }
        tmpBytes.clear();
        Base64Encoding.writeQuotedBase64(value.array, value.length, tmpBytes); // "<content>"
        String str = new String(tmpBytes.array, 1, tmpBytes.length - 2, Charsets.ASCII); // <content>
        writeString(str);
    }

    protected abstract void writeMessageValue(ProtoMessage<?> value) throws IOException;

    public abstract JsonSink beginObject() throws IOException;

    public abstract JsonSink endObject() throws IOException;

    protected abstract void beginArray() throws IOException;

    protected abstract void endArray() throws IOException;

    private RepeatedByte tmpBytes = null;

    /**
     * Json output using a custom encoder that does not require
     * any intermediate memory allocations.
     */
    static class DefaultJsonSink extends JsonSink {

        // ==================== Extra API ====================

        @Override
        public JsonSink setOutput(RepeatedByte output) {
            if (output == null)
                throw new NullPointerException();
            this.output = output;
            return this;
        }

        @Override
        public JsonSink clear() {
            this.output.clear();
            return this;
        }

        @Override
        public JsonSink setPrettyPrinting(boolean prettyPrinting) {
            this.pretty = prettyPrinting;
            return this;
        }

        @Override
        public JsonSink reserve(int length) {
            output.reserve(length);
            return this;
        }

        @Override
        public RepeatedByte getBytes() {
            return output;
        }

        @Override
        public CharSequence getChars() {
            return toString();
        }

        @Override
        public String toString() {
            return new String(output.array, 0, output.length, Charsets.UTF_8);
        }

        public JsonSink writeRepeatedMessage(RepeatedMessage<?> value) throws IOException {
            super.writeRepeatedMessage(value);
            removeTrailingComma();
            return this;
        }

        // ==================== Encoding Implementations ====================

        @Override
        protected void writeFieldName(final FieldName name) {
            final byte[] key = !preserveProtoFieldNames ? name.getJsonKeyBytes() : name.getProtoKeyBytes();
            final int pos = output.addLength(key.length);
            System.arraycopy(key, 0, output.array, pos, key.length);
            writeSpaceAfterFieldName();
        }

        @Override
        protected void writeNumber(double value) {
            if (ENABLE_SCHUBFACH) {
                DoubleToDecimal.appendJsonTo(value, output);
            } else {
                NumberEncoding.writeDouble(value, output);
            }
            writeMore();
        }

        @Override
        protected void writeNumber(float value) {
            if (ENABLE_SCHUBFACH) {
                FloatToDecimal.appendJsonTo(value, output);
            } else {
                NumberEncoding.writeFloat(value, output);
            }
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
            if (value.hasBytes()) {
                StringEncoding.writeQuotedUtf8(value, output);
            } else {
                StringEncoding.writeQuotedUtf8(value.getString(), output);
            }
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

        // ==================== Utilities ====================

        private void writeMore() {
            isEmptyObjectOrArray = false;
            final int pos = output.length;
            writeChar(',');
            writeNewline();
            trailingSpace = output.length - pos;
        }

        private void removeTrailingComma() {
            // Called after at least one character, so no need to check bounds
            output.length -= trailingSpace;
            trailingSpace = 0;
        }

        private final void writeSpaceAfterFieldName() {
            if (pretty) {
                writeChar(' ');
            }
        }

        private void writeNewline() {
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

        private void writeChar(char c) {
            final int pos = output.addLength(1);
            output.array[pos] = (byte) c;
        }

        @Override
        public void close() throws IOException {
            output.extendCapacityTo(0);
        }

        @Override
        public void flush() throws IOException {
        }

        protected DefaultJsonSink() {
        }

        protected RepeatedByte output;
        protected boolean pretty = false;
        protected int indentLevel = 0;
        protected int trailingSpace = 0;
        private boolean isEmptyObjectOrArray = false;

        private static final int SPACES_PER_LEVEL = 2;

        private static final boolean ENABLE_SCHUBFACH = !Boolean.getBoolean("quickbuf.disable_schubfach");

    }
}
