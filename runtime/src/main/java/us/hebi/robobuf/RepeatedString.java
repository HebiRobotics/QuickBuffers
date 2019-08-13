package us.hebi.robobuf;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedString extends RepeatedField<RepeatedString> {

    public void copyFrom(RepeatedString other) {
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

    public String get(int index) { // TODO: make char sequence
        return "";
    }


    public void add(CharSequence value) {

    }

    public void set(int index, CharSequence value) {

    }

    public StringBuilder getAndAdd() {
        return null;
    }
}
