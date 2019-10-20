package us.hebi.robobuf;

import java.util.Arrays;

/**
 * Class that represents the data for a repeated float field.
 *
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public final class RepeatedFloat extends RepeatedField<RepeatedFloat> {

    public static RepeatedFloat newEmptyInstance() {
        return new RepeatedFloat();
    }

    RepeatedFloat() {
    }

    @Override
    protected void extendCapacityTo(int desiredSize) {
        final float[] newValues = new float[desiredSize];
        System.arraycopy(array, 0, newValues, 0, length);
        this.array = newValues;
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

    public RepeatedFloat add(float value) {
        reserve(1);
        array[length++] = value;
        return this;
    }

    public RepeatedFloat addAll(float[] values) {
        return addAll(values, 0, values.length);
    }

    public RepeatedFloat addAll(float[] buffer, int offset, int length) {
        reserve(length);
        System.arraycopy(buffer, offset, array, this.length, length);
        this.length += length;
        return this;
    }

    public RepeatedFloat copyFrom(float[] buffer) {
        return copyFrom(buffer, 0, buffer.length);
    }

    public RepeatedFloat copyFrom(float[] buffer, int offset, int length) {
        this.length = 0;
        addAll(buffer, offset, length);
        return this;
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
    public final RepeatedFloat setLength(int length) {
        if (array.length < length) {
            extendCapacityTo(length);
        }
        this.length = length;
        return this;
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
            if (!ProtoUtil.isEqual(array[i], other.array[i]))
                return false;
        }
        return true;
    }

    float[] array = EMPTY_ARRAY;
    private static final float[] EMPTY_ARRAY = new float[0];

}
