package us.hebi.robobuf;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedMessage<T extends MessageNano> extends RepeatedField<RepeatedMessage<T>> {

    public void set(int index, T message) {

    }

    public void add(T message) {

    }

    public void addAll(T... messages) {

    }

    public T get(int index) {
        return null;
    }

    public T getAndAdd() {
        return null;
    }

    @Override
    public void copyFrom(RepeatedMessage<T> other) {

    }

    @Override
    public int capacity() {
        return 0;
    }

    @Override
    protected void growCapacity(int desiredSize) {

    }

    @Override
    protected void clearRange(int fromIndex, int toIndex) {

    }
}
