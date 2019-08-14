package us.hebi.robobuf;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedMessage<T extends MessageNano<T>> extends RepeatedObject<RepeatedMessage<T>, T, T> {

    public RepeatedMessage(MessageFactory<T> factory) {
        if (factory == null) throw new NullPointerException();
        this.factory = factory;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final void copyDataFrom0(RepeatedMessage<T> other) {
        for (int i = 0; i < other.length; i++) {
            array[i].copyFrom(other.array[i]);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final void setIndex0(int index, T value) {
        array[index].copyFrom(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final T[] allocateArray0(int desiredSize) {
        return (T[]) new MessageNano[desiredSize];
    }

    @Override
    protected final void clearIndex0(int index) {
        array[index].clear();
    }

    @Override
    protected final boolean isEqual(T a, Object b) {
        return a.equals(b);
    }

    @Override
    protected T createEmpty() {
        return factory.create();
    }

    final MessageFactory<T> factory;

}
