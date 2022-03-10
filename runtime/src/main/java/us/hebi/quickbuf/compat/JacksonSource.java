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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import us.hebi.quickbuf.AbstractJsonSource;
import us.hebi.quickbuf.ProtoEnum;
import us.hebi.quickbuf.Utf8String;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * @author Florian Enner
 * @since 26 Feb 2022
 */
@Deprecated // not working yet
public class JacksonSource extends AbstractJsonSource {

    public JacksonSource(String string) throws IOException {
        this(new StringReader(string));
    }

    public JacksonSource(Reader reader) throws IOException {
        this(new JsonFactory().createParser(reader));
        //this.reader.setLenient(true); // allow nan/infinity
    }

    /**
     * @param jsonReader custom json reader. Should be lenient to allow nan/infinity
     */
    public JacksonSource(JsonParser jsonReader) {
        this.reader = jsonReader;
    }

    final JsonParser reader;

    @Override
    public double nextDouble() throws IOException {
        next();
        return reader.getDoubleValue();
    }

    @Override
    public int nextInt() throws IOException {
        next();
        return reader.getIntValue();
    }

    @Override
    public long nextLong() throws IOException {
        next();
        return reader.getLongValue();
    }

    @Override
    public boolean nextBoolean() throws IOException {
        next();
        return reader.getBooleanValue();
    }

    @Override
    public <T extends ProtoEnum<?>> T nextEnum(ProtoEnum.EnumConverter<T> converter) throws IOException {
        switch (next()) {
            case VALUE_NULL:
                return null;
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return converter.forNumber(reader.getIntValue());
            default:
                return converter.forName(reader.getValueAsString());
        }
    }

    @Override
    public void nextString(Utf8String store) throws IOException {
        if (tryReadNull()) {
            store.clear();
        } else {
            next();
            store.copyFrom(reader.getValueAsString());
        }
    }

    @Override
    public void skipValue() throws IOException {
        reader.getValueAsString(); // TODO: is there a better way to do this?
    }

    @Override
    public boolean beginObject() throws IOException {
        if (tryReadNull()) return false;
//        reader.beginObject();
        return true;
    }

    @Override
    public void endObject() throws IOException {
//       reader.endObject();
    }

    @Override
    public boolean beginArray() throws IOException {
        if (tryReadNull()) return false;
//        reader.beginArray();
        return true;
    }

    private boolean tryReadNull() throws IOException {
        if (peek() == JsonToken.VALUE_NULL) {
            skipValue();
            return true;
        }
        return false;
    }

    @Override
    public void endArray() throws IOException {
//        reader.endArray();
    }

    @Override
    public boolean hasNext() throws IOException {
        return !reader.isClosed();
    }

    @Override
    protected CharSequence nextName() throws IOException {
        return reader.nextFieldName();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private JsonToken next() throws IOException {
        try {
            return peekToken != null ? peekToken : reader.nextToken();
        } finally {
            peekToken = null;
        }
    }

    private JsonToken peek() throws IOException {
        return peekToken != null ? peekToken : (peekToken = reader.nextToken());
    }

    JsonToken peekToken = null;

}
