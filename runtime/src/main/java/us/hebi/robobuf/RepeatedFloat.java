package us.hebi.robobuf;

import java.util.Arrays;

/**
 * Class that represents the data for a repeated float field.
 *
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
    protected void clearRange(int fromIndex, int toIndex) {
        Arrays.fill(array, fromIndex, toIndex, DEFAULT_VALUE);
    }

    public float get(int index) {
        checkIndex(index);
        return array[index];
    }

    public void set(int index, float value) {
        checkIndex(index);
        array[index] = value;
    }

    public void add(float value) {
        requestSize(1);
        array[length++] = value;
    }

    public void addAll(float[] values) {
        addAll(values, 0, values.length);
    }

    public void addAll(float[] buffer, int offset, int length) {
        requestSize(length);
        System.arraycopy(buffer, offset, array, this.length, length);
        this.length += length;
    }

    public void copyFrom(float[] buffer) {
        copyFrom(buffer, 0, buffer.length);
    }

    public void copyFrom(float[] buffer, int offset, int length) {
        this.length = 0;
        addAll(buffer, offset, length);
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
            if (!RoboUtil.equals(array[i], other.array[i]))
                return false;
        }
        return true;
    }

    float[] array = EMPTY;
    private static final float[] EMPTY = new float[0];
    private static final float DEFAULT_VALUE = RoboUtil._floatDefault;

}
