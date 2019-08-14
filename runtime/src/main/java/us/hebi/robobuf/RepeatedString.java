package us.hebi.robobuf;

import java.util.Arrays;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedString extends RepeatedField<RepeatedString> {

    @Override
    public int capacity() {
        return array.length;
    }

    public StringBuilder getAndAdd() {
        requestSize(1);
        return array[length++];
    }

    @Override
    protected void growCapacity(int desiredSize) {
        final StringBuilder[] newValues = new StringBuilder[desiredSize];
        System.arraycopy(array, 0, newValues, 0, length);
        this.array = newValues;
        for (int i = length; i < array.length; i++) {
            array[i] = new StringBuilder(0);
        }
    }

    @Override
    protected void clearRange(int fromIndex, int toIndex) {
        for (int i = fromIndex; i < toIndex; i++) {
            this.array[i].setLength(0);
        }
    }

    public StringBuilder get(int index) {
        checkIndex(index);
        return array[index];
    }

    public void set(int index, CharSequence value) {
        checkIndex(index);
        setIndex0(index, value);
    }

    public void addAll(CharSequence[] values) {
        addAll(values, 0, values.length);
    }

    public void addAll(CharSequence[] buffer, int offset, int length) {
        requestSize(length);
        for (int i = offset; i < length; i++) {
            add(buffer[i]);
        }
    }
    public void add(CharSequence value) {
        requestSize(1);
        setIndex0(length++, value);
    }

    private void setIndex0(int index, CharSequence value) {
        array[index].setLength(0);
        array[index].append(value);
    }

    @Override
    public void copyFrom(RepeatedString other) {
        if (other.length > length) {
            growCapacity(other.length);
        }
        for (int i = 0; i < length; i++) {
            setIndex0(i, other.array[i]);
        }
        length = other.length;
    }

    public void copyFrom(CharSequence[] buffer) {
        copyFrom(buffer, 0, buffer.length);
    }

    public void copyFrom(CharSequence[] buffer, int offset, int length) {
        this.length = 0;
        addAll(buffer, offset, length);
    }

    public StringBuilder[] toArray() {
        if (length == 0) return EMPTY;
        return Arrays.copyOf(array, length);
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
        RepeatedString other = (RepeatedString) o;

        if (length != other.length)
            return false;

        for (int i = 0; i < length; i++) {
            if (!RoboUtil.equals(array[i], other.array[i]))
                return false;
        }
        return true;
    }

    StringBuilder[] array = EMPTY;
    private static final StringBuilder[] EMPTY = new StringBuilder[0];

}
