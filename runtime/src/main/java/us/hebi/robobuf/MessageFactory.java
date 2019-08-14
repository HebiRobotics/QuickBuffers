package us.hebi.robobuf;

/**
 * Factory interface for creating messages
 *
 * @author Florian Enner
 * @since 15 Aug 2019
 */
public interface MessageFactory<T extends MessageNano<T>> {

    T create();

}
