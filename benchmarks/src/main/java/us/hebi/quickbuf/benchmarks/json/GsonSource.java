/*-
 * #%L
 * quickbuf-benchmarks
 * %%
 * Copyright (C) 2019 - 2020 HEBI Robotics
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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import us.hebi.quickbuf.AbstractJsonSource;
import us.hebi.quickbuf.ProtoEnum;
import us.hebi.quickbuf.RepeatedByte;
import us.hebi.quickbuf.Utf8String;

import java.io.IOException;
import java.io.Reader;
import java.util.Base64;

/**
 * @author Florian Enner
 * @since 07 Sep 2020
 */
public class GsonSource extends AbstractJsonSource {

    public GsonSource(Reader reader) {
        this.reader = new JsonReader(reader);
        this.reader.setLenient(true); // allow nan/infinity
    }

    final JsonReader reader;

    @Override
    public double readDouble() throws IOException {
        return reader.nextDouble();
    }

    @Override
    public int readInt32() throws IOException {
        return reader.nextInt();
    }

    @Override
    public long readInt64() throws IOException {
        return reader.nextLong();
    }

    @Override
    public boolean readBool() throws IOException {
        return reader.nextBoolean();
    }

    @Override
    public <T extends ProtoEnum<?>> T readEnum(ProtoEnum.EnumConverter<T> converter) throws IOException {
        if (reader.peek() == JsonToken.NUMBER) {
            return converter.forNumber(reader.nextInt());
        } else {
            return converter.forName(reader.nextString());
        }
    }

    @Override
    public void readString(Utf8String store) throws IOException {
        store.copyFrom(reader.nextString());
    }

    @Override
    public void readBytes(RepeatedByte store) throws IOException {
        store.copyFrom(Base64.getDecoder().decode(reader.nextString()));
    }

    @Override
    public void skipField() throws IOException {
        reader.skipValue();
    }

    @Override
    public boolean isNull() throws IOException {
        return reader.peek() == JsonToken.NULL;
    }

    @Override
    public boolean beginObject() throws IOException {
        if (isNull()) {
            reader.nextNull();
            return false;
        }
        reader.beginObject();
        return true;
    }

    @Override
    public void endObject() throws IOException {
        reader.endObject();
    }

    @Override
    public boolean beginArray() throws IOException {
        if (isNull()) {
            reader.nextNull();
            return false;
        }
        reader.beginArray();
        return true;
    }

    @Override
    public void endArray() throws IOException {
        reader.endArray();
    }

    @Override
    public boolean isAtEndOfObject() throws IOException {
        return reader.peek() == JsonToken.END_OBJECT;
    }

    @Override
    public boolean isAtEndOfArray() throws IOException {
        return reader.peek() == JsonToken.END_ARRAY;
    }

    @Override
    public boolean isNumber() throws IOException {
        return reader.peek() == JsonToken.NUMBER;
    }

    @Override
    public int nextFieldHash() throws IOException {
        return reader.nextName().hashCode();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

}
