/*-
 * #%L
 * robobuf-runtime
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

package us.hebi.robobuf;

import java.util.Arrays;

/**
 * Class that represents the data for a repeated float field.
 *
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public final class RepeatedFloat extends RepeatedField<RepeatedFloat> {

    public static RepeatedFloat newEmptyInstance() {
        return new RepeatedFloat();
    }

    RepeatedFloat() {
    }

    @Override
    protected void extendCapacityTo(int desiredSize) {
        final float[] newValues = new float[desiredSize];
        System.arraycopy(array, 0, newValues, 0, length);
        this.array = newValues;
    }

    public float get(int index) {
        checkIndex(index);
        return array[index];
    }

    public RepeatedFloat set(int index, float value) {
        checkIndex(index);
        array[index] = value;
        return this;
    }

    public RepeatedFloat add(float value) {
        reserve(1);
        array[length++] = value;
        return this;
    }

    public RepeatedFloat addAll(float[] values) {
        return addAll(values, 0, values.length);
    }

    public RepeatedFloat addAll(float[] buffer, int offset, int length) {
        reserve(length);
        System.arraycopy(buffer, offset, array, this.length, length);
        this.length += length;
        return this;
    }

    public RepeatedFloat copyFrom(float[] buffer) {
        return copyFrom(buffer, 0, buffer.length);
    }

    public RepeatedFloat copyFrom(float[] buffer, int offset, int length) {
        this.length = 0;
        addAll(buffer, offset, length);
        return this;
    }

    @Override
    public void copyFrom(RepeatedFloat other) {
        if (other.length > length) {
            extendCapacityTo(other.length);
        }
        System.arraycopy(other.array, 0, array, 0, other.length);
        length = other.length;
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
    public final float[] toArray() {
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
    public final float[] array() {
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
    public final RepeatedFloat setLength(final int length) {
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
     * See {@link RepeatedFloat#setLength(int)}
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
    public String toString() {
        return Arrays.toString(toArray());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toArray());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepeatedFloat other = (RepeatedFloat) o;

        if (length != other.length)
            return false;

        for (int i = 0; i < length; i++) {
            if (!ProtoUtil.isEqual(array[i], other.array[i]))
                return false;
        }
        return true;
    }

    float[] array = EMPTY_ARRAY;
    private static final float[] EMPTY_ARRAY = new float[0];

}
