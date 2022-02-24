/*-
 * #%L
 * quickbuf-runtime
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

import java.io.IOException;

/**
 * Prints proto messages in a JSON compatible format
 *
 * @author Florian Enner
 * @since 26 Oct 2019
 */
public abstract class AbstractJsonSink<SubType extends AbstractJsonSink> {

    public SubType writeMessage(ProtoMessage value) throws IOException {
        value.writeTo(this);
        return thisObj();
    }

    // ==================== Common Type Forwarders ====================

    public SubType writeFixed64(final FieldName name, final long value) throws IOException {
        return writeInt64(name, value);
    }

    public SubType writeSFixed64(final FieldName name, final long value) throws IOException {
        return writeInt64(name, value);
    }

    public SubType writeUInt64(final FieldName name, final long value) throws IOException {
        return writeInt64(name, value);
    }

    public SubType writeSInt64(final FieldName name, final long value) throws IOException {
        return writeInt64(name, value);
    }

    public SubType writeFixed32(final FieldName name, final int value) throws IOException {
        return writeInt32(name, value);
    }

    public SubType writeSFixed32(final FieldName name, final int value) throws IOException {
        return writeInt32(name, value);
    }

    public SubType writeUInt32(final FieldName name, final int value) throws IOException {
        return writeInt32(name, value);
    }

    public SubType writeSInt32(final FieldName name, final int value) throws IOException {
        return writeInt32(name, value);
    }

    public SubType writeGroup(final FieldName name, final ProtoMessage value) throws IOException {
        return writeMessage(name, value);
    }

    public SubType writeRepeatedFixed64(final FieldName name, final RepeatedLong value) throws IOException {
        return writeRepeatedInt64(name, value);
    }

    public SubType writeRepeatedSFixed64(final FieldName name, final RepeatedLong value) throws IOException {
        return writeRepeatedInt64(name, value);
    }

    public SubType writeRepeatedUInt64(final FieldName name, final RepeatedLong value) throws IOException {
        return writeRepeatedInt64(name, value);
    }

    public SubType writeRepeatedSInt64(final FieldName name, final RepeatedLong value) throws IOException {
        return writeRepeatedInt64(name, value);
    }

    public SubType writeRepeatedFixed32(final FieldName name, final RepeatedInt value) throws IOException {
        return writeRepeatedInt32(name, value);
    }

    public SubType writeRepeatedSFixed32(final FieldName name, final RepeatedInt value) throws IOException {
        return writeRepeatedInt32(name, value);
    }

    public SubType writeRepeatedUInt32(final FieldName name, final RepeatedInt value) throws IOException {
        return writeRepeatedInt32(name, value);
    }

    public SubType writeRepeatedSInt32(final FieldName name, final RepeatedInt value) throws IOException {
        return writeRepeatedInt32(name, value);
    }

    public SubType writeRepeatedGroup(final FieldName name, final RepeatedMessage<?> value) throws IOException {
        return writeRepeatedMessage(name, value);
    }

    // ==================== Shared Implementations ====================

    public SubType writeDouble(final FieldName name, final double value) throws IOException {
        writeFieldName(name);
        writeNumber(value);
        return thisObj();
    }

    public SubType writeFloat(final FieldName name, final float value) throws IOException {
        writeFieldName(name);
        writeNumber(value);
        return thisObj();
    }

    public SubType writeInt64(final FieldName name, final long value) throws IOException {
        writeFieldName(name);
        writeNumber(value);
        return thisObj();
    }

    public SubType writeInt32(final FieldName name, final int value) throws IOException {
        writeFieldName(name);
        writeNumber(value);
        return thisObj();
    }

    public SubType writeEnum(final FieldName name, final int value, ProtoEnum.EnumConverter converter) throws IOException {
        writeFieldName(name);
        writeEnumValue(value, converter);
        return thisObj();
    }

    public SubType writeBool(final FieldName name, final boolean value) throws IOException {
        writeFieldName(name);
        writeBoolean(value);
        return thisObj();
    }

