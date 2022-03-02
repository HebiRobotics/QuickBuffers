/*-
 * #%L
 * quickbuf-benchmarks
 * %%
 * Copyright (C) 2019 - 2020 HEBI Robotics
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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import us.hebi.quickbuf.AbstractJsonSource;
import us.hebi.quickbuf.ProtoEnum;
import us.hebi.quickbuf.Utf8String;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Implementation of a JsonSource using GSON.
 *
 * Warning: this implementation has not been tested extensively against bad inputs.
 *
 * @author Florian Enner
 * @since 07 Sep 2020
 */
public class GsonSource extends AbstractJsonSource<GsonSource> {

    public GsonSource(String string) {
        this(new StringReader(string));
    }

    public GsonSource(Reader reader) {
        this(new JsonReader(reader));
        this.reader.setLenient(true); // allow nan/infinity
    }

    /**
     * @param jsonReader custom json reader. Should be lenient to allow nan/infinity
     */
    public GsonSource(JsonReader jsonReader) {
        this.reader = jsonReader;
    }

    final JsonReader reader;

    @Override
    protected GsonSource thisObj() {
        return this;
    }

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
