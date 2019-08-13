package us.hebi.robobuf;

import java.util.Arrays;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedBoolean extends RepeatedField<RepeatedBoolean> {

    @Override
    protected void growCapacity(int desiredSize) {
        final boolean[] newValues = new boolean[desiredSize];
        System.arraycopy(array, 0, newValues, 0, length);
        this.array = newValues;
    }

    @Override
    public void clearRange(int fromIndex, int toIndex) {
        Arrays.fill(array, fromIndex, toIndex, DEFAULT_VALUE);
    }

    public boolean get(int index) {
        return array[index];
    }

    public void set(int index, boolean value) {
        if (index >= length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        array[index] = value;
    }

    public void add(boolean value) {
        ensureSpace(1);
        array[length++] = value;
    }

    @Override
    public void copyFrom(RepeatedBoolean other) {
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

    public boolean[] toArray() {
        if (length == 0) return EMPTY;
        return Arrays.copyOf(array, length);
    }

    boolean[] array = EMPTY;
    private static boolean[] EMPTY = new boolean[0];
    private static boolean DEFAULT_VALUE = false;

}
