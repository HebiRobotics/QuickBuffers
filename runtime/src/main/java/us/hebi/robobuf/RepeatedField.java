package us.hebi.robobuf;

/**
 * @author Florian Enner
 * @since 10 Aug 2019
 */
abstract class RepeatedField<T extends RepeatedField> {

    public final int length() {
        return length;
    }

    public final int remainingCapacity() {
        return capacity() - length;
    }

    public final void ensureSpace(int numEntries) {
        final int desiredSize = length + numEntries;
        if (desiredSize > capacity()) {
            growCapacity(desiredSize);
        }
    }

    public final void clear() {
        clearRange(0, length);
        length = 0;
    }

    public abstract void copyFrom(T other);

    public abstract int capacity();

    protected abstract void growCapacity(int desiredSize);

    protected abstract void clearRange(int fromIndex, int toIndex);

    protected int length = 0;

}
