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

package us.hebi.quickbuf;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.Reader;

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
    public double nextDouble() throws IOException {
        return reader.nextDouble();
    }

    @Override
    public int nextInt() throws IOException {
        return reader.nextInt();
    }

    @Override
    public long nextLong() throws IOException {
        return reader.nextLong();
    }

    @Override
    public boolean nextBoolean() throws IOException {
        return reader.nextBoolean();
    }

    @Override
    public <T extends ProtoEnum<?>> T nextEnum(ProtoEnum.EnumConverter<T> converter) throws IOException {
        if (tryReadNull()) {
            return null;
        } else if (reader.peek() == JsonToken.NUMBER) {
            return converter.forNumber(reader.nextInt());
        } else {
            return converter.forName(reader.nextString());
        }
    }

    @Override
    public void nextString(Utf8String store) throws IOException {
        if (tryReadNull()) {
            store.clear();
        } else {
            store.copyFrom(reader.nextString());
        }
    }

    @Override
    public void nextBase64(RepeatedByte store) throws IOException {
        if (tryReadNull()) {
            store.clear();
        } else {
            store.copyFrom(Base64.decodeFast(reader.nextString()));
        }
    }

    @Override
    public void skipValue() throws IOException {
        reader.skipValue();
    }

    @Override
    public boolean beginObject() throws IOException {
        if (tryReadNull()) return false;
        reader.beginObject();
        return true;
    }

    @Override
    public void endObject() throws IOException {
        reader.endObject();
    }

    @Override
    public boolean beginArray() throws IOException {
        if (tryReadNull()) return false;
        reader.beginArray();
        return true;
    }

    private boolean tryReadNull() throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return true;
        }
        return false;
    }

    @Override
    public void endArray() throws IOException {
        reader.endArray();
    }

    @Override
    public boolean hasNext() throws IOException {
        return reader.hasNext();
    }

    @Override
    protected CharSequence nextName() throws IOException {
        return reader.nextName();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

}
