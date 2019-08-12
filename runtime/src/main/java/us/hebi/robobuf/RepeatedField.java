package us.hebi.robobuf;

/**
 * @author Florian Enner
 * @since 10 Aug 2019
 */
public interface RepeatedField {

    default void ensureSpace(int length){
    }

    int length();

    int remainingCapacity();

    void clear();

}
