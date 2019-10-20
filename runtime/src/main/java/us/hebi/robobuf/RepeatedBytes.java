package us.hebi.robobuf;

/**
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class RepeatedBytes extends RepeatedObject<RepeatedBytes, RepeatedByte, byte[]> {

    public static RepeatedBytes newEmptyInstance(){
        return new RepeatedBytes();
    }

    private RepeatedBytes(){
    }

    @Override
    protected void copyDataFrom0(RepeatedBytes other) {
        for (int i = 0; i < other.length; i++) {
            array[i].copyFrom(other.array[i]);
        }
    }

    @Override
    protected void clearIndex0(int index) {
        array[index].clear();
    }

    @Override
    protected void setIndex0(int index, byte[] value) {
        array[index].copyFrom(value);
    }

    @Override
    protected boolean isEqual(RepeatedByte a, Object b) {
        return a.equals(b);
    }

    @Override
    protected RepeatedByte createEmpty() {
        return new RepeatedByte();
    }

    @Override
    protected RepeatedByte[] allocateArray0(int desiredSize) {
        return new RepeatedByte[desiredSize];
    }

}