    public SubType writeBytes(final FieldName name, final RepeatedByte value) throws IOException {
        writeFieldName(name);
        writeBinary(value);
        return thisObj();
    }

    public SubType writeMessage(final FieldName name, final ProtoMessage value) throws IOException {
        writeFieldName(name);
        writeMessageValue(value);
        return thisObj();
    }

    public SubType writeString(final FieldName name, final Utf8String value) throws IOException {
        writeFieldName(name);
        writeString(value);
        return thisObj();
    }

    public SubType writeString(final FieldName name, final CharSequence value) throws IOException {
        writeFieldName(name);
        writeString(value);
        return thisObj();
    }

    public SubType writeRepeatedDouble(final FieldName name, final RepeatedDouble value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeNumber(value.array[i]);
        }
        endArray();
        return thisObj();
    }

    public SubType writeRepeatedInt64(final FieldName name, final RepeatedLong value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeNumber(value.array[i]);
        }
        endArray();
        return thisObj();
    }

    public SubType writeRepeatedFloat(final FieldName name, final RepeatedFloat value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeNumber(value.array[i]);
        }
        endArray();
        return thisObj();
    }

    public SubType writeRepeatedInt32(final FieldName name, final RepeatedInt value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeNumber(value.array[i]);
        }
        endArray();
        return thisObj();
    }

    public SubType writeRepeatedMessage(final FieldName name, final RepeatedMessage<?> value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeMessageValue(value.array[i]);
        }
        endArray();
        return thisObj();
    }

    public SubType writeRepeatedString(final FieldName name, final RepeatedString value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeString(value.array[i]);
        }
        endArray();
        return thisObj();
    }

    public SubType writeRepeatedEnum(final FieldName name, final RepeatedEnum<?> value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeEnumValue(value.array[i], value.converter);
        }
        endArray();
        return thisObj();
    }

    public SubType writeRepeatedBool(final FieldName name, final RepeatedBoolean value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeBoolean(value.array[i]);
        }
        endArray();
        return thisObj();
    }

    public SubType writeRepeatedBytes(final FieldName name, final RepeatedBytes value) throws IOException {
        writeFieldName(name);
        beginArray();
        for (int i = 0; i < value.length; i++) {
            writeBinary(value.array[i]);
        }
        endArray();
        return thisObj();
    }

    protected void writeEnumValue(final int number, final ProtoEnum.EnumConverter converter) throws IOException {
        final ProtoEnum value;
        if (writeEnumStrings && (value = converter.forNumber(number)) != null) {
            writeString(value.getName());
        } else {
            writeNumber(number);
        }
    }

    // ==================== Configuration ====================

    /**
     * Allows to serialize enums as human readable strings or
     * as JSON numbers. Compatible parsers are able to parse
     * either case.
     *
     * Unknown values will still be serialized as numbers.
     *
     * @param value true if values should use strings
     * @return this
     */
    public SubType setWriteEnumStrings(final boolean value) {
        this.writeEnumStrings = value;
        return thisObj();
    }

    protected boolean writeEnumStrings = false;

    // ==================== Child Interface ====================

    /**
     * @param name utf8 encoded bytes including end quotes and colon
     */
    protected abstract void writeFieldName(FieldName name) throws IOException;

    protected abstract void writeNumber(double value) throws IOException;

    protected abstract void writeNumber(float value) throws IOException;

    protected abstract void writeNumber(long value) throws IOException;

    protected abstract void writeNumber(int value) throws IOException;

    protected abstract void writeBoolean(boolean value) throws IOException;

    protected abstract void writeString(Utf8String value) throws IOException;

    protected abstract void writeString(CharSequence value) throws IOException;

    protected abstract void writeBinary(RepeatedByte value) throws IOException;

    protected abstract void writeMessageValue(ProtoMessage value) throws IOException;

    public abstract SubType beginObject() throws IOException;

    public abstract SubType endObject() throws IOException;

    protected abstract void beginArray() throws IOException;

    protected abstract void endArray() throws IOException;

    protected abstract SubType thisObj();

}
