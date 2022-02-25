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

package us.hebi.quickbuf;

import com.google.gson.stream.JsonWriter;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.StringWriter;

/**
 * @author Florian Enner
 * @since 28 Nov 2019
 */
public class GsonSink extends AbstractJsonSink<GsonSink> implements Closeable, Flushable {

    /**
     * @return A reusable sink that writes to String and resets after calling toString()
     */
    public static GsonSink newStringWriter() {
        final StringWriter output = new StringWriter();
        return new GsonSink(new JsonWriter(output)) {
            @Override
            public String toString() {
                try {
                    flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String string = output.getBuffer().toString();
                output.getBuffer().setLength(0);
                return string;
            }
        };
    }

    public GsonSink(JsonWriter writer) {
        this.writer = writer;
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
        writer.name(name.getValue());
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
    protected GsonSink thisObj() {
        return this;
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
