package us.hebi.robobuf;

import java.util.Arrays;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedInt extends RepeatedField<RepeatedInt> {

    @Override
    protected void growCapacity(int desiredSize) {
        final int[] newValues = new int[desiredSize];
        System.arraycopy(array, 0, newValues, 0, length);
        this.array = newValues;
    }

    @Override
    public void clearRange(int fromIndex, int toIndex) {
        Arrays.fill(array, fromIndex, toIndex, DEFAULT_VALUE);
    }

    public int get(int index) {
        return array[index];
    }

    public void set(int index, int value) {
        if (index >= length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        array[index] = value;
    }

    public void add(int value) {
        ensureSpace(1);
        array[length++] = value;
    }

    @Override
    public void copyFrom(RepeatedInt other) {
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

    public int[] toArray() {
        if (length == 0) return EMPTY;
        return Arrays.copyOf(array, length);
    }

    int[] array = EMPTY;
    private static int[] EMPTY = new int[0];
    private static int DEFAULT_VALUE = 0;

}
