/*-
 * #%L
 * quickbuf-benchmarks
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

package us.hebi.quickbuf.compat;

import com.google.gson.stream.JsonWriter;
import us.hebi.quickbuf.FieldName;
import us.hebi.quickbuf.JsonSink;
import us.hebi.quickbuf.ProtoMessage;
import us.hebi.quickbuf.Utf8String;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.StringWriter;

/**
 * @author Florian Enner
 * @since 28 Nov 2019
 */
public class GsonSink extends JsonSink implements Closeable, Flushable {

    /**
     * @return A reusable sink that writes to String
     */
    public static GsonSink newStringWriter() {
        return newStringWriter(new StringWriter());
    }

    public static GsonSink newStringWriter(final StringWriter output) {
        return new GsonSink(new JsonWriter(output)) {

            @Override
            public CharSequence getChars() {
                try {
                    flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return output.getBuffer();
            }

            @Override
            public String toString() {
                return getChars().toString();
            }

        };
    }

    public GsonSink(JsonWriter writer) {
        this.writer = writer;
    }

    @Override
    public JsonSink clear() {
        throw new UnsupportedOperationException("GSON does not support reusing writers");
    }

    @Override
    public GsonSink beginObject() throws IOException {
        writer.beginObject();
        return this;
    }

    @Override
    public GsonSink endObject() throws IOException {
        writer.endObject();
        return this;
    }

    @Override
    protected void writeFieldName(final FieldName name) throws IOException {
        writer.name(!preserveProtoFieldNames ? name.getJsonName() : name.getProtoName());
    }

    @Override
    protected void writeNumber(double value) throws IOException {
        if (Double.isNaN(value)) {
            writer.value("NaN");
        } else if (value == Double.POSITIVE_INFINITY) {
            writer.value("Infinity");
        } else if (value == Double.NEGATIVE_INFINITY) {
            writer.value("-Infinity");
        } else {
            writer.value(value);
        }
    }

    @Override
    protected void writeNumber(float value) throws IOException {
        if (Float.isNaN(value)) {
            writer.value("NaN");
        } else if (value == Float.POSITIVE_INFINITY) {
            writer.value("Infinity");
        } else if (value == Float.NEGATIVE_INFINITY) {
            writer.value("-Infinity");
        } else {
            writer.value(value);
        }
    }

    @Override
    protected void writeNumber(long value) throws IOException {
        writer.value(value);
    }

    @Override
    protected void writeNumber(int value) throws IOException {
        writer.value(value);
    }

    @Override
    protected void writeBoolean(boolean value) throws IOException {
        writer.value(value);
    }

    @Override
    protected void writeString(Utf8String value) throws IOException {
        writer.value(value.toString());
    }

    @Override
    protected void writeString(CharSequence value) throws IOException {
        writer.value(value.toString());
    }

    @Override
    protected void writeMessageValue(ProtoMessage<?> value) throws IOException {
        value.writeTo(this);
    }

    @Override
    protected void beginArray() throws IOException {
        writer.beginArray();
    }

    @Override
    protected void endArray() throws IOException {
        writer.endArray();
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    final JsonWriter writer;

}
