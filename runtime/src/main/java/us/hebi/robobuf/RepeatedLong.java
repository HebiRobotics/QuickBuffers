package us.hebi.robobuf;

import java.util.Arrays;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedLong extends RepeatedField<RepeatedLong> {

    @Override
    protected void growCapacity(int desiredSize) {
        final long[] newValues = new long[desiredSize];
        System.arraycopy(array, 0, newValues, 0, length);
        this.array = newValues;
    }

    @Override
    public void clearRange(int fromIndex, int toIndex) {
        Arrays.fill(array, fromIndex, toIndex, DEFAULT_VALUE);
    }

    public long get(int index) {
        return array[index];
    }

    public void set(int index, long value) {
        if (index >= length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        array[index] = value;
    }

    public void add(long value) {
        ensureSpace(1);
        array[length++] = value;
    }

    @Override
    public void copyFrom(RepeatedLong other) {
        if (other.length > length) {
            growCapacity(other.length);
        }
        System.arraycopy(other.array, 0, array, 0, other.length);
        length = other.length;
    }

    @Override
    public int capacity() {
        return array.length;
    }

    public long[] toArray() {
        if (length == 0) return EMPTY;
        return Arrays.copyOf(array, length);
    }

    long[] array = EMPTY;
    private static long[] EMPTY = new long[0];
    private static long DEFAULT_VALUE = 0;

}
