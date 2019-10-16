package us.hebi.robobuf;

/**
 * @author Florian Enner
 * @since 10 Aug 2019
 */
abstract class RepeatedField<RepeatedType extends RepeatedField> {

    public final int length() {
        return length;
    }

    public final int remainingCapacity() {
        return capacity() - length;
    }

    /**
     * Makes sure that the internal storage capacity has at least
     * space for the requested number of entries. This method will
     * increase the internal capacity if needed.
     *
     * @param count number of entries to be added
     */
    @SuppressWarnings("unchecked")
    public final RepeatedType reserve(int count) {
        final int desiredSize = length + count;
        if (desiredSize > capacity()) {
            extendCapacityTo(desiredSize);
        }
        return (RepeatedType) this;
    }

    public final void clear() {
        clearRange(0, length);
        length = 0;
    }

    protected final void checkIndex(int index) {
        if (index < 0 || index >= length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public abstract void copyFrom(RepeatedType other);

    public abstract int capacity();

    protected abstract void extendCapacityTo(int desiredSize);

    protected abstract void clearRange(int fromIndex, int toIndex);

    protected int length = 0;

}
