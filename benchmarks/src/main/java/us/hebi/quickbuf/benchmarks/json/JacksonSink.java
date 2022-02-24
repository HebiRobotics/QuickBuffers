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

package us.hebi.quickbuf.benchmarks.json;

import com.fasterxml.jackson.core.JsonGenerator;
import us.hebi.quickbuf.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Florian Enner
 * @since 28 Nov 2019
 */
public class JacksonSink extends AbstractJsonSink<JacksonSink> {

    @Override
    protected void writeFieldName(final FieldName name) throws IOException {
        writer.writeFieldName(name.getValue());
    }

    @Override
    protected void writeNumber(double value) throws IOException {
        writer.writeNumber(value);
    }

    @Override
    protected void writeNumber(float value) throws IOException {
        writer.writeNumber(value);
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
    protected void writeMessageValue(ProtoMessage value) throws IOException {
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

    public JacksonSink(JsonGenerator writer) {
        this.writer = writer;
    }

    final JsonGenerator writer;

}
