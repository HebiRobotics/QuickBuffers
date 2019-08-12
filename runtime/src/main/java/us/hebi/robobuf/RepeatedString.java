package us.hebi.robobuf;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedString implements RepeatedField {

    public void copyFrom(RepeatedString other) {
    }

    public String get(int index) { // TODO: make char sequence
        return "";
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
}
