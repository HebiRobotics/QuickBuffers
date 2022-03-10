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

import java.util.Arrays;

/**
 * Contains a string and its corresponding utf8 encoded byte sequence,
 * and lazily converts between them when appropriate.
 *
 * @author Florian Enner
 * @since 26 Nov 2019
 */
public final class Utf8String {

    public static Utf8String newEmptyInstance() {
        return new Utf8String();
    }

    public static Utf8String newInstance(String initialValue) {
        return newEmptyInstance().copyFrom(initialValue);
    }

    /**
     * Internal backing array. Only call after
     * size() or setSize().
     *
     * @return internal backing array
     */
    byte[] bytes() {
        return bytes;
    }

    boolean hasString() {
        return string != null;
    }

    boolean hasBytes() {
        return serializedSize >= 0;
    }

    /**
     * Gets the number of bytes.
     *
     * @return size in bytes
     */
    public int size() {
        ensureSerialized();
        return serializedSize;
    }

    void setSize(final int size) {
        ensureCapacity(size);
        serializedSize = size;
        string = null;
    }

    private void ensureSerialized() {
        if (serializedSize < 0) {
            ensureCapacity((string.length() * Utf8.MAX_UTF8_EXPANSION));
            serializedSize = Utf8.encodeArray(string, bytes, 0, bytes.length);
        }
    }

    public String getString() {
        return getString(ProtoUtil.DEFAULT_UTF8_DECODER);
    }

    public String getString(Utf8Decoder decoder) {
        if (string == null) {
            string = decoder.decode(bytes, 0, serializedSize);
        }
        return string;
    }

    public StringBuilder getChars(StringBuilder store) {
        store.setLength(0);
        if (string == null) {
            Utf8.decodeArray(bytes, 0, serializedSize, store);
        } else {
            store.append(string);
        }
        return store;
    }

    /**
     * Holds on to immutable Strings and lazily encodes them, or
     * encodes the sequence directly if it is not a String.
     *
     * @param other
     * @return
     */
    public Utf8String copyFrom(CharSequence other) {
        return other instanceof String ? copyFrom((String) other) : copyFromEncoded(other);
    }

    public Utf8String copyFromUtf8(final byte[] bytes) {
        return copyFromUtf8(bytes, 0, bytes.length);
    }

    public Utf8String copyFromUtf8(final byte[] bytes, final int offset, final int length) {
        setSize(length);
        System.arraycopy(bytes, offset, this.bytes, 0, length);
        return this;
    }

    /**
     * Encodes the sequence immediately and does not hold on to the reference
     *
     * @param other sequence
     * @return this
     */
    public Utf8String copyFromEncoded(CharSequence other) {
        // Store in encoded utf8 form
        ensureCapacityInternal((other.length() * Utf8.MAX_UTF8_EXPANSION));
        serializedSize = Utf8.encodeArray(other, bytes, 0, bytes.length);
        string = null;
        return this;
    }

    /**
     * Holds on to the reference of the String and encodes it when required.
     *
     * @param other string
     * @return this
     */
    public Utf8String copyFrom(String other) {
        serializedSize = -1;
        string = other;
        return this;
    }

    public Utf8String copyFrom(Utf8String other) {
        string = other.string;
        serializedSize = other.serializedSize;
        if (serializedSize >= 0) {
            ensureCapacityInternal(serializedSize);
            System.arraycopy(other.bytes, 0, bytes, 0, serializedSize);
        }
        return this;
    }

    public void clear() {
        serializedSize = 0;
        string = "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Utf8String other = (Utf8String) o;

        if (string != null && other.string != null) {
            return string.equals(other.string);
        }

        if (size() != other.size())
            return false;

        for (int i = 0; i < serializedSize; i++) {
            if (bytes[i] != other.bytes[i])
                return false;
        }
        return true;
    }

    /**
     * Utf8Strings have no immutable state and should not
     * be used in hashing structures. This method returns
     * a constant value.
     *
     * @return 0
     */
    @Override
    public final int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return getString();
    }

    /**
     * Ensures that the capacity is at least equal to the specified minimum.
     * If the current capacity is less than the argument, then a new internal
     * array is allocated with greater capacity. The new capacity is the
     * larger of:
     * <ul>
     * <li>The {@code minimumCapacity} argument.
     * <li>Twice the old capacity, plus {@code 2}.
     * </ul>
     * If the {@code minimumCapacity} argument is nonpositive, this
     * method takes no action and simply returns.
     * Note that subsequent operations on this object can reduce the
     * actual capacity below that requested here.
     *
     * @param minimumCapacity the minimum desired capacity.
     */
    public void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity > 0)
            ensureCapacityInternal(minimumCapacity);
    }

    /**
     * For positive values of {@code minimumCapacity}, this method
     * behaves like {@code ensureCapacity}, however it is never
     * synchronized.
     * If {@code minimumCapacity} is non positive due to numeric
     * overflow, this method throws {@code OutOfMemoryError}.
     */
    private void ensureCapacityInternal(int minimumCapacity) {
        // overflow-conscious code
        if (minimumCapacity - bytes.length > 0) {
            bytes = Arrays.copyOf(bytes, newCapacity(minimumCapacity));
        }
    }

    /**
     * Returns a capacity at least as large as the given minimum capacity.
     * Returns the current capacity increased by the same amount + 2 if
     * that suffices.
     * Will not return a capacity greater than {@code MAX_ARRAY_SIZE}
     * unless the given minimum capacity is greater than that.
     *
     * @param minCapacity the desired minimum capacity
     * @throws OutOfMemoryError if minCapacity is less than zero or
     *                          greater than Integer.MAX_VALUE
     */
    private int newCapacity(int minCapacity) {
        // overflow-conscious code
        int newCapacity = (bytes.length << 1) + 2;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }
        return (newCapacity <= 0 || MAX_ARRAY_SIZE - newCapacity < 0)
                ? hugeCapacity(minCapacity)
                : newCapacity;
    }

    private int hugeCapacity(int minCapacity) {
        if (Integer.MAX_VALUE - minCapacity < 0) { // overflow
            throw new OutOfMemoryError();
        }
        return Math.max(minCapacity, MAX_ARRAY_SIZE);
    }

    /**
     * The maximum size of array to allocate (unless necessary).
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private Utf8String() {
    }

    private int serializedSize = 0;
    private byte[] bytes = ProtoUtil.EMPTY_BYTE_ARRAY;
    private String string = "";

}
