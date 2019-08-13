package us.hebi.robobuf;

import java.util.Arrays;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class ByteStore extends RepeatedField<ByteStore> {

    @Override
    protected void growCapacity(int desiredSize) {
        final byte[] newValues = new byte[desiredSize];
        System.arraycopy(array, 0, newValues, 0, length);
        this.array = newValues;
    }

    @Override
    public void clearRange(int fromIndex, int toIndex) {
        Arrays.fill(array, fromIndex, toIndex, DEFAULT_VALUE);
    }

    public byte get(int index) {
        checkIndex(index);
        return array[index];
    }

    public void set(int index, byte value) {
        checkIndex(index);
        array[index] = value;
    }

    public void add(byte value) {
        ensureSpace(1);
        array[length++] = value;
    }

    public void copyFrom(byte[] buffer) {
        copyFrom(buffer, 0, buffer.length);
    }

    public void copyFrom(byte[] buffer, int offset, int length) {
        if (capacity() < length) {
            growCapacity(length);
        }
        System.arraycopy(buffer, offset, array, 0, length);
        this.length = length;
    }

    @Override
    public void copyFrom(ByteStore other) {
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

    public byte[] toArray() {
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
        ByteStore other = (ByteStore) o;

        if (length != other.length)
            return false;

        for (int i = 0; i < length; i++) {
            if (array[i] != other.array[i])
                return false;
        }
        return true;
    }

    byte[] array = EMPTY;
    private static final byte[] EMPTY = new byte[0];
    private static final byte DEFAULT_VALUE = 0;

}
