package us.hebi.robobuf;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedMessage<T extends MessageNano> implements RepeatedField {

    public void copyFrom(RepeatedMessage<T> other) {
    }

    public void set(int index, T message) {

    }

    public void add(T message) {

    }

    public T get(int index) {
        return null;
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public int remainingCapacity() {
        return 0;
    }

    @Override
    public void clear() {

    }

    public T getAndAdd() {
        return null;
    }
}
