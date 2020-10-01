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

import java.util.Arrays;

/**
 * Base class for repeated fields of non-primitive values such as
 * messages, bytes, or strings.
 *
 * @author Florian Enner
 * @since 14 Aug 2019
 */
abstract class RepeatedObject<SubType extends RepeatedObject<SubType, STORE, IN, OUT>, STORE, IN, OUT> extends RepeatedField<SubType, OUT> {

    public final STORE next() {
        reserve(1);
        return array[length++];
    }

    @Override
    protected OUT getValueAt(int index) {
        return get(index);
    }

    public final OUT get(int index) {
        checkIndex(index);
        return getIndex0(index);
    }

    public final void set(int index, IN value) {
        checkIndex(index);
        setIndex0(index, value);
    }

    public final void addAll(IN[] values) {
        addAll(values, 0, values.length);
    }

    public final void addAll(IN[] buffer, int offset, int length) {
        reserve(length);
        for (int i = offset; i < length; i++) {
            add(buffer[i]);
        }
    }

    public final void add(IN value) {
        reserve(1);
        setIndex0(length++, value);
    }

    public final void copyFrom(IN[] buffer) {
        copyFrom(buffer, 0, buffer.length);
    }

    public final void copyFrom(IN[] buffer, int offset, int length) {
        this.length = 0;
        addAll(buffer, offset, length);
    }

    @Override
    public final void copyFrom(SubType other) {
        if (other.length > length) {
            extendCapacityTo(other.length);
        }
        length = other.length;
        for (int i = 0; i < length; i++) {
            copyFrom0(array[i], other.array[i]);
        }
    }

    @Override
    public void addAll(SubType values) {
        final int newLength = length + values.length;
        extendCapacityTo(newLength);
        for (int i = 0; i < values.length; i++) {
            copyFrom0(array[length + i], values.array[i]);
        }
        this.length = newLength;
    }

    @Override
    public final int capacity() {
        return array.length;
    }

    @Override
    public final String toString() {
        return Arrays.toString(Arrays.copyOf(array, length));
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepeatedObject other = (RepeatedObject) o;

        if (length != other.length)
            return false;

        for (int i = 0; i < length; i++) {
            if (!isEqual(array[i], other.array[i]))
                return false;
        }
        return true;
    }

    private boolean isEqual(STORE a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    @Override
    public final void clear() {
        for (int i = 0; i < length; i++) {
            clearIndex0(i);
        }
        length = 0;
    }

    @Override
    protected final void extendCapacityTo(int desiredSize) {
        final STORE[] newValues = allocateArray0(desiredSize);
        System.arraycopy(array, 0, newValues, 0, length);
        this.array = newValues;
        for (int i = length; i < array.length; i++) {
            array[i] = createEmpty();
        }
    }

    protected abstract void copyFrom0(STORE store, STORE other);

    protected abstract void clearIndex0(int index);

    protected abstract void setIndex0(int index, IN value);

    protected abstract OUT getIndex0(int index);

    protected abstract STORE createEmpty();

    protected abstract STORE[] allocateArray0(int desiredSize);

    final STORE[] EMPTY = allocateArray0(0);
    protected STORE[] array = EMPTY;

}
