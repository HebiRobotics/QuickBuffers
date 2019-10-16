package us.hebi.robobuf;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Base class for repeated fields of non-primitive values such as
 * messages, bytes, or strings.
 *
 * @author Florian Enner
 * @since 14 Aug 2019
 */
abstract class RepeatedObject<SubType extends RepeatedObject, T, IN> extends RepeatedField<SubType> implements Iterable<T> {

    public final T next() {
        reserve(1);
        return array[length++];
    }

    public final T get(int index) {
        checkIndex(index);
        return array[index];
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
        copyDataFrom0(other);
        length = other.length;
    }

    @Override
    public final int capacity() {
        return array.length;
    }

    public final T[] toArray() {
        if (length == 0) return EMPTY;
        return Arrays.copyOf(array, length);
    }

    @Override
    public final String toString() {
        return Arrays.toString(toArray());
    }

    @Override
    public final int hashCode() {
        return Arrays.hashCode(toArray());
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

    @Override
    public Iterator<T> iterator() {
        return new ArrayIterator<T>(array, length);
    }

    static class ArrayIterator<T> implements Iterator<T> {

        ArrayIterator(T[] array, int length) {
            this.array = array;
            this.length = length;
        }

        @Override
        public boolean hasNext() {
            return position < length;
        }

        @Override
        public T next() {
            return array[position++];
        }

        private int position = 0;
        private final T[] array;
        private final int length;

    }

    @Override
    protected final void clearRange(int fromIndex, int toIndex) {
        for (int i = fromIndex; i < toIndex; i++) {
            clearIndex0(i);
        }
    }

    @Override
    protected final void extendCapacityTo(int desiredSize) {
        final T[] newValues = allocateArray0(desiredSize);
        System.arraycopy(array, 0, newValues, 0, length);
        this.array = newValues;
        for (int i = length; i < array.length; i++) {
            array[i] = createEmpty();
        }
    }

    protected abstract void copyDataFrom0(SubType other);

    protected abstract void clearIndex0(int index);

    protected abstract void setIndex0(int index, IN value);

    protected abstract boolean isEqual(T a, Object b);

    protected abstract T createEmpty();

    protected abstract T[] allocateArray0(int desiredSize);

    final T[] EMPTY = allocateArray0(0);
    protected T[] array = EMPTY;

}
