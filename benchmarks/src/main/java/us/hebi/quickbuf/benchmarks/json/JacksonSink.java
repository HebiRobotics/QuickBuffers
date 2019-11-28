/*-
 * #%L
 * quickbuf-benchmarks
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
