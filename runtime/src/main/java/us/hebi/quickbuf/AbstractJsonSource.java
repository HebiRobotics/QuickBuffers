/*-
 * #%L
 * quickbuf-runtime
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

import java.io.Closeable;
import java.io.IOException;

/**
 * Reads proto messages from a JSON format
 *
 * @author Florian Enner
 * @since 08 Sep 2020
 */
public abstract class AbstractJsonSource implements Closeable {

    // ==================== Common Type Forwarders ====================

    /**
     * Read a {@code float} field value from the source.
     */
    public float readFloat() throws IOException {
        return (float) readDouble();
    }

    /**
     * Read a {@code uint64} field value from the source.
     */
    public long readUInt64() throws IOException {
        return readInt64();
    }

    /**
     * Read a {@code fixed64} field value from the source.
     */
    public long readFixed64() throws IOException {
        return readInt64();
    }

    /**
     * Read a {@code fixed32} field value from the source.
     */
    public int readFixed32() throws IOException {
        return readInt32();
    }

    /**
     * Read a {@code group} field value from the source.
     */
    public void readGroup(final ProtoMessage msg)
            throws IOException {
        readMessage(msg);
    }

    /**
     * Read a {@code uint32} field value from the source.
     */
    public int readUInt32() throws IOException {
        return readInt32();
    }

    /**
     * Read an {@code sfixed32} field value from the source.
     */
    public int readSFixed32() throws IOException {
        return readInt32();
    }

    /**
     * Read an {@code sfixed64} field value from the source.
     */
    public long readSFixed64() throws IOException {
        return readInt64();
    }

    /**
     * Read an {@code sint32} field value from the source.
     */
    public int readSInt32() throws IOException {
        return readInt32();
    }

    /**
     * Read an {@code sint64} field value from the source.
     */
    public long readSInt64() throws IOException {
        return readInt64();
    }

    public void readRepeatedFixed64(final RepeatedLong value) throws IOException {
        readRepeatedInt64(value);
    }

    public void readRepeatedSFixed64(final RepeatedLong value) throws IOException {
        readRepeatedInt64(value);
    }

    public void readRepeatedUInt64(final RepeatedLong value) throws IOException {
        readRepeatedInt64(value);
    }

    public void readRepeatedSInt64(final RepeatedLong value) throws IOException {
        readRepeatedInt64(value);
    }

    public void readRepeatedFixed32(final RepeatedInt value) throws IOException {
        readRepeatedInt32(value);
    }

    public void readRepeatedSFixed32(final RepeatedInt value) throws IOException {
        readRepeatedInt32(value);
    }

    public void readRepeatedUInt32(final RepeatedInt value) throws IOException {
        readRepeatedInt32(value);
    }

    public void readRepeatedSInt32(final RepeatedInt value) throws IOException {
        readRepeatedInt32(value);
    }

    public void readRepeatedGroup(final RepeatedMessage<?> value) throws IOException {
        readRepeatedMessage(value);
    }

    // ==================== Shared Implementations ====================

    public void readRepeatedDouble(final RepeatedDouble value) throws IOException {
        if (!beginArray()) return;
        while (!isAtEndOfArray()) {
            value.add(readDouble());
        }
        endArray();
    }

    public void readRepeatedInt64(final RepeatedLong value) throws IOException {
        if (!beginArray()) return;
        while (!isAtEndOfArray()) {
            value.add(readInt64());
        }
        endArray();
    }

    public void readRepeatedFloat(final RepeatedFloat value) throws IOException {
        if (!beginArray()) return;
        while (!isAtEndOfArray()) {
            value.add(readFloat());
        }
        endArray();
    }

    public void readRepeatedInt32(final RepeatedInt value) throws IOException {
        if (!beginArray()) return;
        while (!isAtEndOfArray()) {
            value.add(readInt32());
        }
        endArray();
    }

    public void readRepeatedMessage(final RepeatedMessage<?> value) throws IOException {
        if (!beginArray()) return;
        while (!isAtEndOfArray()) {
            readMessage(value.next());
        }
        endArray();
    }

    public void readRepeatedString(final RepeatedString value) throws IOException {
        if (!beginArray()) return;
        while (!isAtEndOfArray()) {
            readString(value.next());
        }
        endArray();
    }

    public <E extends ProtoEnum<?>> void readRepeatedEnum(final RepeatedEnum<E> value, final ProtoEnum.EnumConverter<E> converter) throws IOException {
        if (!beginArray()) return;
        while (!isAtEndOfArray()) {
            if (isNumber()) {
                value.addValue(readInt32());
            } else {
                E val = readEnum(converter);
                value.addValue(val == null ? 0 : val.getNumber());
            }
        }
        endArray();
    }

    public void readRepeatedBool(final RepeatedBoolean value) throws IOException {
        if (!beginArray()) return;
        while (!isAtEndOfArray()) {
            value.add(readBool());
        }
        endArray();
    }

    public void readRepeatedBytes(final RepeatedBytes value) throws IOException {
        if (!beginArray()) return;
        while (!isAtEndOfArray()) {
            readBytes(value.next());
        }
        endArray();
    }

    // ==================== Child Interface ====================

    /**
     * Read a {@code double} field value from the source.
     */
    public abstract double readDouble() throws IOException;

    /**
     * Read an {@code int32} field value from the source.
     */
    public abstract int readInt32() throws IOException;

    /**
     * Read an {@code int64} field value from the source.
     */
    public abstract long readInt64() throws IOException;

    /**
     * Read a {@code bool} field value from the source.
     */
    public abstract boolean readBool() throws IOException;

    /**
     * Read a {@code bool} field value from the source.
     */
    public abstract <T extends ProtoEnum<?>> T readEnum(final ProtoEnum.EnumConverter<T> converter) throws IOException;

    /**
     * Read a {@code string} field value from the source.
     */
    public abstract void readString(final Utf8String store) throws IOException;

    /**
     * Read a nested message value from the source
     */
    public void readMessage(final ProtoMessage msg) throws IOException {
        msg.mergeFrom(this);
    }

    /**
     * Read a {@code bytes} field value from the source.
     */
    public abstract void readBytes(RepeatedByte store) throws IOException;

    /**
     * Reads and discards the value of a single field.
     */
    public abstract void skipField() throws IOException;

    public abstract boolean isNull() throws IOException;

    /**
     * Consumes the begin element token or null element.
     *
     * @return false on null, true otherwise
     */
    public abstract boolean beginObject() throws IOException;

    public abstract void endObject() throws IOException;

    /**
     * Consumes the begin array token or null element.
     *
     * @return false on null, true otherwise
     */
    public abstract boolean beginArray() throws IOException;

    public abstract void endArray() throws IOException;

    public abstract boolean isAtEndOfObject() throws IOException;

    public abstract boolean isAtEndOfArray() throws IOException;

    public abstract boolean isNumber() throws IOException;

    public abstract int nextFieldHash() throws IOException;

}
