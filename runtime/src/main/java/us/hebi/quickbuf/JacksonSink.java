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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.StringWriter;

/**
 * @author Florian Enner
 * @since 28 Nov 2019
 */
public class JacksonSink extends AbstractJsonSink<JacksonSink> {

    /**
     * @return A reusable sink that writes to String and resets after calling toString()
     */
    public static JacksonSink newStringWriter() {
        try {
            final StringWriter output = new StringWriter();
            return new JacksonSink(new JsonFactory().createGenerator(output)) {
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
        } catch (IOException e) {
            throw new RuntimeException("IOException without I/O", e);
        }
    }

    public JacksonSink(JsonGenerator writer) {
        this.writer = writer;
    }

    @Override
    protected void writeFieldName(final FieldName name) throws IOException {
        writer.writeFieldName(name.getValue());
    }

    @Override
    protected void writeNumber(double value) throws IOException {
        if (Double.isNaN(value)) {
            writer.writeString("NaN");
        } else if (value == Double.POSITIVE_INFINITY) {
            writer.writeString("Infinity");
        } else if (value == Double.NEGATIVE_INFINITY) {
            writer.writeString("-Infinity");
        } else {
            writer.writeNumber(value);
        }
    }

    @Override
    protected void writeNumber(float value) throws IOException {
        if (Float.isNaN(value)) {
            writer.writeString("NaN");
        } else if (value == Float.POSITIVE_INFINITY) {
            writer.writeString("Infinity");
        } else if (value == Float.NEGATIVE_INFINITY) {
            writer.writeString("-Infinity");
        } else {
            writer.writeNumber(value);
        }
    }

    @Override
    protected void writeNumber(long value) throws IOException {
        writer.writeNumber(value);
    }

    @Override
    protected void writeNumber(int value) throws IOException {
        writer.writeNumber(value);
    }

    @Override
    protected void writeBoolean(boolean value) throws IOException {
        writer.writeBoolean(value);
    }

    @Override
    protected void writeString(Utf8String value) throws IOException {
        writer.writeString(value.toString());
    }

    @Override
    protected void writeString(CharSequence value) throws IOException {
        writer.writeString(value.toString());
    }

    @Override
    protected void writeBinary(RepeatedByte value) throws IOException {
        writer.writeBinary(value.array(), 0, value.length());
    }

    @Override
    protected void writeMessageValue(ProtoMessage<?> value) throws IOException {
        value.writeTo(this);
    }

    @Override
    public JacksonSink beginObject() throws IOException {
        writer.writeStartObject();
        return this;
    }

    @Override
    public JacksonSink endObject() throws IOException {
        writer.writeEndObject();
        return this;
    }

    @Override
    protected void beginArray() throws IOException {
        writer.writeStartArray();
    }

    @Override
    protected void endArray() throws IOException {
        writer.writeEndArray();
    }

    @Override
    protected JacksonSink thisObj() {
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

    final JsonGenerator writer;

}
