package us.hebi.robobuf;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedBytes implements RepeatedField {

    public void copyFrom(RepeatedBytes other) {
    }

    public ByteStore get(int index) {
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

    public ByteStore getAndAdd() {
        return null;
    }

    public void add(byte[] buffer, int offset, int length) {

    }

}
