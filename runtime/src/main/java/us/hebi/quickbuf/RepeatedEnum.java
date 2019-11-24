/*-
 * #%L
 * quickbuf-runtime
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

package us.hebi.quickbuf;

import us.hebi.quickbuf.ProtoEnum.EnumConverter;

import java.util.Arrays;

/**
 * Enum values are internally stored as an integers. This class
 * contains helper methods to convert to and from the enum
 * representations. The *Value methods access the internal
 * storage directly and do not go through any validity checks.
 *
 * @author Florian Enner
 * @since 17 Nov 2019
 */
public final class RepeatedEnum<E extends ProtoEnum> extends RepeatedField<RepeatedEnum<E>> {

    @SuppressWarnings("unchecked")
    public static <E extends ProtoEnum> RepeatedEnum<E> newEmptyInstance(EnumConverter<E> converter) {
        return new RepeatedEnum(converter);
    }

    public E get(int index) {
        return converter.forNumber(getValue(index));
    }

    public RepeatedEnum<E> set(int index, E value) {
        return setValue(index, value.getNumber());
    }

    public RepeatedEnum<E> add(final E value) {
        return addValue(value.getNumber());
    }

    public RepeatedEnum<E> addAll(final E... values) {
        return addAll(values, 0, values.length);
    }

    public RepeatedEnum<E> addAll(final E[] buffer, final int offset, final int length) {
        final int pos = addLength(length);
        for (int i = 0; i < length; i++) {
            array[pos + i] = buffer[i].getNumber();
        }
        return this;
    }

    public RepeatedEnum<E> copyFrom(final E[] buffer) {
        return setLength(0).addAll(buffer);
    }

    public RepeatedEnum<E> copyFrom(final E[] buffer, final int offset, final int length) {
        return setLength(0).addAll(buffer, offset, length);
    }

    @Override
    public void copyFrom(RepeatedEnum<E> other) {
        if (converter != other.converter) {
            throw new IllegalArgumentException("Enum types do not match");
        }
        if (other.length > length) {
            extendCapacityTo(other.length);
        }
        System.arraycopy(other.array, 0, array, 0, other.length);
        length = other.length;
    }

    public int getValue(int index) {
        checkIndex(index);
        return array[index];
    }

    public RepeatedEnum<E> setValue(int index, int value) {
        checkIndex(index);
        array[index] = value;
        return this;
    }

    public RepeatedEnum<E> addValue(final int value) {
        reserve(1);
        array[length++] = value;
        return this;
    }

    public RepeatedEnum<E> addAllValues(final int... values) {
        return addAllValues(values, 0, values.length);
    }

    public RepeatedEnum<E> addAllValues(final int[] buffer, final int offset, final int length) {
        final int pos = addLength(length);
        System.arraycopy(buffer, offset, array, pos, length);
        return this;
    }

    public RepeatedEnum<E> copyFromValues(final int[] buffer) {
        return setLength(0).addAllValues(buffer);
    }

    public RepeatedEnum<E> copyFromValues(final int[] buffer, final int offset, final int length) {
        return setLength(0).addAllValues(buffer, offset, length);
    }

    /**
     * @return total capacity of the internal storage array
     */
    @Override
    public int capacity() {
        return array.length;
    }

    /**
     * Creates a copy of the valid data contained in the
     * internal storage.
     *
     * @return copy of valid data
     */
    public final int[] toArray() {
        if (length == 0) return EMPTY_ARRAY;
        return Arrays.copyOf(array, length);
    }

    /**
     * Provides access to the internal storage array. Do not hold
     * on to this reference as it can change during a resize.
     * <p>
     * The array may be larger than the amount of contained data,
     * but the data is only valid between index 0 and length.
     *
     * @return internal storage array
     */
    public final int[] array() {
        return array;
    }

    /**
     * Sets the absolute length of the data that can be serialized. The
     * internal storage array may get extended to accommodate at least
     * the desired length.
     * <p>
     * This does not change the underlying data, so setting a length
     * longer than the current one may result in arbitrary data being
     * serialized.
     *
     * @param length desired length
     * @return this
     */
    public final RepeatedEnum<E> setLength(final int length) {
        if (array.length < length) {
            extendCapacityTo(length);
        }
        this.length = length;
        return this;
    }

    /**
     * Sets the length to length + offset and returns
     * the previous length. The internal storage array
     * may get extended to accomodate at least the
     * desired length.
     *
     * See {@link RepeatedEnum#setLength(int)}
     *
     * @param length added to the current length
     * @return previous length
     */
    public final int addLength(final int length) {
        final int oldLength = this.length;
        final int newLength = oldLength + length;
        if (array.length < newLength) {
            extendCapacityTo(newLength);
        }
        this.length = newLength;
        return oldLength;
    }

    @Override
    protected void extendCapacityTo(int desiredSize) {
        final int[] newValues = new int[desiredSize];
        System.arraycopy(array, 0, newValues, 0, length);
        this.array = newValues;
    }

    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepeatedEnum other = (RepeatedEnum) o;

        if (converter != other.converter || length != other.length)
            return false;

        for (int i = 0; i < length; i++) {
            if (array[i] != other.array[i])
                return false;
        }
        return true;
    }

    private RepeatedEnum(EnumConverter<E> converter) {
        if (converter == null)
            throw new NullPointerException();
        this.converter = converter;
    }

    final EnumConverter<E> converter;
    int[] array = EMPTY_ARRAY;
    private static final int[] EMPTY_ARRAY = new int[0];

}
