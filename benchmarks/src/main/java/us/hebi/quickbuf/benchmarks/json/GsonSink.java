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

import com.google.gson.stream.JsonWriter;
import us.hebi.quickbuf.*;

import java.io.IOException;
import java.util.Base64;

/**
 * @author Florian Enner
 * @since 28 Nov 2019
 */
public class GsonSink extends AbstractJsonSink<GsonSink> {

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
        writer.value(value);
    }

    @Override
    protected void writeNumber(float value) throws IOException {
        writer.value(value);
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
    protected void writeBinary(RepeatedByte value) throws IOException {
        writer.value(Base64.getEncoder().encodeToString(value.toArray()));
    }

    @Override
    protected void writeMessageValue(ProtoMessage value) throws IOException {
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

    public GsonSink(JsonWriter writer) {
        this.writer = writer;
    }

    final JsonWriter writer;

}
