package us.hebi.robobuf;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedString extends RepeatedObject<RepeatedString, StringBuilder, CharSequence> {

    @Override
    protected void setIndex0(int index, CharSequence value) {
        array[index].setLength(0);
        array[index].append(value);
    }

    @Override
    protected void clearIndex0(int index) {
        array[index].setLength(0);
    }

    @Override
    protected boolean isEqual(StringBuilder a, Object b) {
        return (b instanceof CharSequence) && InternalUtil.equals(a, (CharSequence) b);
    }

    @Override
    protected StringBuilder[] allocateArray0(int desiredSize) {
        return new StringBuilder[desiredSize];
    }

    @Override
    protected StringBuilder createEmpty() {
        return new StringBuilder(0);
    }

    @Override
    protected void copyDataFrom0(RepeatedString other) {
        for (int i = 0; i < length; i++) {
            setIndex0(i, other.array[i]);
        }
    }

}
