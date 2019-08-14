package us.hebi.robobuf;

import java.lang.reflect.Constructor;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedMessage<T extends MessageNano> extends RepeatedObject<RepeatedMessage<T>, T, T> {

    public RepeatedMessage(Class<T> clazz) {
        try {
            // Due to type erasure we need to allocate the instance via reflection. Alternatively,
            // we could also use anonymous classes (although I wasn't able to figure out how to
            // generate the appropriate code), or create a default instance that we can clone.
            this.constructor = clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Can't find constructor for " + clazz);
        }
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
        try {
            return constructor.newInstance(initArgs);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to construct repeated instance. Error: " + e.getMessage());
        }
    }

    final Constructor<T> constructor;
    private static final Object[] initArgs = new Object[0];

}
