package us.hebi.robobuf;

import java.util.Arrays;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedDouble extends RepeatedField<RepeatedDouble> {

    @Override
    protected void growCapacity(int desiredSize) {
        final double[] newValues = new double[desiredSize];
        System.arraycopy(array, 0, newValues, 0, length);
        this.array = newValues;
    }

    @Override
    public void clearRange(int fromIndex, int toIndex) {
        Arrays.fill(array, fromIndex, toIndex, DEFAULT_VALUE);
    }

    public double get(int index) {
        return array[index];
    }

    public void set(int index, double value) {
        if (index >= length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        array[index] = value;
    }

    public void add(double value) {
        ensureSpace(1);
        array[length++] = value;
    }

    @Override
    public void copyFrom(RepeatedDouble other) {
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

    public double[] toArray() {
        if (length == 0) return EMPTY;
        return Arrays.copyOf(array, length);
    }

    double[] array = EMPTY;
    private static double[] EMPTY = new double[0];
    private static double DEFAULT_VALUE = 0;

}
