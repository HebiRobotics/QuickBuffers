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
 * Class that represents the data for a repeated float field.
 *
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public final class RepeatedFloat extends RepeatedField<RepeatedFloat, Float> {

    public static RepeatedFloat newEmptyInstance() {
        return new RepeatedFloat();
    }

    public static RepeatedFloat newInstance(float[] initialValue) {
        return newEmptyInstance().copyFrom(initialValue);
    }

    RepeatedFloat() {
    }

    @Override
    protected void extendCapacityTo(int desiredSize) {
        array = Arrays.copyOf(array, desiredSize);
    }

    @Override
    protected Float getValueAt(int index) {
        return get(index);
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

    public RepeatedFloat add(final float value) {
        final int pos = addLength(1);
        array[pos] = value;
        return this;
    }

    public RepeatedFloat addAll(final float[] values) {
        return addAll(values, 0, values.length);
    }

    public RepeatedFloat addAll(final float[] buffer, final int offset, final int length) {
        final int pos = addLength(length);
        System.arraycopy(buffer, offset, array, pos, length);
        return this;
    }

    public RepeatedFloat copyFrom(final float[] buffer) {
        return copyFrom(buffer, 0, buffer.length);
    }

    public RepeatedFloat copyFrom(final float[] buffer, final int offset, final int length) {
        setLength(length);
        System.arraycopy(buffer, offset, array, 0, length);
        return this;
    }

    @Override
    public void addAll(RepeatedFloat values) {
        addAll(values.array, 0, values.length);
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
        if (length - array.length > 0) {
            extendCapacityTo(length);
        }
        this.length = length;
        return this;
    }

    /**
     * Sets the length to length + offset and returns
     * the previous length. The internal storage array
     * may get extended to accommodate at least the
     * desired length.
     * <p>
     * It is expected that users don't know the exact
     * desired size, so the growth rate is the same
     * as a generic ArrayList.
     * <p>
     * See {@link RepeatedFloat#setLength(int)}
     *
     * @param length added to the current length
     * @return previous length
     */
    public final int addLength(final int length) {
        final int oldLength = this.length;
        final int newLength = oldLength + length;
        final int oldCapacity = array.length;
        if (newLength - oldCapacity > 0) {
            // overflow-conscious code (copied from ArrayList::grow)
            int minCapacity = (array == EMPTY_ARRAY) ? Math.max(newLength, DEFAULT_CAPACITY) : newLength;
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            if (newCapacity - minCapacity < 0)
                newCapacity = minCapacity;
            if (newCapacity - MAX_ARRAY_SIZE > 0)
                newCapacity = hugeCapacity(minCapacity);
            // minCapacity is usually close to size, so this is a win:
            extendCapacityTo(newCapacity);
        }
        this.length = newLength;
        return oldLength;
    }

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private static final int DEFAULT_CAPACITY = 10;

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }

    @Override
    public String toString() {
        return Arrays.toString(toArray());
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
