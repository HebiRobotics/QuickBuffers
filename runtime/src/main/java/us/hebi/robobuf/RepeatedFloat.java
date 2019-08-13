package us.hebi.robobuf;

import java.util.Arrays;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedFloat extends RepeatedField<RepeatedFloat> {

    @Override
    protected void growCapacity(int desiredSize) {
        final float[] newValues = new float[desiredSize];
        System.arraycopy(array, 0, newValues, 0, length);
        this.array = newValues;
    }

    @Override
    public void clearRange(int fromIndex, int toIndex) {
        Arrays.fill(array, fromIndex, toIndex, DEFAULT_VALUE);
    }

    public float get(int index) {
        return array[index];
    }

    public void set(int index, float value) {
        if (index >= length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        array[index] = value;
    }

    public void add(float value) {
        ensureSpace(1);
        array[length++] = value;
    }

    @Override
    public void copyFrom(RepeatedFloat other) {
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

    public float[] toArray() {
        if (length == 0) return EMPTY;
        return Arrays.copyOf(array, length);
    }

    float[] array = EMPTY;
    private static float[] EMPTY = new float[0];
    private static float DEFAULT_VALUE = 0;

}
