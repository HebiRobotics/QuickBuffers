package us.hebi.robobuf;

/**
 * @author Florian Enner
 * @since 12 Aug 2019
 */
public class ByteStore implements RepeatedField {

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

    public void copyFrom(ByteStore other) {

    }

    byte[] array;
    int length;

}
